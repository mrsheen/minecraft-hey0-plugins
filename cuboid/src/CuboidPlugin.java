import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;


import net.minecraft.server.MinecraftServer;

public class CuboidPlugin extends Plugin {
	// Version 4.2 : 29/09 14h00 GMT+2
	// for servermod100
	
	@SuppressWarnings("unused")
	private String name = "CuboidPlugin";
	
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Boolean> lastStatus = new ArrayList<Boolean>();
	static ArrayList<Block> correspondingBloc = new ArrayList<Block>();
	
	public void enable(){
		ProtectedArea.loadProtectedAreas();
		ProtectedArea.loadClaimedAreas();
		log.info("[CuboidPlugin] Mod Enabled.");
		etc.getInstance().addCommand("/protect", "<player> g:<groupName> <name of the cuboid>");
		etc.getInstance().addCommand("/listprotected", "- List the protected cuboids by name"); 
		etc.getInstance().addCommand("/removeprotected", "<protected cuboid name> - Remove the protected cuboid");
		etc.getInstance().addCommand("/toggleprot", "- Turns on/off the cuboid protection (worldwide)");
		etc.getInstance().addCommand("/creload", "- Reloads the data file, in case you edited it manually");
		etc.getInstance().addCommand("/cfill","<bloc ID|name> - Fills the selected cuboid with the specified blocType" );
		etc.getInstance().addCommand("/creplace","<bloc ID|name> <bloc ID|name> - Replaces blocTypes within the selected cuboid");
		etc.getInstance().addCommand("/cdel", "- Removes any bloc within the selected cuboid");
		etc.getInstance().addCommand("/csize", "- Displays the number of blocs inside the selected cuboid");
		
		etc.getInstance().addCommand("/stakeclaim", "<name of area> - Stake a claim");
		etc.getInstance().addCommand("/grantclaim", "<playername> <name of area> - Grant a claim");
		
		
	}
	
	public void disable(){
		etc.getInstance().removeCommand("/protect");
		etc.getInstance().removeCommand("/listprotected"); 
		etc.getInstance().removeCommand("/removeprotected");
		etc.getInstance().removeCommand("/toggleprot");
		etc.getInstance().removeCommand("/creload");
		etc.getInstance().removeCommand("/cfill");
		etc.getInstance().removeCommand("/creplace");
		etc.getInstance().removeCommand("/cdel");
		etc.getInstance().removeCommand("/csize");
		etc.getInstance().removeCommand("/stakeclaim");
		etc.getInstance().removeCommand("/grantclaim");
		log.info("[CuboidPlugin] Mod disabled.");
	}
	
