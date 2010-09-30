import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Arrays;





public class mapper extends Plugin {


	// From WorldMap
	public String filename;
    public final int SCALE = 1;
    public final int MARKER_PADDING = 5; //Pixels
    public final int BOX_PADDING = 1; //Pixels
    public final int IMAGE_PADDING = 10; //Pixels 
    public final int CHUNK_SIZE = 16; //Pixels
    public String worldDir;
    public String homesFile;
    public String labelsFile;
    public String playerPosFile;
    public Properties properties;
	public SimpleDateFormat dateFormat ;
	public Date date;
	public Date oldDate;
	public Calendar cal;
	public String[] lineArray;

    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
    
    public mapper() {
		properties = new Properties();
		dateFormat = new SimpleDateFormat("yyMMdd-HH.mm.ss");
	}
	
	public boolean load() {
        try {
            File f = new File("mapper.properties");
            if (f.exists())
			{
                properties.load(new FileInputStream("mapper.properties"));
				}
            else
			{
                f.createNewFile();
				}
        } catch (Exception e) {
            log.log(Level.SEVERE, "[mapper] : Exception while creating mapper properties file.", e);
        }
        
        
        worldDir = properties.getProperty("worlddir");
        homesFile = properties.getProperty("homes", "homes.txt");
        labelsFile = properties.getProperty("labels", "mapper-labels.txt");
        playerPosFile = properties.getProperty("playerpos", "mapper-playerpos.txt");
        
        String[] filesToCheck = { homesFile, labelsFile, playerPosFile };
        for (String f : filesToCheck) {
            try {
                File fileCreator = new File(f);
                if (!fileCreator.exists())
                    fileCreator.createNewFile();
            } catch (IOException e) {
                log.log(Level.SEVERE, "[mapper] : Exception while creating mapper file.", e);
            }
        }
        
        try {
            properties.store(new FileOutputStream("mapper.properties"), null);
        } catch (Exception e) {
                log.log(Level.SEVERE, "[mapper] : Exception while saving mapper properties file.", e);
        }
		
		
        
        return true;
		
		
    }
    
    public void enable() {
        if (load())
		{
            log.info("[Mapper] Mod Enabled.");
			etc.getInstance().addCommand("/newlabel", "[label] - Adds new label at the current position");
			etc.getInstance().addCommand("/dellabel", "[label] - Deletes label");
		}	
        else
		{
            log.info("[Mapper] Error while loading.");
			}
    }
    
    
    public void disable() {
		etc.getInstance().removeCommand("/newlabel");
		etc.getInstance().removeCommand("/dellabel");
        log.info("[Mapper] Mod Disabled.");
		
    }

    public String onLoginChecks(String user) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onLogin(Player player) {
		
		
        if (delLabel(playerPosFile, player.getName())) {}
        if (saveLabel(playerPosFile, player.getName(), player.getX(), player.getY(), player.getZ(), 4)) { }
    }

    public boolean onChat(Player player, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean onCommand(Player player, String[] split) {
        if (!etc.getInstance().canUseCommand(player.getName(), split[0]))
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
            
            if (saveLabel(labelsFile, label, player.getX(), player.getY(), player.getZ(), labelId)) {
                log.info("[Mapper] "+player.getName()+" created a new label called "+split[1]+".");
                player.sendMessage(Colors.Green + "Label Created!");
            } else {
                log.info("Exception while creating new label");
                player.sendMessage(Colors.Rose + "[Mapper] Error Serverside");
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
            
            if (delLabel(labelsFile, label)) {
                log.info("[Mapper] "+player.getName()+" deleted a label called "+split[1]+".");
                player.sendMessage(Colors.Green + "Label Deleted!");
            } else {
                log.info("Exception while deleting label");
                player.sendMessage(Colors.Rose + "[Mapper] Error Serverside");
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
	
	public boolean saveLabel(String file, String label, double x, double y, double z, int id) {
		date = new java.util.Date();
		//System.out.println("Current Date Time : " + dateFormat.format(date));
        return saveLabel(file, label, x, y, z, id, dateFormat.format(date));
    }
    
    public boolean saveLabel(String file, String label, double x, double y, double z, int id, String date) {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(file, true));
            fout.write(newLine + label + ":" + Double.toString(x) + ":" + Double.toString(y) + ":" + Double.toString(z) + ":" + Integer.toString(id) + ":" + date);
            fout.close();
        } catch (Exception e) {
            log.log(Level.SEVERE, "[mapper] : Exception while creating new label", e);
            return false;
        }
        return true;
    }
    
    public boolean delLabel(String file, String label) {
        try {
            File inFile = new File(file);
            File tempFile = new File(file+".tmp");
            BufferedReader fin = new BufferedReader(new FileReader(inFile));
            BufferedWriter fout = new BufferedWriter(new FileWriter(tempFile));
            String line = null;

			// Work out 5 minutes ago
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -5);
			date = cal.getTime();
			
			Date oldDate = new java.util.Date();


            while ( (line = fin.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
                else if (!line.startsWith(label)) {
					lineArray = line.split(":");
					if (lineArray.length == 6)
					{						
						//Cheftoad:1894.3887077620723:66.0:-1637.4236299882737:4:01-00.16.2
						try {
							oldDate = dateFormat.parse (lineArray[5]);
						}
						catch(Exception e) {
							//ee.printStackTrace();
						}
						if (oldDate.after(date))
						{
							fout.write(line + newLine);
							fout.flush();
						}
						
					}
					else
					{
						continue;
					}
				}
				else
				{
					continue;
				}
                
            }
            fin.close();
            fout.close();
            inFile.delete();
            tempFile.renameTo(inFile);
        } catch (Exception e) {
            log.log(Level.SEVERE, "[mapper] : Exception while deleting label", e);
            return false;
        }
        return true;
    }
    
    public boolean onBlockCreate(Player player, Block block) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public boolean onBlockDestroy(Player player, Block block) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onPlayerMoved(Player player) {
	
		if (delLabel(playerPosFile, player.getName())) {}
        if (saveLabel(playerPosFile, player.getName(), player.getX(), player.getY(), player.getZ(), 4)) { }
		
	}
	
}