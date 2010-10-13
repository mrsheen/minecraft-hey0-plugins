import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;
import java.util.*;

public class mcau extends Plugin {
	static final Logger log = Logger.getLogger("Minecraft");
	static Server world = etc.getServer();
    private Properties  props; 
    private int[] disalloweditems;
    



	
    public void enable() {
		log.info("[mcau] Mod Enabled.");
		loadprops();
			//etc.getInstance().addCommand("/jump", "<Username> - Request a jump to a player");
			//etc.getInstance().addCommand("/accept", "<Username> - Accept a jump from a player");
    }

    public void disable() {
		//etc.getInstance().removeCommand("/jump");
		//etc.getInstance().removeCommand("/accept");
		log.info("[mcau] Mod Disabled");
    }
    
        private void loadprops()
    {
        props = new Properties();
       	props.setProperty("disalloweditems", "19,66,328,342,343");
        
        try {
             props.load(new FileInputStream("mcau.properties"));
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
        
		try {
			OutputStream propOut = new FileOutputStream(new File("mcau.properties"));
        	props.store(propOut, "Properties for the MCAU plugin");
		}
		catch(IOException e) {
             e.printStackTrace();
        }
        return;
    }


    public boolean onCommand(Player player, String[] split) {
    	if (split[0].equalsIgnoreCase("/whycantibuild")) {
    		player.sendMessage("You can't build because I hate you");
    		return true;
    	} else {
    		return false;
    	}
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
		// block items listed in the properties file
		if(disalloweditems.length > 0 ) {
			for (int i = 0; i<disalloweditems.length;i++) {
				if (itemInHand == disalloweditems[i]) {
					player.sendMessage("The use of this item has been blocked, please don't attempt to use this item in the future.");
					player.sendMessage("You may see the item still, but no one else will be able to.");
					return true;
				}
			}
			
		}
		//feather
		if ( itemInHand==288){
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
