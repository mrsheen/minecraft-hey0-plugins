import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

public class Mcautilities extends Plugin {
	
    private McautilitiesListener listener = new McautilitiesListener();
	static final Logger log = Logger.getLogger("Minecraft");
	private Server server = etc.getServer();

	 private Timer SaveAllTicker;
	 private int SaveAllTickInterval = 900000;
	
    public void enable() {
		log.info("[Mcautilities] Mod Enabled.");
		SaveAllTicker = new Timer();
		SaveAllTicker.schedule(new SaveAllTickerTask(), SaveAllTickInterval, SaveAllTickInterval);
    }

    public void disable() {
		log.info("[Mcautilities] Mod Disabled");
        if (SaveAllTicker != null) {
            SaveAllTicker.cancel();
        }
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
    }
    
    private class SaveAllTickerTask extends TimerTask {
        public void run() {
        	log.info("Mcautilities is calling a save-all");
            server.useConsoleCommand("save-all");
        }
    }
    

    public class McautilitiesListener extends PluginListener
    {
    	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
			if(itemInHand==259 || itemInHand==51) {
				if(player.canUseCommand("/usefire")) {
					player.sendMessage("Be careful with that fire");
					return false;
				} else {
					player.sendMessage("You can't use fire. Ask an admin to use it for you");
					return true;
				}
			}
			return false;
		}
    }
    
}