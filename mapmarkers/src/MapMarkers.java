import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class MapMarkers extends Plugin {
	
	private static final String LOG_PREFIX = "[MapMarkers] : "; // Ricin
	
	// Properties read from file
	public int staleTimeout;
	public int updateMarkerFile;
	public String markersFile;
	public boolean showSpawn;
	
	// Internal variables
	public PropertiesFile propertiesFile;
	public SimpleDateFormat dateFormat ;
	public Date date;
	public Date oldDate;
	public Calendar cal;
	public String[] lineArray;
	
	// In-memory marker array
	static ArrayList<String> markerList = new ArrayList<String>();
	static JSONArray markersArray = new JSONArray();
	
	// Standard plugin varaibles
	private MapMarkersListener listener = new MapMarkersListener();
	protected static final Logger log = Logger.getLogger("Minecraft");
	private final String newLine = System.getProperty("line.separator");
	
	// Semaphore used for throttling (see onPlayerMove)
	private final Semaphore available = new Semaphore(1, true);
	
	//!TODO!Extend superplugin from forum.hey0.net, to remove boilerplate code
	public MapMarkers() {
		propertiesFile = new PropertiesFile("mapmarkers.properties");
		dateFormat = new SimpleDateFormat("yyMMdd HH:mm:ss");
	}
	
	public void initialize() {
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.LOW);
	}
	
	public void enable() {
		if (load()) {
			log.info(LOG_PREFIX + "Mod Enabled.");
			etc.getInstance().addCommand("/newlabel", "[label] - Adds new label at the current position");
			etc.getInstance().addCommand("/dellabel", "[label] - Deletes label");
		}
		else {
			log.info(LOG_PREFIX + "Error while loading.");
		}
	}
	
	public void disable() {
		etc.getInstance().removeCommand("/newlabel");
		etc.getInstance().removeCommand("/dellabel");
		log.info(LOG_PREFIX + "Mod Disabled.");
		
	}
	
	public boolean load() {
		// Load properties
		try {
			propertiesFile.load();
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "[MapMarkers] : Exception while loading mapmarkers properties file.", e);
		}
		
		staleTimeout = propertiesFile.getInt("stale-timeout", 300);
		updateMarkerFile = propertiesFile.getInt("update-markerfile", 3);
		markersFile = propertiesFile.getString("markers", "world/markers.json");
		showSpawn = propertiesFile.getBoolean("show-spawn", false);
		
		try {
			dateFormat = new SimpleDateFormat(propertiesFile.getString("date.format", "yyyyMMdd HH:mm:ss"));
		}
		catch (IllegalArgumentException e) {
			//!TODO! Implement standard logging in superplugin
			log.log(Level.SEVERE, LOG_PREFIX + "Invalid date format.  Please check the properties file.  For more info on the date format see here: http://goo.gl/YSes");
			return false;
		} catch (NullPointerException e) {
			log.log(Level.SEVERE, LOG_PREFIX + "No date format specified.  Please check the properties file. For more info on the date format see here: http://goo.gl/YSes");
			return false;
		}
		
		// Check for markers file, create if it doesnt exist
		try {
			File fileCreator = new File(markersFile);
			if (!fileCreator.exists())
				fileCreator.createNewFile();
				BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
				fout.write(markersArray.toString());
				fout.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, LOG_PREFIX + "Exception while creating mapmarkers file.", e);
		}
		
		// Save properties, useful in creating file with default values
		try {
			propertiesFile.save();
		} catch (Exception e) {
				log.log(Level.SEVERE, LOG_PREFIX + "Exception while saving mapmarkers properties file.", e);
		}
		
		// Try to parse any existing markers
		loadMarkers();
		
		//!TODO!Replace id with ENUM
		if (showSpawn) {
			// Add current spawn to marker list (will update if changed)
			Location spawn = etc.getInstance().getServer().getSpawnLocation();
			setMarker("Spawn",spawn.x, spawn.y, spawn.z, 0);
			
			// Write out markers, to ensure file contains spawn
			// (if there are no player logins, no other writes will occur)
			writeMarkers();
		}
		
		
		
		
		return true;
	}
	
	 // START getters/setters
	 
	// Get marker index, create if doesnt exist
	private static int getMarkerIndex(String label){
		boolean inList = false;
		for (String l : markerList) {
			if (l.equals(label))
				inList = true;
		}
		
		if (!inList) {
			markerList.add(label);
			markersArray.add(new JSONObject());
		}
		
		return markerList.indexOf(label);
	}
	
	// Overload setMarker, will set date to current time
	public void setMarker(String label, double x, double y, double z, int id) {
		setMarker(label, x, y, z,id, new java.util.Date());
	}
	
	// Set marker properties in memory, will create marker if doesnt already exist, otherwise overwrite
	@SuppressWarnings("unchecked") // Ricin
	public void setMarker(String label, double x, double y, double z, int id, Date markerDate) {
		int index = getMarkerIndex(label);
		JSONObject newMarker = new JSONObject();
		newMarker.put("msg",label);
		newMarker.put("x",x);
		newMarker.put("y",y);
		newMarker.put("z",z);
		newMarker.put("id",id);
		newMarker.put("timestamp",dateFormat.format(markerDate));
		markersArray.set(index,newMarker);
	}
	
	// Remove marker from memory
	public void removeMarker(String label) {
		int index = getMarkerIndex(label);
		markersArray.remove(index);
		markerList.remove(index);
	}
	
	// END getters/setters
	
	// Load markers from file into memory
	public void loadMarkers() {
		JSONArray tempmarkersArray = new JSONArray();
		try {
			File inFile = new File(markersFile);
			BufferedReader fin = new BufferedReader(new FileReader(inFile));
			
			JSONParser parser = new JSONParser();
			
			try {
				Object obj= parser.parse(fin);
				
				tempmarkersArray =(JSONArray)obj;
				
				for(int i = 0; i < tempmarkersArray.size(); i++)
				{
					try {
						JSONObject marker = (JSONObject)tempmarkersArray.get(i);
						setMarker((String)marker.get("msg"), (Double)marker.get("x"), (Double)marker.get("y"), (Double)marker.get("z"), (Integer)marker.get("id"), dateFormat.parse((String)marker.get("timestamp")));
					}
					catch(Exception e) {
						//ee.printStackTrace();
					}
					
				}	

			}
			catch(ParseException pe){
				log.log(Level.SEVERE, LOG_PREFIX + "Parse exception while parsing line", pe);
			}
			catch (Exception e) {
				log.log(Level.SEVERE, LOG_PREFIX + "Exception while parsing line", e);
			}
			
			fin.close();
			
		} catch (Exception e) {
			log.log(Level.SEVERE, LOG_PREFIX + "Exception while reading markers", e);
		}
		
	}
	
	// Write markers from memory into file
	public synchronized boolean writeMarkers() {
		try {
			if (staleTimeout > 0) {
				// Remove stale markers
				
				// Determine timeout date
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, -staleTimeout); // Reil: Should be negative
				date = cal.getTime();
				
				// Only dealing with player positons
				//!TODO!Use enums
				int markerId = 4;
				
				try {
					for(Object obj : markersArray){
						try {
							JSONObject marker = (JSONObject)obj;
							markerId = Integer.parseInt((String)marker.get("id"));
							//!TODO!Enums
							if (markerId == 4) {
								// Only remove player positions
								oldDate = dateFormat.parse ((String)marker.get("timestamp"));
								if (oldDate.before(date)) {
									removeMarker((String)marker.get("msg"));
								}
							}
						} catch (java.text.ParseException e) {
							log.log(Level.WARNING, LOG_PREFIX + "Unable to parse existing timestamp.  If you changed the format and reloaded the plugin, that is probably the cause.", e);
						} catch(Exception e) {
							//ee.printStackTrace();
						}
					}	
				}
				catch (Exception e) {
				
				}
			}
			
			// Write file and close
			BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
			fout.write(markersArray.toString());
			fout.close();
			
		} catch (Exception e) {
			log.log(Level.SEVERE, LOG_PREFIX + "Exception while updating label", e);
		
			return false;
		}
		
		return true;
	}
	
	
	// Plugin listeners, to register for server mod hooks
	public class MapMarkersListener extends PluginListener {
		public void onLogin(Player player) {
			try {
				setMarker(player.getName(),player.getX(), player.getY(), player.getZ(), 4);
				// Update file
				writeMarkers();
			}
			catch (Exception e) {
				
			}
			
		}
		
		public void onDisconnect(Player player) { // Ricin
			try {
				log.info(LOG_PREFIX + "Removing marker for " + player.getName());
				removeMarker(player.getName());
				writeMarkers();
			}
			catch (Exception e) {
				
			}
		}
		
		public boolean onCommand(Player player, String[] split) {
			if (!player.canUseCommand(split[0]))
				return false;
			
			if (split[0].equalsIgnoreCase("/newlabel")) {
				//!TODO!add checking for existing labels
				if (split.length < 2) {
					player.sendMessage(Colors.Rose + "Correct usage is: /newlabel [name] ");
					return true;
				}
				
				int labelId = 3;
				String label = split[1];
				if (split.length >= 2) {
					for (int i = 2; i < split.length; i++)
						label += " " + split[i]; // Reil
				}
				
				setMarker(label, player.getX(), player.getY(), player.getZ(), labelId);
				
				//!TODO!Check for success/failure before informing player
				log.info(LOG_PREFIX+player.getName()+" created a new label called "+label+".");
				player.sendMessage(Colors.Green + "Label Created!");
				
			}
			else if (split[0].equalsIgnoreCase("/dellabel")) {
				//!TODO!add checks to delete only existing labels
				if (split.length < 2) {
					player.sendMessage(Colors.Rose + "Correct usage is: /dellabel [name] ");
					return true;
				}
				String label = split[1];
				if (split.length >= 2) {
					for (int i = 2; i < split.length; i++)
						label += " " + split[i]; // Reil
				}
				
				removeMarker(label);
				
				//!TODO!Check for success/failure before informing player
				log.info(LOG_PREFIX + player.getName()+" deleted a label called "+label+".");
				player.sendMessage(Colors.Green + "Label Deleted!");
			
		  
			}
			//!TODO!add listlabels
			else {
				return false;
			}
			return true;
		}
		
		public void onPlayerMove(Player player, Location from, Location to) {
			try {
				setMarker(player.getName(),to.x, to.y, to.z, 4);
				
				if (available.tryAcquire()) {
					// Update file
					writeMarkers();
					// Set timer to release in 3 secs
					Timer timer = new Timer();
					timer.schedule(new TimerTask() {
							 public void run() {
								available.release();
							}
						}
						, updateMarkerFile*1000); // Reil: Use timeout from configuration, default 3 seconds
				}
			}
			catch (Exception e) {
				
			}
		}

		public boolean onTeleport(Player player, Location from, Location to) {
			try {
				setMarker(player.getName(), to.x, to.y, to.z, 4);
				// Update file
				writeMarkers();
			}
			catch (Exception e) {
				
			}
			return false;
			
		}
	}
}
