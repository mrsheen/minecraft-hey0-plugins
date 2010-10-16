import java.util.logging.Level;
import java.util.logging.Logger;

public class furnacequickfix extends Plugin {

	static final Logger log = Logger.getLogger("Minecraft");
	
	int[] smeltable = {4,12,14,15,337};
	int[] smelted = {1,20,266,265,336};
	
    public void enable() {
		log.info("[furnacequickfix] Mod Enabled.");
    }

    public void disable() {
		log.info("[furnacequickfix] Mod Disabled");
    }
    public boolean onBlockDestroy(Player player,Block block) {
    	if(block.getType() == 61 || block.getType() == 62) {
    		if(block.getStatus() == 0) {
    			int iteminhand = player.getItemInHand();
    			for(int i=0;i<smeltable.length;i++) {
    				if(iteminhand == smeltable[i]) {
    					disregardmatsacquiresmelted(player,i);
    					return true;
    				}
    			}
    		}
    	}
    	
    	return false;
    }
    
    public void disregardmatsacquiresmelted (Player player, int mat) {
    	Inventory playerinventory = player.getInventory();
    	playerinventory.removeItem(new Item(smeltable[mat],1));
    	player.giveItem(new Item(smelted[mat],1));
    	playerinventory.removeItem(35);
    	playerinventory.updateInventory();
    	return;
    }
}
