import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditLog extends Plugin {


    protected static final Logger log = Logger.getLogger("Minecraft");
    
    static final int chunkSize = 16;
    
    static String version = "v0.08";
    
    private static class EditDetail {
    	public int blockType;
    	public int added = 0;
    	public int removed = 0;
    	
    	public EditDetail(int type) {
			blockType = type;
		}
    }
    
    private static class EditKey {
    	public int user;
    	public int chunkX;
    	public int chunkZ;
    	
    	@Override
    	public int hashCode() {
    		return user + chunkX*31 + chunkZ*511;
    	}

		public EditKey(int user, int chunkX, int chunkZ) {
			this.user = user;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}
		
		@Override
		public boolean equals(Object other) {
			EditKey e = (EditKey) other;
			if (null == e) return false;
			return e.user == user && e.chunkX == chunkX && e.chunkZ == chunkZ;
		}
    }
    
    private static class PendingEdit extends EditKey {
    	public HashMap<Integer, EditDetail> detail = new HashMap<Integer, EditDetail>();
    	public int startTime;
    	public int lastActivity;
    	public int expiresAt;
    	
    	public PendingEdit(int user, int chunkX, int chunkZ) {
    		super(user, chunkX, chunkZ);
    		startTime = (int) (System.currentTimeMillis() / 1000L);
    	}
    	
    	synchronized public void CountBlock(int type, boolean removed) {
    		EditDetail d = detail.get(type);
    		if (d == null) {
    			d = new EditDetail(type);
    			detail.put(type, d);
    		}
    		if (removed)
    			d.removed++;
    		else
    			d.added++;

    		lastActivity = (int) (System.currentTimeMillis() / 1000L);
    	}
    }
    
    private static class ExpiringComparator implements Comparator<PendingEdit> {
		public int compare(PendingEdit arg0, PendingEdit arg1) {
			if (arg0.equals(arg1)) return 0;
			
			if (arg0.expiresAt != arg1.expiresAt)
				return arg0.expiresAt - arg1.expiresAt;
			if (arg0.chunkX != arg1.chunkX)
				return arg0.chunkX - arg1.chunkX;
			if (arg0.chunkZ != arg1.chunkZ)
				return arg0.chunkZ - arg1.chunkZ;
			if (arg0.user != arg1.user)
				return arg0.user - arg1.user;
			return arg0.hashCode() - arg1.hashCode();
		}
    };

	private static String join(Collection<String> l, String sep) {
		if (l.isEmpty()) return "";
		if (l.size() == 1) return l.iterator().next();
		
		StringBuffer sb = new StringBuffer();
		
		for(String s : l) {
			sb.append(s).append(sep);
		}
		sb.setLength(sb.length()-sep.length());
		
		return sb.toString();
	}
	
    HashMap<EditKey, PendingEdit> pendingEdits = new HashMap<EditKey, PendingEdit>();
    TreeSet<PendingEdit> expiringEdits = new TreeSet<PendingEdit>(new ExpiringComparator());
    
    LinkedHashMap<String, Integer> userMap = new LinkedHashMap<String, Integer>(16, 0.75f, true);
    
	private int minEditWin, maxEditWin, editLogLength, maxResultRows;

	
	private ArrayList<PluginRegisteredListener> listeners = new ArrayList<PluginRegisteredListener>();
	
	private HashMap<String, Block> playerDigging = new HashMap<String, Block>();
	

	private CommitThread commitThread;

	private Map<String, String> pagingHeaders = new HashMap<String, String>();
	private Map<String, List<String>> pagingBuffers = new HashMap<String, List<String>>();
	
	private Driver dbDriver;
	private String dbURL;
	private Properties dbProperties;
	private Queue<DBConnection> connectionPool = new LinkedList<DBConnection>();
	private int numConnections;
	
	class DBConnection {
		private Connection con;

		public PreparedStatement getPlayerIDStmt, createNewPlayerStmt;
		public PreparedStatement saveEditStmt, saveEditDetailStmt;
		public PreparedStatement editsToPruneStmt, pruneEditDetailsStmt, pruneEditsStmt;

		public PreparedStatement chunkEditsStmt;
		public PreparedStatement chunkBlockEditsStmt;
		public PreparedStatement chunkPlayerEditsStmt;
		public PreparedStatement editDetailsStmt;
		public PreparedStatement playerEditsStmt;
		public PreparedStatement editChunkStmt;
		
		public DBConnection() throws SQLException {
			this(false);
		}
		
		public DBConnection(boolean installSchema) throws SQLException {
			try {
				if (null != dbDriver)
					con = dbDriver.connect(dbURL, dbProperties);
				else
					con = DriverManager.getConnection(dbURL, dbProperties);
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "Could not connect to database", ex);
				throw ex;
			}
			
			if (installSchema) try {
				String dbtype = dbURL.split(":")[1];
				if (!checkSchema(dbtype))
					createSchema(dbtype);
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "Could not create schema.", ex);
				throw ex;
			}
			
			try {	
				prepareStatements();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "Could not prepare statements.", ex);
				throw ex;
			}
		}
		
		public void begin() throws SQLException {
			con.setAutoCommit(false);
		}
		
		public void commit() throws SQLException {
			con.setAutoCommit(true);
		}

		public boolean isValid() {
			try {
				if (con == null || con.isClosed())
					return false;
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery("Select 42;");
				return rs.next();
			} catch (SQLException ex) {
				return true;
			}
		}
		
		public void close() throws SQLException {
			con.close();
			con = null;
		}

		private boolean checkSchema(String dbtype) {
			try {
				synchronized(con) {
					Statement stmt = con.createStatement();
					
					// check that the tables are there and have the columns we expect.
					stmt.executeQuery("SELECT playerid, name "+
							"FROM editlog_players LIMIT 1;").close();
					stmt.executeQuery("SELECT editid, chunkX, chunkY, startTime, endTime, playerid "+
							"FROM editlog_edits LIMIT 1;").close();
					stmt.executeQuery("SELECT editid, blocktype, added, removed "+
							"FROM editlog_detail LIMIT 1;").close();
				}
				return true;
			} catch (SQLException ex) {
				log.log(Level.INFO, "EditLog: Schema check failed because: ", ex);
				return false;
			}
		}

		private void createSchema(String dbtype) throws SQLException {
			
			String serialtype;
			
			if (dbtype.equals("mysql")) {
				serialtype = "INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT";
			} else if (dbtype.equals("sqlite")) {
				serialtype = "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT";
			} else if (dbtype.equals("postgresql")) {
				serialtype = "SERIAL NOT NULL PRIMARY KEY";
			} else {
				throw new UnsupportedOperationException("Don't know how to set up schema for this DB type");
			}
			
			log.log(Level.INFO, "EditLog: Setting up tables");
			
			Statement stmt = con.createStatement();
			
			try{
				stmt.execute("DROP TABLE editlog_players;");
			} catch (SQLException e) {}
			
			stmt.execute("CREATE TABLE editlog_players ("+
				"playerid    " + serialtype + ","+
				"name        VARCHAR(31) NOT NULL UNIQUE);");
		
			try{
				stmt.execute("DROP TABLE editlog_edits;");
			} catch (SQLException e) {}
			
			stmt.execute("CREATE TABLE editlog_edits ("+
				"editid      " + serialtype + ","+
				"chunkX      INTEGER NOT NULL,"+
				"chunkY      INTEGER NOT NULL,"+
				"startTime   INTEGER NOT NULL,"+
				"endTime     INTEGER NOT NULL,"+
				"playerid    INTEGER NOT NULL);");
			
			stmt.execute("CREATE INDEX editlog_edits_player ON editlog_edits(playerid, startTime);");
			stmt.execute("CREATE INDEX editlog_edits_chunk ON editlog_edits(chunkX, chunkY, startTime);");
			stmt.execute("CREATE INDEX editlog_edits_endTime ON editlog_edits(endTime);");
		
			try{
				stmt.execute("DROP TABLE editlog_detail;");
			} catch (SQLException e) {}
			
			stmt.execute("CREATE TABLE editlog_detail ("+
				"editid      INTEGER NOT NULL,"+
				"blocktype   INTEGER NOT NULL,"+
				"added       INTEGER NOT NULL,"+
				"removed     INTEGER NOT NULL,"+
				"PRIMARY KEY(editid, blocktype));");
		}

		private void prepareStatements() throws SQLException {
			getPlayerIDStmt = con.prepareStatement(
					"SELECT playerid FROM editlog_players "+
					"WHERE name = ?;");
			
			if (con.getClass().getCanonicalName().contains("sqlite")) {
				// SQLite driver doesn't support explicitly requesting generated keys, but produces them.
				createNewPlayerStmt = con.prepareStatement(
						"INSERT INTO editlog_players(name) "+
						"VALUES (?);");
				saveEditStmt = con.prepareStatement(
						"INSERT INTO editlog_edits(chunkX, chunkY, startTime, endTime, playerid) "+
						"VALUES (?, ?, ?, ?, ?);");
			} else {
				createNewPlayerStmt = con.prepareStatement(
						"INSERT INTO editlog_players(name) "+
						"VALUES (?);", Statement.RETURN_GENERATED_KEYS);
				saveEditStmt = con.prepareStatement(
						"INSERT INTO editlog_edits(chunkX, chunkY, startTime, endTime, playerid) "+
						"VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
			}
			
			
			saveEditDetailStmt = con.prepareStatement(
					"INSERT INTO editlog_detail(editid, blocktype, added, removed) "+
					"VALUES (?, ?, ?, ?);");
			
			editsToPruneStmt = con.prepareStatement(
					"SELECT editid FROM editlog_edits WHERE endTime < ?;");
			
			pruneEditDetailsStmt = con.prepareStatement(
					"DELETE FROM editlog_detail WHERE editid = ?;");
			
			pruneEditsStmt = con.prepareStatement(
					"DELETE FROM editlog_edits WHERE endTime < ?");
			
			chunkEditsStmt = con.prepareStatement(
					"SELECT a.editid, startTime, endTime, name, sum(added), sum(removed)  "+
					"FROM editlog_edits a "+
					"JOIN editlog_players b ON a.playerid = b.playerid "+
					"JOIN editlog_detail c ON a.editid = c.editid " +
					"WHERE chunkX = ? AND chunkY = ? "+
					"GROUP BY a.editid ORDER BY starttime DESC LIMIT ?;");
			chunkEditsStmt.setInt(3, maxResultRows);
			
			chunkBlockEditsStmt = con.prepareStatement(
					"SELECT a.editid, startTime, endTime, name, added, removed "+
					"FROM editlog_edits a "+
					"JOIN editlog_players b ON a.playerid = b.playerid "+
					"JOIN editlog_detail c ON a.editid = c.editid " +
					"WHERE chunkX = ? AND chunkY = ? AND blocktype = ? ORDER BY starttime DESC LIMIT ?;");
			chunkBlockEditsStmt.setInt(4, maxResultRows);
			
			chunkPlayerEditsStmt = con.prepareStatement(
					"SELECT a.editid, startTime, endTime, name, sum(added), sum(removed)  "+
					"FROM editlog_edits a "+
					"JOIN editlog_players b ON a.playerid = b.playerid "+
					"JOIN editlog_detail c ON a.editid = c.editid " +
					"WHERE chunkX = ? AND chunkY = ? AND name LIKE ?"+
					"GROUP BY a.editid ORDER BY starttime DESC LIMIT ?;");
			chunkPlayerEditsStmt.setInt(4, maxResultRows);
			
			editDetailsStmt = con.prepareStatement(
					"SELECT blocktype, added, removed FROM editlog_detail "+
					"WHERE editid = ? ORDER BY blocktype");
		
			editChunkStmt = con.prepareStatement("SELECT chunkX, chunkY FROM editlog_edits WHERE editid = ?");
			
			playerEditsStmt = con.prepareStatement(
					"SELECT a.editid, startTime, endTime, chunkX, chunkY, sum(added), sum(removed) " +
					"FROM editlog_edits a JOIN editlog_detail USING (editid) " +
					"WHERE playerid = ? GROUP BY editid ORDER BY starttime DESC LIMIT ?");
			playerEditsStmt.setInt(2, maxResultRows);
			
		}
	}
	
	// get the next connection from the pool in round-robin order
	private DBConnection getConnection() throws SQLException {
		DBConnection con;
		synchronized (connectionPool) {
			do {
				con = connectionPool.poll();
			} while(con != null && !con.isValid());
			
			if (con == null)
				con = new DBConnection();
			
			connectionPool.add(con);
		}
		return con;
	}
	
	private int GetPlayerID(String name)
	{
		Integer out = userMap.get(name);
		if (null != out) {
		//	log.log(Level.INFO, String.format("cached %s = %d", name, out));
			return out;
		}
		
		try {
			DBConnection con = getConnection();
			synchronized(con) {
				con.getPlayerIDStmt.setString(1, name);
				ResultSet rs = con.getPlayerIDStmt.executeQuery();
				if (rs.next())
				{
					int outi = rs.getInt(1);
		//			log.log(Level.INFO, String.format("got %s = %d", name, outi));
					userMap.put(name, outi);
					rs.close();
					return outi;
				}
				
				con.createNewPlayerStmt.setString(1, name);
				con.createNewPlayerStmt.executeUpdate();
				rs = con.createNewPlayerStmt.getGeneratedKeys();
				
				rs.next();
				int outi = rs.getInt(1);
		//		log.log(Level.INFO, String.format("set %s = %d", name, outi));
				userMap.put(name, outi);
				rs.close();
				return outi;
			}
		} catch (SQLException ex) {
            log.log(Level.SEVERE, "EditLog: Unable to look up/allocate player ID", ex);
		}
		return -1;
	}
	
	private PendingEdit getPendingEdit(int player, int chunkX, int chunkZ) {
		EditKey ek = new EditKey(player, chunkX, chunkZ);
		synchronized(expiringEdits) {
			PendingEdit out = pendingEdits.get(ek);
			if (null == out) {
				out = new PendingEdit(player, chunkX, chunkZ);
				pendingEdits.put(out, out);
				out.expiresAt = out.lastActivity+minEditWin;
				expiringEdits.add(out);
			}
			return out;
		}
	}
	
	private void commitEdit(PendingEdit pe) throws SQLException {
		DBConnection con = getConnection();
		synchronized(con) {
			con.begin();
			
			PreparedStatement stmt = con.saveEditStmt;
			stmt.setInt(1, pe.chunkX);
			stmt.setInt(2, pe.chunkZ);
			stmt.setInt(3, pe.startTime);
			stmt.setInt(4, pe.lastActivity);
			stmt.setInt(5, pe.user);
			
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			rs.next();
			int editid = rs.getInt(1);
			rs.close();
			
			stmt = con.saveEditDetailStmt;
			stmt.setInt(1, editid);
			
			for(EditDetail ed : pe.detail.values()) {
				stmt.setInt(2, ed.blockType);
				stmt.setInt(3, ed.added);
				stmt.setInt(4, ed.removed);
				stmt.addBatch();
			}
			stmt.executeBatch();
			
			con.commit();
		}
	}

	private int chunkCoord(int x) {
		if(x >= 0) {
			return x/chunkSize;
		} else {
			x--;
			return x/chunkSize - 1;
		}
	}

	private void pagedOutput(Player player, String header, List<String> body) {
		pagingHeaders.put(player.getName(), header);
		pagingBuffers.put(player.getName(), body);
		showPage(player);
	}

	private boolean showPage(Player player) {
		if (!pagingBuffers.containsKey(player.getName()))
			return false;
		
		int lines = 8;
		
		if (pagingHeaders.containsKey(player.getName()))
		{
			lines--;
			player.sendMessage(pagingHeaders.get(player.getName()));
		}
		
		List<String> buffer = pagingBuffers.get(player.getName());
		
		while (lines > 0 && !buffer.isEmpty()) {
			String line = buffer.remove(0);
			player.sendMessage(line);
			lines--;
		}
		
		if (buffer.isEmpty()) {
			pagingBuffers.remove(player.getName());
			pagingHeaders.remove(player.getName());
			player.sendMessage(Colors.LightGray+"end of output");
		} else {
			player.sendMessage(Colors.LightGray+String.format("%d lines remain.  /more to continue.", buffer.size()));
		}
		return true;
	}

	private class CommitThread extends Thread {
		boolean stopped = false;
		
		public CommitThread() {
			super("EditLog Commit/Prune Thread");
		}
		
		public void shutdown() {
			stopped = true;
			if (isAlive())
				interrupt();
		}
		
		public void run() {
			while(true) {
				try {
					Thread.sleep(minEditWin/4*1000);
				} catch (InterruptedException e) {
				}
				if (stopped)
					return;
				
				synchronized (connectionPool) {
					while (connectionPool.size() < numConnections) {
						try {
							connectionPool.add(new DBConnection());
						} catch (SQLException ex) {
							log.log(Level.SEVERE, "Error refilling connection pool ", ex);
							return;
						}
					}
				}
				int now = (int) (System.currentTimeMillis() / 1000L);
				
				int numsaved = 0;
				
				try {
					while(true) {
						synchronized(expiringEdits) {
							if(expiringEdits.isEmpty() || expiringEdits.first().expiresAt > now-5) {
								break;
							}
							PendingEdit pe = expiringEdits.first();
							expiringEdits.remove(pe);
							// make sure expiration time is current...
							pe.expiresAt = Math.min(pe.lastActivity + minEditWin, pe.startTime + maxEditWin);
							
							if (pe.expiresAt <= now) {
								pendingEdits.remove(pe);
								commitEdit(pe);
								numsaved++;
							} else {
								// not ready to expire yet, put back into the list
								expiringEdits.add(pe);
							}
						}
					}
				} catch (SQLException ex) {
		            log.log(Level.SEVERE, "EditLog: Error saving log entries", ex);
		            return;
				}
				
				if (numsaved > 0)
					log.log(Level.INFO, String.format("EditLog: wrote %d entries to log", numsaved));
				
				
				
				int[] toPrune = new int[1024];
				int numToPrune = 0;
				
				int prunetime = now - editLogLength;
				
				try {
					DBConnection con = getConnection();
					synchronized (con) {
						con.editsToPruneStmt.setInt(1, prunetime);
						ResultSet rs = con.editsToPruneStmt.executeQuery();
						
						while(rs.next()) {
							if (numToPrune == toPrune.length) {
								int[] newarr = new int[toPrune.length * 2];
								System.arraycopy(toPrune, 0, newarr, 0, toPrune.length);
							}
							toPrune[numToPrune] = rs.getInt(1);
							numToPrune++;
						}
						
						
						if (numToPrune > 0) {
							log.log(Level.INFO, String.format("Pruning %d entries from edit log", numToPrune));
							
							rs.close();
							con.pruneEditsStmt.setInt(1, prunetime);
							con.pruneEditsStmt.execute();
						}
					}
					
					// group the deletes into batches of 100 that get written
					// to disk together.  This reduces overhead while avoiding
					// a huge lag spike if thousands of edits need to be pruned
					// at once (e.g. if the plugin was loaded after a long time
					// of not being loaded)
					for(int chunk=0 ; chunk<numToPrune ; chunk+=100) {
						synchronized(con) {
							PreparedStatement stmt = con.pruneEditDetailsStmt;
							for(int i=chunk ; i<numToPrune && i<chunk+100 ; i++) {
								stmt.setInt(1, toPrune[i]);
								stmt.addBatch();
							}
							stmt.executeBatch();
						}
					}
					
					
				} catch (SQLException ex) {
		            log.log(Level.SEVERE, "EditLog: Error pruning old log entries", ex);
		            return;
				}
			}
		}
	}
	
	
	
	private class Listener extends PluginListener {
		@Override
		public boolean onBlockCreate(Player player, Block blockPlaced,
				Block blockClicked, int itemInHand) {
			
			if (!checkHealth()) return false;
			
			
			int type = blockPlaced.getType();
			
			if (type > 255 || type < 0)
			{
				switch(type) {
				case 259:
				case 321:
				case 323:
				case 324:
				case 325:
				case 326:
				case 327:
				case 330:
					break;
				default:
					return false;
				}
			}
			

			switch(blockClicked.getType()) {
			case 54:
			case 61:
			case 62:
				return false; // blocks don't place on these types
			}
			
			// TODO: have this put into a queue, and a bit later actually check if the block placed
			
			
			PendingEdit pe = getPendingEdit(
					GetPlayerID(player.getName()),
					blockPlaced.getX()/chunkSize,
					blockPlaced.getZ()/chunkSize);
			
			pe.CountBlock(type, false);
			
			return false;
		}
		
		@Override
		public boolean onBlockDestroy(Player player, Block block) {

			if (!checkHealth()) return false;
			
			if (block.getStatus() == 0 || block.getStatus() == 1) {
				// started digging, digging
				if (block.getType() != 0)
					playerDigging.put(player.getName(), block);
			} else if (block.getStatus() == 3) {
				// broke block
				Block oldBlock = playerDigging.get(player.getName());
				if (null == oldBlock ||
					oldBlock.getX() != block.getX() ||
					oldBlock.getY() != block.getY() ||
					oldBlock.getZ() != block.getZ())
				{
					log.log(Level.WARNING, String.format("'%s' destroyed a block they hadn't been digging", 
							player.getName()));
					oldBlock = block;
				}

				PendingEdit pe = getPendingEdit(
						GetPlayerID(player.getName()),
						chunkCoord(oldBlock.getX()),
						chunkCoord(oldBlock.getZ()));
				
				pe.CountBlock(oldBlock.getType(), true);
				
				playerDigging.remove(player.getName());
			} else if (block.getStatus() == 2) {
				// stopped digging
				playerDigging.remove(player.getName());
			}
			
			return false;
		}
		
		@Override
		public boolean onCommand(Player player, String[] split) {

			if (!checkHealth()) return false;
			
			if (!player.canUseCommand("/editlog"))
				return false;
			
			String cmd = split[0].toLowerCase();
			
			if (cmd.length()>=2 && "/more".startsWith(cmd))
				return showPage(player);
			
			if (cmd.equals("/help")) {
				String topic = split[1].toLowerCase();
				if (!(topic.equals("editlog") || topic.equals("/editlog")))
					return false;
				
				player.sendMessage(Colors.Blue + 
					"EditLog "+version+" by inio");
				player.sendMessage(Colors.Blue + 
						"Log review commands (Page 1 of 1) [] = required <> = optional:");
				player.sendMessage(Colors.Rose +
					"/editlog <player pattern> - Show log for current chunk.");
				player.sendMessage(Colors.Rose +
					"/blockeditlog [blocktype] - Current chunk edit for blocktype.");
				player.sendMessage(Colors.Rose +
					"/editdetail [edit id] - Block breakdown for an edit.");
				player.sendMessage(Colors.Rose +
					"/playeredits [player] - Recent edits by player.");
				player.sendMessage(Colors.Rose +
					"/tpedit [edit id] - Warp to an edit's chunk.");
				player.sendMessage(Colors.Blue +
					"(Start and End times are shown as hh:mm ago)");
				
				return true;
			}
			
			try {
				if (cmd.equals("/editlog")) {	
					if (split.length == 1) {
						showChunkEditLog(player, -1, null);
					} else if (split.length == 2) {
						showChunkEditLog(player, -1, split[1]);
					} else {
						player.sendMessage(Colors.Rose + "Usage: /editlog OR /editlog <player>");
						player.sendMessage(Colors.Rose + "Shows edits for current chunk (% is wildcard for player).");
					}
					return true;
				}
				

				if (cmd.equals("/blockeditlog")) {	
					if (split.length == 2) {
						int blockType;
						
						try {
							blockType = Integer.parseInt( split[1] );
						} catch (NumberFormatException n) {
							blockType = etc.getDataSource().getItem( split[1] );
						}
						
						showChunkEditLog(player, blockType, null);
					} else {
						player.sendMessage(Colors.Rose + "Usage: /blockeditlog [blocktype]");
						player.sendMessage(Colors.Rose + "Shows edits for current chunk of a given block type");
					}
					return true;
				}
				
				if (cmd.equals("/editdetail")) {
					if (split.length == 2) {
						try {
							int edit = Integer.parseInt(split[1], 16);
							showEditDetails(player, edit);
						} catch(NumberFormatException ex) {
							player.sendMessage(Colors.Rose + "Invalid edit ID");
						}
					} else {
						player.sendMessage(Colors.Rose + "Usage: /editdetail [editid]");
						player.sendMessage(Colors.Rose + "Block breakdown for an editid (1st column of other cmds).");
					}
					return true;
				}
				
				if (cmd.equals("/playeredits")) {
					if (split.length == 2) {
						showPlayerEdits(player, split[1]);
					} else {
						player.sendMessage(Colors.Rose + "Usage: /playeredits [player]");
						player.sendMessage(Colors.Rose + "Recent edits by that player.");
					}
					return true;
				}
				
				if (cmd.equals("/tpedit")) {
					if (split.length == 2) {
						try {
							int edit = Integer.parseInt(split[1], 16);
							warpToEdit(player, edit);
						} catch(NumberFormatException ex) {
							player.sendMessage(Colors.Rose + "Invalid edit ID");
						}
					} else {
						player.sendMessage(Colors.Rose + "Usage: /tpedit [editid]");
						player.sendMessage(Colors.Rose + "Warp to the chunk of a specific edit");
					}
					return true;
				}
				
				if (cmd.equals("/profileeditlog")) {
					Thread threads[] = {Thread.currentThread(), commitThread};
					new SimpleProfiler(threads, new File("editlog.profile")).start();
				}
			
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "Error doing log query", ex);
				player.sendMessage(Colors.Red + "Error doing query, check the log.");
				softDisable();
			}
			
			return false;
		}
		
		@Override
		public void onDisconnect(Player player) {

			if (!checkHealth()) return;
			
			if (playerDigging.containsKey(player.getName()))
				playerDigging.remove(player.getName());
		}
	};


	public void showChunkEditLog(Player player, int blockType, String playerName) throws SQLException {
		int x = chunkCoord((int)player.getX());
		int z = chunkCoord((int)player.getZ());
		commitPendingEditsForChunk(x, z);
		
		LinkedList<String> output = new LinkedList<String>();
		
		DBConnection con = getConnection();
		synchronized (con) {
			ResultSet rs;
			
			if (blockType >= 0) {
				con.chunkBlockEditsStmt.setInt(1, x);
				con.chunkBlockEditsStmt.setInt(2, z);
				con.chunkBlockEditsStmt.setInt(3, blockType);
				
				rs = con.chunkBlockEditsStmt.executeQuery();
				
				player.sendMessage(Colors.Gold + String.format("Block %d edits for chunk %d,%d", blockType, x, z));
			} else if (null != playerName) {
				String pattern;
				if (playerName.contains("%")) {
					pattern = playerName;
				} else {
					pattern = String.format("%%%s%%", playerName);
				}
				
				con.chunkPlayerEditsStmt.setInt(1, x);
				con.chunkPlayerEditsStmt.setInt(2, z);
				con.chunkPlayerEditsStmt.setString(3, pattern);
				
				rs = con.chunkPlayerEditsStmt.executeQuery();
				
				player.sendMessage(Colors.Gold + String.format("Player '%s' edits for chunk %d,%d", pattern, x, z));
			} else {
				con.chunkEditsStmt.setInt(1, x);
				con.chunkEditsStmt.setInt(2, z);
				
				rs = con.chunkEditsStmt.executeQuery();
				
				player.sendMessage(Colors.Gold + String.format("All edits for chunk %d,%d", x, z));
			}
			
			while(rs.next()) {
				
				int now = (int)(System.currentTimeMillis()/1000L);
				
				int editID = rs.getInt(1);
				int startTime = (now - rs.getInt(2))/60;
				int endTime = (now - rs.getInt(3))/60;
				String name = rs.getString(4);
				int added = rs.getInt(5);
				int removed = rs.getInt(6);
				
				int starth = startTime/60;
				int startm = startTime%60;
				int endh = endTime/60;
				int endm = endTime%60;
				
				
				output.add(Colors.Gold + String.format("%08X  %02d:%02d  %02d:%02d  %-16s  %5d    %5d",
						editID, starth, startm, endh, endm, name, added, removed));
			}
			
			rs.close();
		}
		
		pagedOutput(player,
				Colors.Gold + "EditID       Start   End   Player           Added  Removed",
				output);
	}

	public void showEditDetails(Player player, int editid) throws SQLException {
		LinkedList<String> output = new LinkedList<String>();
		
		DBConnection con = getConnection();
		synchronized (con) {
			con.editDetailsStmt.setInt(1, editid);
			ResultSet rs = con.editDetailsStmt.executeQuery();
			
			while(rs.next()) {
				int block = rs.getInt(1);
				int added = rs.getInt(2);
				int removed = rs.getInt(3);
				
				output.add(Colors.Gold + String.format("%3d     %5d    %5d",
						block, added, removed));
			}
			
			rs.close();
		}
		
		if (output.isEmpty()) {
			player.sendMessage(Colors.Rose + "Invalid edit ID");
			return;
		}
		
		pagedOutput(player,
				Colors.Gold + "Block  Added  Removed",
				output);
	}
	
	public void showPlayerEdits(Player player, String target) throws SQLException {
		LinkedList<String> output = new LinkedList<String>();

		DBConnection con = getConnection();
		synchronized (con) {
			ResultSet rs;
			con.getPlayerIDStmt.setString(1, target);
			rs = con.getPlayerIDStmt.executeQuery();
			
			if (!rs.next()) {
				player.sendMessage(Colors.Rose + String.format("Player '%s' has no edits on file", target));
				rs.close();
				return;
			}
			
			int targetid = rs.getInt(1);
			rs.close();
			
			commitPendingEditsForPlayer(targetid);
			
			con.playerEditsStmt.setInt(1, targetid);
			rs = con.playerEditsStmt.executeQuery();
			//"SELECT editid, startTime, endTime, chunkX, chunkY, sum(added), sum(removed) " +
			
			while(rs.next()) {
				
				int now = (int)(System.currentTimeMillis()/1000L);
				
				int editID = rs.getInt(1);
				int startTime = (now - rs.getInt(2))/60;
				int endTime = (now - rs.getInt(3))/60;
				int chunkx = rs.getInt(4);
				int chunkz = rs.getInt(5);
				int added = rs.getInt(6);
				int removed = rs.getInt(7);
				
				int starth = startTime/60;
				int startm = startTime%60;
				int endh = endTime/60;
				int endm = endTime%60;

				output.add(Colors.Gold + String.format("%08X  %02d:%02d  %02d:%02d  %+05d %+05d  %5d   %5d",
						editID, starth, startm, endh, endm, chunkx, chunkz, added, removed));
			}
			
			rs.close();
		}
		
		pagedOutput(player,
				Colors.Gold + "EditID       Start   End   ChnkX ChnkZ  Added  Removed",
				output);
	}
	
	public void warpToEdit(Player player, int editid) throws SQLException {
		DBConnection con = getConnection();
		synchronized (con) {
			con.editChunkStmt.setInt(1, editid);
			ResultSet rs = con.editChunkStmt.executeQuery();
			
			if (!rs.next()) {
				player.sendMessage(Colors.Rose + "Invalid edit ID");
				rs.close();
				return;
			}
			
			int cx = rs.getInt(1);
			int cz = rs.getInt(2);

			int x = cx*chunkSize + 8;			
			int z = cz*chunkSize + 8;
			
			int y = etc.getServer().getHighestBlockY(x, z);
			
			player.teleportTo(x, y, z, player.getRotation(), player.getPitch());
		}
	}

	public void commitPendingEditsForChunk(int x, int z) throws SQLException {
		synchronized (expiringEdits) {
			ArrayList<EditKey> toCommit = new ArrayList<EditKey>();
			for(EditKey ek : pendingEdits.keySet()) {
				if (ek.chunkX == x && ek.chunkZ == z) {
					toCommit.add(ek);
				}
			}
			for(EditKey ek : toCommit) {
				PendingEdit pe = pendingEdits.remove(ek);
				expiringEdits.remove(pe);
				commitEdit(pe);
			}
		}
	}

	public void commitPendingEditsForPlayer(int p) throws SQLException {
		synchronized (expiringEdits) {
			ArrayList<EditKey> toCommit = new ArrayList<EditKey>();
			for(EditKey ek : pendingEdits.keySet()) {
				if (ek.user == p) {
					toCommit.add(ek);
				}
			}
			for(EditKey ek : toCommit) {
				PendingEdit pe = pendingEdits.remove(ek);
				expiringEdits.remove(pe);
				commitEdit(pe);
			}
		}
	}
	
	private boolean checkHealth() {
		if (null != commitThread && commitThread.isAlive())
			return true;
		
		softDisable();
		return false;
	}

	public void softDisable() {
		etc.getInstance().removeCommand("/editlog");
		
		commitThread.shutdown();
		
		try {
			synchronized (expiringEdits) {
				
				for(PendingEdit pe : expiringEdits) {
					commitEdit(pe);
				}
				
				expiringEdits.clear();
				pendingEdits.clear();
			}
		} catch (SQLException ex) {
            log.log(Level.SEVERE, "EditLog: Unable to submit pending edits while shutting down:", ex);
		}
		
		synchronized (connectionPool) {
			for(DBConnection con : connectionPool) {
				if (null != con) {
					try {
						con.close();
					} catch (SQLException ex) {
			            log.log(Level.SEVERE, "EditLog: Unable to close DB connection", ex);
					}
					con = null;
				}
			}
			connectionPool.clear();
		}
        log.log(Level.INFO, "EditLog "+version+" disabled.");
	}
	
	@Override
	public void disable() {
		softDisable();
		
		PluginLoader loader = etc.getLoader();
		for (PluginRegisteredListener rl : listeners)
			loader.removeListener(rl);
		listeners.clear();
	}

	@Override
	public void enable() {
        PropertiesFile properties = new PropertiesFile("editlog.properties");
        minEditWin = properties.getInt("minEditWindow", 60*5);
        maxEditWin = properties.getInt("maxEditWin", 60*20);
        editLogLength = properties.getInt("editLogLength", 60*60*48);
        maxResultRows = properties.getInt("maxResultRows", 100);
        String dbpropfile = properties.getString("dbPropertiesFile", "mysql.properties");
        properties.save();
        
        PropertiesFile dbprop = new PropertiesFile(dbpropfile);
        
        Map<String,String> defaultConProps = new HashMap<String, String>();
        
        String driver, driverjar;
        if (dbpropfile.contains("mysql")) {
	        driver = dbprop.getString("driver", "com.mysql.jdbc.Driver");
	        driverjar = dbprop.getString("driverjar", "");
	        dbURL = dbprop.getString("db", "jdbc:mysql://localhost:3306/minecraft");
	        numConnections = dbprop.getInt("numConnections", 2);
        } else if (dbpropfile.contains("sqlite")) {
	        driver = dbprop.getString("driver", "org.sqlite.JDBC");
	        driverjar = dbprop.getString("driverjar", "sqlitejdbc-v056.jar");
	        dbURL = dbprop.getString("db", "jdbc:sqlite:editlog.sqlite");
	        numConnections = dbprop.getInt("numConnections", 1);
        } else if (dbpropfile.contains("postgre")) {
	        driver = dbprop.getString("driver", "org.postgresql.Driver");
	        driverjar = dbprop.getString("driverjar", "postgresql-9.0-801.jdbc3.jar");
	        dbURL = dbprop.getString("db", "jdbc:postgresql:minecraft");
	        numConnections = dbprop.getInt("numConnections", 2);
        } else  {
	        driver = dbprop.getString("driver", "");
	        driverjar = dbprop.getString("driverjar", "");
	        dbURL = dbprop.getString("db", "jdbc:protocol:minecraft");
	        numConnections = dbprop.getInt("numConnections", 2);
        }
        
        String dbtype = dbURL.split(":")[1];
        
        if (dbtype.equals("mysql")) {
        	defaultConProps.put("user", "username");
        	String dummypass = "copy password here if setting up hey0 MySQL storage";
        	String defaultpass = dbprop.getString("pass", dummypass);
        	if (defaultpass.equals(dummypass))
        		defaultpass = "password";
        	defaultConProps.put("password", dbprop.getString("pass", defaultpass));
        	defaultConProps.put("autoReconnect", "true");
        } else if (dbtype.equals("postgresql")) {
        	defaultConProps.put("user", "username");
        	defaultConProps.put("password", "password");
        }
        
        
        String defaultPropNames = join(defaultConProps.keySet(), " ");

        String propNames[] = dbprop.getString("dbprops", defaultPropNames).split(" ");
        dbProperties = new Properties();
        for(String prop : propNames) {
        	if (prop.length() == 0) continue;
        	String def = defaultConProps.get(prop);
        	if (null == def) def = "";
        	dbProperties.setProperty(prop, dbprop.getString(prop, def));
        }
        
        properties.save();

        dbDriver = null;
        
    	try {
    		DriverManager.getDriver(dbURL);
    	} catch (SQLException e_ignored) {
        	log.log(Level.INFO, "EditLog: No suitable driver loaded, trying to load one");
        	
	        try {
	        	Class<?> driverclass;
	        	if (driverjar.length() > 0) {
	        		File f = new File(driverjar);
	        		if (!f.exists()) {
			        	log.log(Level.SEVERE, "EditLog: requested JDBC driver jar '"+driverjar+"' was not found.");
			        	disable();
			        	return;
	        		}
	        		URL urls[] = {f.toURI().toURL()};
	        		ClassLoader cl = new URLClassLoader(urls, this.getClass().getClassLoader());
	        		
	        		driverclass = Class.forName(driver, true, cl);
	        	} else {
	        		driverclass = Class.forName(driver);
	        	}
	        	
	        	log.log(Level.INFO, "EditLog: Got JDBC driver "+driverclass.getCanonicalName());
	        	
	        	try {
	        		DriverManager.getDriver(dbURL);
	        	} catch (SQLException ex) {
	        		dbDriver = (Driver) driverclass.newInstance();
		        	if (!dbDriver.acceptsURL(dbURL)) {
			        	log.log(Level.SEVERE, "EditLog: Requested JDBC driver "+driverclass.getCanonicalName()+
			        			"Doesn't accept url '"+dbURL+"'");
			        	disable();
			        	return;
		        	}
	        	}
	        } catch (MalformedURLException ex) {
	            log.log(Level.SEVERE, "EditLog: Malformed JDBC driver jar url ", ex);
	            disable();
	            return;
	        } catch (ClassNotFoundException ex) {
	            log.log(Level.SEVERE, "EditLog: Unable to find class " + driver, ex);
	            disable();
	            return;
	        } catch (Exception ex) {
	            log.log(Level.SEVERE, "EditLog: Error setting up driver " + driver, ex);
	            disable();
	            return;
			}
		}
        
    	DBConnection con;
    	
        try {
        	con = new DBConnection(true);
        } catch (SQLException ex) {
        	// already been reported, no need to confuse the log
            disable();
            return;
        }
        
        connectionPool.add(con);
		
        Listener l = new Listener();

        listeners.add(etc.getLoader().addListener(
			PluginLoader.Hook.BLOCK_CREATED, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(
    			PluginLoader.Hook.BLOCK_DESTROYED, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(
    			PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(
    			PluginLoader.Hook.DISCONNECT, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(
    			PluginLoader.Hook.SERVERCOMMAND, l, this, PluginListener.Priority.LOW));
        
        etc.getInstance().addCommand("/editlog", "see '/help editlog' for details");
        
        commitThread = new CommitThread();
        commitThread.start();
        
        log.log(Level.INFO, "EditLog "+version+" enabled.");
	}
	
	@Override
	protected void finalize() throws Throwable {
		softDisable();
		super.finalize();
	}
	
}
