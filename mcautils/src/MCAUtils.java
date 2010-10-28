import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

public class MCAUtils extends Plugin {
	
    private MCAUtilsListener listener = new MCAUtilsListener();
	static final Logger log = Logger.getLogger("Minecraft");
	private Server server = etc.getServer();
    private Properties  props; 
    private int[] disalloweditems;

	private Timer SaveAllTicker;
	private long SaveAllTickInterval = 3600000;
	
    public void enable() {
		log.info("[MCAUtils] Mod Enabled.");
		loadprops();
		SaveAllTicker = new Timer();
		SaveAllTicker.schedule(new SaveAllTickerTask(), SaveAllTickInterval, SaveAllTickInterval);
    }

    public void disable() {
		log.info("[MCAUtils] Mod Disabled");
        if (SaveAllTicker != null) {
            SaveAllTicker.cancel();
        }
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
    }
    
    private void loadprops()
    {
        props = new Properties();
       	props.setProperty("disalloweditems", "19,66,328,342,343");
       	props.setProperty("saveallintervalinminutes", "60");
        
        try {
             props.load(new FileInputStream("MCAUtils.properties"));
       	}
        catch(IOException e) {
             e.printStackTrace();
        }
        
        if(props.getProperty("disalloweditems") != "") {
       		String[] stringdisalloweditems = props.getProperty("disalloweditems").split(",");
       		disalloweditems = new int[stringdisalloweditems.length];
       		for (int i=0;i<stringdisalloweditems.length;i++) {
       			disalloweditems[i] = Integer.parseInt(stringdisalloweditems[i]);
       		}
        }
        
        if(props.getProperty("saveallintervalinminutes") != "") {
        	try {
        		SaveAllTickInterval = Long.parseLong(props.getProperty("saveallintervalinminutes").trim()) * 60000;
        	} catch (NumberFormatException nfe) {
        		log.info(props.getProperty("saveallintervalinminutes") + " is not a valid time in minutes");
        	}
        }
        
		try {
			OutputStream propOut = new FileOutputStream(new File("MCAUtils.properties"));
        	props.store(propOut, "Properties for the MCAUtils plugin");
		}
		catch(IOException e) {
             e.printStackTrace();
        }
        return;
    }
    
    private class SaveAllTickerTask extends TimerTask {
        public void run() {
        	log.info("MCAUtils is calling a save-all");
            server.useConsoleCommand("save-all");
        }
    }
    
    public class MCAUtilsListener extends PluginListener
    {
    	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
    		//whitelist fire
			if(itemInHand==259 || itemInHand==51) {
				if(player.canUseCommand("/usefire")) {
					player.sendMessage("Be careful with that fire");
					return false;
				} else {
					player.sendMessage("You can't use fire. Ask an admin to use it for you");
					return true;
				}
			}
			//feather
			else if ( itemInHand==288 && player.canUseCommand("/allowfirefeather")){
				for ( int i = (blockClicked.getX()-8); i<= blockClicked.getX()+8; i++ ){
					for ( int j = blockClicked.getY(); j<= 128; j++ ){
						for ( int k = (blockClicked.getZ()-8); k<= (blockClicked.getZ()+8); k++ ){
							if( server.getBlockIdAt(i, j, k) == 51 ){
								server.setBlockAt(0,i,j,k);
							}
						}
					}
				}
				return true;
			}
			// String
			else if ( itemInHand==287 && player.canUseCommand("/allowlavastring")){
				for ( int i = (blockClicked.getX()-2); i<= blockClicked.getX()+2; i++ ){
					for ( int j = blockClicked.getY(); j<= 128; j++ ){
						for ( int k = (blockClicked.getZ()-2); k<= (blockClicked.getZ()+2); k++ ){
							if( server.getBlockIdAt(i, j, k) == 11 || server.getBlockIdAt(i, j, k) == 10){
								server.setBlockAt(0,i,j,k);
							}
						}
					}
				}
				return true;
			}
			return false;
		}
    }
    
}