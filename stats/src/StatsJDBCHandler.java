import java.util.logging.*;
import java.sql.*;

/**
 * JDBC Logging Article
 *
 * This is a reusable class that implements
 * a JDK1.4 log handler that will write the
 * contents of the log to a JDBC data source.
 * 
 * author Jeff Heaton (http://www.jeffheaton.com)
 * version 1.0
 * since January 2002
 */
public class StatsJDBCHandler extends Handler {


    protected static final Logger log = Logger.getLogger("Minecraft");
  /**
   * A string that contains the classname of the JDBC driver.
   * This value is filled by the constructor.
   */
  String driverString;

  /**
   * A string that contains the connection string used by the
   * JDBC driver. This value is filled by the constructor.
   */
  String connectionString;

  /**
   * Used to hold the connection to the JDBC data source.
   */
  Connection connection;

	private PreparedStatement getPlayerIDStmt, createNewPlayerStmt;
	
	
	private Thread commitThread;

	LinkedHashMap<String, Integer> userMap = new LinkedHashMap<String, Integer>(16, 0.75f, true);
	
	private int GetPlayerID(String name)
	{
		Integer out = userMap.get(name);
		if (null != out) {
		//	log.log(Level.INFO, String.format("cached %s = %d", name, out));
			return out;
		}
		
		try {
			synchronized(con) {
				getPlayerIDStmt.setString(1, name);
				ResultSet rs = getPlayerIDStmt.executeQuery();
				if (rs.next())
				{
					int outi = rs.getInt(1);
		//			log.log(Level.INFO, String.format("got %s = %d", name, outi));
					userMap.put(name, outi);
					rs.close();
					con.commit();
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
				con.commit();
				return outi;
			}
		} catch (SQLException ex) {
            log.log(Level.SEVERE, "[Stats] : Unable to look up/allocate player ID", ex);
		}
		return -1;
	}
	
  /**
   * @param driverString The JDBC driver to use.
   * @param connectionString The connection string that
   * specifies the database to use.
   */
  public StatsJDBCHandler(String driverString, String connectionString)
  {
    try {
      this.driverString = driverString;
      this.connectionString = connectionString;

      //Class.forName(driverString);
      connection = getConnection();
      prepActionInsert = connection.prepareStatement(actionInsertSQL);
      //prepClear = connection.prepareStatement(clearSQL);
    
    } catch ( SQLException e ) {
      log.log(Level.SEVERE, "Error on open",e);
	  
    }
  }

  
    private Connection getConnection() {
        try {
            return DriverManager.getConnection(connectionString);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "Unable to retreive connection", ex);
        }
        return null;
    }
	
	
	private void prepareStatements() throws SQLException {
		getPlayerIDStmt = connection.prepareStatement(
				"SELECT playerid FROM players "+
				"WHERE playername = ?;");
		createNewPlayerStmt = con.prepareStatement(
				"INSERT INTO players(playername) "+
				"VALUES (?);");
				
		saveEditStmt = con.prepareStatement(
				"INSERT INTO actions(chunkX, chunkY, startTime, endTime, playerid) "+
				"VALUES (?, ?, ?, ?, ?);");
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
	
	
	private void CreateSchema() throws SQLException {
		log.log(Level.INFO, "[Stats] : Setting up tables");
		
		connection.setAutoCommit(true);
		
		Statement createStatement = connection.createStatement();
		
		try{
			createStatement.execute("DROP TABLE players;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE players ("+
			"playerid        INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"playername      VARCHAR(31) NOT NULL UNIQUE KEY);");
			
		try{
			createStatement.execute("DROP TABLE actiontypes;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE actiontypes ("+
			"actiontypeid    INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"actionname      VARCHAR(31) NOT NULL UNIQUE KEY);");
		
		try{
			createStatement.execute("DROP TABLE movtypes;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE movtypes ("+
			"movtypeid       INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"movtypename     VARCHAR(31) NOT NULL UNIQUE KEY);");

		try{
			createStatement.execute("DROP TABLE conntypes;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE conntypes ("+
			"conntypeid    INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"conntypename      VARCHAR(31) NOT NULL UNIQUE KEY);");

			
		try{
			createStatement.execute("DROP TABLE actions;");
		} catch (SQLException e) {}
		createStatement.execute("CREATE TABLE actions ("+
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
			createStatement.execute("DROP TABLE movements;");
		} catch (SQLException e) {}
		
		createStatement.execute("CREATE TABLE movements ("+
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
			createStatement.execute("DROP TABLE connections;");
		} catch (SQLException e) {}
		
		createStatement.execute("CREATE TABLE connections ("+
			"connid          INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
			"conntypeid      INTEGER NOT NULL,"+
			"playerid        INTEGER NOT NULL,"+
			"timestamp       timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"+
			"message         varchar(256) DEFAULT NULL,"+
			"INDEX(playerid, timestamp),"+ // for player movements queries
			"INDEX(playerid, movementtypeid),"+ // for player movements queries
			"INDEX(timestamp));"); // for expired entries query
			//!TODO!decide on appropriate additional indexes
			
		connection.setAutoCommit(false);
	}
  
  //Users
    public void addAction(StatsLogRecord logRecord) {
		
		// Switch on key
		if (logRecord.key == "DESTROY") {
				 // now store the log entry into the table
               try {
                 prepActionInsert.setInt(1,record.getLevel().intValue());
                 prepInsert.setString(2,truncate(record.getLoggerName(),63));
                 prepInsert.setString(3,truncate(record.getMessage(),255));
                 prepInsert.setLong(4,record.getSequenceNumber());
                 prepInsert.setString(5,truncate(record.getSourceClassName(),63));
                 prepInsert.setString(6,truncate(record.getSourceMethodName(),31));
                 prepInsert.setInt(7,record.getThreadID());
                 prepInsert.setTimestamp(8,new Timestamp (System.currentTimeMillis()) );
                 prepInsert.executeUpdate();
               } catch ( SQLException e ) {
                 System.err.println("Error on open: " + e);
               }

			}
		else if (logRecord.key == "CREATE") {
				
		}
		else {	

			log.info("Invalid key");

		}
    }
	
	
  //Users
    public void addMovement(StatsLogRecord logRecord) {
		
		// Switch on key
		if (logRecord.key == "MOVEMENT") {
				//break;
				//addAction(statsLogRecord); break;
		}			
		else {
			log.info("Invalid key");
				
		}
    }
	
	
  //Users
    public void addConnection(StatsLogRecord logRecord) {
		
		// Switch on key
		if (logRecord.key ==  "LOGIN") {
				
				//addAction(statsLogRecord); break;
			}	
		else if (logRecord.key ==  "DISCONNECT") {
				
				//addAction(statsLogRecord); break;
			}	
		else if (logRecord.key == "KICK") {
				
				//addAction(statsLogRecord); break;
			}	
		else if (logRecord.key == "BAN") {
				
				//addAction(statsLogRecord); break;
			}	
		else if (logRecord.key == "IPBAN") {
				
				//addAction(statsLogRecord); break;
			}	
		else if (logRecord.key == "CHAT") {
				
				//addAction(statsLogRecord); break;
			}		
		else if (logRecord.key ==  "COMMAND") {
				
				//addAction(statsLogRecord); break;
			}		
		else {
			log.info("Invalid key");
				
		}
    }
	
	
  
  /**
   * Internal method used to truncate a string to a specified width.
   * Used to ensure that SQL table widths are not exceeded.
   * 
   * @param str The string to be truncated.
   * @param length The maximum length of the string.
   * @return The string truncated.
   */
  static public String truncate(String str,int length)
  {
    if ( str.length()<length )
      return str;
    return( str.substring(0,length) );
  }

  /**
   * Overridden method used to capture log entries and put them
   * into a JDBC database.
   * 
   * @param record The log record to be stored.
   */
  public void publish(LogRecord record)
  {
    // first see if this entry should be filtered out
    if ( getFilter()!=null ) {
      if ( !getFilter().isLoggable(record) )
        return;
    }

	StatsLogRecord statsLogRecord = (StatsLogRecord)record;
	// Switch on table
	if (statsLogRecord.table == "actions") {
		addAction(statsLogRecord);
	}
	else if (statsLogRecord.table == "movements") {
		addMovement(statsLogRecord); 
	}
	else if (statsLogRecord.table == "connections") {
		addConnection(statsLogRecord);
	}
	else {
		log.info("Invalid table");
	}
	

  }

  /**
   * Called to close this log handler.
   */
  public void close()
  {
    try {
      if ( connection!=null )
        connection.close();
    } catch ( SQLException e ) {
      System.err.println("Error on close: " + e);
    }
  }

  /**
   * Called to clear all log entries from the database.
   */
  public void clear()
  {
    try {
      prepClear.executeUpdate();
    } catch ( SQLException e ) {
      System.err.println("Error on clear: " + e);
    }
  }


  /**
   * Not really used, but required to implement a handler. Since 
   * all data is immediately sent to the database, there is no 
   * reason to flush.
   */
  public void flush()
  {
  }
  
  
	private class ComitThread implements Runnable {
		public void run() {
			while(!stopped) {
				try {
					Thread.sleep(minEditWin/4*1000);
				} catch (InterruptedException e) {
					// we got bumped by disable
					return;
				}
				
				int now = (int) (System.currentTimeMillis() / 1000L);
				
				int numsaved = 0;
				
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
				
				if (numsaved > 0)
					log.log(Level.INFO, String.format("EditLog: wrote %d entries to log", numsaved));
				
				
				
				int[] toPrune = new int[1024];
				int numToPrune = 0;
				
				int prunetime = now - editLogLength;
				
				try {
					synchronized (con) {
						editsToPruneStmt.setInt(1, prunetime);
						ResultSet rs = editsToPruneStmt.executeQuery();
						
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
							pruneEditsStmt.setInt(1, prunetime);
							pruneEditsStmt.execute();
						}
						con.commit();
					}
					
					// group the deletes into batches of 100 that get written
					// to disk together.  This reduces overhead while avoiding
					// a huge lag spike if thousands of edits need to be pruned
					// at once (e.g. if the plugin was loaded after a long time
					// of not being loaded)
					for(int chunk=0 ; chunk<numToPrune ; chunk+=100) {
						synchronized(con) {
							for(int i=chunk ; i<numToPrune && i<chunk+100 ; i++) {
								pruneEditDetailsStmt.setInt(1, toPrune[i]);
								pruneEditDetailsStmt.execute();
							}
							con.commit();
						}
					}
					
					
				} catch (SQLException ex) {
		            log.log(Level.SEVERE, "EditLog: Error pruning old log entries", ex);
				}
			}
		}
	}
	
  
}