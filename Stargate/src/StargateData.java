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

public class StargateData {
	
	static Server world = etc.getServer();
	static final Logger log = Logger.getLogger("Minecraft");
	
	static ArrayList<String> StargateList = new ArrayList<String>();
	static ArrayList<Integer> StargateLocations = new ArrayList<Integer>();
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Integer> selectionStatus = new ArrayList<Integer>();
	static ArrayList<Integer> pointsCoordinates = new ArrayList<Integer>();
	static ArrayList<Integer> landing = new ArrayList<Integer>();
		
	private static int getPlayerIndex(String playerName){
		
		boolean inList = false;
		for (String p : playerList){
			if (p==playerName)
				inList = true;
		}
		
		if (!inList){
			playerList.add(playerName);
			selectionStatus.add(1);
			landing.add(-1);
			pointsCoordinates.add(null);	
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
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
			pointsCoordinates.set(index*12, X);
			pointsCoordinates.set(index*12+1, Y);
			pointsCoordinates.set(index*12+2, Z);
			pointsCoordinates.set(index*12+3, null);
			pointsCoordinates.set(index*12+4, null);
			pointsCoordinates.set(index*12+5, null);
			pointsCoordinates.set(index*12+6, null);
			pointsCoordinates.set(index*12+7, null);
			pointsCoordinates.set(index*12+8, null);
			pointsCoordinates.set(index*12+9, null);
			pointsCoordinates.set(index*12+10, null);
			pointsCoordinates.set(index*12+11, null);
			selectionStatus.set(index, 2);
			
		}
		else if ( selectionStatus.get(index) == 2){
			pointsCoordinates.set(index*12+3, X);
			pointsCoordinates.set(index*12+4, Y);
			pointsCoordinates.set(index*12+5, Z);
			selectionStatus.set(index, 3);
		}
		else if ( selectionStatus.get(index) == 3){
			pointsCoordinates.set(index*12+6, X);
			pointsCoordinates.set(index*12+7, Y);
			pointsCoordinates.set(index*12+8, Z);
			selectionStatus.set(index, 4);
		}
		else {
			pointsCoordinates.set(index*12+9, X);
			pointsCoordinates.set(index*12+10, Y);
			pointsCoordinates.set(index*12+11, Z);
			selectionStatus.set(index, 1);
		}
		return selectionStatus.get(index);
	}
	
	public static void Savestargate(Player player, String[] split) {
		int playerindex = getPlayerIndex(player.getName())*12;
		if(pointsCoordinates.get(playerindex+11) != null) {
			int facingin =enumdirections(split[2]);
			int facingout = enumdirections(split[3]);
			int ringblocktype = 0;
			if( (facingin == -1) || (facingout == -1) ) {
				player.sendMessage("Incorrect");
				return;
			}
			try {
				ringblocktype = Integer.parseInt(split[4]);
			} catch (NumberFormatException nfe) {
				player.sendMessage("Incorrect");
				return;
			}
			StargateList.add(split[1]);
			for(int i=0;i<3;i++) {
				if(pointsCoordinates.get(playerindex+i) > pointsCoordinates.get(playerindex+i+3)) {
					StargateLocations.add(pointsCoordinates.get(playerindex+i+3));
					StargateLocations.add(pointsCoordinates.get(playerindex+i)+1);
				} else {
					StargateLocations.add(pointsCoordinates.get(playerindex+i));
					StargateLocations.add(pointsCoordinates.get(playerindex+i+3)+1);
				}
			}
			for(int i=6;i<9;i++) {
				if(pointsCoordinates.get(playerindex+i) > pointsCoordinates.get(playerindex+i+3)) {
					StargateLocations.add(pointsCoordinates.get(playerindex+i+3));
					StargateLocations.add(pointsCoordinates.get(playerindex+i)+1);
				} else {
					StargateLocations.add(pointsCoordinates.get(playerindex+i));
					StargateLocations.add(pointsCoordinates.get(playerindex+i+3)+1);
				}
			}
			StargateLocations.add(facingout-facingin);
			StargateLocations.add(ringblocktype);
			
						try {
				BufferedWriter writer = new BufferedWriter(new FileWriter("Stargatedata.txt", true));
				String throwawaystring = split[1];
				int stargateindex = StargateList.indexOf(split[1])*14;
				player.sendMessage("stargate length : " + StargateLocations.size());
				for(int i=0; i<14;i++) {
					throwawaystring = throwawaystring + ',' + StargateLocations.get(stargateindex+i);
				}
				writer.append(throwawaystring);
				writer.newLine();
				writer.close();
			} catch (Exception e) {
				log.log(Level.SEVERE, "[Stargate] : Error while writing Stargatedata.txt", e);
				return;
			}
			
			player.sendMessage("Stargate saved");
		} else {
			player.sendMessage("Incorrect");
		}
		return;
	}
	
