import java.util.ArrayList;

import net.minecraft.server.MinecraftServer;

public class Cuboid {
	// Version 7 : 06/10 14h00 GMT+2
	// for servermod 103 to 111
	static MinecraftServer world = etc.getMCServer();
		
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Boolean> selectionStatus = new ArrayList<Boolean>();
	static ArrayList<Boolean> undoAble = new ArrayList<Boolean>();
	static ArrayList<Integer> pointsCoordinates = new ArrayList<Integer>();
	static ArrayList<int[][][]> lastSelectedCuboid = new ArrayList<int[][][]>();
		
	private static int getPlayerIndex(String playerName){
		
		boolean inList = false;
		for (String p : playerList){
			if (p==playerName)
				inList = true;
		}
		
		if (!inList){
			playerList.add(playerName);
			selectionStatus.add(false);
			undoAble.add(false);
			pointsCoordinates.add(null);	// A faire : plus élégant
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			lastSelectedCuboid.add(new int[][][]{});
		}
				
		return playerList.indexOf(playerName);
	}
	
	public static boolean isUndoAble(String playerName) {
		return undoAble.get(getPlayerIndex(playerName));
	}
	
	public static boolean setPoint(String playerName, int X, int Y, int Z){
		
		int index = getPlayerIndex(playerName);
		boolean secondPoint = selectionStatus.get(index);
		if ( !secondPoint ){
			pointsCoordinates.set(index*6, X);
			pointsCoordinates.set(index*6+1, Y);
			pointsCoordinates.set(index*6+2, Z);
			pointsCoordinates.set(index*6+3, null);
			pointsCoordinates.set(index*6+4, null);
			pointsCoordinates.set(index*6+5, null);
			undoAble.set(index, false);
			
		}
		else{
			pointsCoordinates.set(index*6+3, X);
			pointsCoordinates.set(index*6+4, Y);
			pointsCoordinates.set(index*6+5, Z);
			lastSelectedCuboid.set(index, new int[][][]{});
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
		
		if ( deuxPoints && pointsCoordinates.get(index*6)!=null && pointsCoordinates.get(index*6+1)!=null && pointsCoordinates.get(index*6+2)!=null && pointsCoordinates.get(index*6+3)!=null && pointsCoordinates.get(index*6+4)!=null && pointsCoordinates.get(index*6+5)!=null ){
			selectionStatus.set(index, false);
			return true;
		}
		else if( !deuxPoints && pointsCoordinates.get(index*6)!=null && pointsCoordinates.get(index*6+1)!=null && pointsCoordinates.get(index*6+2)!=null) {
			selectionStatus.set(index, false);
			return true;
		}
		return false;
	}
	
	public static void copyCuboid(String playerName, boolean manual){
		int index = getPlayerIndex(playerName);
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		int Xsize = Math.abs(pointsCoordinates.get(index*6+3)-pointsCoordinates.get(index*6))+1;
		int Ysize = Math.abs(pointsCoordinates.get(index*6+4)-pointsCoordinates.get(index*6+1))+1;
		int Zsize = Math.abs(pointsCoordinates.get(index*6+5)-pointsCoordinates.get(index*6+2))+1;
		// De Nord-Est vers Sud-Ouest
		
		int[][][] tableaux = new int[Xsize][][];
		for (int i = 0; i<Xsize; i++){
			tableaux[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				tableaux[i][j] = new int[Zsize];
				 for (int k = 0; k < Zsize; ++k)
					 tableaux[i][j][k] = world.e.a( startX+i,startY+j,startZ+k );
			}
		}
		
		lastSelectedCuboid.set(index, tableaux);
		if (!manual){
			undoAble.set(index, true);
		}
	}
	
	private static void copyCuboid(String playerName, int Xmin, int Xmax, int Ymin, int Ymax, int Zmin, int Zmax){
		int index = getPlayerIndex(playerName);
		int Xsize = Math.abs(Xmax-Xmin+1);
		int Ysize = Math.abs(Ymax-Ymin+1);
		int Zsize = Math.abs(Zmax-Zmin+1);
		
		int[][][] tableaux = new int[Xsize][][];
		for (int i = 0; i<Xsize; i++){
			tableaux[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				tableaux[i][j] = new int[Zsize];
				 for (int k = 0; k < Zsize; ++k)
					 tableaux[i][j][k] = world.e.a( Xmin+i,Ymin+j,Zmin+k );
			}
		}
		lastSelectedCuboid.set(index, tableaux);
		undoAble.set(index, false);		// TODO : rendre les cercles reversibles
	}
			
	public static byte paste(String playerName){
		int index = getPlayerIndex(playerName);
		int startX, startY, startZ;
		if ( pointsCoordinates.get(index*6+3)!=null ){
			startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
			startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
			startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		}
		else{
			startX = pointsCoordinates.get(index*6);
			startY = pointsCoordinates.get(index*6+1);
			startZ = pointsCoordinates.get(index*6+2);
		}
		

		int[][][] tableau = lastSelectedCuboid.get(index);
		int Xsize = tableau.length;
		if (Xsize==0){
			return 1;
		}
		int Ysize = tableau[0].length;
		int Zsize = tableau[0][0].length;
		// De Nord-Est vers Sud-Ouest
				
		for (int i = 0; i<Xsize; i++){
			for (int j = 0; j < Ysize; ++j) {
				 for (int k = 0; k < Zsize; ++k)
					 world.e.d( startX+i,startY+j,startZ+k, tableau[i][j][k] );
			}
		}
		undoAble.set(index, false);	// TODO : rendre le paste reversible
		return 0;

	}
	
	public static byte saveCuboid(String playerName, String cuboidName){
		int index = getPlayerIndex(playerName);
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		int Xsize = Math.abs(pointsCoordinates.get(index*6+3)-pointsCoordinates.get(index*6))+1;
		int Ysize = Math.abs(pointsCoordinates.get(index*6+4)-pointsCoordinates.get(index*6+1))+1;
		int Zsize = Math.abs(pointsCoordinates.get(index*6+5)-pointsCoordinates.get(index*6+2))+1;
		// De Nord-Est vers Sud-Ouest
		
		int[][][] tableaux = new int[Xsize][][];
		for (int i = 0; i<Xsize; i++){
			tableaux[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				tableaux[i][j] = new int[Zsize];
				 for (int k = 0; k < Zsize; ++k)
					 tableaux[i][j][k] = world.e.a( startX+i,startY+j,startZ+k );
			}
		}
		return new CuboidData(playerName, cuboidName, tableaux).save();	
	}
	
	public static byte loadCuboid(String playerName, String cuboidName){
		
		int index = getPlayerIndex(playerName);
		int startX, startY, startZ;
		if ( pointsCoordinates.get(index*6+3)!=null ){
			startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
			startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
			startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		}
		else{
			startX = pointsCoordinates.get(index*6);
			startY = pointsCoordinates.get(index*6+1);
			startZ = pointsCoordinates.get(index*6+2);
		}
		
		CuboidData cuboid = new CuboidData(playerName, cuboidName);
		
		if (cuboid.loadReturnCode == 0){
			int[][][] tableau = cuboid.getData();
			int Xsize = tableau.length;
			int Ysize = tableau[0].length;
			int Zsize = tableau[0][0].length;
			// De Nord-Est vers Sud-Ouest
				
			for (int i = 0; i<Xsize; i++){
				for (int j = 0; j < Ysize; ++j) {
					 for (int k = 0; k < Zsize; ++k)
						 world.e.d( startX+i,startY+j,startZ+k, tableau[i][j][k] );
				}
			}
			undoAble.set(index, false);	// TODO : rendre le paste reversible
		}
		
		return cuboid.loadReturnCode;
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
		copyCuboid(playerName, false);
		
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
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" emptied a cuboid");
	}
	
