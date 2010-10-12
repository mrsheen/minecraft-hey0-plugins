import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class mcau extends Plugin {
	static final Logger log = Logger.getLogger("Minecraft");
	static Server world = etc.getServer();

	
    public void enable() {
		log.info("[mcau] Mod Enabled.");
			//etc.getInstance().addCommand("/jump", "<Username> - Request a jump to a player");
			//etc.getInstance().addCommand("/accept", "<Username> - Accept a jump from a player");
    }

    public void disable() {
		//etc.getInstance().removeCommand("/jump");
		//etc.getInstance().removeCommand("/accept");
		log.info("[mcau] Mod Disabled");
    }
    


    public boolean onCommand(Player player, String[] split) {
    	if (split[0].equalsIgnoreCase("/whycantibuild")) {
    		player.sendMessage("You can't build because I hate you");
    		return true;
    	} else {
    		return false;
    	}
        /*
        if (!player.canUseCommand(split[0])) {
            return false;
        }

        if (split[0].equalsIgnoreCase("/jump")) {
        } else if (split[0].equalsIgnoreCase("/accept")) {
        }
        else {
            return false;
        }*/
    }

    public String onLoginChecks(String user) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onLogin(Player player) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean onChat(Player player, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public List<Player> getPlayersWithPrefix(String playerName)
    {
    	List<Player> players = new ArrayList<Player>();
    	for (Player p : etc.getServer().getPlayerList()) {
            if (p != null) {
                if (p.getName().toLowerCase().startsWith(playerName)) {
                   players.add(p);
                }
                	
            }
        }
    	return players;
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
    
    
	public boolean onBlockCreate(Player player,Block blockPlaced,  Block blockClicked, int itemInHand) {
		// block minecarts and tracks
		if( itemInHand==328 || itemInHand==66 || itemInHand==19) {
			player.sendMessage("The use of this item has been blocked, please don't attempt to use this item in the future.");
			player.sendMessage("You may see the item still, but no one else will be able to.");
			return true;
		} 		// Feather 
		else if ( itemInHand==288){
			/*boolean whichPoint = Cuboid.setPoint(player.getName(), blockClicked.getX()+8, blockClicked.getY()+128, blockClicked.getZ()+8);
			boolean throwaway2 = Cuboid.setPoint(player.getName(), blockClicked.getX()-8, blockClicked.getY()-8, blockClicked.getZ()-8);
			if (whichPoint) {
				boolean throwaway1 = Cuboid.setPoint(player.getName(), blockClicked.getX()+8, blockClicked.getY()+128, blockClicked.getZ()+40);
			}
			int[] fireairparams = {51,0}; // change fire blocks to air
			*/
			for ( int i = (blockClicked.getX()-8); i<= blockClicked.getX()+8; i++ ){
				for ( int j = blockClicked.getY(); j<= 128; j++ ){
					for ( int k = (blockClicked.getZ()-8); k<= (blockClicked.getZ()+8); k++ ){
						if( world.getBlockIdAt(i, j, k) == 51 ){
							world.setBlockAt(0,i,j,k);
						}
					}
				}
			}
			return true;
		}
		// String
		else if ( itemInHand==287){
			for ( int i = (blockClicked.getX()-2); i<= blockClicked.getX()+2; i++ ){
				for ( int j = blockClicked.getY(); j<= 128; j++ ){
					for ( int k = (blockClicked.getZ()-2); k<= (blockClicked.getZ()+2); k++ ){
						if( world.getBlockIdAt(i, j, k) == 11 || world.getBlockIdAt(i, j, k) == 10){
							world.setBlockAt(0,i,j,k);
						}
					}
				}
			}
			return true;
		}
		
		else {
			return false;	
		}
	}

	public boolean onBlockDestroy(Player arg0, Block arg1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
