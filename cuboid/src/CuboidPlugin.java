import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CuboidPlugin extends Plugin {
	// Version 14 : 11/11 15h30 GMT+1
	// for servermod 123-125+
		
	public String name = "CuboidPlugin";
	
	static final Logger log = Logger.getLogger("Minecraft");

	static boolean logging = false;
	static ArrayList<Integer> operableItems;
	static boolean allowBlacklistedBlocks = false;
	static boolean chestProtection = true;
	static int mainToolID = 269;
	static int checkToolID = 268;
	
	static Timer timer = new Timer();	// TODO
	
	public void enable(){
		checkFolder();
		CuboidProtection.loadProtectedAreas();
		// TODO cuboidAreas
		loadProperties(); 
	}
	
	public void disable(){
		
	}
	
	private boolean checkFolder(){
		
		File folder = new File("cuboids");
		if ( !folder.exists() ){
			if ( !folder.mkdir() ){
				log.severe("CuboidPlugin : could not create the cuboids folder");
				return false;
			}
		}
		
		return true;		
	}
	
	private void loadProperties(){
		if ( !new File("cuboids/cuboidPlugin.properties").exists() ){
			FileWriter writer = null;
            try {
            	writer = new FileWriter("cuboids/cuboidPlugin.properties");
                writer.write("#The selection tool, default : 269 = wooden shovel\r\n");
                writer.write("mainToolID=269\r\n");
                writer.write("#The information tool, default : 268 = wooden sword\r\n");
                writer.write("checkToolID=268\r\n");
                writer.write("#Do you want the chests protected too ?\r\n");
                writer.write("chestProtection=true\r\n");
                writer.write("#Which protected cuboids have priority ? Newest or oldest ?\r\n");
                writer.write("newestHavePriority=true\r\n");
                writer.write("#List of groups that are forbiden to build on the entire world.\r\n");
                writer.write("#Delimiter is a coma. Leave blank if none\r\n");
                writer.write("restrictedGroups=\r\n");
                writer.write("#List of block id that are activable in protected areas\r\n");
                writer.write("operableItemIDs=64,69,77,84\r\n");
                writer.write("#Display a warning when touching a protected block ?\r\n");
                writer.write("protectionWarning=false\r\n");
                writer.write("#Should every cuboid action be logged ?\r\n");
                writer.write("fullLogging=false\r\n");
                writer.write("#Should players be able to spawn blacklisted blocks with cuboid ?\r\n");
                writer.write("allowBlacklistedBlocks=false\r\n");
                writer.write("#Height and deptht added to protected zones\r\n");
                writer.write("#(only when a flat area is selected to be protected)\r\n");
                writer.write("minProtectedHeight=0\r\n");
            }
            catch (Exception e){
                log.log(Level.SEVERE, "Could not create cuboidPlugin.properties file inside 'cuboids' folder.", e);
            }
            finally {
                try{
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Exception while closing writer for cuboidPlugin.properties", e);
                }
            }
		}
		
		PropertiesFile properties = new PropertiesFile("cuboids/cuboidPlugin.properties");
		
		try {
			// Protection properties
			CuboidProtection.addedHeight = properties.getInt("minProtectedHeight", 0);
			CuboidProtection.protectionWarn = properties.getBoolean("protectionWarning", false);
			CuboidProtection.newestHavePriority = properties.getBoolean("newestHavePriority", true);
			String[] restrictedGroups = properties.getString("restrictedGroups", "").split(",");
			for (int i = 0; i < restrictedGroups.length; i++){
				restrictedGroups[i] = restrictedGroups[i].trim();
			}
			CuboidProtection.restrictedGroups = restrictedGroups;
			
			// general cuboid properties
			logging = properties.getBoolean("fullLogging", false);
			allowBlacklistedBlocks = properties.getBoolean("allowBlacklistedBlocks", false);
			chestProtection = properties.getBoolean("chestProtection", true);	
			mainToolID = properties.getInt("mainToolID", 269);
			checkToolID = properties.getInt("checkToolID", 268);
			
			// generating list of operable items within protected areas
			operableItems = new ArrayList<Integer>();
			String[] operableString = properties.getString("operableItemIDs", "").split(",");
			for (String operableItem : operableString){
				try{
					int operableItemID = Integer.parseInt(operableItem);
					operableItems.add(operableItemID);
				}
				catch( NumberFormatException e ){
					log.info("CuboidPlugin : invalid item ID skipped : " + operableItem);
				}
			}
			
			log.info("CuboidPlugin : properties loaded");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while reading from server.properties", e);
        }

	}
	
	////////////////////////
	////	FONCTIONS	////
	////////////////////////

	private boolean isValidBlockID(int blocID){
		if (blocID >= 0 && blocID <=91){
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
	
	private boolean isBlackListedBlockID(int blocID){	
		if ( allowBlacklistedBlocks ){
			return false;
		}
		return etc.getInstance().isOnItemBlacklist(blocID);
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
	
	private static String ownerString (ArrayList<String> list){
		String owners = "";
		for ( String owner : list ){
			owners += " " + owner;
		}
		return owners.trim();
	}
	
	//////////////////////////////
	////	LISTENER STUFF    ////
	//////////////////////////////
	
	public void initialize(){
		CuboidListener listener = new CuboidListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_BROKEN, listener, this, PluginListener.Priority.MEDIUM);
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
					if ( CuboidProtection.isOwner(player, areaName) || player.canUseCommand("/ignoresOwnership") ){
						
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
					if ( CuboidProtection.isOwner(player, areaName) || player.canUseCommand("/ignoresOwnership") ){
						
						String[] parameters = new String[split.length-2];
						for (int i=1; i<split.length-1; i++){
							String toRemove = split[i].trim().toLowerCase();
							if ( toRemove.startsWith("o:") && !player.canUseCommand("/ignoresOwnership") ){
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
				String cuboidList = CuboidProtection.displayProtectedCuboids();
				player.sendMessage(Colors.Yellow + "Protected areas"+Colors.White+" :"+cuboidList);
				return true;
			}
			
			else if (split[0].equalsIgnoreCase("/owners" )){
				if (split.length==2){
					String liste = CuboidProtection.ownersString( split[1] );
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
			
			else if ( player.canUseCommand("/protect") ){
				boolean highProtect = split[0].equalsIgnoreCase("/highprotect" );
				if ( split[0].equalsIgnoreCase("/protect") || highProtect ){
					if (Cuboid.isReady(playerName, true)){
						ArrayList<String> ownersList = new ArrayList<String>();
						int paramSize = split.length;
						if (paramSize > 2){
							for (short i=1; i<paramSize-1; i++){
								ownersList.add(split[i]);
							}
							String cuboidName = split[paramSize-1].trim().toLowerCase();
							
							short returnCode = CuboidProtection.protegerCuboid(playerName, ownersList, cuboidName, highProtect);
							
							if (returnCode==0){
								player.sendMessage(Colors.LightGreen + "New protected zone created.");
								player.sendMessage(Colors.LightGreen + "Name : "+Colors.White+cuboidName);
								player.sendMessage(Colors.LightGreen + "Owners : "+Colors.White+ownerString(ownersList));
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
					loadProperties();
					player.sendMessage(Colors.Green + "CuboidPlugin properties reloaded");
					
					CuboidProtection.toggle = false;
					CuboidProtection.loadProtectedAreas();
					CuboidProtection.toggle = true;
					player.sendMessage(Colors.Green + "Cuboids coordinates reloaded");
					
					// TODO : reload cuboidAreas
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
						player.sendMessage(Colors.LightGreen +"The selected cuboid size is : "
								+ Cuboid.blocksCount(playerName) +" blocks" );
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
			    }
							
				else if (split[0].equalsIgnoreCase("/cdel")){
					if (Cuboid.isReady(playerName, true)){
						Cuboid.emptyCuboid(playerName);
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
								
								if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blocID) ){
									player.sendMessage(Colors.Rose + blocID + " is a blacklisted block type !");
									return true;
								}
								
								Cuboid.fillCuboid(playerName, blocID);
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
							
							int blockID = replaceParams[replaceParams.length-1];
							if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blockID) ){
								player.sendMessage(Colors.Rose + blockID + " is a blacklisted block type !");
								return true;
							}
							
							Cuboid.replaceBlocks(playerName, replaceParams);
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
				
				////////////////////////////////
				////	MOVEMENT COMMANDS	////
				////////////////////////////////
				
				else if (split[0].equalsIgnoreCase("/cmove")){
					if (Cuboid.isReady(playerName, true)){
						if ( split.length < 3 ){
							player.sendMessage(Colors.Rose + "Usage : /cmove <direction> <distance>");
							player.sendMessage(Colors.Rose + "Direction : Up/Down/North/East/West/South");
							return true;
						}
						
						int howFar = 0;
						try {
							howFar = Integer.parseInt( split[2] );
							if ( howFar < 0 ){
								player.sendMessage(Colors.Rose + "Distance must be > 0 !");
								return true;
							}
						}
						catch (NumberFormatException n) {
							player.sendMessage(Colors.Rose + split[2] +" is not a valid distance.");
							return true;
						}
						
						Cuboid.moveCuboidContent(player, split[1], howFar);
	
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}	
					return true;
				}
				
				///////////////////////////////////////////////
				////	CIRCLE/SPHERE BUILDING COMMANDS    ////
				///////////////////////////////////////////////
							
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
							
							if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blockID) ){
								player.sendMessage(Colors.Rose + blockID + " is a blacklisted block type !");
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
								Cuboid.buildCircle(playerName, radius, blockID, height, true);
								player.sendMessage(Colors.LightGreen + "The "+((height==0)?"disc":"cylinder")+" has been build");
							}
							else{
								Cuboid.buildCircle(playerName, radius, blockID, height, false);
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
								player.sendMessage(Colors.Rose + "The radius has to be greater than 1");
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
							
							if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blockID) ){
								player.sendMessage(Colors.Rose + blockID + " is a blacklisted block type !");
								return true;
							}
							
							if(ball){
								Cuboid.buildShpere(playerName, radius, blockID, true);
								player.sendMessage(Colors.LightGreen + "The ball has been built");
							}
							else{
								Cuboid.buildShpere(playerName, radius, blockID, false);
								player.sendMessage(Colors.LightGreen + "The sphere has been built");
							}
							
						}
						else{
							if (ball){
								player.sendMessage(Colors.Rose + "Usage : /cball <radius> <block id|name>");
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
							
							if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blockID) ){
								player.sendMessage(Colors.Rose + blockID + " is a blacklisted block type !");
								return true;
							}
							
							Cuboid.buildCuboidFaces(playerName, blockID, true);
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
							
							if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blockID) ){
								player.sendMessage(Colors.Rose + blockID + " is a blacklisted block type !");
								return true;
							}
							
							Cuboid.buildCuboidFaces(playerName, blockID, false);
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
				
				///////////////////////////////////////
				////    OTHER BUILDING COMMANDS    ////
				///////////////////////////////////////
				
				else if (split[0].equalsIgnoreCase("/cpyramid") ){
					if (Cuboid.isReady(playerName, false)){
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
								player.sendMessage(Colors.Rose + "The radius has to be greater than 1");
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
							if ( !player.canIgnoreRestrictions() && isBlackListedBlockID(blockID) ){
								player.sendMessage(Colors.Rose + blockID + " is a blacklisted block type !");
								return true;
							}
							
							boolean filled = true;
							if ( split.length == 4 && split[3].equalsIgnoreCase("empty") ){
								filled = false;
							}

							Cuboid.buildPyramid(playerName, radius, blockID, filled);
							player.sendMessage(Colors.LightGreen + "The pyramid has been built");	
						}
						else{
							player.sendMessage(Colors.Rose + "Usage : /cpyramid <radius> <block id|name>");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "No point has been selected");
					}
					return true;
				}
				
				
				/////////////////////////////////
				////    COPY/PASTE SYSTEM    ////
				/////////////////////////////////
				
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
				
				else if (split[0].equalsIgnoreCase("/ccopy")){
					if (Cuboid.isReady(playerName, true)){
						Cuboid.copyCuboid(playerName, true);
						player.sendMessage(Colors.Green + "Selected cuboid is copied. Ready to paste !");
					}
					else{
						player.sendMessage(Colors.Rose + "No cuboid has been selected");
					}
					return true;
				}
				
				else if (split[0].equalsIgnoreCase("/cpaste")){	
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
					else if( split.length==2 && player.isAdmin() ){
						String list = listOfCuboids(split[1]);
						if (list!=null){
							player.sendMessage(Colors.Green+split[1]+"'s saved cuboids :"+Colors.White+list);
						}
						else{
							player.sendMessage(Colors.Rose + split[1] +" has no saved cuboid");
						}
					}
					else{
						if ( player.isAdmin() )
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
				
				else if (split[0].equalsIgnoreCase("/cshare")){
					if (split.length>2){
						String cuboidName = split[1].toLowerCase();
						String targetPlayerName = "";
						Player targetPlayer = etc.getServer().matchPlayer(split[2]);
						
						if ( targetPlayer!=null ){
							targetPlayerName = targetPlayer.getName();
						}
						else{
							player.sendMessage(Colors.Rose + "Player " + split[2] + " seems to be offline");
							return true;
						}
		
						if (cuboidExists(playerName, cuboidName)){
							if (!cuboidExists(targetPlayerName, cuboidName)){
								
								File ownerFolder = new File("cuboids/"+targetPlayerName);
					            try {
						            if (!ownerFolder.exists()){
						            	ownerFolder.mkdir();
						            }
					            }
					            catch( Exception e){
					            	player.sendMessage(Colors.Rose + "Error while creating targer folder");
					            	return true;
					            }
								
								if ( CuboidData.copyFile(new File("cuboids/"+playerName+"/"+cuboidName+".cuboid"),
										new File("cuboids/"+targetPlayerName+"/"+cuboidName+".cuboid")) ){
									player.sendMessage(Colors.LightGreen + "You shared "+ cuboidName + " with " + targetPlayerName);
									for  (Player p : etc.getServer().getPlayerList() ) {
										if( p.getName().equals(targetPlayerName) ){
											p.sendMessage(Colors.LightGreen + playerName + " shared "+ cuboidName +
													".cuboid with you");
										}
									}	
								}
								else{
									player.sendMessage(Colors.Rose + "Error while copying the the cuboid file");
								}	
							}
							else{
								player.sendMessage(Colors.Rose + targetPlayerName+" already has a cuboid named "+cuboidName);
							}			
						}
						else{
							player.sendMessage(Colors.Rose + "This cuboid does not exist.");
						}
					}
					else{
						player.sendMessage(Colors.Rose + "Usage : /cshare <cuboid name> <player name>");
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
				String owners = CuboidProtection.areaInfo(blockClicked.getX(), blockClicked.getY(), blockClicked.getZ());
				if (owners != null)
					player.sendMessage(Colors.Yellow + "Area infos : "+Colors.White+owners);
				else
					player.sendMessage(Colors.Yellow + "This area belongs to nobody");
				return true;
			}
			
			else if ( (itemInHand>267 && itemInHand<280) || itemInHand==256 || itemInHand==257 || itemInHand==258 || itemInHand==290
					|| itemInHand==291|| itemInHand==292 || itemInHand==293 || itemInHand==0 ){
				return false;	// allow tools rightclicks for runecraft
			}
			
			else{
				if ( CuboidProtection.toggle && !player.canUseCommand("/ignoresOwnership") ){
					
					// TODO
					boolean allowed = CuboidProtection.isAllowed(player, blockPlaced, true);
					if ( !allowed && (blockPlaced.getType()==326 || blockPlaced.getType()==327) ){
						timer.schedule(new RemoveFluids(blockPlaced), 200);
					}
					return !allowed;
					// return !CuboidProtection.isAllowed(player, blockPlaced, true);
				}
			}
					
			return false;
		}
	
		////////////////////////////
		////	BLOCK REMOVAL   ////
		////////////////////////////
		
		
		public boolean onBlockBreak(Player player, Block block) {
			if (CuboidProtection.toggle && !player.canUseCommand("/ignoresOwnership") ){
				return !CuboidProtection.isAllowed(player, block, true);
			}
			return false;
		}
		
		////////////////////////////////
		////	BLOCK INTERACTION   ////
		////////////////////////////////
		
		public boolean onBlockDestroy(Player player, Block block) {
			int blockType = block.getType();
			
			if ( operableItems.contains(blockType) ){
				return false;	// allows some items to be manipulated
			}
			
			if (CuboidProtection.toggle && !player.canUseCommand("/ignoresOwnership") ){
				if (!CuboidProtection.isAllowed(player, block, false)) {
					if(block.getStatus()==3) {
						player.sendMessage(Colors.Blue + "This area is owned by another player");
					}
					return true;
				}
			}
			
			return false;
		}
		
		public boolean onComplexBlockChange(Player player, ComplexBlock block){
			if ( chestProtection && !player.canUseCommand("/ignoresOwnership") && etc.getServer().getBlockIdAt(block.getX(),
					block.getY(), block.getZ())==54 ){
				return !CuboidProtection.isAllowed(player, block, false);
			}
			else{
				return false;
			}
		}
		
		public boolean onSendComplexBlock(Player player, ComplexBlock block){
			if ( chestProtection && !player.canUseCommand("/ignoresOwnership") && etc.getServer().getBlockIdAt(block.getX(),
					block.getY(), block.getZ())==54 ){
				return !CuboidProtection.isAllowed(player, block, false);
			}
			else{
				return false;
			}
		}
	} // end cuboidListener
	
	public class RemoveFluids extends TimerTask{
		Block block;
		public RemoveFluids( Block block ){
			this.block = block;
		}
		public void run(){	// TODO			
			etc.getServer().setBlockAt(0, this.block.getX(), this.block.getY(), this.block.getZ());
		}
	}

}