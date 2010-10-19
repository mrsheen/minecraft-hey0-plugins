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
			player.sendMessage("Stargate saved");
		} else {
			player.sendMessage("Incorrect");
		}
		return;
	}
	
	public static void Checkplayerpos(Location to,Player player) {
		if(StargateLocations.size()>0) {
			for(int i=0;i<StargateLocations.size()/12;i++) {
				if(player.getX()>=StargateLocations.get(i*12) && player.getX()<StargateLocations.get(i*12+1) &&
				player.getY()>=StargateLocations.get(i*12+2) && player.getY()<StargateLocations.get(i*12+3) &&
				player.getZ()>=StargateLocations.get(i*12+4) && player.getZ()<StargateLocations.get(i*12+5) ) {
					player.sendMessage("woosh");
					
					player.teleportTo(fromto((double)StargateLocations.get(i*12), 
											(double)StargateLocations.get(i*12+1), 
											(double)StargateLocations.get(i*12+6), 
											(double)StargateLocations.get(i*12+7), 
											player.getX()),
					fromto((double)StargateLocations.get(i*12+2), 
											(double)StargateLocations.get(i*12+3), 
											(double)StargateLocations.get(i*12+8), 
											(double)StargateLocations.get(i*12+9), 
											player.getY()),
					fromto((double)StargateLocations.get(i*12+4), 
											(double)StargateLocations.get(i*12+5), 
											(double)StargateLocations.get(i*12+10), 
											(double)StargateLocations.get(i*12+11), 
											player.getZ()),
					player.getRotation(),
					player.getPitch());
				}
			}
		}
	}
	
	public static double fromto(double minfrom, double maxfrom, double minto, double maxto, double playerloc) {
	
	return (minto + ((maxto-minto) * (playerloc - minfrom) / (maxfrom - minfrom)));	
	}
}





















