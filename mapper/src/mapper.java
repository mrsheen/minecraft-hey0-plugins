import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



public class mapper implements Plugin {
    protected static final Logger log = Logger.getLogger("Minecraft");
    private WorldMap wm;
    private final String newLine = System.getProperty("line.separator");
    
    public mapper() {
        wm = WorldMap.getInstance();
    }
    
    public void enable() {
        if (wm.load())
            log.info("[Mapper] Mod Enabled.");
        else
            log.info("[Mapper] Error while loading.");
    }
    
    
    public void disable() {
        log.info("[Mapper] Mod Disabled.");
    }

    public String onLoginChecks(String user) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onLogin(Player player) {
        if (delLabel(wm.playerPosFile, player.getName())) {}
        if (saveLabel(wm.playerPosFile, player.getName(), player.getX(), player.getY(), player.getZ(), 4)) { }
    }

    public boolean onChat(Player player, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean onCommand(Player player, String[] split) {
        if (!etc.getInstance().canUseCommand(player.getName(), split[0]))
            return false;
        
		if (split[0].equalsIgnoreCase("/newlabel")) {
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
            
            if (saveLabel(wm.labelsFile, label, player.getX(), player.getY(), player.getZ(), labelId)) {
                log.info("[Mapper] "+player.getName()+" created a new label called "+split[1]+".");
                player.sendMessage(Colors.Green + "Label Created!");
            } else {
                log.info("Exception while creating new label");
                player.sendMessage(Colors.Rose + "[Mapper] Error Serverside");
            }
        }
		else if (split[0].equalsIgnoreCase("/dellabel")) {
            if (split.length < 2) {
                player.sendMessage(Colors.Rose + "Correct usage is: /dellabel [name] ");
                return true;
            }
            String label = split[1];
            if (split.length >= 2) {
                for (int i = 2; i < split.length; i++)
                    label += split[i];
            }
            
            if (delLabel(wm.labelsFile, label)) {
                log.info("[Mapper] "+player.getName()+" deleted a label called "+split[1]+".");
                player.sendMessage(Colors.Green + "Label Deleted!");
            } else {
                log.info("Exception while deleting label");
                player.sendMessage(Colors.Rose + "[Mapper] Error Serverside");
            }
        }
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
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(file, true));
            fout.write(newLine + label + ":" + Double.toString(x) + ":" + Double.toString(y) + ":" + Double.toString(z) + ":" + Integer.toString(id));
            fout.close();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while creating new label", e);
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
            while ( (line = fin.readLine()) != null) {
                if (!line.startsWith(label)) {
                    fout.write(line + newLine);
                    fout.flush();
                }
            }
            fin.close();
            fout.close();
            inFile.delete();
            tempFile.renameTo(inFile);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while deleting label", e);
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
}