import java.util.ArrayList;
import net.minecraft.server.MinecraftServer;

public class Cuboid {
	// Version 4.2 : 29/09 14h00 GMT+2
	// for servermod100
	static MinecraftServer world = etc.getMCServer();
		
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Boolean> selectionStatus = new ArrayList<Boolean>();
	static ArrayList<Integer> pointsCoordinates = new ArrayList<Integer>();
	
	//static ArrayList<Integer> Cuboid;
	
	private static int getPlayerIndex(String playerName){
		
		boolean inList = false;
		for (String p : playerList){
			if (p==playerName)
				inList = true;
		}
		
		if (!inList){
			playerList.add(playerName);
			selectionStatus.add(false);
			pointsCoordinates.add(0);	// TODO : plus elegant
			pointsCoordinates.add(0);
			pointsCoordinates.add(0);
			pointsCoordinates.add(0);
			pointsCoordinates.add(0);
			pointsCoordinates.add(0);
		}
				
		return playerList.indexOf(playerName);
	}
	
	public static boolean setPoint(String playerName, int X, int Y, int Z){
		
		int index = getPlayerIndex(playerName);
		boolean secondPoint = selectionStatus.get(index);
		if ( !secondPoint ){
			pointsCoordinates.set(index*6, X);
			pointsCoordinates.set(index*6+1, Y);
			pointsCoordinates.set(index*6+2, Z);
		}
		else{
			pointsCoordinates.set(index*6+3, X);
			pointsCoordinates.set(index*6+4, Y);
			pointsCoordinates.set(index*6+5, Z);
		}
		selectionStatus.set(index, !secondPoint);
		return secondPoint;
	}
	
	public static int[] getPoint(String playerName, boolean secondPoint){
		int index = getPlayerIndex(playerName);
		int[] coords;
		if (!secondPoint){
			coords = new int[]{ pointsCoordinates.get(index*6) ,pointsCoordinates.get(index*6+1) ,pointsCoordinates.get(index*6+2) };
		}
		else{
			coords = new int[]{ pointsCoordinates.get(index*6+3) ,pointsCoordinates.get(index*6+4) ,pointsCoordinates.get(index*6+5) };
		}
		return coords;
	}
	
	public static boolean isReady(String playerName, boolean deuxPoints){
		int index = getPlayerIndex(playerName);
		
		if ( deuxPoints && pointsCoordinates.get(index*6)!=0 && pointsCoordinates.get(index*6+1)!=0 && pointsCoordinates.get(index*6+2)!=0 && pointsCoordinates.get(index*6+3)!=0 && pointsCoordinates.get(index*6+4)!=0 && pointsCoordinates.get(index*6+5)!=0 ){
			selectionStatus.set(index, false);
			return true;
		}
		else if(pointsCoordinates.get(index*6)!=0 && pointsCoordinates.get(index*6+1)!=0 && pointsCoordinates.get(index*6+2)!=0) {
			selectionStatus.set(index, false);
			return true;
		}
		return false;
	}

	public static int calculerTaille(String playerName){
		int index = getPlayerIndex(playerName);
		
		int Xsize = Math.abs(pointsCoordinates.get(index*6+3)-pointsCoordinates.get(index*6))+1;
		int Ysize = Math.abs(pointsCoordinates.get(index*6+4)-pointsCoordinates.get(index*6+1))+1;
		int Zsize = Math.abs(pointsCoordinates.get(index*6+5)-pointsCoordinates.get(index*6+2))+1;
		return Xsize*Ysize*Zsize;
	}
	
	public static void supprimerCuboid(String playerName){
		int index = getPlayerIndex(playerName);
		
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		
		int endX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3)  ) ? pointsCoordinates.get(index*6+3) : pointsCoordinates.get(index*6);
		int endY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+4) : pointsCoordinates.get(index*6+1);
		int endZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+5) : pointsCoordinates.get(index*6+2);
		
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					world.e.d(i,j,k,20);
					world.e.d(i,j,k,0);
				}
			}
		}
	}
	
	public static void remplirCuboid(String playerName, int bloctype){
		int index = getPlayerIndex(playerName);
		
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		
		int endX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3)  ) ? pointsCoordinates.get(index*6+3) : pointsCoordinates.get(index*6);
		int endY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+4) : pointsCoordinates.get(index*6+1);
		int endZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+5) : pointsCoordinates.get(index*6+2);
		
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					world.e.d(i,j,k,bloctype);
				}
			}
		}
	}
	
	public static void remplacerDansCuboid(String playerName, int[] replaceParams){
		int index = getPlayerIndex(playerName);
				
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		
		int endX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3)  ) ? pointsCoordinates.get(index*6+3) : pointsCoordinates.get(index*6);
		int endY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+4) : pointsCoordinates.get(index*6+1);
		int endZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+5) : pointsCoordinates.get(index*6+2);
		
		int targetBlockIndex = replaceParams.length-1;
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					for ( int l = 0; l < targetBlockIndex; l++ ){
						if( world.e.a(i, j, k) == replaceParams[l] ){
							world.e.d(i,j,k,replaceParams[targetBlockIndex]);
						}
					}
				}
			}
		}
	}
	
	/*
	public static void enregisterCuboid(){
		Cuboid = new ArrayList<Integer>();
		Cuboid.add(pointsCoordinates.get(index*6+3)-pointsCoordinates.get(index*6));
		Cuboid.add(pointsCoordinates.get(index*6+4)-pointsCoordinates.get(index*6+1));
		Cuboid.add(pointsCoordinates.get(index*6+5)-pointsCoordinates.get(index*6+2));
		
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		
		int endX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3)  ) ? pointsCoordinates.get(index*6+3) : pointsCoordinates.get(index*6);
		int endY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+4) : pointsCoordinates.get(index*6+1);
		int endZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+5) : pointsCoordinates.get(index*6+2);
		
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					Cuboid.add( world.e.a(i,j,k) );
				}
			}
		}
	}

	public static void poserCuboid(){
		if ( !Cuboid.isEmpty() ){
			//if (CuboidDimension[0] >= Cuboid.get(0) && CuboidDimension[1] >= Cuboid.get(1) && CuboidDimension[2] >= Cuboid.get(2)){

				int startX = pointsCoordinates.get(index*6);
				int startY = pointsCoordinates.get(index*6+1);
				int startZ = pointsCoordinates.get(index*6+2);
				
				int endX = startX+Cuboid.get(0);
				int endY = startY+Cuboid.get(1);
				int endZ = startZ+Cuboid.get(2);
				
				int iterator = 3;
				
				for ( int i = startX; i<= endX; i++ ){
					for ( int j = startY; j<= endY; j++ ){
						for ( int k = startZ; k<= endZ; k++ ){
							world.e.d(i,j,k,Cuboid.get(iterator));
							iterator++;
						}
					}
				}
			//}
		}
	}

	public static void drawCircle(int taille){
	}*/
}
