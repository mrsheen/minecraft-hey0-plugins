import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

public class MinersHelmet extends Plugin {

    private MinersHelmetListener listener = new MinersHelmetListener();

	static final Logger log = Logger.getLogger("Minecraft");
	static MinecraftServer mcworld = etc.getMCServer();

	static ArrayList<String> playerList = new ArrayList<String>();
	
	static ArrayList<Integer> torchfollowints = new ArrayList<Integer>();
	
    public void enable() {
		log.info("[Jim's Test Plugin] Mod Enabled.");
    }

    public void disable() {
		log.info("[Jim's Test Plugin] Mod Disabled");
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
    	etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
    }
    
        public class MinersHelmetListener extends PluginListener
    {
    
    public boolean onCommand(Player player,java.lang.String[] split) {
    	if (split[0].equalsIgnoreCase("/minehelmet") || split[0].equalsIgnoreCase("/mh")) {
    		int torchplayerIndex = getPlayerIndex(player.getName())*6;
    		if(torchfollowints.get(torchplayerIndex) == 2) {
    			player.sendMessage("You now have a miners helmet!");
    			torchfollowints.set(torchplayerIndex,1);
    		} else {
    			player.sendMessage("Aww. No more helmet for you.");
    			torchfollowints.set(torchplayerIndex,2);
    		}
    	return true;
    	} else if (split[0].equalsIgnoreCase("/mhleash")) {
    		if(split.length == 2) {
    			
    			int torchplayerIndex = getPlayerIndex(player.getName())*6;
    			 try
    				{
    				  torchfollowints.set(torchplayerIndex+5, Integer.parseInt(split[1]));
    				}
    				catch (NumberFormatException nfe)
    				{
    				  player.sendMessage("Input was not a number");
    				}
    			return true;
    		} else {
    			player.sendMessage("Incorrect number of inputs");
    			return true;
    		}
    	}
    return false;
   	}
	
	public void onPlayerMove(Player player, Location from, Location to) {
    	int torchplayerIndex = getPlayerIndex(player.getName())*6;
    	//if player has torch turned on
		if(torchfollowints.get(torchplayerIndex) == 1) {
			//if the new block will hold a torch
			int toX = (int)to.x;
			int toY = (int)to.y;
			int toZ = (int)to.z;
			if(willholdtorch(toX,toY,toZ)) {
				//if there is an old torch
				if(torchfollowints.get(torchplayerIndex+1) == 1) {
					//if we are more than 5 blocks away from it
					if(Math.abs(toX-torchfollowints.get(torchplayerIndex+2)) + 
						Math.abs(toY-torchfollowints.get(torchplayerIndex+3)) + 
						Math.abs(toZ-torchfollowints.get(torchplayerIndex+4)) > torchfollowints.get(torchplayerIndex+5)) {
						//if the torch is still there remove it
						if(getablocktype(torchfollowints.get(torchplayerIndex+2),
									torchfollowints.get(torchplayerIndex+3),
									torchfollowints.get(torchplayerIndex+4))==50) {
									setablocktype(torchfollowints.get(torchplayerIndex+2),
									torchfollowints.get(torchplayerIndex+3),
									torchfollowints.get(torchplayerIndex+4),0);
									}
						torchfollowints.set(torchplayerIndex+1,0);
				
					//place a torch
					setablocktype(toX,toY,toZ,50);
					torchfollowints.set(torchplayerIndex+1,1);
					torchfollowints.set(torchplayerIndex+2,toX);
					torchfollowints.set(torchplayerIndex+3,toY);
					torchfollowints.set(torchplayerIndex+4,toZ);
					}
				} else {
					setablocktype(toX,toY,toZ,50);
					torchfollowints.set(torchplayerIndex+1,1);
					torchfollowints.set(torchplayerIndex+2,toX);
					torchfollowints.set(torchplayerIndex+3,toY);
					torchfollowints.set(torchplayerIndex+4,toZ);
					
				}	
			}
		}
		return;
	}
	
    }

	private static int getPlayerIndex(String playerName){
		
		boolean inList = false;
		for (String p : playerList){
			if (p==playerName)
				inList = true;
		}
		
		if (!inList){
			playerList.add(playerName);
			
			torchfollowints.add(2);
			torchfollowints.add(0);
			torchfollowints.add(null);
			torchfollowints.add(null);
			torchfollowints.add(null);
			torchfollowints.add(0);
		}
				
		return playerList.indexOf(playerName);
	}
		
	public static int getablocktype(int X, int Y, int Z) {
		return mcworld.e.a(X,Y,Z);
	}

	public static void setablocktype(int X, int Y, int Z, int Blocktype) {
		mcworld.e.d(X,Y,Z,Blocktype);
		return;
	}
	
	public static boolean willholdtorch(int X, int Y, int Z) {
		
		if(getablocktype(X,Y,Z)==0) {
			if(isblocksolid(getablocktype(X,Y-1,Z)) ||
				isblocksolid(getablocktype(X+1,Y,Z)) ||
				isblocksolid(getablocktype(X-1,Y,Z)) ||
				isblocksolid(getablocktype(X,Y,Z+1)) ||
				isblocksolid(getablocktype(X,Y,Z-1))) {
				return true;	
			}
		}
		
		return false;
	}
	
	public static boolean isblocksolid(int type) {
		if(type == 1 ||
			type == 2 ||
			type == 3 ||
			type == 4 ||
			type == 5 ||
			type == 17 ||
			type == 18 ||
			type == 12) {
				return true;
		} else {
			return false;	
		}
	}
}
