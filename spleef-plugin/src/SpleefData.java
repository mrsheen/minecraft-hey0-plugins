import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpleefData {
	
	static Server world = etc.getServer();
	static final Logger log = Logger.getLogger("Minecraft");
	
	static ArrayList<String> spleefList = new ArrayList<String>();
	static ArrayList<Integer> triggerBlocks = new ArrayList<Integer>();
	static ArrayList<ArrayList<Integer>> changeBlocks = new ArrayList<ArrayList<Integer>>();
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Integer> selectionStatus = new ArrayList<Integer>();
	static ArrayList<Integer> pointsCoordinates = new ArrayList<Integer>();
		
	private static int getPlayerIndex(String playerName){
		
		boolean inList = false;
		for (String p : playerList){
			if (p==playerName)
				inList = true;
		}
		
		if (!inList){
			playerList.add(playerName);
			selectionStatus.add(1);
			pointsCoordinates.add(null);	// A faire : plus élégant
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
		}
				
		return playerList.indexOf(playerName);
	}
	
	public static int setPoint(String playerName, int X, int Y, int Z){
		
		int index = getPlayerIndex(playerName);
		int secondPoint = selectionStatus.get(index);
		if ( selectionStatus.get(index) == 1 ){
			pointsCoordinates.set(index*9, X);
			pointsCoordinates.set(index*9+1, Y);
			pointsCoordinates.set(index*9+2, Z);
			pointsCoordinates.set(index*9+3, null);
			pointsCoordinates.set(index*9+4, null);
			pointsCoordinates.set(index*9+5, null);
			pointsCoordinates.set(index*9+6, null);
			pointsCoordinates.set(index*9+7, null);
			pointsCoordinates.set(index*9+8, null);
			selectionStatus.set(index, 2);
			
		}
		else if ( selectionStatus.get(index) == 2){
			pointsCoordinates.set(index*9+3, X);
			pointsCoordinates.set(index*9+4, Y);
			pointsCoordinates.set(index*9+5, Z);
			selectionStatus.set(index, 3);
		}
		else {
			pointsCoordinates.set(index*9+6, X);
			pointsCoordinates.set(index*9+7, Y);
			pointsCoordinates.set(index*9+8, Z);
			selectionStatus.set(index, 1);
		}
		return selectionStatus.get(index);
	}
	
	
	public static void loadSpleefData(){
		File dataSource = new File("spleefdata.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("spleefdata.txt", true);
                writer.append("#Data for the Spleef plugin is located in this file\r\n");
                writer.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "[Spleef] : Exception while creating spleefdata.txt");
            } finally {
            	try{
            		writer.close();
            	}
            	catch(IOException e){
            		 log.log(Level.SEVERE, "[Spleef] : Exception while closing loadSpleefData writer", e);
            	}
            }
		} else {
			try {
				spleefList = new ArrayList<String>();
				triggerBlocks = new ArrayList<Integer>();
				changeBlocks = new ArrayList<ArrayList<Integer>>();
				Scanner scanner = new Scanner(dataSource);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.startsWith("#") || line.equals("")) {
						continue;
					}
					String[] donnees = line.split(",");
					
					if (donnees.length != 6) {
						continue;
					}
			        spleefList.add(donnees[0]);
					int spleefindex = spleefList.indexOf(donnees[0]);
			        triggerBlocks.add(Integer.parseInt(donnees[1]));
			        triggerBlocks.add(Integer.parseInt(donnees[2]));
			        triggerBlocks.add(Integer.parseInt(donnees[3]));
					changeBlocks.add(new ArrayList<Integer>());
					changeBlocks.add(new ArrayList<Integer>());
			        
			        String[] Airblocks = donnees[4].split(";");
			        	for (int i = 1; i<Airblocks.length;i++) {
							changeBlocks.get(spleefindex*2).add(Integer.parseInt(Airblocks[i]));
			      	  }
			        
			        String[] Solidblocks = donnees[5].split(";");
			      	  for (int i = 1; i<Solidblocks.length;i++) {
							changeBlocks.get(spleefindex*2+1).add(Integer.parseInt(Solidblocks[i]));
			     	   }
				}
				scanner.close();
				log.info("Spleef plugin : successfuly loaded.");
			} catch (Exception e) {
				log.log(Level.SEVERE, "Spleef plugin : Error while reading spleefdata.txt", e);
			}
			
		}
		return;
	}
	
	public static void addSpleefBlocks(String playerName, Integer AirID, Integer SolidID, String spleefName)
	{
		int index = getPlayerIndex(playerName);
		if( pointsCoordinates.get(index*9+8) != null) {
			
			spleefList.add(spleefName);
			int spleefindex = spleefList.indexOf(spleefName);
			triggerBlocks.add(pointsCoordinates.get(index*9+6));
			triggerBlocks.add(pointsCoordinates.get(index*9+7));
			triggerBlocks.add(pointsCoordinates.get(index*9+8));
			changeBlocks.add(new ArrayList<Integer>());
			changeBlocks.add(new ArrayList<Integer>());
			
			int startX = ( pointsCoordinates.get(index*9) <= pointsCoordinates.get(index*9+3) ) ? pointsCoordinates.get(index*9) : pointsCoordinates.get(index*9+3);
			int startY = ( pointsCoordinates.get(index*9+1) <= pointsCoordinates.get(index*9+4) ) ? pointsCoordinates.get(index*9+1) : pointsCoordinates.get(index*9+4);
			int startZ = ( pointsCoordinates.get(index*9+2) <= pointsCoordinates.get(index*9+5) ) ? pointsCoordinates.get(index*9+2) : pointsCoordinates.get(index*9+5);
		
			int endX = ( pointsCoordinates.get(index*9) <= pointsCoordinates.get(index*9+3)  ) ? pointsCoordinates.get(index*9+3) : pointsCoordinates.get(index*9);
			int endY = ( pointsCoordinates.get(index*9+1) <= pointsCoordinates.get(index*9+4) ) ? pointsCoordinates.get(index*9+4) : pointsCoordinates.get(index*9+1);
			int endZ = ( pointsCoordinates.get(index*9+2) <= pointsCoordinates.get(index*9+5) ) ? pointsCoordinates.get(index*9+5) : pointsCoordinates.get(index*9+2);
		
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					if(world.getBlockIdAt(i,j,k) == AirID) {
						changeBlocks.get(spleefindex*2).add(i);
						changeBlocks.get(spleefindex*2).add(j);
						changeBlocks.get(spleefindex*2).add(k);
					} else if (world.getBlockIdAt(i,j,k) == SolidID) {
						changeBlocks.get(spleefindex*2+1).add(i);
						changeBlocks.get(spleefindex*2+1).add(j);
						changeBlocks.get(spleefindex*2+1).add(k);
					}
				}
			}
		}
			
			
			
			
			//Write to file
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter("spleefdata.txt", true));
				String throwawaystring = spleefName+","+pointsCoordinates.get(index*9+6)+","+pointsCoordinates.get(index*9+7)+","+pointsCoordinates.get(index*9+8)+",";
				writer.append(throwawaystring+"0;");
				for(int i = 0; i<changeBlocks.get(spleefindex*2).size(); i++) {
					if( i == 0) {
						writer.append(Integer.toString(changeBlocks.get(spleefindex*2).get(i)));
					} else {
						writer.append(";"+Integer.toString(changeBlocks.get(spleefindex*2).get(i)));
					}
				}
				writer.append(",0;");
				for(int i = 0; i<changeBlocks.get(spleefindex*2+1).size(); i++) {
					if( i == 0) {
						writer.append(Integer.toString(changeBlocks.get(spleefindex*2+1).get(i)));
					} else {
						writer.append(";"+Integer.toString(changeBlocks.get(spleefindex*2+1).get(i)));
					}
				}
				
				writer.newLine();
				writer.close();
			} catch (Exception e) {
				log.log(Level.SEVERE, "[Spleef] : Error while writing spleefdata.txt", e);
				return;
			}
		} else {
			return;
		}
	
	}
	
	public static void listthespleefs(Player player)
	{
		for (int i = 0; i<spleefList.size(); i++)
		{
			player.sendMessage(spleefList.get(i));
		}
		return;
	}
	
	public static boolean trytoResetArena(Player player, Block blockClicked) {
		boolean returnbool = false;
		if( (triggerBlocks.size()%3) == 0) {
			for(int i = 0; i<triggerBlocks.size()/3;i++) {
				if((triggerBlocks.get(i*3) == blockClicked.getX()) && 
					(triggerBlocks.get(i*3+1) == blockClicked.getY()) && 
					(triggerBlocks.get(i*3+2) == blockClicked.getZ())) {
					//player.sendMessage("well this works");
					resetSpleefArena(spleefList.get(i), blockClicked,player);
					returnbool = true;
				}
			}
		} else {
			
		}
		return returnbool;
	}
	
	public static void resetSpleefArena(String SpleefName, Block blockClicked, Player player) {
		//player.sendMessage("well this works");
		int spleefindex = spleefList.indexOf(SpleefName);
		if(((changeBlocks.get(spleefindex*2).size()%3) == 0) && ((changeBlocks.get(spleefindex*2+1).size()%3) == 0)) {
			for (int i = 0; i<changeBlocks.get(spleefindex*2).size()/3;i++) {
				world.setBlockAt(0,
									changeBlocks.get(spleefindex*2).get(i*3),
									changeBlocks.get(spleefindex*2).get(i*3+1),
									changeBlocks.get(spleefindex*2).get(i*3+2));
			}
			//player.sendMessage("replaced air");
			for (int i = 0; i<changeBlocks.get(spleefindex*2+1).size()/3;i++) {
				world.setBlockAt(blockClicked.getType(),
									changeBlocks.get(spleefindex*2+1).get(i*3),
									changeBlocks.get(spleefindex*2+1).get(i*3+1),
									changeBlocks.get(spleefindex*2+1).get(i*3+2));
			}
			//player.sendMessage("replaced blocks");
		} else {
			
		}
		return;	
	}
	
	public static boolean removeSpleef(String Spleeftoremove) {
		
		return true;
	}
}





















