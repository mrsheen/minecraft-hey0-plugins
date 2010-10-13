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
    public Properties properties;
	public SimpleDateFormat dateFormat ;
	public Date date;
	public Date oldDate;
	public Calendar cal;
	public String[] lineArray;

    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
	
	private final Semaphore available = new Semaphore(1, true);
    
    public MapMarkers() {
		properties = new Properties();
		dateFormat = new SimpleDateFormat("yyMMdd-HH.mm.ss");
	}
	
	public boolean load() {
        try {
            File f = new File("mapmarkers.properties");
            if (f.exists())
			{
                properties.load(new FileInputStream("mapmarkers.properties"));
				}
            else
			{
                f.createNewFile();
				}
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while creating mapmarkers properties file.", e);
        }
        
        markersFile = properties.getProperty("playerpos", "world/markers.json");
        
        String[] filesToCheck = { markersFile };
        for (String f : filesToCheck) {
            try {
                File fileCreator = new File(f);
                if (!fileCreator.exists())
                    fileCreator.createNewFile();
            } catch (IOException e) {
                log.log(Level.SEVERE, "[MapMarkers] : Exception while creating mapmarkers file.", e);
            }
        }
        
        try {
            properties.store(new FileOutputStream("mapmarkers.properties"), null);
        } catch (Exception e) {
                log.log(Level.SEVERE, "[MapMarkers] : Exception while saving mapmarkers properties file.", e);
        }
		
		
        
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
			if (available.tryAcquire(3,TimeUnit.SECONDS)) {
				updatePosition(player.getName(),player.getX(), player.getY(), player.getZ(), 4);
				
			}	
		}
		catch (InterruptedException e) {
			
		}
		
    }

    public boolean onChat(Player player, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

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
            
            if (addLabel(label, player.getX(), player.getY(), player.getZ(), labelId)) {
                log.info("[MapMarkers] "+player.getName()+" created a new label called "+split[1]+".");
                player.sendMessage(Colors.Green + "Label Created!");
            } else {
                log.info("Exception while creating new label");
                player.sendMessage(Colors.Rose + "[MapMarkers] Error Serverside");
            }
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
            
            if (delLabel(label)) {
                log.info("[MapMarkers] "+player.getName()+" deleted a label called "+split[1]+".");
                player.sendMessage(Colors.Green + "Label Deleted!");
            } else {
                log.info("Exception while deleting label");
                player.sendMessage(Colors.Rose + "[MapMarkers] Error Serverside");
            }
        }
		//!TODO!add listlabels
		
        else
            return false;
        return true;
    }

    public void onBan(Player player, String reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onIpBan(Player player, String reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onKick(Player player, String reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
	
    public boolean addLabel(String label, double x, double y, double z, int id) {
        try {
            File inFile = new File(markersFile);
            BufferedReader fin = new BufferedReader(new FileReader(inFile));
            
			JSONParser parser = new JSONParser();
			JSONArray markersArray = new JSONArray();

			try{
				Object obj= parser.parse(fin);
				
				markersArray =(JSONArray)obj;
				
				//Marker newLabel = new Marker(label, x, y, z, id);
				JSONObject newLabel = new JSONObject();
				newLabel.put("msg",label);
				newLabel.put("x",x);
				newLabel.put("y",y);
				newLabel.put("z",z);
				newLabel.put("id",id);
				
				markersArray.add(newLabel);
			}
			catch(ParseException pe){
				log.log(Level.SEVERE, "[MapMarkers] : Exception while parsing line", pe);
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "[MapMarkers] : Exception while parsing line", e);
			}
			
            fin.close();
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
            fout.write(markersArray.toString());
            fout.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while deleting label", e);
            return false;
        }
        return true;
    }
    
    public boolean delLabel(String label) {
		try {
            File inFile = new File(markersFile);
            BufferedReader fin = new BufferedReader(new FileReader(inFile));
            
			date = new java.util.Date();
			
			JSONParser parser = new JSONParser();
			JSONArray markersArray = new JSONArray();

			try{
				Object obj= parser.parse(fin);
				
				markersArray =(JSONArray)obj;
				for(int i = 0; i < markersArray.size(); i++)
				{
					JSONObject marker = (JSONObject)markersArray.get(i);
					if (marker.get("msg").equals(label)) {
						markersArray.remove(i);
						
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
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
            fout.write(markersArray.toString());
            fout.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while deleting label", e);
            return false;
        }
        return true;
        
    }
	
	
	
    public synchronized boolean updatePosition(String label, double x, double y, double z, int id) {
        try {
            File inFile = new File(markersFile);
            BufferedReader fin = new BufferedReader(new FileReader(inFile));
            
			date = new java.util.Date();
			boolean updated=false;
			JSONParser parser = new JSONParser();
			JSONArray markersArray = new JSONArray();

			try{
				Object obj= parser.parse(fin);
				
				markersArray =(JSONArray)obj;
				for(int i = 0; i < markersArray.size(); i++)
				{
					try {
						JSONObject marker = (JSONObject)markersArray.get(i);
						if (marker.get("msg").equals(label)) {
							// Line matches label, update
							marker.put("x",x);
							marker.put("y",y);
							marker.put("z",z);
							marker.put("timestamp",dateFormat.format(date));
							updated=true;
						}
						else{
							try {
								oldDate = dateFormat.parse ((String)marker.get("timestamp"));
								if (oldDate.after(date)) {
									markersArray.remove(i);
								}
							}
							catch(Exception e) {
								//ee.printStackTrace();
							}
						}		
					}
					catch(Exception e) {
								//ee.printStackTrace();
					}
					
				}	
				if (!updated) {
					JSONObject newLabel = new JSONObject();
					newLabel.put("msg",label);
					newLabel.put("x",x);
					newLabel.put("y",y);
					newLabel.put("z",z);
					newLabel.put("id",id);
					
					markersArray.add(newLabel);
				}
			}
			catch(ParseException pe){
				log.log(Level.SEVERE, "[MapMarkers] : Exception while parsing line", pe);
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "[MapMarkers] : Exception while parsing line", e);
			}
			
            fin.close();
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(markersFile));
            fout.write(markersArray.toString());
            fout.close();
            
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MapMarkers] : Exception while updating label", e);
			available.release();
            return false;
        }
		available.release();
        return true;
    }
    
    public boolean onBlockCreate(Player player, Block block) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean onBlockDestroy(Player player, Block block) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
	
	public void onPlayerMove(Player player, Location from, Location to) {
		try {
			if (available.tryAcquire()) {
				updatePosition(player.getName(),to.x, to.y, to.z, 4);
			}	
		}
		catch (Exception e) {
			
		}
	}
	
	private class Marker  {
		String msg; 
		double x ;
		double y;
		double z ;
		int id ;		
		
		Marker (String label, double x, double y, double z, int id) {
			this.msg = label; 
			this.x = x;
			this.y = y;
			this.z = z;
			this.id = id;	
		}
	}
	
}