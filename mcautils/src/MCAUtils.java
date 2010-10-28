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

	 private Timer SaveAllTicker;
	 private int SaveAllTickInterval = 900000;
	
    public void enable() {
		log.info("[MCAUtils] Mod Enabled.");
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
    
    private class SaveAllTickerTask extends TimerTask {
        public void run() {
        	log.info("MCAUtils is calling a save-all");
            server.useConsoleCommand("save-all");
        }
    }
    

    public class MCAUtilsListener extends PluginListener
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