	public static void remplirCuboid(String playerName, int bloctype){
		copyCuboid(playerName, false);
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
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" filled a cuboid");
	}
	
	public static void remplacerDansCuboid(String playerName, int[] replaceParams){
		copyCuboid(playerName, false);
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
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" replaced blocks inside a cuboid");
	}
	
	public static void dessinerCuboid(String playerName, int bloctype, boolean sixFaces){
		copyCuboid(playerName, false);
		int index = getPlayerIndex(playerName);
		
		int startX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3) ) ? pointsCoordinates.get(index*6) : pointsCoordinates.get(index*6+3);
		int startY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+1) : pointsCoordinates.get(index*6+4);
		int startZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+2) : pointsCoordinates.get(index*6+5);
		
		int endX = ( pointsCoordinates.get(index*6) <= pointsCoordinates.get(index*6+3)  ) ? pointsCoordinates.get(index*6+3) : pointsCoordinates.get(index*6);
		int endY = ( pointsCoordinates.get(index*6+1) <= pointsCoordinates.get(index*6+4) ) ? pointsCoordinates.get(index*6+4) : pointsCoordinates.get(index*6+1);
		int endZ = ( pointsCoordinates.get(index*6+2) <= pointsCoordinates.get(index*6+5) ) ? pointsCoordinates.get(index*6+5) : pointsCoordinates.get(index*6+2);
	
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				world.e.d(i,j,startZ,bloctype);
				world.e.d(i,j,endZ,bloctype);
			}
		}		
		for ( int i = startY; i<= endY; i++ ){
			for ( int j = startZ; j<= endZ; j++ ){
				world.e.d(startX,i,j,bloctype);
				world.e.d(endX,i,j,bloctype);
			}
		}
		if (sixFaces){
			for ( int i = startX; i<= endX; i++ ){
				for ( int j = startZ; j<= endZ; j++ ){
					world.e.d(i,startY,j,bloctype);
					world.e.d(i,endY,j,bloctype);
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built the "+((sixFaces)? "faces" : "walls")+" of a cuboid");
	}
	
	public static void tracerCercle(String playerName,int radius, int blocktype, int height){
		
		int index = getPlayerIndex(playerName);
		int Xcenter = pointsCoordinates.get(index*6);
		int Ycenter = pointsCoordinates.get(index*6+1);
		int Zcenter = pointsCoordinates.get(index*6+2);
		int Xmin=Xcenter-radius;
		int Xmax=Xcenter+radius;
		int Zmin=Zcenter-radius;
		int Zmax=Zcenter+radius;
		int Ymin = (height+Ycenter >= Ycenter) ? Ycenter : height+Ycenter;
		int Ymax = (height+Ycenter <= Ycenter) ? Ycenter : height+Ycenter;
		
		copyCuboid(playerName, Xmin, Xmax, Ymin, Ymax, Zmin, Zmax);

		for ( int i = Xmin; i<= Xmax; i++ ){
			for ( int j = Ymin; j<= Ymax; j++ ){
				for (int k = Zmin; k <=Zmax ; k++){
				    double diff = Math.sqrt( Math.pow(i-Xcenter, 2.0D) + Math.pow(k-Zcenter, 2.0D) );
				    if( diff>radius-0.5 && diff<radius+0.5 ){
				    	world.e.d(i,j,k,blocktype);
				    }
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((height!=0)? "cylinder" : "circle") );
		
	}
	
	public static void dessinerShpere(String playerName,int radius, int blocktype, boolean remplir){
		
		int index = getPlayerIndex(playerName);
		int Xcenter = pointsCoordinates.get(index*6);
		int Ycenter = pointsCoordinates.get(index*6+1);
		int Zcenter = pointsCoordinates.get(index*6+2);
		int Xmin=Xcenter-radius;
		int Xmax=Xcenter+radius;
		int Ymin=Ycenter-radius;
		int Ymax=Ycenter+radius;
		int Zmin=Zcenter-radius;
		int Zmax=Zcenter+radius;
		
		copyCuboid(playerName, Xmin, Xmax, Ymin, Ymax, Zmin, Zmax);
		
		for ( int i = Xmin; i<= Xmax; i++ ){
			for ( int j = Ymin; j<= Ymax; j++ ){
				for (int k = Zmin; k <=Zmax ; k++){
				    double diff = Math.sqrt( Math.pow(i-Xcenter, 2.0D) + Math.pow(j-Ycenter, 2.0D) + Math.pow(k-Zcenter, 2.0D) );
				    if( diff<radius+0.5 ){
				    	if (remplir || (!remplir && diff>radius-0.5) ){
				    			world.e.d(i,j,k,blocktype);
				    	}
				    }
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((remplir)? "ball" : "sphere") );
	}

}
