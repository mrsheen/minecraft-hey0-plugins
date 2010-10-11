import java.util.logging.Level;
import java.util.logging.Logger;

public class blockitemuse extends Plugin {


	static final Logger log = Logger.getLogger("Minecraft");

	
    public void enable() {
		log.info("[blockitemplace] Mod Enabled.");
    }

    public void disable() {
		log.info("[blockitemplace] Mod Disabled");
    }
    

	public boolean onBlockCreate(Player player,Block blockPlaced,  Block blockClicked, int itemInHand) {
		if( itemInHand==328 || itemInHand==66 ) {
			player.sendMessage("The use of this item has been blocked because the item could crash clients");
			return true;
		} else {
			return false;	
		}
	}

}