	public boolean onCommand(Player player, String[] split) {
		String playerName = player.getName();

		if (player.canUseCommand("/protect")){
			if ( split[0].equalsIgnoreCase("/protect" ) ){
				if (Cuboid.isReady(playerName, true)){
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
				return true;
			}
			else if ( split[0].equalsIgnoreCase("/grantclaim" ) ){
				//!TODO! update this
				int paramSize = split.length;
				if (paramSize == 2){
					String claimerName = split[1].trim().toLowerCase();
					String cuboidName = Cuboid.getClaimName(claimerName);
					//String claimerName = cuboidName.split("_")[0];
					if (Cuboid.isReady(claimerName, true)){
						short returnCode = ProtectedArea.protegerCuboid(claimerName, " "+claimerName, cuboidName);
						if (returnCode==0){
							player.sendMessage(Colors.LightGreen + "New protected zone created.");
							player.sendMessage(Colors.LightGreen + "Name : "+Colors.White+cuboidName);
							player.sendMessage(Colors.LightGreen + "Owners :"+Colors.White+claimerName);
							Cuboid.hideClaim(claimerName);
							Cuboid.setPoint(claimerName, 0, 0, 0, 0);
							Cuboid.setPoint(claimerName, 0, 0, 0, 0);
							Cuboid.setClaimName(claimerName, "");
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
					if (Cuboid.isReady(claimerName, true)){
						// Replace A-G of cuboid with sponge
						Cuboid.showClaim(claimerName);
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
					player.sendMessage(Colors.Yellow + "Waiting claims: " + Cuboid.getClaims());
				}
				
				return true;
			}
			else if ( split[0].equalsIgnoreCase("/rejectclaim" ) ){
				//!TODO! update this
				int paramSize = split.length;
				if (paramSize >= 2){
					String claimerName = split[1].trim().toLowerCase();
					
					Cuboid.hideClaim(claimerName);
					Cuboid.setPoint(claimerName, 0, 0, 0, 0);
					Cuboid.setPoint(claimerName, 0, 0, 0, 0);
					Cuboid.setClaimName(claimerName, "");
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
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/listprotected")){
				String cuboidList = ProtectedArea.listerCuboids();
				player.sendMessage(Colors.Yellow + "Protected areas"+Colors.White+" :"+cuboidList);
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/toggleprot")){
				ProtectedArea.toggle=!ProtectedArea.toggle;
				player.sendMessage(Colors.Yellow + "Cuboids protection : "+ (ProtectedArea.toggle ? "enabled" : "disabled"));
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/creload")){
				ProtectedArea.loadProtectedAreas();
				ProtectedArea.loadClaimedAreas();
				player.sendMessage(Colors.Green + "Cuboids coordinates reloaded");
				return true;
			}
		}
		else if (player.canUseCommand( "/stakeclaim")){
			//!TODO!Replace this 
			if ( split[0].equalsIgnoreCase("/stakeclaim" ) ){
				if (Cuboid.isReady(playerName.toLowerCase(), true)){
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
		if (player.canUseCommand( "/cuboid")){	
			if (split[0].equalsIgnoreCase("/csize")){
				if ( Cuboid.isReady(playerName, true) ){
					player.sendMessage(Colors.LightGreen +"The selected cuboid size is : " + Cuboid.calculerTaille(playerName) +" blocks" );
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
		    }
						
			else if (split[0].equalsIgnoreCase("/cdel")){
				if (Cuboid.isReady(playerName, true)){
					Cuboid.supprimerCuboid(playerName);
					player.sendMessage(Colors.LightGreen + "The cuboid is now empty");
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/cfill")){
				if (Cuboid.isReady(playerName, true)){
					if (split.length>1){
						int blocID = 0;
						try {
							blocID = Integer.parseInt( split[1] );
						} catch (NumberFormatException n) {
							blocID = etc.getInstance().getDataSource().getItem( split[1] );
							if (blocID == 0){
								player.sendMessage(Colors.Rose + split[1] +" is not a valid block name.");
								return true;
							}
						}					
						if ( isValidBlockID(blocID) ){
							Cuboid.remplirCuboid(playerName, blocID);
							player.sendMessage(Colors.LightGreen + "The cuboid has been filled");
						}
						else{
							player.sendMessage(Colors.Rose +blocID+ " is not a valid block ID.");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "Usage : /cfill <block id|name>");
					}
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/creplace")){
				if (Cuboid.isReady(playerName, true)){

					int paramSize = split.length-1;
					if (paramSize>1){
						int[] replaceParams = new int[paramSize];
						for (int i = 0; i<paramSize; i++){
							try {
								replaceParams[i] = Integer.parseInt( split[i+1] );
							}
							catch (NumberFormatException n) {
								replaceParams[i] = etc.getInstance().getDataSource().getItem( split[i+1] );
								if ( replaceParams[i] == 0){
									player.sendMessage(Colors.Rose + split[i+1] +" is not a valid block name.");
									return true;
								}
							}
							if ( !isValidBlockID(replaceParams[i]) ){
								player.sendMessage(Colors.Rose +replaceParams[i]+ " is not a valid block ID.");
								return true;
							}
						}
						
						Cuboid.remplacerDansCuboid(playerName, replaceParams);
						player.sendMessage(Colors.LightGreen + "The blocks have been replaced");
					}
					else{
						player.sendMessage(Colors.Rose + "Usage : /creplace <block id|name> <block id|name>");
					}
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
			}	
			
			/*
			else if (split[0].equalsIgnoreCase("/ccopy")){
				if (Cuboid.isReady(true)){
					Cuboid.enregisterCuboid();
					player.sendMessage(Colors.LightGreen + "The cuboid was recorded.");
				}
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/cpaste")){
				if (Cuboid.isReady(false)){
					Cuboid.poserCuboid();
					player.sendMessage(Colors.LightGreen + "The cuboid has been reconstituted.");
				}									
				else{
					player.sendMessage(Colors.Rose + "No cuboid has been selected");
				}
				return true;
			}			
			
			else if (split[0].equalsIgnoreCase("/ccircle")){
				if (Cuboid.isReady(false)){
					int taille = 0;
					if (split[1] != null){
						taille = Integer.parseInt( split[1] );
					}
					else{
						player.sendMessage(Colors.Rose + "Concerns with the size parameter.");
					}
				}
				return true;
			}*/
		}
	    return false;
	}

	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand){	
		int targetBlockClickedType = blockClicked.getType();
		if ( blockPlaced.getType() == 19){ // NO MORE SPONGE!
			return true;
		}
		else if ( targetBlockClickedType == 64 || targetBlockClickedType == 69 || targetBlockClickedType == 77 ){
			// Wood doors, buttons and levers
			return false;
		}
		else if ( itemInHand==269 && (player.canUseCommand( "/protect") || player.canUseCommand( "/cuboid")) ){
				boolean whichPoint = Cuboid.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
				player.sendMessage(Colors.Blue + ((!whichPoint) ? "First" : "Second")+ " point is set." );	
				return true;
		}
		else if ( itemInHand==269 && player.canUseCommand( "/stakeclaim") ){
				int[] blocks = Cuboid.getBlocks(player.getName().toLowerCase());
				if ( blocks[0] == 19 && blocks[1] == 19 ) {
					player.sendMessage(Colors.Rose + "Your previous claim is being processed, please wait" );	
				}
				boolean whichPoint = Cuboid.setPoint(player.getName().toLowerCase(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ(), blockClicked.getType());
				player.sendMessage(Colors.Blue + ((!whichPoint) ? "First" : "Second")+ " point is set." );	
				return true;
		}
		else if ( itemInHand==268 ){
			String owners = ProtectedArea.inProtectedZone(blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
			if (owners != null)
				player.sendMessage(Colors.Yellow + "This area is owned by :"+Colors.White+owners);
			else
				player.sendMessage(Colors.Yellow + "This area belongs to nobody");
			return true;
		}
		else if ( itemInHand==288 ){
			player.sendMessage(Colors.Yellow + "Begone, fire!");
			boolean whichPoint = Cuboid.setPoint(player.getName(), blockClicked.getX()+8, blockClicked.getY()+8, blockClicked.getZ()+8);
			boolean throwaway2 = Cuboid.setPoint(player.getName(), blockClicked.getX()-8, blockClicked.getY()-8, blockClicked.getZ()-8);
			if (whichPoint) {
				boolean throwaway1 = Cuboid.setPoint(player.getName(), blockClicked.getX()+8, blockClicked.getY()+8, blockClicked.getZ()+8);
			}
			int[] fireairparams = {51,0};
			Cuboid.remplacerDansCuboid(player.getName(), fireairparams);
			// Message admins
			for  (Player p : etc.getServer().getPlayerList() ) {
				if (player.canUseCommand("/protect")){
					p.sendMessage(playerName+" just banished some fire!");
				}
			}
			
			return true;
		}
			// Feather 
		else if ( itemInHand==288 ){
			player.sendMessage(Colors.Yellow + "Begone, fire!");
			boolean whichPoint = Cuboid.setPoint(player.getName(), blockClicked.getX()+8, blockClicked.getY()+128, blockClicked.getZ()+8);
			boolean throwaway2 = Cuboid.setPoint(player.getName(), blockClicked.getX()-8, blockClicked.getY()-8, blockClicked.getZ()-8);
			if (whichPoint) {
				boolean throwaway1 = Cuboid.setPoint(player.getName(), blockClicked.getX()+8, blockClicked.getY()+128, blockClicked.getZ()+40);
			}
			int[] fireairparams = {51,0}; // change fire blocks to air
			Cuboid.remplacerDansCuboid(player.getName(), fireairparams);
			return true;
		}
		// String
		else if ( itemInHand==287 ){
			player.sendMessage(Colors.Yellow + "Begone, lava!");
			boolean whichPoint = Cuboid.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY()+128, blockClicked.getZ());
			boolean throwaway2 = Cuboid.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
			if (whichPoint) {
				boolean throwaway1 = Cuboid.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY()+128, blockClicked.getZ());
			}
			int[] lavaairparams = {11,10,0}; // change lava source and flowing lava to air
			Cuboid.remplacerDansCuboid(player.getName(), lavaairparams);
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