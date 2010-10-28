import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatsLog extends Plugin {
    static String version = "v0.01";

	public enum RecordType {
		CREATE (1),
		DESTROY (2),
		MOVE (10),
		TELEPORT (11),
		WARP (12),
		CHAT (20),
		COMMAND (21),
		LOGIN (22),
		DISCONNECT (23),
		KICK (24),
		BAN (25),
		IPBAN (26);
		
		int type;
		
		RecordType(int type) {
			this.type = type;
		}
		
		int getType() {
			return type;
		}
	}
	
    // Standard plugin varaibles
	public PropertiesFile properties;
	private Listener listener = new Listener();
	private ArrayList<PluginRegisteredListener> listeners = new ArrayList<PluginRegisteredListener>();
	protected static final Logger log = Logger.getLogger("Minecraft");
	private static final String LOG_PREFIX = "[StatsLog] : ";
	
	private int minEditWin,maxEditWin, maxLogLines,maxResultRows;
	
	private Connection connection;
	
	
    private LinkedHashMap<String, Integer> userMap = new LinkedHashMap<String, Integer>(16, 0.75f, true);
	private ArrayList<StatRecord> pendingRecords = new ArrayList<StatRecord>();
    
	private PreparedStatement getPlayerIDStmt, createNewPlayerStmt;
	
	private PreparedStatement actionStmt, movementStmt, connectionStmt;
	
	private boolean stopped;

	private Thread commitThread;
	
	
    public StatsLog() {
		
	}
	
	
	
	public void initialize() {
		// Connections
				
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.LOGINCHECK, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW));
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.SERVERCOMMAND, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.LOW));
		
		// Actions
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.LOW));
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.LOW));
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.LOW));
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_CHANGE, listener, this, PluginListener.Priority.LOW));
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.INVENTORY_CHANGE, listener, this, PluginListener.Priority.LOW));
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_SEND, listener, this, PluginListener.Priority.LOW));
		
		// Movements
		listeners.add(etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.LOW));
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.LOW));
		
		//listeners.add(etc.getLoader().addListener(PluginLoader.Hook.NUM_HOOKS, listener, this, PluginListener.Priority.LOW));
        
	}
	
	public boolean load() {
        properties = new PropertiesFile("statslog.properties");
		minEditWin = properties.getInt("minEditWindow", 60*5);
        maxEditWin = properties.getInt("maxEditWin", 60*20);
        maxLogLines = properties.getInt("maxLogLines", 200);
		maxResultRows = properties.getInt("maxResultRows", 100);
		boolean doSchemaCheck = properties.getBoolean("doSchemaCheck", true);
		String dbpropfile = properties.getString("dbPropertiesFile", "mysql.properties");
		properties.save();
        
        PropertiesFile dbprop = new PropertiesFile(dbpropfile);
        String driver = dbprop.getString("driver", "com.mysql.jdbc.Driver");
        String username = dbprop.getString("user", "minecraft");
        String password = dbprop.getString("pass", "minecraft");
        String db = dbprop.getString("db", "jdbc:mysql://localhost:3306/minecraft");
        dbprop.save();
		
		try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            log.log(Level.SEVERE, LOG_PREFIX+" Unable to find class " + driver, ex);
        }
        
        try {
        	connection = DriverManager.getConnection(db +"?autoReconnect=true&user=" + username + "&password=" + password);
        	connection.setAutoCommit(false);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, LOG_PREFIX+"Unable to connect to DB", ex);
            return false;
        }
        
        synchronized (connection) {   
        	if (doSchemaCheck) {
		        try {
					DatabaseMetaData meta = connection.getMetaData();
					ResultSet rs = meta.getTables(connection.getCatalog(), null, "stats_%", null);
					List<String> neededTables = new ArrayList<String>();
					neededTables.add("stats_players");
					neededTables.add("stats_edits");
					neededTables.add("stats_detail");
		
					while(rs.next())
					{
						System.out.printf("Got table %s\n", rs.getString("TABLE_NAME"));
						neededTables.remove(rs.getString("TABLE_NAME"));
					}
					
					if (neededTables.size() > 0)
						CreateSchema();
				} catch (SQLException ex) {
		            log.log(Level.SEVERE, LOG_PREFIX+"Error checking for or creating schema", ex);
		            return false;
		        }
        	}
        	
	        try {
				prepareStatements();
			} catch (SQLException ex) {
	            log.log(Level.SEVERE, LOG_PREFIX+"Error preparing statements", ex);
	            return false;
	        }
        }
		
        commitThread = new Thread(new ComitThread());
        commitThread.setName("StatsLog Commit Thread");
        commitThread.start();
        
        return true;
		
    }
	
	
	@Override
	public void enable() {
        if (load())
		{
            log.info(LOG_PREFIX+"Mod "+version+" enabled.");
		}	
        else
		{
            log.info(LOG_PREFIX+"Error while loading.");
			disable();
		}
    }	
	
	@Override
	public void disable() {
		stopped = true;
		
		if (null != commitThread)
			commitThread.interrupt();
		
		
		synchronized (pendingRecords) {
			PluginLoader loader = etc.getLoader();
			for (PluginRegisteredListener rl : listeners)
				loader.removeListener(rl);
			listeners.clear();
			
			pendingRecords.clear();
		}
		
		commitThread = null;
		
		if (null != connection) {
			try {
				connection.close();
			} catch (SQLException ex) {
	            log.log(Level.SEVERE,LOG_PREFIX+"Unable to close DB connection", ex);
			}
			connection = null;
		}
        log.log(Level.INFO, LOG_PREFIX+"Mod "+version+" disabled.");
	}
	
	
   
	
    

	public int GetPlayerID(String name)
	{
		Integer out = userMap.get(name);
		if (null != out) {
		//	log.log(Level.INFO, String.format("cached %s = %d", name, out));
			return out;
		}
		
		try {
			synchronized(connection) {
				getPlayerIDStmt.setString(1, name);
				ResultSet rs = getPlayerIDStmt.executeQuery();
				if (rs.next())
				{
					int outi = rs.getInt(1);
		//			log.log(Level.INFO, String.format("got %s = %d", name, outi));
					userMap.put(name, outi);
					rs.close();
					connection.commit();
					return outi;
				}
				
				createNewPlayerStmt.setString(1, name);
				createNewPlayerStmt.executeUpdate();
				rs = createNewPlayerStmt.getGeneratedKeys();
				
				rs.next();
				int outi = rs.getInt(1);
		//		log.log(Level.INFO, String.format("set %s = %d", name, outi));
				userMap.put(name, outi);
				rs.close();
				connection.commit();
				return outi;
			}
		} catch (SQLException ex) {
            log.log(Level.SEVERE, LOG_PREFIX+"Unable to look up/allocate player ID", ex);
		}
		return -1;
	}
	
	private class ComitThread implements Runnable {
		public void run() {
			int numSaved = 0;
			while(!stopped) {
				try {
					Thread.sleep(minEditWin/4*1000);
				} catch (InterruptedException e) {
					// we got bumped by disable
					return;
				}
				
				try {
					// group the inserts into batches of 100 that get written to db together.
					for(StatRecord record : pendingRecords) {
						synchronized(connection) {
							record.executeStatement();
							numSaved++;
							
							if (numSaved == 100) {
								connection.commit();
								log.log(Level.INFO, String.format(LOG_PREFIX+"Wrote %d entries to database", numSaved));
								numSaved = 0;
							}
						}
					}
					pendingRecords.clear();
					
				} catch (SQLException ex) {
		            log.log(Level.SEVERE, LOG_PREFIX+"Error writing to database", ex);
				} 
			}
		}
	}
	
	
	
	private class Listener extends PluginListener {
		//GetPlayerID(player.getName())
		
		public String onLoginChecks(String user) {
			throw new UnsupportedOperationException("Not supported yet.");
		}
 
		public void onLogin(Player player) {
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setLogin();
			pendingRecords.add(record);
		}
		
		public void onDisconnect(Player player) { 
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setDisconnect();
			pendingRecords.add(record);
		}

		public boolean onChat(Player player, String chatMessage) {
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setChat(chatMessage);
			pendingRecords.add(record);
			
			return false;
		}

		public boolean onCommand(Player player, String[] split) {
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setCommand(split);
			pendingRecords.add(record);
			
			return false;
		}

		public void onBan(Player player, String reason) {
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setBan(reason);
			pendingRecords.add(record);
		}

		public void onIpBan(Player player, String reason) {
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setIpBan(reason);
			pendingRecords.add(record);
		}

		public void onKick(Player player, String reason) {
			ConnectionRecord record = new ConnectionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setKick(reason);
			pendingRecords.add(record);
		}
		
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int item) { 
			ActionRecord record = new ActionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setCreate(blockPlaced, blockClicked, item);
			pendingRecords.add(record);
			
			return false;
		}
		public boolean onBlockDestroy(Player player, Block block) { 
			ActionRecord record = new ActionRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setDestroy(block);
			pendingRecords.add(record);
			
			return false;
		}

		public void onPlayerMove(Player player, Location fromLocation, Location toLocation) {
			MovementRecord record = new MovementRecord(GetPlayerID(player.getName().toLowerCase()));
			record.setMovement(fromLocation, toLocation, Math.abs(fromLocation.x - toLocation.x) + Math.abs(fromLocation.y - toLocation.y) + Math.abs(fromLocation.z - toLocation.z));
			
			pendingRecords.add(record);
		} 
	};



	private void prepareStatements() throws SQLException {
		getPlayerIDStmt = connection.prepareStatement(
				"SELECT playerid FROM stats_players "+
				"WHERE playername = ?;");
		createNewPlayerStmt = connection.prepareStatement(
				"INSERT INTO stats_players(playername) "+
				"VALUES (?);");
				
			
		actionStmt = connection.prepareStatement(
				"INSERT INTO stats_actions(actiontypeid, playerid, timestamp, x, y, z, blockDestroyed, blockClicked, blockPlaced, itemInHand) "+
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
				
		movementStmt = connection.prepareStatement(
				"INSERT INTO stats_movements(movtypeid, playerid, timestamp, from_x, from_y, from_z, to_x, to_y, to_z, distance) "+
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
				
		connectionStmt = connection.prepareStatement(
				"INSERT INTO stats_connections(conntypeid, playerid, timestamp, message) "+
				"VALUES (?, ?, ?, ?);");
		
		
	}
	
	private void CreateSchema() throws SQLException {
		log.log(Level.INFO, "[Stats] : Setting up tables");
		
		connection.setAutoCommit(true);
		
		Statement createStatement = connection.createStatement();
		
		try{
			createStatement.execute("DROP TABLE stats_players;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE stats_players ("+
			"playerid        INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"playername      VARCHAR(31) NOT NULL UNIQUE KEY);");
			
		try{
			createStatement.execute("DROP TABLE stats_actiontypes;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE stats_actiontypes ("+
			"actiontypeid    INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"actionname      VARCHAR(31) NOT NULL UNIQUE KEY);");
		createStatement.execute("INSERT INTO stats_actiontypes (actiontypeid, actionname)" +
			"values ("+RecordType.CREATE.getType()+", 'create')");
		createStatement.execute("INSERT INTO stats_actiontypes (actiontypeid, actionname)" +
			"values ("+RecordType.DESTROY.getType()+", 'destroy')");

		
		try{
			createStatement.execute("DROP TABLE stats_movtypes;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE stats_movtypes ("+
			"movtypeid       INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"movtypename     VARCHAR(31) NOT NULL UNIQUE KEY);");
		createStatement.execute("INSERT INTO stats_movtypes (movtypeid, movtypename)" +
			"values ("+RecordType.MOVE.getType()+", 'move')");
		createStatement.execute("INSERT INTO stats_movtypes (movtypeid, movtypename)" +
			"values ("+RecordType.TELEPORT.getType()+", 'teleport')");
		createStatement.execute("INSERT INTO stats_movtypes (movtypeid, movtypename)" +
			"values ("+RecordType.WARP.getType()+", 'warp')");
			
		try{
			createStatement.execute("DROP TABLE stats_conntypes;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE stats_conntypes ("+
			"conntypeid    INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"conntypename      VARCHAR(31) NOT NULL UNIQUE KEY);");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.CHAT.getType()+", 'chat')");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.COMMAND.getType()+", 'command')");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.LOGIN.getType()+", 'login')");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.DISCONNECT.getType()+", 'disconnect')");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.KICK.getType()+", 'kick')");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.BAN.getType()+", 'ban')");
		createStatement.execute("INSERT INTO stats_conntypes (conntypeid, conntypename)" +
			"values ("+RecordType.IPBAN.getType()+", 'ipban')");

			
		try{
			createStatement.execute("DROP TABLE stats_actions;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE stats_actions ("+
			"actionid        INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"actiontypeid    INTEGER NOT NULL,"+
			"playerid        INTEGER NOT NULL,"+
			"timestamp       timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"+
			"x               double NOT NULL,"+
			"y               double NOT NULL,"+
			"z               double NOT NULL,"+
			"blockDestroyed  INTEGER DEFAULT NULL,"+
			"blockClicked    INTEGER DEFAULT NULL,"+
			"blockPlaced     INTEGER DEFAULT NULL,"+
			"itemInHand      INTEGER DEFAULT NULL,"+
			"INDEX(playerid, timestamp),"+ // for player actions queries
			"INDEX(x, y, z, timestamp),"+ // for block actions queries
			"INDEX(timestamp));"); // for expired entries query
			//!TODO!decide on appropriate additional indexes
	
		try{
			createStatement.execute("DROP TABLE stats_movements;");
		} catch (SQLException e) {}
		
		createStatement.execute("CREATE TABLE stats_movements ("+
			"movid           INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"movtypeid       INTEGER NOT NULL,"+
			"playerid        INTEGER NOT NULL,"+
			"timestamp       timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"+
			"from_x          double NOT NULL,"+
			"from_y          double NOT NULL,"+
			"from_z          double NOT NULL,"+
			"to_x            double NOT NULL,"+
			"to_y            double NOT NULL,"+
			"to_z            double NOT NULL,"+
			"distance        double NOT NULL,"+
			"INDEX(playerid, timestamp),"+ // for player movements queries
			"INDEX(playerid, movtypeid),"+ // for player movements queries
			"INDEX(timestamp));"); // for expired entries query
			//!TODO!decide on appropriate additional indexes
			

		try{
			createStatement.execute("DROP TABLE stats_connections;");
		} catch (SQLException e) {}
		
		createStatement.execute("CREATE TABLE stats_connections ("+
			"connid          INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"conntypeid      INTEGER NOT NULL,"+
			"playerid        INTEGER NOT NULL,"+
			"timestamp       timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"+
			"message         varchar(256) DEFAULT NULL,"+
			"INDEX(playerid, timestamp),"+ // for player movements queries
			"INDEX(playerid, conntypeid),"+ // for player movements queries
			"INDEX(timestamp));"); // for expired entries query
			//!TODO!decide on appropriate additional indexes
			
		connection.setAutoCommit(false);
	}
	
	
	private static class StatRecord {
    	public int playerId;
		public RecordType recordType;
		
		public Date timestamp;
		
		public StatRecord(int player) {
			playerId = player;
			timestamp = new java.util.Date();
		}
		
		public void executeStatement() {
			log.info(LOG_PREFIX+"If you see this line, something has gone wrong: " + Integer.toString(recordType.getType()));
		}
	}
	
	
	
	private class ActionRecord extends StatRecord {
		public int itemInHand;
		public Block blockDestroyed;
		public Block blockPlaced, blockClicked;
		
		public ActionRecord(int player) {
			super(player);
		}
		
		// ACTIONS
		public void setDestroy(Block block) {
			this.recordType = RecordType.DESTROY;
			this.blockDestroyed = block;
		}		
		
		public void setCreate(Block blockPlaced, Block blockClicked, int item) {
			this.recordType = RecordType.CREATE;
			this.blockPlaced = blockPlaced;
			this.blockClicked = blockClicked;
			this.itemInHand = item;
		}
		
		
		public void executeStatement() {
			try{
			
		actionStmt = connection.prepareStatement(
				"INSERT INTO stats_actions(actiontypeid, playerid, timestamp, x, y, z, blockDestroyed, blockClicked, blockPlaced, itemInHand) "+
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			
			
				switch(this.recordType) {
					case CREATE:
						actionStmt.setInt(1,RecordType.CREATE.getType());
						actionStmt.setInt(2,playerId);
						actionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						actionStmt.setDouble(4, blockPlaced.getX());
						actionStmt.setDouble(5, blockPlaced.getY());
						actionStmt.setDouble(6, blockPlaced.getZ());
						actionStmt.setNull(7, java.sql.Types.NULL);
						actionStmt.setInt(8, blockClicked.getType());
						actionStmt.setInt(9, blockPlaced.getType());
						actionStmt.setInt(10, itemInHand);
						actionStmt.execute();
						break;
					case DESTROY:
						actionStmt.setInt(1,RecordType.DESTROY.getType());
						actionStmt.setInt(2,playerId);
						actionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						actionStmt.setDouble(4, blockDestroyed.getX());
						actionStmt.setDouble(5, blockDestroyed.getY());
						actionStmt.setDouble(6, blockDestroyed.getZ());
						actionStmt.setInt(7, blockDestroyed.getType());
						actionStmt.setNull(8, java.sql.Types.NULL);
						actionStmt.setNull(9, java.sql.Types.NULL);
						actionStmt.setNull(10, java.sql.Types.NULL);
						actionStmt.execute();
						break;
					
				}
			} catch (SQLException e) {}
		}
	}
	
	private class MovementRecord extends StatRecord {
		public double x,y,z,distance;
		public Location from, to;
		
		public MovementRecord(int player) {
			super(player);
		}
		
		// MOVEMENTS
		public void setMovement(Location from, Location to, double distance) {
			this.recordType = RecordType.MOVE;
			this.from = from;
			this.to = to;
			this.distance = distance;
		}		
		
		public void setTeleport(Location from, Location to, double distance) {
			this.recordType = RecordType.TELEPORT;
			this.from = from;
			this.to = to;
			this.distance = distance;
		}		
		
		public void setWarp(Location from, Location to, double distance) {
			this.recordType = RecordType.WARP;
			this.from = from;
			this.to = to;
			this.distance = distance;
		}
		
		public void executeStatement() {
			try{
				
				movementStmt.setInt(1,recordType.getType());
				movementStmt.setInt(2,playerId);
				movementStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
				movementStmt.setDouble(4, from.x);
				movementStmt.setDouble(5, from.y);
				movementStmt.setDouble(6, from.z);
				movementStmt.setDouble(7, to.x);
				movementStmt.setDouble(8, to.y);
				movementStmt.setDouble(9, to.z);
				movementStmt.setDouble(10, distance);
				movementStmt.execute();
			} catch (SQLException e) {}
		}
	}
	
	private class ConnectionRecord extends StatRecord {
		public String[] command;
		public String chatMessage, reason;
		
		public ConnectionRecord(int player) {
			super(player);
		}
		
		// CONNECTIONS
		public void setKick(String reason) {
			this.recordType = RecordType.KICK;
			this.reason = reason;
			
		}		
		
		
		public void setIpBan(String reason) {
			this.recordType = RecordType.IPBAN;
			this.reason = reason;
		}	
		
		
		public void setBan(String reason) {
			this.recordType = RecordType.BAN;
			this.reason = reason;
		}	
		
		
		public void setCommand(String[] command) {
			this.recordType = RecordType.COMMAND;
			this.command = command;
		}	
		
			
		public void setChat(String chatMessage) {
			this.recordType = RecordType.CHAT;
			this.chatMessage = chatMessage;
		}	
		
		
		public void setLogin() {
			this.recordType = RecordType.LOGIN;
			
		}	
		
		
		public void setDisconnect() {
			this.recordType = RecordType.DISCONNECT;
			
		}

		public void executeStatement() {
			try{
			
			
				switch(this.recordType) {
					case LOGIN:
						connectionStmt.setInt(1,RecordType.LOGIN.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setNull(4, java.sql.Types.NULL);
						connectionStmt.execute();
						break;
					case DISCONNECT:
						connectionStmt.setInt(1,RecordType.DISCONNECT.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setNull(4, java.sql.Types.NULL);
						connectionStmt.execute();
						break;
					case BAN:
						connectionStmt.setInt(1,RecordType.BAN.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setString(4, reason);
						connectionStmt.execute();
						break;
					case IPBAN:
						connectionStmt.setInt(1,RecordType.IPBAN.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setString(4, reason);
						connectionStmt.execute();
						break;
					case KICK:
						connectionStmt.setInt(1,RecordType.KICK.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setString(4, reason);
						connectionStmt.execute();
						break;
					case CHAT:
						connectionStmt.setInt(1,RecordType.DISCONNECT.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setString(4, chatMessage);
						connectionStmt.execute();
						break;
					case COMMAND:
						connectionStmt.setInt(1,RecordType.COMMAND.getType());
						connectionStmt.setInt(2,playerId);
						connectionStmt.setTimestamp(3,new java.sql.Timestamp(timestamp.getTime()));
						connectionStmt.setString(4, arrayToString(command," "));
						connectionStmt.execute();
						break;
				}
			} catch (SQLException e) {}
		}
	}
	
	public static String arrayToString(String[] a, String separator) {
    if (a == null || separator == null) {
        return null;
    }
    StringBuilder result = new StringBuilder();
    if (a.length > 0) {
        result.append(a[0]);
        for (int i=1; i < a.length; i++) {
            result.append(separator);
            result.append(a[i]);
        }
    }
    return result.toString();
  }
  
  
}
