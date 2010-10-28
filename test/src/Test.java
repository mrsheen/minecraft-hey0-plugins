import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Test extends Plugin {
	
    private TestListener listener = new TestListener();
	static final Logger log = Logger.getLogger("Minecraft");

	
    public void enable() {
		log.info("[Test] Mod Enabled.");
    }

    public void disable() {
		log.info("[Test] Mod Disabled");
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
    }
    

    public class TestListener extends PluginListener
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