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

  /**
   * A SQL statement used to insert into the log table.
   */
  protected final static String actionInsertSQL="insert into actions (key,player,timestamp,"
  +"blockDestroyed,"
  +"blockCreated_Clicked,blockCreated_Placed,blockCreated_Item,"
  +"commandText)"
  +"values(?,?,?,?,?,?,?,?)";
  
   protected final static String movementInsertSQL="insert into movements (key,player,timestamp,"
  +"x,y,z,distance"
  +"values(?,?,?,?,?,?,?)";
  
   protected final static String connectionInsertSQL="insert into connections (key,player,timestamp"
  +"values(?,?,?)";

  protected PreparedStatement prepActionInsert, prepMovementInsert, prepConnectionInsert;

  
  /**
   * A SQL statement used to clear the log table.
   */
  protected final static String clearSQL="delete from log;";

  /**
   * A PreparedStatement object used to hold the clear
   * statement.
   */
  protected PreparedStatement prepClear;


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
      System.err.println("Error on open: " + e);
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
  
  //Users
    public void addAction(String playerName) {
	
		 /* // now store the log entry into the table
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
		 */
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

   // Switch on table
   
   
   // Switch on key

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
}