import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

public class jimtest extends Plugin {


	static final Logger log = Logger.getLogger("Minecraft");
	static MinecraftServer mcworld = etc.getMCServer();
	
	static ArrayList<Integer> watergateList = new ArrayList<Integer>();

	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Integer> selectionStatus = new ArrayList<Integer>();
	static ArrayList<Integer> pointsCoordinates = new ArrayList<Integer>();
	
	static ArrayList<Integer> torchfollowints = new ArrayList<Integer>();
	
	 private Timer watergateTicker;
	 private int watergateTickInterval = 200;
	
    public void enable() {
		log.info("[Jim's Test Plugin] Mod Enabled.");
		if (watergateTickInterval > 0) {
            watergateTicker = new Timer();
            watergateTicker.schedule(new watergateTickerTask(), 400, watergateTickInterval);
        }
    }

    public void disable() {
		log.info("[Jim's Test Plugin] Mod Disabled");
        if (watergateTicker != null) {
            watergateTicker.cancel();
        }
    }
    
    public boolean onCommand(Player player,java.lang.String[] split) {
    	if (split[0].equalsIgnoreCase("/saveWatergate")) {
    		int savestatus = saveWaterGate(player.getName());
    		if(savestatus == 1) {
    			player.sendMessage("Not enough points set");
    		} else if (savestatus == 2) {
    			player.sendMessage("Watergate saved");
    		}
    	return true;
    	} else if (split[0].equalsIgnoreCase("/clearunusedWatergates")) {
    		if(watergateList.size()>0) {
    			for(int i=0; i<watergateList.size()/6;i++) {
    				if( mcworld.e.a(watergateList.get(i*6+3), watergateList.get(i*6+4), watergateList.get(i*6+5)) != 76 && 
    					mcworld.e.a(watergateList.get(i*6+3), watergateList.get(i*6+4), watergateList.get(i*6+5)) != 75 ) {
    					for(int j=0;j<6;j++) {
    						watergateList.remove(i*6);
    					}
    				}
    			}
    		}
    	player.sendMessage("Unused watergates have been cleared");
    	return true;
    	} else if (split[0].equalsIgnoreCase("/toggletorchfollow")) {
    		int torchplayerIndex = getPlayerIndex(player.getName())*5;
    		if(torchfollowints.get(torchplayerIndex) == 2) {
    			player.sendMessage("Turned torchfollow on");
    			torchfollowints.set(torchplayerIndex,1);
    		} else {
    			player.sendMessage("Turned torchfollow off");
    			torchfollowints.set(torchplayerIndex,2);
    			
    		}
    	return true;
    	} else if (split[0].equalsIgnoreCase("/trytogetunloadedblock")) {
    		int test = 0;
    		for(int i=0;i<500;i++) {
    			if(getablocktype((int)player.getX()+1000+i,(int)player.getY(),(int)player.getZ()) != 0) {
    				test = 1;
    			}
    		}
    		player.sendMessage(Integer.toString(test));
	    	return true;
    	}
    return false;
   	}

	public boolean onBlockCreate(Player player,  Block block2, Block block, int ItemInHand) {
		if(ItemInHand == -1) {
			int whichPoint = setPoint(player.getName(),block.getX(),block.getY(),block.getZ());
					if(whichPoint == 2) {
						player.sendMessage("Watergate block selected");
					} else if (whichPoint == 1) {
						player.sendMessage("Redstone trigger selected");
					}
		}
		return false;
	}
	
	public void onPlayerMove(Player player, Location from, Location to) {
    	int torchplayerIndex = getPlayerIndex(player.getName())*5;
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
						Math.abs(toZ-torchfollowints.get(torchplayerIndex+4)) > 6) {
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
					torchfollowints.set(torchplayerIndex+2,(int)to.x);
					torchfollowints.set(torchplayerIndex+3,(int)to.y);
					torchfollowints.set(torchplayerIndex+4,(int)to.z);
					}
				} else {
					setablocktype(toX,toY,toZ,50);
					torchfollowints.set(torchplayerIndex+1,1);
					torchfollowints.set(torchplayerIndex+2,(int)to.x);
					torchfollowints.set(torchplayerIndex+3,(int)to.y);
					torchfollowints.set(torchplayerIndex+4,(int)to.z);
					
				}	
			}
		}
		return;
	}
	
    private class watergateTickerTask extends TimerTask {
        public void run()
        {
            updateWaterGates();
        }
    }
    
    private void updateWaterGates() {
    	if(watergateList.size()>0) {
    		for(int i=0; i<watergateList.size()/6;i++) {
    			if(mcworld.e.a(watergateList.get(i*6+3), watergateList.get(i*6+4), watergateList.get(i*6+5)) == 76 && 
    			mcworld.e.a(watergateList.get(i*6), watergateList.get(i*6+1), watergateList.get(i*6+2)) == 47) {
    				mcworld.e.d(watergateList.get(i*6), watergateList.get(i*6+1), watergateList.get(i*6+2),0);
    			} else if(mcworld.e.a(watergateList.get(i*6+3), watergateList.get(i*6+4), watergateList.get(i*6+5)) == 75) {
    				mcworld.e.d(watergateList.get(i*6), watergateList.get(i*6+1), watergateList.get(i*6+2),47);
    			}
    		}
    	}
    }
	
	public int saveWaterGate(String playerName) {
		if(pointsCoordinates.get(getPlayerIndex(playerName)) != null) {
			int playerIndex = getPlayerIndex(playerName);
			for(int i = 0;i<6;i++) {
				watergateList.add(pointsCoordinates.get(playerIndex*6+i));
			}
			return 2;
		} else {
			return 1;
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
			selectionStatus.add(1);
			pointsCoordinates.add(null);	
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			
			torchfollowints.add(1);
			torchfollowints.add(0);
			torchfollowints.add(null);
			torchfollowints.add(null);
			torchfollowints.add(null);
		}
				
		return playerList.indexOf(playerName);
	}
	
	public static int setPoint(String playerName, int X, int Y, int Z){
		
		int index = getPlayerIndex(playerName);
		int secondPoint = selectionStatus.get(index);
		if ( selectionStatus.get(index) == 1 ){
			pointsCoordinates.set(index*6, X);
			pointsCoordinates.set(index*6+1, Y);
			pointsCoordinates.set(index*6+2, Z);
			pointsCoordinates.set(index*6+3, null);
			pointsCoordinates.set(index*6+4, null);
			pointsCoordinates.set(index*6+5, null);
			selectionStatus.set(index, 2);
			
		}
		else if ( selectionStatus.get(index) == 2){
			pointsCoordinates.set(index*6+3, X);
			pointsCoordinates.set(index*6+4, Y);
			pointsCoordinates.set(index*6+5, Z);
			selectionStatus.set(index, 1);
		}
		return selectionStatus.get(index);
	}
	
	public static int getablocktype(int X, int Y, int Z) {
		return mcworld.e.a(X,Y,Z);
	}

	public static void setablocktype(int X, int Y, int Z, int Blocktype) {
		mcworld.e.d(X,Y,Z,Blocktype);
		return;
	}
	
	public static boolean willholdtorch(int X, int Y, int Z) {
		if(isblocksolid(getablocktype(X,Y-1,Z)) ||
			isblocksolid(getablocktype(X+1,Y,Z)) ||
			isblocksolid(getablocktype(X-1,Y,Z)) ||
			isblocksolid(getablocktype(X,Y,Z+1)) ||
			isblocksolid(getablocktype(X,Y,Z-1))) {
			return true;	
		} else {
			return false;	
		}
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
