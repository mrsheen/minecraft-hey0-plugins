import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CuboidPlugin extends Plugin {
	// Version 10 : 17/10 11h00 GMT+2
	// for servermod 116-117+
		
	public String name = "CuboidPlugin";
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Boolean> lastStatus = new ArrayList<Boolean>();
	static ArrayList<Block> correspondingBloc = new ArrayList<Block>();
	static boolean logging = false;
	static boolean protectionWarn = false;
	static boolean chestProtection = true;
	static boolean newestHavePriority = true;
	static int mainToolID = 269;
	static int checkToolID = 268;
	
	public void enable(){
		loadProperties();
        CuboidProtection.loadProtectedAreas();
	}
	
	public void disable(){
	}
	
	private void loadProperties(){
		PropertiesFile properties = new PropertiesFile("cuboidPlugin.properties");
		try {
			CuboidProtection.addedHeight = properties.getInt("minProtectedHeight", 0);
			logging = properties.getBoolean("fullLogging", false);
			protectionWarn = properties.getBoolean("protectionWarning", false);
			chestProtection = properties.getBoolean("chestProtection", true);
			newestHavePriority = properties.getBoolean("newestHavePriority", true);
			mainToolID = properties.getInt("mainToolID", 269);
			checkToolID = properties.getInt("checkToolID", 268);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while reading from server.properties", e);
        }
        // TODO : non-existant file
	}
	
	////////////////////////
	////	FONCTIONS	////
	////////////////////////
	
	private boolean isAllowed(Player player, ComplexBlock block, boolean warn){
		Block simpleBlock = new Block();
		simpleBlock.setX( block.getX() );
		simpleBlock.setY( block.getY() );
		simpleBlock.setZ( block.getZ() );
		return isAllowed(player, simpleBlock, warn);
	}
	
	private boolean isAllowed(Player player, Block block, boolean warn){
		String cuboidOwners = CuboidProtection.inProtectedZone(block);
		if ( cuboidOwners != null && cuboidOwners.indexOf(" "+player.getName().toLowerCase())==-1
				&& cuboidOwners.indexOf("o:"+player.getName().toLowerCase())==-1 ){
			
			int groupIndex = cuboidOwners.indexOf(" g:")+3;
			int endIndex = -1;

			while ( groupIndex >= 3 ){
				endIndex = cuboidOwners.indexOf(" ", groupIndex);
				if (endIndex == -1)
					endIndex = cuboidOwners.length();
				

				if ( player.isInGroup(cuboidOwners.substring(groupIndex, endIndex)) ){
					return false;
				}

				cuboidOwners = cuboidOwners.substring(0, groupIndex-3) + cuboidOwners.substring(endIndex, cuboidOwners.length());
				groupIndex = cuboidOwners.indexOf(" g:")+3;
			}
	
			if (warn && protectionWarn){
				player.sendMessage(Colors.Rose+"This block is protected !" );
			}
			return true;
		}
		return false;
	}
	
	private boolean isOwner( Player player, String protectedAreaName ){
		String cuboidOwners = CuboidProtection.listOwners(protectedAreaName);

		if( cuboidOwners != null ){
			String playerName = player.getName();
			int ownerIndex = cuboidOwners.indexOf(" o:")+3;
			int endIndex = -1;

			while ( ownerIndex >= 3 ){
				endIndex = cuboidOwners.indexOf(" ", ownerIndex);
				if (endIndex == -1)
					endIndex = cuboidOwners.length();				

				if ( cuboidOwners.substring(ownerIndex, endIndex).equalsIgnoreCase(playerName) ){
					return true;
				}

				cuboidOwners = cuboidOwners.substring(0, ownerIndex-3) + cuboidOwners.substring(endIndex, cuboidOwners.length());
				ownerIndex = cuboidOwners.indexOf(" o:")+3;
			}
		}
		else{
			player.sendMessage(Colors.Rose+"Could not find any protected area named "+protectedAreaName);
		}
		
		return false;
	}
	
	private boolean isValidBlockID(int blocID){
		if (blocID >= 0 && blocID <=85){
			if ( (blocID > 20 && blocID < 35) || blocID==36 ){
				return false;
			}
			else{
				return true;
			}
		}
		else
			return false;
		
	}
	
	private boolean cuboidExists(String playerName, String cuboidName){
		return new File("cuboids/"+playerName+"/"+cuboidName+".cuboid").exists();
	}
	
	private String listOfCuboids(String owner){
		
		if ( !new File("cuboids").exists() || !new File("cuboids/"+owner).exists() ){
			return null;
		}
		String [] fileList = new File("cuboids/"+owner).list();
		String result = (fileList.length>0)? "" : null ;
			
		for(int i=0; i<fileList.length;i++){
			if(fileList[i].endsWith(".cuboid")==true){
				result+=" "+fileList[i].substring(0, fileList[i].length()-7 );
			}
		}
		
		return result;
	}
	
	//////////////////////////////
	////	LISTENER STUFF    ////
	//////////////////////////////
	
	public void initialize(){
		CuboidListener listener = new CuboidListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_CHANGE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMPLEX_BLOCK_SEND, listener, this, PluginListener.Priority.MEDIUM);
	}
	
	public class CuboidListener extends PluginListener{
	
		public boolean onCommand(Player player, String[] split) {
			String playerName = player.getName();
			
			///////////////////////////////////
			////	PROTECTION COMMANDS    ////
			///////////////////////////////////
	
			if (split[0].equalsIgnoreCase("/allow" )){
				if( split.length > 2){
					String areaName = split[split.length-1].trim();
					if (player.canIgnoreRestrictions() || isOwner(player, areaName) ){
						
						String[] parameters = new String[split.length-2];
						for (int i=1; i<split.length-1; i++){
							parameters[i-1] = split[i].trim().toLowerCase();
						}
						byte returnCode = CuboidProtection.addPlayer( parameters, areaName );
						if (returnCode==0){
							player.sendMessage(Colors.Green+"Successful addition to the allowed list");
						}
						else if (returnCode==1){
							player.sendMessage(Colors.Yellow+"Protected area not found : "+Colors.White+areaName);
						}
						else if (returnCode==2){
							player.sendMessage(Colors.Rose+"protectedCuboids.txt seems to have been removed");
						}
						else if (returnCode==3){
							player.sendMessage(Colors.Rose+"Error while modifying the file");
						}
						
					}
					else{
						player.sendMessage(Colors.Rose+"You are not an owner of this protected zone.");
					}
				}
				else{
					player.sendMessage(Colors.Rose+"Usage : /allow <player list> <protected area name>");
					player.sendMessage(Colors.Yellow+"Use o:<playername> to set a new owner");
					player.sendMessage(Colors.Yellow+"Use g:<groupname> to allow an entire group");
				}
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/revoke" )){
				if( split.length > 2){
					String areaName = split[split.length-1].trim();
					if (player.canIgnoreRestrictions() || isOwner(player, areaName) ){
						
						String[] parameters = new String[split.length-2];
						for (int i=1; i<split.length-1; i++){
							String toRemove = split[i].trim().toLowerCase();
							if ( toRemove.startsWith("o:") && !player.canIgnoreRestrictions() ){
								player.sendMessage(Colors.Rose+"You cannot revoke ownership");
								return true;
							}
							parameters[i-1] = toRemove;
						}
						byte returnCode = CuboidProtection.removePlayer( parameters, areaName );
						if (returnCode==0){
							player.sendMessage(Colors.Green+"Successful removal from the allowed list");
						}
						else if (returnCode==1){
							player.sendMessage(Colors.Yellow+"Protected area not found : "+Colors.White+areaName);
						}
						else if (returnCode==2){
							player.sendMessage(Colors.Rose+"protectedCuboids.txt seems to have been removed");
						}
						else if (returnCode==3){
							player.sendMessage(Colors.Rose+"Error while modifying the file");
						}
					}
					else{
						player.sendMessage(Colors.Rose+"You are not an owner of this protected zone.");
					}
				}
				else{
					player.sendMessage(Colors.Rose+"Usage : /revoke <player list> <protected area name>");
				}
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/listprotected")){
				String cuboidList = CuboidProtection.listerCuboids();
				player.sendMessage(Colors.Yellow + "Protected areas"+Colors.White+" :"+cuboidList);
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/owners" )){
				if (split.length==2){
					String liste = CuboidProtection.listOwners( split[1] );
					if (liste != null){
						player.sendMessage(Colors.Yellow + "Owners :"+Colors.White+liste);
					}
					else
						player.sendMessage(Colors.Yellow + "This area belongs to nobody");
				}
				else{
					player.sendMessage(Colors.Rose+"Usage : /owners <protected area name>");
				}	
				return true;
			}
			
			else if (player.canUseCommand("/protect")){
				if ( split[0].equalsIgnoreCase("/protect" ) ){
					if (Cuboid.isReady(playerName, true)){
						String parameters = "";
						int paramSize = split.length;
						if (paramSize > 2){
							for (short i=1; i<paramSize-1; i++){
								parameters += " "+split[i];
							}
							String cuboidName = split[paramSize-1].trim().toLowerCase();
							short returnCode = CuboidProtection.protegerCuboid(playerName, parameters.toLowerCase(), cuboidName);
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
							player.sendMessage(Colors.Yellow + "You need to specify at least one player or group, and a name.");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
				}
				
				if ( split[0].equalsIgnoreCase("/moveprotection" ) ){
					if (Cuboid.isReady(playerName, true)){
						if ( split.length>1 ){
							byte returnCode = CuboidProtection.moveProtection( playerName, split[1] );
							if ( returnCode == 0 ){
								player.sendMessage(Colors.LightGreen + "Protected area successfuly moved");
							}
							else if ( returnCode == 1 ){
								player.sendMessage(Colors.Rose + "Protected area not found : "+split[1]);
							}
							else if (returnCode == 2){
								player.sendMessage(Colors.Rose + "Exception while moving the protected area...");
							}
							else if (returnCode == 3){
								player.sendMessage(Colors.Rose + "protectedCuboids.txt not found !");
							}
						}
						else{
							player.sendMessage(Colors.Rose + "Usage : /moveprotection <protected area name>");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
				}
				
				else if ( split[0].equalsIgnoreCase("/unprotect") || split[0].equalsIgnoreCase("/removeprotected") ){
					if(split.length == 2){
						short returnCode = CuboidProtection.removeProtectedZone(playerName, split[1].trim().toLowerCase() );
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
							player.sendMessage(Colors.Rose + "protectedCuboids.txt not found !");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "Usage : /unprotect <protected area name>");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/toggleprot")){
					CuboidProtection.toggle=!CuboidProtection.toggle;
					player.sendMessage(Colors.Yellow + "Cuboids protection : "+ (CuboidProtection.toggle ? "enabled" : "disabled"));
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/creload")){
					CuboidProtection.toggle = false;
					CuboidProtection.loadProtectedAreas();
					CuboidProtection.toggle = true;
					player.sendMessage(Colors.Green + "Cuboids coordinates reloaded");
					return true;
				}
			}
			
			///////////////////////////////
			////	CUBOID COMMANDS    ////
			///////////////////////////////
			
			if (player.canUseCommand("/cuboid")){
				
				/////////////////////////////
				////	CORE COMMANDS    ////
				/////////////////////////////
				
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
						Cuboid.viderCuboid(playerName);
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
								blocID = etc.getDataSource().getItem( split[1] );
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
									replaceParams[i] = etc.getDataSource().getItem( split[i+1] );
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
				
				//////////////////////////////////////////////
				////	CIRCLE/SPHERE BUILDING COMMANDS    ///
				//////////////////////////////////////////////
							
				else if (split[0].equalsIgnoreCase("/ccircle") || split[0].equalsIgnoreCase("/cdisc") ){
					if (Cuboid.isReady(playerName, false)){
						boolean disc = split[0].equalsIgnoreCase("/cdisc")? true : false;
						int radius = 0;
						int blockID = 4;
						int height = 0;
						if (split.length>2){
							try {
								radius = Integer.parseInt( split[1] );
							}
							catch (NumberFormatException n) {
								player.sendMessage(Colors.Rose + split[1] +" is not a valid radius.");
								return true;
							}
							if (radius < 1){
								player.sendMessage(Colors.Rose + split[1] +" is not a valid radius.");
								return true;
							}
							
							try {
								blockID = Integer.parseInt( split[2] );
							}
							catch (NumberFormatException n) {
								blockID = etc.getDataSource().getItem( split[2] );
							}
							
							if ( !isValidBlockID( blockID ) ){
								player.sendMessage(Colors.Rose +split[2]+ " is not a valid block ID.");
								return true;
							}
							
							if (split.length==4){
								try {
									height = Integer.parseInt( split[3] );
								}
								catch (NumberFormatException n) {
									player.sendMessage(Colors.Rose +split[3]+ " is not a valid height.");
									return true;
								}
								if (height>0){
									height--;
								}
								else if (height<0){
									height++;
								}
							}
							
							if (disc){
								Cuboid.tracerCercle(playerName, radius, blockID, height, true);
								player.sendMessage(Colors.LightGreen + "The "+((height==0)?"disc":"cylinder")+" has been build");
							}
							else{
								Cuboid.tracerCercle(playerName, radius, blockID, height, false);
								player.sendMessage(Colors.LightGreen + "The "+((height==0)?"circle":"cylinder")+" has been build");
							}
							
						}
						else{
							if (disc){
								player.sendMessage(Colors.Rose + "Usage : /cdisc <radius> <block id|name> [height]");
							}
							else{
								player.sendMessage(Colors.Rose + "Usage : /ccircle <radius> <block id|name> [height]");
							}
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No point has been selected");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/csphere") || split[0].equalsIgnoreCase("/cball") ){
					if (Cuboid.isReady(playerName, false)){
						boolean ball = (split[0].equalsIgnoreCase("/cball")) ? true : false;
						int radius = 0;
						int blockID = 4;
						if (split.length>2){
							try {
								radius = Integer.parseInt( split[1] );
							}
							catch (NumberFormatException n) {
								player.sendMessage(Colors.Rose + split[1] +" is not a valid radius.");
								return true;
							}
							if (radius < 2){
								player.sendMessage(Colors.Rose + split[1] +" is not a valid radius.");
								return true;
							}
							
							try {
								blockID = Integer.parseInt( split[2] );
							}
							catch (NumberFormatException n) {
								blockID = etc.getDataSource().getItem( split[2] );
							}
							if ( !isValidBlockID( blockID ) ){
								player.sendMessage(Colors.Rose +split[2]+ " is not a valid block ID.");
								return true;
							}
							
							if(ball){
								Cuboid.dessinerShpere(playerName, radius, blockID, true);
								player.sendMessage(Colors.LightGreen + "The ball has been built");
							}
							else{
								Cuboid.dessinerShpere(playerName, radius, blockID, false);
								player.sendMessage(Colors.LightGreen + "The sphere has been built");
							}
							
						}
						else{
							if (ball){
								player.sendMessage(Colors.Rose + "Usage : /ball <radius> <block id|name>");
							}
							else{
								player.sendMessage(Colors.Rose + "Usage : /csphere <radius> <block id|name>");
							}
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No point has been selected");
					}
					return true;
				}
				
				///////////////////////////////////////
				////	CUBOIDS BUILDING COMMANDS	///
				///////////////////////////////////////
				
				else if (split[0].equalsIgnoreCase("/cfaces")){
					if (Cuboid.isReady(playerName, true)){
						int blockID = 4;
						if (split.length>1){						
							try {
								blockID = Integer.parseInt( split[1] );
							}
							catch (NumberFormatException n) {
								blockID = etc.getDataSource().getItem( split[1] );
							}
							
							if ( !isValidBlockID( blockID ) ){
								player.sendMessage(Colors.Rose +split[1]+ " is not a valid block ID.");
								return true;
							}
							
							Cuboid.dessinerCuboid(playerName, blockID, true);
							player.sendMessage(Colors.LightGreen + "The faces of the cuboid have been built");
						}
						else{
							player.sendMessage(Colors.Rose + "Usage : /cfaces <block id|name>");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/cwalls")){
					if (Cuboid.isReady(playerName, true)){
						int blockID = 4;
						if (split.length>1){						
							try {
								blockID = Integer.parseInt( split[1] );
							}
							catch (NumberFormatException n) {
								blockID = etc.getDataSource().getItem( split[1] );
							}
							
							if ( !isValidBlockID( blockID ) ){
								player.sendMessage(Colors.Rose +split[1]+ " is not a valid block ID.");
								return true;
							}
							
							Cuboid.dessinerCuboid(playerName, blockID, false);
							player.sendMessage(Colors.LightGreen + "The walls have been built");
						}
						else{
							player.sendMessage(Colors.Rose + "Usage : /cwalls <block id|name>");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/undo")){
					if (Cuboid.isUndoAble(playerName)){
						Cuboid.undo(playerName);
						player.sendMessage(Colors.Green + "Your last action has been undone !");
					}
					else{
						player.sendMessage(Colors.Rose + "Your last action is non-reversible.");
					}
					return true;
				}
				
				///////////////////////////////
				////	COPY/PASTE SYSTEM	///
				///////////////////////////////
				
				else if (split[0].equalsIgnoreCase("/copy")){
					if (Cuboid.isReady(playerName, true)){
						Cuboid.copyCuboid(playerName, true);
						player.sendMessage(Colors.Green + "Selected cuboid is copied. Ready to paste !");
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/paste")){	
					if (Cuboid.isReady(playerName, false)){
						byte returnCode = Cuboid.paste(playerName);
						if (returnCode == 0){
							player.sendMessage(Colors.Green + "The cuboid has been placed.");
						}
						else if (returnCode == 1){
							player.sendMessage(Colors.Rose + "Nothing to paste !");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No point has been selected");
					}
					return true;
				}
				
				///////////////////////////////
				////	SAVE/LOAD SYSTEM	///
				///////////////////////////////
				
				else if (split[0].equalsIgnoreCase("/csave")){
					
					if (split.length>1){
						String cuboidName = split[1].toLowerCase();
						if (!cuboidExists(playerName, cuboidName) || split.length==3 && split[2].startsWith("over")){
							if (Cuboid.isReady(playerName, true)){
								byte returnCode = Cuboid.saveCuboid(playerName, cuboidName);
								if (returnCode==0){
									player.sendMessage(Colors.Green + "Selected cuboid is saved with the name "+cuboidName);
								}
								else if (returnCode==1){
									player.sendMessage(Colors.Rose + "Could not create the target folder.");
								}
								else if (returnCode==2){
									player.sendMessage(Colors.Rose + "Error while writing the file.");
								}
							}
							else{
								player.sendMessage(Colors.Rose + "No cuboid has been selected");
							}
						}
						else{
							player.sendMessage(Colors.Rose + "This cuboid name is already taken.");
						}
					}		
					else{
						player.sendMessage(Colors.Rose + "Usage : /csave <cuboid name>");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/cload")){
					if (split.length>1){
						String cuboidName = split[1].toLowerCase();
						if (cuboidExists(playerName, cuboidName)){
							if (Cuboid.isReady(playerName, false)){
								byte returnCode = Cuboid.loadCuboid(playerName, cuboidName);
								if (returnCode==0){
									player.sendMessage(Colors.Green + "The cuboid has been loaded.");
								}
								else if (returnCode==1){
									player.sendMessage(Colors.Rose + "Could not find the file.");
								}
								else if (returnCode==2){
									player.sendMessage(Colors.Rose + "Reading error while accessing the file.");
								}
								else if (returnCode==3){
									player.sendMessage(Colors.Rose + "The file seems to be corrupted");
								}
							}
							else{
								player.sendMessage(Colors.Rose + "No point has been selected");
							}
						}
						else{
							player.sendMessage(Colors.Rose + "This cuboid does not exist.");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "Usage : /cload <cuboid name>");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/clist")){
					if (split.length==1){
						String list = listOfCuboids(playerName);
						if (list!=null){
							player.sendMessage(Colors.Green+"Your saved cuboids :"+Colors.White+list);
						}
						else{
							player.sendMessage(Colors.Rose + "You have no saved cuboid");
						}
					}
					else if( split.length==2 && player.canIgnoreRestrictions() ){
						String list = listOfCuboids(split[1]);
						if (list!=null){
							player.sendMessage(Colors.Green+split[1]+"'s saved cuboids :"+Colors.White+list);
						}
						else{
							player.sendMessage(Colors.Rose + split[1] +" has no saved cuboid");
						}
					}
					else{
						if ( player.canIgnoreRestrictions() )
							player.sendMessage(Colors.Rose + "Usage : /clist <player name>");
						player.sendMessage(Colors.Rose + "Usage : /clist");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/cremove")){
					if (split.length>1){
						String cuboidName = split[1].toLowerCase();
						if (cuboidExists(playerName, cuboidName)){
							File toDelete = new File("cuboids/"+playerName+"/"+cuboidName+".cuboid");
							if ( toDelete.delete() ){
								player.sendMessage(Colors.LightGreen + "Cuboid sucessfuly deleted");
							}
							else{
								player.sendMessage(Colors.Rose + "Error while deleting the cuboid file");
							}
						}
						else{
							player.sendMessage(Colors.Rose + "This cuboid does not exist.");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "Usage : /cremove <cuboid name>");
					}
					return true;
				}
				
			}
		    return false;
		}
		
		/////////////////////////////
		////	BLOCK CREATION   ////
		/////////////////////////////
	
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand){		
			
			if ( itemInHand==mainToolID && (player.canUseCommand("/protect") || player.canUseCommand("/cuboid")) ){
					boolean whichPoint = Cuboid.setPoint(player.getName(), blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
					player.sendMessage(Colors.Blue + ((!whichPoint) ? "First" : "Second")+ " point is set." );	
					return true;
			}
			else if ( itemInHand==checkToolID ){
				String owners = CuboidProtection.inProtectedZone(blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
				if (owners != null)
					player.sendMessage(Colors.Yellow + "Area infos : "+Colors.White+owners);
				else
					player.sendMessage(Colors.Yellow + "This area belongs to nobody");
				return true;
			}
			else if ( (itemInHand>267 && itemInHand<280) || itemInHand==256|| itemInHand==257|| itemInHand==258|| itemInHand==290|| itemInHand==291|| itemInHand==292|| itemInHand==293 ){
				return false;
			}
			else{
				if ( CuboidProtection.toggle && !player.canIgnoreRestrictions() ){
					return isAllowed(player, blockPlaced, true);
				}
			}
					
			return false;
		}
	
		////////////////////////////
		////	BLOCK REMOVAL   ////
		////////////////////////////
		
		public boolean onBlockDestroy(Player player, Block block) {
			int targetBlockType = block.getType();
			if ( targetBlockType == 64 || targetBlockType == 69 || targetBlockType == 77 ){
				// Wood doors, buttons and levers
				return false;
			}
			else if (CuboidProtection.toggle && !player.canIgnoreRestrictions() ){
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
						lastStatus.set(indexInList, isAllowed(player, block, true));
						
						return lastStatus.get(playerList.indexOf(playerName));
					}
					else{
						return lastStatus.get(playerList.indexOf(playerName));
					}
				}
				else{
					playerList.add(playerName);
					correspondingBloc.add(block);
					lastStatus.add(isAllowed(player, block, true));
					return lastStatus.get(playerList.indexOf(playerName));
				}
			}
			return false;
		}
		
		public boolean onComplexBlockChange(Player player, ComplexBlock block){
			if ( chestProtection && !player.canIgnoreRestrictions() && etc.getServer().getBlockIdAt(block.getX(), block.getY(), block.getZ())==54 ){
				return isAllowed(player, block, false);
			}
			else{
				return false;
			}
		}
		
		public boolean onSendComplexBlock(Player player, ComplexBlock block){
			if ( chestProtection && !player.canIgnoreRestrictions() && etc.getServer().getBlockIdAt(block.getX(), block.getY(), block.getZ())==54 ){
				return isAllowed(player, block, false);
			}
			else{
				return false;
			}
		}
	
	}

}