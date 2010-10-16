import java.util.ArrayList;

public class Cuboid {
	// Version 9 : 14/10 11h45 GMT+2
	// for servermod 115-116
	static Server server = etc.getServer();
		
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Boolean> selectionStatus = new ArrayList<Boolean>();
	static ArrayList<Boolean> undoAble = new ArrayList<Boolean>();
	static ArrayList<Integer> pointsCoordinates = new ArrayList<Integer>();
	static ArrayList<int[][][]> lastSelectedCuboid = new ArrayList<int[][][]>();
	static ArrayList<int[]> pastePoint = new ArrayList<int[]>();
	static Object lock = new Object();
		
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
			pointsCoordinates.add(null);	// A faire : plus elegant
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			pointsCoordinates.add(null);
			lastSelectedCuboid.add(new int[][][]{});
			pastePoint.add(new int[]{});
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
			pastePoint.set(index, new int[]{X, Y, Z});
			
		}
		else{
			pointsCoordinates.set(index*6+3, X);
			pointsCoordinates.set(index*6+4, Y);
			pointsCoordinates.set(index*6+5, Z);
			lastSelectedCuboid.set(index, new int[][][]{});
			pastePoint.set(index, new int[]{});
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
	
	public static int[] getCorner(String playerName, boolean secondCorner){		// TODO
		return new int[]{};
	}
	
	public static boolean isReady(String playerName, boolean deuxPoints){
		int index = getPlayerIndex(playerName);
		
		if ( deuxPoints && pointsCoordinates.get(index*6)!=null && pointsCoordinates.get(index*6+1)!=null
				&& pointsCoordinates.get(index*6+2)!=null && pointsCoordinates.get(index*6+3)!=null
				&& pointsCoordinates.get(index*6+4)!=null && pointsCoordinates.get(index*6+5)!=null ){
			selectionStatus.set(index, false);
			return true;
		}
		else if( !deuxPoints && pointsCoordinates.get(index*6)!=null && pointsCoordinates.get(index*6+1)!=null
				&& pointsCoordinates.get(index*6+2)!=null) {
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
					 tableaux[i][j][k] = server.getBlockIdAt( startX+i,startY+j,startZ+k );
			}
		}
		
		lastSelectedCuboid.set(index, tableaux);
		pastePoint.set(index, new int[]{startX, startY, startZ});
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
					 tableaux[i][j][k] = server.getBlockIdAt( Xmin+i,Ymin+j,Zmin+k );
			}
		}
		lastSelectedCuboid.set(index, tableaux);
		pastePoint.set(index, new int[]{Xmin, Ymin, Zmin});
		undoAble.set(index, true);
	}
			
	public static byte paste(String playerName){
		synchronized(lock){
			int index = getPlayerIndex(playerName);
			int[] startPoint = pastePoint.get(index);
	
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
					 for (int k = 0; k < Zsize; ++k){
						 server.setBlockAt( tableau[i][j][k], startPoint[0]+i,startPoint[1]+j,startPoint[2]+k );
					 }
				}
			}
			undoAble.set(index, false);	// TODO : rendre le paste reversible
			return 0;
		}
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
					 tableaux[i][j][k] = server.getBlockIdAt( startX+i,startY+j,startZ+k );
			}
		}
		return new CuboidData(playerName, cuboidName, tableaux).save();	
	}
	
	public static byte loadCuboid(String playerName, String cuboidName){
		synchronized(lock){
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
			
			copyCuboid(playerName, startX, startX+Xsize, startY, startY+Ysize, startZ, startZ+Zsize);
				
			for (int i = 0; i<Xsize; i++){
				for (int j = 0; j < Ysize; ++j) {
					 for (int k = 0; k < Zsize; ++k)
						 server.setBlockAt( tableau[i][j][k], startX+i,startY+j,startZ+k );
				}
			}

		}
		
		return cuboid.loadReturnCode;
		}
	}
	
	public static int calculerTaille(String playerName){
		int index = getPlayerIndex(playerName);
		
		int Xsize = Math.abs(pointsCoordinates.get(index*6+3)-pointsCoordinates.get(index*6))+1;
		int Ysize = Math.abs(pointsCoordinates.get(index*6+4)-pointsCoordinates.get(index*6+1))+1;
		int Zsize = Math.abs(pointsCoordinates.get(index*6+5)-pointsCoordinates.get(index*6+2))+1;
		return Xsize*Ysize*Zsize;
	}
	
	public static void viderCuboid(String playerName){
		synchronized(lock){
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
					server.setBlockAt(20,i,j,k);
					server.setBlockAt(0,i,j,k);
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" emptied a cuboid");
		}
	}
	
	public static void remplirCuboid(String playerName, int bloctype){
		synchronized(lock){
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
					server.setBlockAt(bloctype,i,j,k);
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" filled a cuboid");
		}
	}
	
	public static void remplacerDansCuboid(String playerName, int[] replaceParams){
		synchronized(lock){
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
						if( server.getBlockIdAt(i, j, k) == replaceParams[l] ){
							server.setBlockAt(replaceParams[targetBlockIndex],i,j,k);
						}
					}
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" replaced blocks inside a cuboid");
		}
	}
	
	public static void dessinerCuboid(String playerName, int bloctype, boolean sixFaces){
		synchronized(lock){
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
				server.setBlockAt(bloctype,i,j,startZ);
				server.setBlockAt(bloctype,i,j,endZ);
			}
		}		
		for ( int i = startY; i<= endY; i++ ){
			for ( int j = startZ; j<= endZ; j++ ){
				server.setBlockAt(bloctype,startX,i,j);
				server.setBlockAt(bloctype,endX,i,j);
			}
		}
		if (sixFaces){
			for ( int i = startX; i<= endX; i++ ){
				for ( int j = startZ; j<= endZ; j++ ){
					server.setBlockAt(bloctype,i,startY,j);
					server.setBlockAt(bloctype,i,endY,j);
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built the "+((sixFaces)? "faces" : "walls")+" of a cuboid");
		}
	}
	
	public static void tracerCercle(String playerName,int radius, int blocktype, int height, boolean remplir){
		
		synchronized(lock){
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
				    if( diff<radius+0.5 && ( remplir || (!remplir && diff>radius-0.5) ) ){
				    	server.setBlockAt(blocktype,i,j,k);
				    }
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((height!=0)? "cylinder" : "circle") );
		}
		
	}
	
	public static void dessinerShpere(String playerName,int radius, int blocktype, boolean remplir){
		
		synchronized(lock){
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
				    if( diff<radius+0.5 && (remplir || (!remplir && diff>radius-0.5) ) ){
				    	server.setBlockAt(blocktype,i,j,k);
				    }
				}
			}
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" built a "+((remplir)? "ball" : "sphere") );
		}
	}
	
	public static void updateChestsState(int firstX, int firstY, int firstZ, int secondX, int secondY, int secondZ){
		synchronized(lock){
		int startX = ( firstX <= secondX ) ? firstX : secondX;
		int startY = ( firstY <= secondY ) ? firstY : secondY;
		int startZ = ( firstZ <= secondZ ) ? firstZ : secondZ;
		
		int endX = ( firstX <= secondX  ) ? secondX : firstX;
		int endY = ( firstY <= secondY ) ? secondY : firstY;
		int endZ = ( firstZ <= secondZ ) ? secondZ : firstZ;
		
		for ( int i = startX; i<= endX; i++ ){
			for ( int j = startY; j<= endY; j++ ){
				for ( int k = startZ; k<= endZ; k++ ){
					if ( server.getBlockIdAt(i, j, k)==54 && server.getComplexBlock(i, j, k)!=null ){
						server.getComplexBlock(i, j, k).update();
					}
					
				}
			}
		}
		}
	}

}
