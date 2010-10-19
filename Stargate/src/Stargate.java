import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Stargate extends Plugin {


    private StargateListener listener = new StargateListener();

	static final Logger log = Logger.getLogger("Minecraft");
	
    public void enable() {
		log.info("[Stargate] Mod Enabled.");
    }

    public void disable() {
		log.info("[Stargate] Mod Disabled");
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
    	etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
    	etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
    }


    public class StargateListener extends PluginListener
    {

	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand){
		if ( (itemInHand==285) && player.canUseCommand("/saveStargate")){
					Integer whichPoint = StargateData.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
					if(whichPoint == 2) {
						player.sendMessage("First corner of teleport area set");
					} else if (whichPoint == 3) {
						player.sendMessage("Second corner of teleport area set");
					} else if (whichPoint == 4) {
						player.sendMessage("First corner of landing area set");
					} else {
						player.sendMessage("Second corner of landing area set"); 
					}
					return true;
			}
			return false;
		}
			


    public boolean onCommand(Player player, String[] split) {
        if (!player.canUseCommand(split[0])) {
            return false;
        }
        if (split[0].equalsIgnoreCase("/saveStargate")) {
        	if (split.length == 3) {
	        	StargateData.Savestargate(player,split);
        	}
			return true;
        } else if (split[0].equalsIgnoreCase("/listStargates")) {
        	return true;
        } else if (split[0].equalsIgnoreCase("/removeStargate")) {
        	boolean worked = false;
        	if (split.length>1) {
        	}
        	if( worked ) {
        		player.sendMessage("Stargate removed");
        	} else {
        		player.sendMessage("Failed to remove Stargate");
        	}
        	return true;
        } else {
            return false;
        }
    }
    
    public void onPlayerMove(Player player,Location from,Location to) {
    	StargateData.Checkplayerpos(to,player);
    	return;
    }
    }

}
