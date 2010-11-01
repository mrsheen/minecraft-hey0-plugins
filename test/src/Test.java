import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Test extends Plugin {

	static final Logger log = Logger.getLogger("Minecraft");
	
	PluginRegisteredListener onportalentranceListener;
	onportalentranceListener listener_portenter = new onportalentranceListener();
	
	PluginRegisteredListener onportalcreateListener;
	onportalcreateListener listener_portcreate = new onportalcreateListener();
	
	PluginRegisteredListener onportaldestroyListener;
	onportaldestroyListener listener_portdestroy = new onportaldestroyListener();
	
	PluginRegisteredListener blockdestroyListener;
	blockdestroyListener listener_blockdestroy = new blockdestroyListener();
    
    PluginRegisteredListener blockcreateListener;
	blockcreateListener listener_blockcreate = new blockcreateListener();
	
    public void enable() {
		log.info("[Portal Test] Mod Enabled.");
		//Load portal data
    }

    public void disable() {
		log.info("[Portal Test] Mod Disabled");
		etc.getLoader().removeListener(onportalentranceListener);
		etc.getLoader().removeListener(onportalcreateListener);
		etc.getLoader().removeListener(onportaldestroyListener);
		etc.getLoader().removeListener(blockdestroyListener);
        etc.getLoader().removeListener(blockcreateListener);
    }
    
    public void initialize() {
    	onportalentranceListener = etc.getLoader().addListener(PluginLoader.Hook.PORTALWARP, listener_portenter, this, PluginListener.Priority.MEDIUM);
    	onportalcreateListener = etc.getLoader().addListener(PluginLoader.Hook.PORTALCREATE, listener_portcreate, this, PluginListener.Priority.MEDIUM);
    	onportaldestroyListener = etc.getLoader().addListener(PluginLoader.Hook.PORTALDESTROY, listener_portdestroy, this, PluginListener.Priority.MEDIUM);
    	blockdestroyListener = etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener_blockdestroy, this, PluginListener.Priority.MEDIUM);
        blockcreateListener = etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED,listener_blockcreate, this, PluginListener.Priority.MEDIUM);
    }
    
    public class onportalentranceListener extends PluginListener {
		public boolean onPortalWarp(Player player, Portal portal) {
            player.sendMessage("Warping from portal: "+portal.Name);
			//check that there is only one non-blank sign attached to the side of portalFrom
			//check that the portalTo name is valid
			//get portalTo
			//translate player from portalFrom to portalTo
			//blank the sign
			return false;
		}
    }
    
    public class onportalcreateListener extends PluginListener {
		public boolean onPortalCreate(Portal portal) {
			// if(single sign attached to top) {
				// if(!multiple rows of text) {
					// if(!text contains delimiter) {
						// add portal name to list
						// add portal naming sign to list of blocks to protect
						// write data to the storage file
						// return false;
					// } else {
						// player.sendMessage("Portal names must not contain the character ;");
						// return true;
					// }
				// } else {
					// player.sendMessage("Naming sign must only have one non-blank line");
					// return true;
				// }
			// } else {
				// player.sendMessage("Naming sign incorrectly positioned. Please put it on the top row of obisidian");
				// return true;
			// }
            return false;
		}
    }
    
    public class onportaldestroyListener extends PluginListener {
		public boolean onPortalDestroy(Portal portal) {
			// if(protected sign blockID == sign) {
				// protect sign blockID =0;
			// } else {
				// log.info("Someone changed the naming sign for portal " + portal.getPortalName());
				// protect sign blockID =0;
			// }
			//remove portal name from list
			//remove sign block from list of blocks to protect
			//delete data from file
			log.info("Destroyed portal: " + portal.Name);
			return false;
		}
    }
    
    public class blockdestroyListener extends PluginListener {
    	public boolean onBlockDestroy(Player player, Block block) {
    		// if(block.getType() == 68 || block.getType() == 63) {
    			// for(int i=0;i<protectedsigns.size()/3;i++) {
    				// if(block.getX() == protectedsigns.get(i*3) ||
    				// block.getY() == protectedsigns.get(i*3+1) ||
    				// block.getZ() == protectedsigns.get(i*3+2)) {
    					// return true;
    				// }
    			// }
    		// }
    		return false;
    	}
    }
    
    public class blockcreateListener extends PluginListener {
    	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
    		 if(itemInHand == 259) {
                // This player may have just created a portal, add to list
                // to check for subsequent onPortalCreate calls
                
    		 }
    		return false;
    	}
    }
}
