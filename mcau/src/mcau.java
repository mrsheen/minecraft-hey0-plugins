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
    
	
	static ArrayList<String> whycantibuildplayerList = new ArrayList<String>();
	static ArrayList<Block> whycantibuildlastBlock = new ArrayList<Block>();
	
	//protected area stuff
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Boolean> lastStatus = new ArrayList<Boolean>();
	static ArrayList<Block> correspondingBloc = new ArrayList<Block>();

	
    public void enable() {
		log.info("[mcau] Mod Enabled.");
		loadprops();
		
		//protected area stuff
		ProtectedArea.loadProtectedAreas();
		ProtectedArea.loadClaimedAreas();
		etc.getInstance().addCommand("/protect", "<player> g:<groupName> <name of the cuboid>");
		etc.getInstance().addCommand("/listprotected", "- List the protected cuboids by name"); 
		etc.getInstance().addCommand("/removeprotected", "<protected cuboid name> - Remove the protected cuboid");
		etc.getInstance().addCommand("/toggleprot", "- Turns on/off the cuboid protection (worldwide)");
		etc.getInstance().addCommand("/stakeclaim", "<name of area> - Stake a claim");
		etc.getInstance().addCommand("/grantclaim", "<playername> <name of area> - Grant a claim");
		etc.getInstance().addCommand("/creload", "- Reloads the data file, in case you edited it manually");
		etc.getInstance().addCommand("/whycantibuild", "- Explains why new users are unable to build");
    }

    public void disable() {
		etc.getInstance().removeCommand("/creload");
		etc.getInstance().removeCommand("/stakeclaim");
		etc.getInstance().removeCommand("/grantclaim");
		etc.getInstance().removeCommand("/protect");
		etc.getInstance().removeCommand("/listprotected"); 
		etc.getInstance().removeCommand("/removeprotected");
		etc.getInstance().removeCommand("/toggleprot");
		etc.getInstance().removeCommand("/whycantibuild");
		
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
		String playerName = player.getName();

		if (player.canUseCommand("/protect")){
			if ( split[0].equalsIgnoreCase("/protect" ) ){
				//
				return false;
				/*
				if (Cuboidold.isReady(playerName, true)){
					String parameters = "";
					int paramSize = split.length;
					if (paramSize > 2){
						for (short i=1; i<paramSize-1; i++){
							parameters += " "+split[i];
						}
						String cuboidName = split[paramSize-1].trim().toLowerCase();
						short returnCode = ProtectedArea.protegerCuboid(playerName, parameters.toLowerCase(), cuboidName);
						if (returnCode==0){
							player.sendMessage(Colors.LightGreen + "New protected zone created.");
							player.sendMessage(Colors.LightGreen + "Name : "+Colors.White+cuboidName);
							player.sendMessage(Colors.LightGreen + "Owners :"+Colors.White+parameters);
						}
						else if (returnCode==1){
							player.sendMessage(Colors.Rose + "This name is already linked to a protected zone.");
						}
						else if (returnCode==0){
							player.sendMessage(Colors.Rose + "Error while adding the protected Area.");
							player.sendMessage("Check server logs for more info");
						}
					}
					else{
					}
						player.sendMessage(Colors.Yellow + "You need to specify at least one player or group, and a name.");
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;*/
			}
			else if ( split[0].equalsIgnoreCase("/grantclaim" ) ){
				//!TODO! update this
				int paramSize = split.length;
				if (paramSize == 2){
					String claimerName = split[1].trim().toLowerCase();
					String cuboidName = Cuboidold.getClaimName(claimerName);
					//String claimerName = cuboidName.split("_")[0];
					if (Cuboidold.isReady(claimerName, true)){
						short returnCode = ProtectedArea.protegerCuboid(claimerName, " "+claimerName, cuboidName);
						if (returnCode==0){
							player.sendMessage(Colors.LightGreen + "New protected zone created.");
							player.sendMessage(Colors.LightGreen + "Name : "+Colors.White+cuboidName);
							player.sendMessage(Colors.LightGreen + "Owners :"+Colors.White+claimerName);
							Cuboidold.hideClaim(claimerName);
							Cuboidold.setPoint(claimerName, 0, 0, 0, 0);
							Cuboidold.setPoint(claimerName, 0, 0, 0, 0);
							Cuboidold.setClaimName(claimerName, "");
							ProtectedArea.removeClaim(claimerName);
							for  (Player p : etc.getServer().getPlayerList() ) {
								if (p.getName().equalsIgnoreCase(claimerName)){
									p.sendMessage("Claim granted :)");
									p.sendMessage("Type /listprotected to check your new land");
								}
							}
							
						}
						else if (returnCode==1){
							player.sendMessage(Colors.Rose + "This name is already linked to a protected zone.");
						}
						else if (returnCode==0){
							player.sendMessage(Colors.Rose + "Error while adding the protected Area.");
							player.sendMessage("Check server logs for more info");
						}
					}
					else{
						player.sendMessage(Colors.Rose + claimerName + " has no waiting claims");
					}
				}
				else{
					player.sendMessage(Colors.Yellow + "Usage : /grantclaim <claimant>");
				}
				
				return true;
			}
			else if ( split[0].equalsIgnoreCase("/checkclaim" ) ){
				//!TODO! update this
				int paramSize = split.length;
				if (paramSize == 2){
					String claimerName = split[1].trim().toLowerCase();
					//String cuboidName = split[1].trim().toLowerCase();
					//String claimerName = cuboidName.split("_")[0];
					if (Cuboidold.isReady(claimerName, true)){
						// Replace A-G of cuboid with sponge
						Cuboidold.showClaim(claimerName);
						player.sendMessage(Colors.LightGreen + "Cuboid A-G blocks replaced with sponge");
						player.sendMessage(Colors.LightGreen + "Either /grantclaim " + claimerName + " or /rejectclaim " + claimerName);
						player.sendMessage(Colors.LightGreen + " (sponge will stay until either command is recieved)");
					}
					else{
						player.sendMessage(Colors.Rose + claimerName + " has no waiting claims");
					}
				}
				else{
					player.sendMessage(Colors.Yellow + "Usage : /checkclaim <claimant>");
					player.sendMessage(Colors.Yellow + "Waiting claims: " + Cuboidold.getClaims());
				}
				
				return true;
			}
			else if ( split[0].equalsIgnoreCase("/rejectclaim" ) ){
				//!TODO! update this
				int paramSize = split.length;
				if (paramSize >= 2){
					String claimerName = split[1].trim().toLowerCase();
					
					Cuboidold.hideClaim(claimerName);
					Cuboidold.setPoint(claimerName, 0, 0, 0, 0);
					Cuboidold.setPoint(claimerName, 0, 0, 0, 0);
					Cuboidold.setClaimName(claimerName, "");
					ProtectedArea.removeClaim(claimerName);
					
					for  (Player p : etc.getServer().getPlayerList() ) {
						if (p.getName().trim().equalsIgnoreCase(claimerName)){
							String optMessage = "";
							if (paramSize > 2){
								for (short i=2; i<paramSize; i++){
									optMessage += " "+split[i];
								}
								p.sendMessage("Claim rejected, reason: " + optMessage + " :(");
							}
							else
							{
								p.sendMessage("Claim rejected :(");
							}
						}
					}
				
				}
				else{
					player.sendMessage(Colors.Yellow + "Usage : /rejectclaim <claimant> [reason]");
					
				}
				
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/removeprotected")){
				//
				return false;
				/*
				if(split.length == 2){
					short returnCode = ProtectedArea.removeProtectedZone(playerName, split[1].trim().toLowerCase() );
					if (returnCode == 0){
						player.sendMessage(Colors.Green + "The protected area has been removed");
					}
					else if (returnCode == 1){
						player.sendMessage(Colors.Rose + "No protected area has this name");
					}
					else if (returnCode == 2){
						player.sendMessage(Colors.Rose + "Exception while removing the protected area...");
					}
					else if (returnCode == 3){
						player.sendMessage(Colors.Rose + "protectedCuboids.txt has been deleted !");
					}
				}
				else{
					player.sendMessage(Colors.Rose + "Usage : /removeprotected <protected area name>");
				}
				return true;*/
			}
			
			else if (split[0].equalsIgnoreCase("/listprotected")){
				//
				return false;
				/*
				String cuboidList = ProtectedArea.listerCuboids();
				player.sendMessage(Colors.Yellow + "Protected areas"+Colors.White+" :"+cuboidList);
				return true;*/
			}
			
			else if (split[0].equalsIgnoreCase("/toggleprot")){
				//
				return false;
				/*
				ProtectedArea.toggle=!ProtectedArea.toggle;
				player.sendMessage(Colors.Yellow + "Cuboids protection : "+ (ProtectedArea.toggle ? "enabled" : "disabled"));
				return true;*/
			}
			
			else if (split[0].equalsIgnoreCase("/creload")){
				//
				return false;
				/*
				ProtectedArea.loadProtectedAreas();
				ProtectedArea.loadClaimedAreas();
				player.sendMessage(Colors.Green + "Cuboids coordinates reloaded");
				return true;*/
			}
		}
		else if (player.canUseCommand( "/stakeclaim")){
			//!TODO!Replace this 
			if ( split[0].equalsIgnoreCase("/stakeclaim" ) ){
				if (Cuboidold.isReady(playerName.toLowerCase(), true)){
					//String parameters = "";
					int paramSize = split.length;
					if (paramSize == 2){
						String cuboidName = playerName.trim().toLowerCase() + "_" + split[1].trim().toLowerCase();
						
						short returnCode = ProtectedArea.stakeClaim(playerName.trim().toLowerCase(), cuboidName);
						if (returnCode==0 || returnCode==1){
							player.sendMessage(Colors.LightGreen + "New claim staked" + ((returnCode==1) ? " (old claim purged)" : ""));
							player.sendMessage(Colors.LightGreen + "Name : "+Colors.White+cuboidName);
							player.sendMessage(Colors.LightGreen + "Protection will not work until you are contacted by and admin");
							// Message admins
							for  (Player p : etc.getServer().getPlayerList() ) {
								if (player.canUseCommand("/protect")){
									p.sendMessage("Player "+playerName+" has staked a claim!");
									p.sendMessage("TP to them, and do /checkclaim "+playerName.toLowerCase()+" to see the claim");
								}
							}
						}
						else if (returnCode==2){
							player.sendMessage(Colors.Rose + "You already have a protected area by this name");
						}
						else if (returnCode==-1){
							player.sendMessage(Colors.Rose + "Error while adding the claim");
							player.sendMessage("Contact an admin for assistance");
						}
					}
					else{
						player.sendMessage(Colors.Yellow + "Command format is /stakeclaim <claim_name>");
					}
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/listprotected")){
				String cuboidList = ProtectedArea.listerCuboids(playerName.trim().toLowerCase()); //!TODO!only return player cuboids
				player.sendMessage(Colors.Yellow + "You own"+Colors.White+" :"+cuboidList);
				return true;
			}
		}
		
		if (!player.canUseCommand("/canbuild") && split[0].equalsIgnoreCase("/whycantibuild")) {
    		player.sendMessage("Add text here");
    		return true;
    	}
		
	    return false;
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
		int targetBlockClickedType = blockClicked.getType();
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
		if ( itemInHand==288 && player.canUseCommand("/allowfirefeather")){
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
		else if ( itemInHand==287 && player.canUseCommand("/allowlavastring")){
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
		
		else if ( targetBlockClickedType == 64 || targetBlockClickedType == 69 || targetBlockClickedType == 77 ){
			// Wood doors, buttons and levers
			return false;
		}
		else if ( itemInHand==271 && (player.canUseCommand( "/protect") || player.canUseCommand( "/cuboid")) ){
				boolean whichPoint = Cuboidold.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
				player.sendMessage(Colors.Blue + ((!whichPoint) ? "First" : "Second")+ " point is set." );	
				return true;
		}
		else if ( itemInHand==271 && player.canUseCommand( "/stakeclaim") ){
				int[] blocks = Cuboidold.getBlocks(player.getName().toLowerCase());
				if ( blocks[0] == 19 && blocks[1] == 19 ) {
					player.sendMessage(Colors.Rose + "Your previous claim is being processed, please wait" );	
				}
				boolean whichPoint = Cuboidold.setPoint(player.getName().toLowerCase(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ(), blockClicked.getType());
				player.sendMessage(Colors.Blue + ((!whichPoint) ? "First" : "Second")+ " point is set." );	
				return true;
		}
		else if ( itemInHand==268 ){
			String owners = ProtectedArea.inProtectedZone(blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
			if (owners != null)
				player.sendMessage(Colors.Yellow + "This area is owned by :"+Colors.White+owners);
			else
				player.sendMessage(Colors.Yellow + "This area is currently unowned.");
			return true;
		}
		else if ( (itemInHand>269 && itemInHand<280) || itemInHand==256|| itemInHand==257|| itemInHand==258|| itemInHand==290|| itemInHand==291|| itemInHand==292|| itemInHand==293 ){
			return false;
		}
		else{
			if ( ProtectedArea.toggle && !player.canIgnoreRestrictions() ){
				return isAllowed(player, blockPlaced) || isAllowed(player, blockClicked);
			}
		}
		return false;
	}

	public boolean onBlockDestroy(Player player, Block block) {
		//log.info ("[CuboidPlugin] "+player.getName()+" attempted block destroy");
		//log.info ("[CuboidPlugin] protectedareas: "+ProtectedArea.toggle+" canIgnoreRestrictions: "+etc.getInstance().canIgnoreRestrictions(player.getName()));
		if (!player.canUseCommand("/canbuild")) {
			int normalplayerIndex = -1;
			if(whycantibuildplayerList.size()>0) {
				for(int i=0; i<whycantibuildplayerList.size(); i++) {
					if( player.getName() == whycantibuildplayerList.get(i)) {
						normalplayerIndex = i;
					}
				}
			}
			if(normalplayerIndex == -1) {
				whycantibuildplayerList.add(player.getName());
				whycantibuildlastBlock.add(block);
				player.sendMessage("You will need to gain build access to destroy and place blocks.");
				player.sendMessage("Please type '/whycantibuild' into chat to find out more.");
			} else {
				if(whycantibuildlastBlock.get(normalplayerIndex).getX()!=block.getX() || 
					whycantibuildlastBlock.get(normalplayerIndex).getY()!=block.getY() || 
					whycantibuildlastBlock.get(normalplayerIndex).getZ()!=block.getZ()) {
					player.sendMessage("You will need to gain build access to destroy and place blocks.");
					player.sendMessage("Please type '/whycantibuild' into chat to find out more.");
					whycantibuildlastBlock.set(normalplayerIndex,block);
					}
			}
			return true;
		}
		int targetBlockType = block.getType();
		if ( targetBlockType == 19){ // NO MORE SPONGE!
			return true;
		}
		else if (ProtectedArea.toggle && !player.canIgnoreRestrictions() ){
			String playerName = player.getName();
			boolean inList = false;
			for (String p : playerList){
				if (p==playerName)
					inList = true;
			}
			
			
			 
			
			if (inList){
				Block lastTouchedBlock = correspondingBloc.get(playerList.indexOf(playerName));
				if (lastTouchedBlock.getX() != block.getX() || lastTouchedBlock.getY() != block.getY() || lastTouchedBlock.getZ() != block.getZ() ){
					
					int indexInList = playerList.indexOf(playerName);
					correspondingBloc.set(indexInList, block);
					lastStatus.set(indexInList, isAllowed(player, block));
					
					return lastStatus.get(playerList.indexOf(playerName));
				}
				else{
					return lastStatus.get(playerList.indexOf(playerName));
				}
			}
			else{
				playerList.add(playerName);
				correspondingBloc.add(block);
				lastStatus.add(isAllowed(player, block));
				return lastStatus.get(playerList.indexOf(playerName));
			}
		}
		return false;
	}
	
		private boolean isAllowed(Player player, Block block){		
		String cuboidOwners = ProtectedArea.inProtectedZone(block);
		if ( cuboidOwners != null && cuboidOwners.indexOf(" "+player.getName().toLowerCase())==-1 ){
			
			int groupIndex = cuboidOwners.indexOf(" g:")+3;
			int endIndex = -1;
			String groupName ="";
			while ( groupIndex >= 3 ){
				endIndex = cuboidOwners.indexOf(" ", groupIndex);
				if (endIndex == -1)
					endIndex = cuboidOwners.length();
				groupName = cuboidOwners.substring(groupIndex, endIndex);
				
				if (player.isInGroup(groupName) ){
					return false;
				}
				cuboidOwners = cuboidOwners.substring(0, groupIndex-3) + cuboidOwners.substring(endIndex, cuboidOwners.length());
				groupIndex = cuboidOwners.indexOf(" g:")+3;
			}
	
			player.sendMessage(Colors.Rose+"This block is protected !" );
			return true;
		}
		return false;
	}
	
	private boolean isValidBlockID(int blocID){
		boolean validity = true;
		
		if ( (blocID > 20 && blocID < 35) || blocID==36 ){
			validity = false;
		}
		
		return validity;
	}

}
