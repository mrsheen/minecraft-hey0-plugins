import java.util.ArrayList;
import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

public class GriefAlert extends Plugin{
	// Version 3 : 28/09 09h00 GMT+2
	// for servermod100
	
	private String name = "GriefAlert";
	
	
	
	static MinecraftServer world = etc.getMCServer();
	static final Logger log = Logger.getLogger("Minecraft");
	static GriefAlertData griefAlertData = new GriefAlertData();
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Block> correspondingBloc = new ArrayList<Block>();
	
	static boolean toggleAlertes = true;
	
	
    public void enable() {
		log.info("[GriefAlert] Mod Enabled.");
			etc.getInstance().addCommand("/griefalert", "Request a jump to a player");
			etc.getInstance().addCommand("/gareload", "Reload GriefAlert data");
			
    }

    public void disable() {
		etc.getInstance().removeCommand("/griefalert");
		etc.getInstance().removeCommand("/gareload");
		
		log.info("[GriefAlert] Mod Disabled");
    }
    
	
	public boolean onCommand(Player player, String[] split) {
		String playername = player.getName();
		if (etc.getInstance().canUseCommand(playername, "/griefalert")){
			if (split[0].equalsIgnoreCase("/griefalert")){
				toggleAlertes=!toggleAlertes;
				for  (Player p : etc.getServer().getPlayerList() ) {
					if (etc.getInstance().canUseCommand(p.getName(), "/griefalert")){
						p.sendMessage(Colors.Yellow + "("+playername+") Antigrief alerts : "+ (toggleAlertes ? "enabled" : "disabled"));
					}
				}
				return true;
			}
			if (split[0].equalsIgnoreCase("/gareload")){
				griefAlertData = new GriefAlertData();
				player.sendMessage(Colors.Green+"GrieferAlert plugin reloaded");
				return true;
			}
		}
		return false;
	}

	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
		
		if ( toggleAlertes  && itemInHand != 280 && !GriefAlertData.useWatchIDs.isEmpty() ){
			short blocIndexInList = GriefAlertData.isUseWatched(blockPlaced.getType());
			if ( blocIndexInList>-1 && !etc.getInstance().canUseCommand(player.getName(), "/griefalert") ){
				String blocName = GriefAlertData.useWatchNames.get(blocIndexInList);
				String playerName = player.getName();
				String messageAlerte = Colors.Rose+playerName+" used "+(("aeiou".contains(blocName.substring(0, 1).toLowerCase())) ? "an " : "a ")+blocName;
				log.info("Antigrief alarm : "+playerName+" used "+(("aeiou".contains(blocName.substring(0, 1).toLowerCase())) ? "an " : "a ")+blocName);
				for  (Player p : etc.getServer().getPlayerList() ) {
					if (etc.getInstance().canUseCommand(p.getName(), "/griefalert")){
						p.sendMessage(messageAlerte);
					}
				}
			}
		}
		else if(itemInHand == 280 && etc.getInstance().canUseCommand(player.getName(), "/degriefstick")){
			world.e.d(blockClicked.getX(), blockClicked.getY(), blockClicked.getZ(), 0);
		}
		return false;
	}
	
	public boolean onBlockDestroy(Player player, Block block) {
		
		int blocID =  block.getType();
		if ( toggleAlertes  && !GriefAlertData.breakWatchIDs.isEmpty() ){
			
			short blocIndexInList = GriefAlertData.isBreakWatched(blocID);
			if ( blocIndexInList>-1 && !etc.getInstance().canUseCommand(player.getName(), "/griefalert") ){
				
				String playerName = player.getName();
				boolean inList = false;
				for (String p : playerList){
					if (p==playerName)
						inList = true;
				}
				
				if (inList){
					Block listedBlock = correspondingBloc.get(playerList.indexOf(playerName));
					if (listedBlock.getX() != block.getX() || listedBlock.getY() != block.getY() || listedBlock.getZ() != block.getZ() ){
						correspondingBloc.remove(playerList.indexOf(playerName));
						playerList.remove(playerName);
						playerList.add(playerName);
						correspondingBloc.add(block);
						
						String blocName = GriefAlertData.breakWatchNames.get(blocIndexInList);
						log.info("Antigrief alarm : "+playerName+" is breaking "+(("aeiou".contains(blocName.substring(0, 1).toLowerCase())) ? "an " : "a ")+blocName);
						
						String messageAlerte = Colors.Rose+playerName+" is breaking "+(("aeiou".contains(blocName.substring(0, 1).toLowerCase())) ? "an " : "a ")+blocName;
						for  (Player p : etc.getServer().getPlayerList() ) {
							if (etc.getInstance().canUseCommand(p.getName(), "/griefalert")){
								p.sendMessage(messageAlerte);
							}
						}
					}
				}
				else{
					playerList.add(playerName);
					correspondingBloc.add(block);
					
					String blocName = GriefAlertData.breakWatchNames.get(blocIndexInList);
					log.info("Antigrief alarm : "+playerName+" is breaking "+(("aeiou".contains(blocName.substring(0, 1).toLowerCase())) ? "an " : "a ")+blocName);
					
					String messageAlerte = Colors.Rose+playerName+" is breaking "+(("aeiou".contains(blocName.substring(0, 1).toLowerCase())) ? "an " : "a ")+blocName;
					for  (Player p : etc.getServer().getPlayerList() ) {
						if (etc.getInstance().canUseCommand(p.getName(), "/griefalert")){
							p.sendMessage(messageAlerte);
						}
					}
				}
			}
		}
		
		return false;
	}



}