	public static int enumdirections(String dir) {
		switch (dir.toLowerCase().charAt(0)) {
			case 'n': return 0;
			case 'e': return 1;
			case 's': return 2;
			case 'w': return 3;
			
			default: return -1;
		}
	}
	
	public static void Checkplayerpos(Location to,Player player) {
		if(StargateLocations.size()>0) {
			int playerIndex = getPlayerIndex(player.getName());
			
			if(landing.get(playerIndex) >= 0) {
				int i = landing.get(playerIndex);
				if(player.getX()>=StargateLocations.get(i*14+6) && player.getX()<StargateLocations.get(i*14+7) &&
				player.getY()>=StargateLocations.get(i*14+8) && player.getY()<StargateLocations.get(i*14+9) &&
				player.getZ()>=StargateLocations.get(i*14+10) && player.getZ()<StargateLocations.get(i*14+11) ) {
					return;
				} else {
					landing.set(playerIndex,-1);
					return;
				}
			} else {
				for(int i=0;i<StargateLocations.size()/14;i++) {
				if(player.getX()>=StargateLocations.get(i*14) && player.getX()<StargateLocations.get(i*14+1) &&
				player.getY()>=StargateLocations.get(i*14+2) && player.getY()<StargateLocations.get(i*14+3) &&
				player.getZ()>=StargateLocations.get(i*14+4) && player.getZ()<StargateLocations.get(i*14+5) ) {
					if(StargateLocations.get(i*14+13) != 0) {
						if(!checkforblockincuboid(new Block(0,(int)Math.floor(player.getX()),(int)Math.floor(player.getY()),(int)Math.floor(player.getZ())),
												new Block(0,(int)Math.floor(player.getX()),StargateLocations.get(i*14+3)+1,(int)Math.floor(player.getZ())),
												StargateLocations.get(i*14+13)) ||
							!checkforblockincuboid(new Block(0,(int)Math.floor(player.getX()),StargateLocations.get(i*14+2)-1,(int)Math.floor(player.getZ())),
												new Block(0,(int)Math.floor(player.getX()),(int)Math.floor(player.getY()),(int)Math.floor(player.getZ())),
												StargateLocations.get(i*14+13))) {
							//player.sendMessage("In but out");
							return;
						}
					}
					player.sendMessage("Woosh!");
					player.teleportTo(fromto((double)StargateLocations.get(i*14), 
											(double)StargateLocations.get(i*14+1), 
											(double)StargateLocations.get(i*14+6), 
											(double)StargateLocations.get(i*14+7), 
											player.getX()),
					fromto((double)StargateLocations.get(i*14+2), 
											(double)StargateLocations.get(i*14+3), 
											(double)StargateLocations.get(i*14+8), 
											(double)StargateLocations.get(i*14+9), 
											player.getY()),
					fromto((double)StargateLocations.get(i*14+4), 
											(double)StargateLocations.get(i*14+5), 
											(double)StargateLocations.get(i*14+10), 
											(double)StargateLocations.get(i*14+11), 
											player.getZ()),
					player.getRotation() + (float)StargateLocations.get(i*14+12) * 90,
					player.getPitch());
					landing.set(playerIndex,i);
					return;
					}
				}
				
			}
		}
	}
	
	public static double fromto(double minfrom, double maxfrom, double minto, double maxto, double playerloc) {
	return (minto + ((maxto-minto) * (playerloc - minfrom) / (maxfrom - minfrom)));	
	}
	
	public static boolean checkforblockincuboid(Block blockone, Block blocktwo, int blocktype) {
		for(int i=blockone.getX();i<=blocktwo.getX();i++) {
			for(int j=blockone.getY();j<=blocktwo.getY();j++) {
				for(int k=blockone.getZ();k<=blocktwo.getZ();k++) {
					if(world.getBlockIdAt(i,j,k) == blocktype) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static void loadStargateData(){
		File dataSource = new File("Stargatedata.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("Stargatedata.txt", true);
                writer.append("#Data for the Stargate plugin is located in this file\r\n");
                writer.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "[Stargate] : Exception while creating Stargatedata.txt");
            } finally {
            	try{
            		writer.close();
            	}
            	catch(IOException e){
            		 log.log(Level.SEVERE, "[Stargate] : Exception while closing StargateData writer", e);
            	}
            }
		} else {
			try {
				StargateList = new ArrayList<String>();
				StargateLocations = new ArrayList<Integer>();
				
				Scanner scanner = new Scanner(dataSource);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.startsWith("#") || line.equals("")) {
						continue;
					}
					String[] split = line.split(",");
					
					if (split.length != 15) {
						continue;
					}
			        StargateList.add(split[0]);
			        for(int i=1;i<15;i++) {
			        	StargateLocations.add(Integer.parseInt(split[i]));
			        }
				}
				scanner.close();
				log.info("Stargate plugin : successfuly loaded.");
			} catch (Exception e) {
				log.log(Level.SEVERE, "Stargate plugin : Error while reading Stargatedata.txt", e);
			}
			
		}
		return;
	}
}





















