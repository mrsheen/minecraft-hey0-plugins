import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Spleef extends Plugin {


    private SpleefListener listener = new SpleefListener();

	static final Logger log = Logger.getLogger("Minecraft");
	static Location flypoint = new Location();
	static ArrayList<Location> locationList = new ArrayList<Location>(1);
	
    public void enable() {
		log.info("[Spleef] Mod Enabled.");
		SpleefData.loadSpleefData();
			etc.getInstance().addCommand("/saveSpleef", "<AirBlockID> <SolidBlockID> <Spleef Name> - Creates a Spleef area which will be reset by the trigger block");
			etc.getInstance().addCommand("/listSpleefs", "Lists the all the Spleef areas");
    }

    public void disable() {
		etc.getInstance().removeCommand("/saveSpleef");
		etc.getInstance().removeCommand("/listSpleefs");
		log.info("[Spleef] Mod Disabled");
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
    	etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
    }


    public class SpleefListener extends PluginListener
    {

	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand){
		if ( (itemInHand==285) && player.canUseCommand("/saveSpleef")){
					Integer whichPoint = SpleefData.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
					if(whichPoint == 2) {
						player.sendMessage("First point is set");
					} else if (whichPoint == 3) {
						player.sendMessage("Second point is set");
					} else {
						player.sendMessage("Trigger point is set"); 
					}
					return true;
			} else if (itemInHand==318) {
				if(blockClicked.getType()!=6 && 
					blockClicked.getType()!=8 && 
					blockClicked.getType()!=9 && 
					blockClicked.getType()!=10 && 
					blockClicked.getType()!=11 && 
					blockClicked.getType()!=12 && 
					blockClicked.getType()!=13 && 
					blockClicked.getType()!=14 && 
					blockClicked.getType()!=15 && 
					blockClicked.getType()!=16 && 
					blockClicked.getType()!=19 && 
					blockClicked.getType()!=37 && 
					blockClicked.getType()!=38 && 
					blockClicked.getType()!=39 && 
					blockClicked.getType()!=40 && 
					blockClicked.getType()!=41 && 
					blockClicked.getType()!=42 && 
					blockClicked.getType()!=45 && 
					blockClicked.getType()!=48 && 
					blockClicked.getType()!=50 && 
					blockClicked.getType()!=52 && 
					blockClicked.getType()!=55 && 
					blockClicked.getType()!=56 && 
					blockClicked.getType()!=57 && 
					blockClicked.getType()!=59 && 
					blockClicked.getType()!=63 && 
					blockClicked.getType()!=68 && 
					blockClicked.getType()!=69 && 
					blockClicked.getType()!=70 && 
					blockClicked.getType()!=71 && 
					blockClicked.getType()!=72 && 
					blockClicked.getType()!=73 && 
					blockClicked.getType()!=74 && 
					blockClicked.getType()!=75 && 
					blockClicked.getType()!=76 && 
					blockClicked.getType()!=77 && 
					blockClicked.getType()!=79 && 
					blockClicked.getType()!=81 && 
					blockClicked.getType()!=82 && 
					blockClicked.getType()!=83 && 
					blockClicked.getType()!=85) {
						
						boolean worked = SpleefData.trytoResetArena(player,blockClicked);
						if(worked) {
							player.sendMessage("Arena reset");
						} else {
							
						}
					} else {
					player.sendMessage("Disallowed Item type: Either for profit or lag");
					}
				return true;
			}
			
		
				
		return false;
	}


    public boolean onCommand(Player player, String[] split) {
        if (!player.canUseCommand(split[0])) {
            return false;
        }
        if (split[0].equalsIgnoreCase("/saveSpleef")) {
        	if (split.length == 4) {
	        	SpleefData.addSpleefBlocks(player.getName(),Integer.parseInt(split[1]),Integer.parseInt(split[2]),split[3]);
	        	player.sendMessage("Spleef Saved");
        	}
			return true;
        } else if (split[0].equalsIgnoreCase("/listSpleefs")) {
        	SpleefData.listthespleefs(player);
        	return true;
        } else if (split[0].equalsIgnoreCase("/removeSpleef")) {
        	boolean worked = false;
        	if (split.length>1) {
        		worked = SpleefData.removeSpleef(split[1]);
        	}
        	if( worked ) {
        		player.sendMessage("Spleef removed");
        	} else {
        		player.sendMessage("Failed to remove spleef");
        	}
        	return true;
        } else {
            return false;
        }
    }
    }

}
