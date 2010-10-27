import java.io.*;
import java.util.*;
import java.util.logging.MemoryHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Formatter;
import java.util.Date.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Arrays;





public class Stats extends Plugin {

	public PropertiesFile propertiesFile;
	
	public Date date;
	public Date oldDate;
	public Calendar cal;
	public String[] lineArray;
	
	public SimpleDateFormat dateFormatLogEntry = new SimpleDateFormat("yyMMdd-HH.mm.ss");
	public SimpleDateFormat dateFormatLogFile = new SimpleDateFormat("yyMMdd");

		// Standard plugin varaibles
	private StatsListener listener = new StatsListener();
	protected static final Logger log = Logger.getLogger("Minecraft");
	protected static final Logger statLogger = Logger.getLogger("Stats");
    private final String newLine = System.getProperty("line.separator");
    private final String fileSep = System.getProperty("file.separator");
	
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<StatsMemoryHandler> playerLoggers = new ArrayList<StatsMemoryHandler>();
	
	
	private String driver, username, password, host, db, logDir, connection;
	
	
	private int maxLogLines;
	
	
    public Stats() {
		propertiesFile = new PropertiesFile("stats.properties");
		statLogger.setUseParentHandlers(false);
	}
	
	public void initialize() {
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGINCHECK, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener(PluginLoader.Hook.SERVERCOMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.LOW);
		//etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.LOW);
		//etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_CHANGE, listener, this, PluginListener.Priority.LOW);
		//etc.getLoader().addListener(PluginLoader.Hook.INVENTORY_CHANGE, listener, this, PluginListener.Priority.LOW);
		//etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_SEND, listener, this, PluginListener.Priority.LOW);
		//etc.getLoader().addListener(PluginLoader.Hook.NUM_HOOKS, listener, this, PluginListener.Priority.LOW);
		//etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.LOW);
		
        
	}
	
	public boolean load() {
        try {
            File f = new File("stats.properties");
            if (f.exists())
			{
                propertiesFile.load();
			}
            else
			{
                f.createNewFile();
			}
        } catch (Exception e) {
            log.log(Level.SEVERE, "[Stats] : Exception while creating Stats properties file.", e);
        }
        
		
		
        maxLogLines = propertiesFile.getInt("maxLogLines", 200);
		driver = propertiesFile.getString("driver", "com.mysql.jdbc.Driver");
        username = propertiesFile.getString("user", "mcstats");
        password = propertiesFile.getString("pass", "mcstats");
        host = propertiesFile.getString("host", "jdbc:mysql://localhost:3306/");
		db = propertiesFile.getString("db", "mcstats");
		
		
        try {
            propertiesFile.save();
        } catch (Exception e) {
                log.log(Level.SEVERE, "[Stats] : Exception while saving Stats properties file.", e);
        }
		
		try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            log.log(Level.SEVERE, "[Stats] : Unable to find class " + driver, ex);
        }
        
		connection = host+db+"?user="+username+"&password="+password+"&database="+db;
		
		
		
		// Create loggers
		createLogger("movements", maxLogLines);
		createLogger("actions", maxLogLines);
		createLogger("connections", 5 );
		
			
			
        return true;
		
		
    }
	
    public void enable() {
        if (load())
		{
            log.info("[Stats] Mod Enabled.");
					}	
        else
		{
            log.info("[Stats] Error while loading.");
			}
    }
    
    
    public void disable() {
		removeAllLoggers();
        log.info("[Stats] Mod Disabled.");
		
    }

		

	public void createLogger(String table, int bufferSize) {
		try {
			// Create a memory handler with a memory of 200 records
			// and dumps the records into the file my.log when a
			// some abitrary condition occurs
			
			// set up the JDBCLogger handler
			StatsJDBCHandler jdbcHandler = new StatsJDBCHandler(driver,connection);

			StatsMemoryHandler mhandler = new StatsMemoryHandler(table, bufferSize, jdbcHandler);
			
			// Add to the desired logger
			statLogger.addHandler(mhandler);

			
		} catch (Exception e) {
			log.info("Exception" + e);
		}
		
	}
	
	private static void removeAllLoggers()
	{
		for (Handler statsHandler : statLogger.getHandlers())
		{
				((StatsMemoryHandler)statsHandler).push();
				statsHandler.flush();
				statsHandler.close();
				statLogger.removeHandler(statsHandler);
		}
	}
	
	
	// Plugin listeners, to register for server mod hooks
	public class StatsListener extends PluginListener {
		
		public String onLoginChecks(String user) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void onLogin(Player player) {
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setLogin();
			
			statLogger.log(logRecord);
		}
		
		public void onDisconnect(Player player) { 
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setDisconnect();
			
			statLogger.log(logRecord);
		}

		public boolean onChat(Player player, String chatMessage) {
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setChat(chatMessage);
			
			statLogger.log(logRecord);
			
			return false;
		}

		public boolean onCommand(Player player, String[] split) {
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setCommand(split);
			
			statLogger.log(logRecord);
			
			return false;
		}

		public void onBan(Player player, String reason) {
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setBan(reason);
			
			statLogger.log(logRecord);
			
			
		}

		public void onIpBan(Player player, String reason) {
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setIpBan(reason);
			
			statLogger.log(logRecord);
		}

		public void onKick(Player player, String reason) {
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setKick(reason);
			
			statLogger.log(logRecord);
		}
		
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int item) { 
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setCreate(blockPlaced, blockClicked, item);
			
			statLogger.log(logRecord);
			
			return false;
		}
		public boolean onBlockDestroy(Player player, Block block) { 
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setDestroy(block);
			
			statLogger.log(logRecord);
			
			return false;
		}

		public void onPlayerMove(Player player, Location fromLocation, Location toLocation) {
		
			StatsLogRecord logRecord = new StatsLogRecord(player.getName().toLowerCase());
			
			logRecord.setMovement(fromLocation, toLocation, Math.abs(fromLocation.x - toLocation.x) + Math.abs(fromLocation.y - toLocation.y) + Math.abs(fromLocation.z - toLocation.z));
			
			statLogger.log(logRecord);
		}
	}
	
}
