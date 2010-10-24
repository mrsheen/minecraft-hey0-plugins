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

	public String markersFile;
    public PropertiesFile propertiesFile;
	public SimpleDateFormat dateFormat ;
	public Date date;
	public Date oldDate;
	public Calendar cal;
	public String[] lineArray;
	
	static ArrayList<String> markerList = new ArrayList<String>();
	static JSONArray markersArray = new JSONArray();
	
	
    private MapMarkersListener listener = new MapMarkersListener();

    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
	
	private final Semaphore available = new Semaphore(1, true);
    
    public MapMarkers() {
		propertiesFile = new PropertiesFile("mapmarkers.properties");
		dateFormat = new SimpleDateFormat("yyMMdd-HH.mm.ss");
	}
	
	    public void initialize()
    {
        etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
    }
	
	public boolean load() {
        try {			
			propertiesFile.load();
			
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while loading mapmarkers properties file.", e);
        }
        
        markersFile = propertiesFile.getString("markers", "world/markers.json");
        
        String[] filesToCheck = { markersFile };
        for (String f : filesToCheck) {
            try {
                File fileCreator = new File(f);
                if (!fileCreator.exists())
                    fileCreator.createNewFile();
					BufferedWriter fout = new BufferedWriter(new FileWriter(f));
					fout.write(markersArray.toString());
					fout.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "[MapMarkers] : Exception while creating mapmarkers file.", e);
            }
        }
        
        try {
            propertiesFile.save();
        } catch (Exception e) {
                log.log(Level.SEVERE, "[MapMarkers] : Exception while saving mapmarkers properties file.", e);
        }
		
		
        
		loadMarkers();
		
        return true;
		
		
    }
    
    public void enable() {
        if (load())
		{
            log.info("[MapMarkers] Mod Enabled.");
			etc.getInstance().addCommand("/newlabel", "[label] - Adds new label at the current position");
			etc.getInstance().addCommand("/dellabel", "[label] - Deletes label");
		}	
        else
		{
            log.info("[MapMarkers] Error while loading.");
			}
    }
    
    
    public void disable() {
		etc.getInstance().removeCommand("/newlabel");
		etc.getInstance().removeCommand("/dellabel");
        log.info("[MapMarkers] Mod Disabled.");
		
    }

    public String onLoginChecks(String user) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onLogin(Player player) {
		try {
			setMarker(player.getName(),player.getX(), player.getY(), player.getZ(), 4);
			// Update file
			writeMarkers();
		}
		catch (Exception e) {
			
		}
		
    }
    
    public synchronized boolean writeMarkers() {
        try {
		
			/* BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
            fout.write(markersArray.toString());
            fout.close();
			
			return true; */
			
			// Work out 5 minutes ago

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -5);
			date = cal.getTime();
			
			// Remove stale markers
			try {
				for(Object obj : markersArray)
				{
					try {
						JSONObject marker = (JSONObject)obj;
						oldDate = dateFormat.parse ((String)marker.get("timestamp"));
						if (oldDate.before(date)) {
							removeMarker((String)marker.get("msg"));
						}
					}
					catch(Exception e) {
						//ee.printStackTrace();
					}
				}	
			}
			catch (Exception e) {
			
			}
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
            fout.write(markersArray.toString());
            fout.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while updating label", e);
		
            return false;
        }
		
        return true;
    }
	
	
	private static int getMarkerIndex(String label){
		
		boolean inList = false;
		for (String l : markerList){
			if (l.equals(label))
				inList = true;
		}
		
		if (!inList){
			markerList.add(label);
			markersArray.add(new JSONObject());
		}
				
		return markerList.indexOf(label);
	}
		
	public void setMarker(String label, double x, double y, double z, int id)
	{
		setMarker(label, x, y, z,id, new java.util.Date());
	}
	
	public void setMarker(String label, double x, double y, double z, int id, Date markerDate)
	{
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
	
	public void removeMarker(String label)
	{
		int index = getMarkerIndex(label);
		markersArray.remove(index);
		markerList.remove(index);
	}
	
	public void loadMarkers() {
		//!TODO!Load existing markers.json into array
		JSONArray tempmarkersArray = new JSONArray();
		try {
            File inFile = new File(markersFile);
            BufferedReader fin = new BufferedReader(new FileReader(inFile));
            
			JSONParser parser = new JSONParser();
			

			try{
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
				log.log(Level.SEVERE, "[MapMarkers] : Exception while parsing line", pe);
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "[MapMarkers] : Exception while parsing line", e);
			}
			
            fin.close();
			
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while reading markers", e);
        }
        
	}
	
	    public class MapMarkersListener extends PluginListener
    {

    public boolean onCommand(Player player, String[] split) {
        if (!player.canUseCommand(split[0]))
            return false;
        
		if (split[0].equalsIgnoreCase("/newlabel")) {
		//!TODO!add error checking to look for existing labels
            if (split.length < 2) {
                player.sendMessage(Colors.Rose + "Correct usage is: /newlabel [name] ");
                return true;
            }
            
            int labelId = 3;
            String label = split[1];
            if (split.length >= 2) {
                for (int i = 2; i < split.length; i++)
                    label += split[i];
            }
            
			setMarker(label, player.getX(), player.getY(), player.getZ(), labelId);
			log.info("[MapMarkers] "+player.getName()+" created a new label called "+split[1]+".");
            player.sendMessage(Colors.Green + "Label Created!");
            
        }
		else if (split[0].equalsIgnoreCase("/dellabel")) {
		//!TODO!add error checking to delete only existing labels
            if (split.length < 2) {
                player.sendMessage(Colors.Rose + "Correct usage is: /dellabel [name] ");
                return true;
            }
            String label = split[1];
            if (split.length >= 2) {
                for (int i = 2; i < split.length; i++)
                    label += split[i];
            }
            
			removeMarker(label);
		
			log.info("[MapMarkers] "+player.getName()+" deleted a label called "+split[1]+".");
			player.sendMessage(Colors.Green + "Label Deleted!");
		
	  
        }
		//!TODO!add listlabels
		
        else
            return false;
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
					, 3*1000);

			}	
		}
		catch (Exception e) {
			
		}
		
	}
    	
    }
	
}