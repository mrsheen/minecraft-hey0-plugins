import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;

public class CuboidProtection {
	// Version 9 : 14/10 11h45 GMT+2
	// for servermod 115-116

	static int addedHeight = 0;
	
	static boolean toggle = true;
	static ArrayList<Integer> ProtectedCuboids = new ArrayList<Integer>();
	static ArrayList<String> ProtectedCuboidsNames = new ArrayList<String>();
	static ArrayList<String> ProtectedCuboidsOwners = new ArrayList<String>();
	
	public static void loadProtectedAreas(){
		File dataSource = new File("protectedCuboids.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("protectedCuboids.txt", true);
                writer.append("#The data about protected Cuboids will go there\r\n");
                writer.close();
            } catch (Exception e) {
                CuboidPlugin.log.log(Level.SEVERE, "Exception while creating watchedBlocks.txt");
            } finally {
            	try{
            		writer.close();
            	}
            	catch(IOException e){
            		CuboidPlugin.log.log(Level.SEVERE, "Exception while closing loadProtectedAreas writer", e);
            	}
            }
		}
		else{
			try {
					ProtectedCuboids = new ArrayList<Integer>();
					ProtectedCuboidsOwners = new ArrayList<String>();
					ProtectedCuboidsNames = new ArrayList<String>();
					Scanner scanner = new Scanner(dataSource);
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						if (line.startsWith("#") || line.equals("")) {
							continue;
						}
						String[] donnees = line.split(",");
						if (donnees.length < 8) {
							continue;
						}
			                
						for (short i=0; i<6; i++){
							ProtectedCuboids.add( Integer.parseInt(donnees[i]) );
						}
						ProtectedCuboidsOwners.add(donnees[6]);
						ProtectedCuboidsNames.add(donnees[7]);
					}
					scanner.close();
					CuboidPlugin.log.info("Cuboid plugin : successfuly loaded.");
			} catch (Exception e) {
				CuboidPlugin.log.log(Level.SEVERE, "Cuboid plugin : Error while reading protectedCuboids.txt", e);
			}
			
		}
	}
			
	public static byte protegerCuboid(String playerName, String ownersList, String cuboidName){
				
		for(String test : ProtectedCuboidsNames){
			if(test.equals(cuboidName)){
				if (CuboidPlugin.logging)
					CuboidPlugin.log.info(playerName+" failed to create a protected cuboid named "+cuboidName+" (aleady used)");
				return 1;
			}
		}
			
		int[] firstPoint = Cuboid.getPoint(playerName, false);
		int[] secondPoint = Cuboid.getPoint(playerName, true);
		
		if( firstPoint[1] == secondPoint[1] ){	// s'ils sont a la meme hauteur
			firstPoint[1]-=addedHeight;
			secondPoint[1]+=addedHeight;
		}
		ProtectedCuboids.add(firstPoint[0]);	// Methode plus elegante
		ProtectedCuboids.add(firstPoint[1]);
		ProtectedCuboids.add(firstPoint[2]);
		ProtectedCuboids.add(secondPoint[0]);
		ProtectedCuboids.add(secondPoint[1]);
		ProtectedCuboids.add(secondPoint[2]);
		ProtectedCuboidsOwners.add(ownersList);
		ProtectedCuboidsNames.add(cuboidName);
			
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("protectedCuboids.txt", true));
			String newProtectedCuboid = firstPoint[0]+","+firstPoint[1]+","+firstPoint[2]+","+secondPoint[0]+","+secondPoint[1]
			       +","+secondPoint[2]+","+ownersList+","+cuboidName;
			writer.append(newProtectedCuboid);
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			CuboidPlugin.log.log(Level.SEVERE, "Cuboid plugin : Error while writing protectedCuboids.txt", e);
			return 2;
		}
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" created a new protected cuboid named "+cuboidName);
		
		Cuboid.updateChestsState(firstPoint[0], firstPoint[1], firstPoint[2], secondPoint[0], secondPoint[1], secondPoint[2]);
		
		return 0;
	}
	
	public static byte moveProtection(String playerName, String cuboidName) {
		
		byte returnCode = 1;
		
		int[] firstPoint = Cuboid.getPoint(playerName, false);
		int[] secondPoint = Cuboid.getPoint(playerName, true);
		
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equals(cuboidName) ){
				Cuboid.updateChestsState(ProtectedCuboids.get(i*6), ProtectedCuboids.get(i*6+1), ProtectedCuboids.get(i*6+2)
						, ProtectedCuboids.get(i*6+3), ProtectedCuboids.get(i*6+4), ProtectedCuboids.get(i*6+5));
				ProtectedCuboids.set( (i*6) , firstPoint[0]);
				ProtectedCuboids.set( (i*6)+1 , firstPoint[1]);
				ProtectedCuboids.set( (i*6)+2 , firstPoint[2]);
				ProtectedCuboids.set( (i*6)+3 , secondPoint[0]);
				ProtectedCuboids.set( (i*6)+4 , secondPoint[1]);
				ProtectedCuboids.set( (i*6)+5 , secondPoint[2]);
				Cuboid.updateChestsState(firstPoint[0], firstPoint[1], firstPoint[2], secondPoint[0], secondPoint[1], secondPoint[2]);
				if (returnCode==1){
					returnCode=0;
				}
        	}
		}
		
		File dataSource = new File("protectedCuboids.txt");
		if (dataSource.exists()){
			try {
	            StringBuilder newFile = new StringBuilder();
	            
	            for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
	            	newFile.append( firstPoint[0]+","+firstPoint[1]+","+firstPoint[2]+","+secondPoint[0]+","+secondPoint[1]
	            	    +","+secondPoint[2]+","+ProtectedCuboidsOwners.get(i)+","+ProtectedCuboidsNames.get(i) ).append("\r\n");
	            }

	            FileWriter writer = new FileWriter("protectedCuboids.txt");
	            writer.write(newFile.toString());
	            writer.close();
    
	        } catch (Exception ex) {
	        	CuboidPlugin.log.log(Level.SEVERE, "A problem occured during cuboid rwriting", ex);
	            return 2;
	        }
		}
		else{
			CuboidPlugin.log.log(Level.SEVERE,"protectedCuboids.txt seems to have been removed");
			return 3;
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" moved a protected cuboid named "+cuboidName);

		return returnCode;
	}
	
	public static byte removeProtectedZone(String playerName, String cuboidName){
		
		ArrayList<String> oldProtectedCuboidsNames = ProtectedCuboidsNames;	        	
		ProtectedCuboidsNames = new ArrayList<String>();
		int indexToDel = -1;
		int oldNumber = oldProtectedCuboidsNames.size();
        for (int i = 0; i < oldNumber; i++){
        	if( oldProtectedCuboidsNames.get(i).equals(cuboidName) ){
        		indexToDel=i;
        		Cuboid.updateChestsState( ProtectedCuboids.get(i*6), ProtectedCuboids.get(i*6+1), ProtectedCuboids.get(i*6+2)
        				, ProtectedCuboids.get(i*6+3), ProtectedCuboids.get(i*6+4), ProtectedCuboids.get(i*6+5));
        	}
        	else{
        		ProtectedCuboidsNames.add(oldProtectedCuboidsNames.get(i));
        	}
        }
        
        if( indexToDel==-1 ){
        	if (CuboidPlugin.logging)
        		CuboidPlugin.log.info(playerName+" failed to remove a protected cuboid named "+cuboidName+" (not found)");
        	return 1;
        }
        
        ArrayList<String> oldProtectedCuboidsOwners = ProtectedCuboidsOwners;
        ProtectedCuboidsOwners = new ArrayList<String>();
        for (int i = 0; i < oldNumber; i++){
        	if( i!=indexToDel ){
        		ProtectedCuboidsOwners.add(oldProtectedCuboidsOwners.get(i) );
        	}
        }
        
        ArrayList<Integer> oldProtectedCuboids = ProtectedCuboids;
        ProtectedCuboids = new ArrayList<Integer>();	            
        for (int i = 0; i < oldNumber; i++){
        	if( i!=indexToDel ){
        		ProtectedCuboids.add(oldProtectedCuboids.get(i*6) );
        		ProtectedCuboids.add(oldProtectedCuboids.get(i*6+1) );
        		ProtectedCuboids.add(oldProtectedCuboids.get(i*6+2) );
        		ProtectedCuboids.add(oldProtectedCuboids.get(i*6+3) );
        		ProtectedCuboids.add(oldProtectedCuboids.get(i*6+4) );
        		ProtectedCuboids.add(oldProtectedCuboids.get(i*6+5) );
        	}
        }
		
		File dataSource = new File("protectedCuboids.txt");
		if (dataSource.exists()){
			try {
	            BufferedReader reader = new BufferedReader(new FileReader(dataSource));
	            StringBuilder newFile = new StringBuilder();
	            String line = "";
	            while ((line = reader.readLine()) != null) {
	            	String[] split = line.split(",");
	            	if( split.length > 7 && !split[7].equals(cuboidName)){
	            		newFile.append(line).append("\r\n");   
	            	}
	            }
	            reader.close();

	            FileWriter writer = new FileWriter("protectedCuboids.txt");
	            writer.write(newFile.toString());
	            writer.close();
    
	        } catch (Exception ex) {
	        	CuboidPlugin.log.log(Level.SEVERE, "A problem occured during cuboid removal", ex);
	            return 2;
	        }
		}
		else{
			CuboidPlugin.log.log(Level.SEVERE,"protectedCuboids.txt seems to have been removed");
			return 3;
		}
		
		if (CuboidPlugin.logging)
			CuboidPlugin.log.info(playerName+" removed a protected cuboid named "+cuboidName);
				
		return 0;
	}
	
	public static String listerCuboids(){
		String cuboidsList = "";
		int size = ProtectedCuboidsNames.size();
		if (size > 0){
			for( int i = 0; i<size; i++){
				cuboidsList += " "+ProtectedCuboidsNames.get(i);
			}
		}
		else{
			cuboidsList = " <list is empty>";
		}
		return cuboidsList;
	}
		
	public static String inProtectedZone(Block block){
		return inProtectedZone( block.getX(), block.getY(), block.getZ() );
	}
	
	public static String inProtectedZone(int X, int Y, int Z){
		// TODO : gestion par Octree
		String lastEntry = null;
		int pt1, pt2;
		for ( int i = 0; i<ProtectedCuboidsNames.size(); i++ ){
			pt1 = ProtectedCuboids.get(i*6+0);	// X
			pt2 = ProtectedCuboids.get(i*6+3);
			if ( isBetween(X, pt1, pt2) ){
				pt1 = ProtectedCuboids.get(i*6+2);	// Z = real Y
				pt2 = ProtectedCuboids.get(i*6+5);
				if ( isBetween(Z, pt1, pt2) ){
					pt1 = ProtectedCuboids.get(i*6+1);	// Y = real Z
					pt2 = ProtectedCuboids.get(i*6+4);
					if ( isBetween(Y, pt1, pt2) ){
						lastEntry= "name = "+ProtectedCuboidsNames.get(i)
							+", owners ="+ProtectedCuboidsOwners.get(i);
					}
				}
			}
		}
		return lastEntry;
	}
	
	private static boolean isBetween(int var, int lim1, int lim2){	
		boolean result = false;
		if (lim1 <= lim2){
			if (var >= lim1 && var <= lim2){
				result = true;
			}
		}
		else{
			if (var <= lim1 && var >= lim2){
				result = true;
			}
		}
		return result;
	}
	
	public static String listOwners( String protectedAreaName ){	
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equalsIgnoreCase(protectedAreaName) ){
				return ProtectedCuboidsOwners.get(i);
			}
		}	
		return null;
	}
	
	public static byte addPlayer( String[] split, String protectedAreaName ){	//	TODO
		byte returnCode = 1;
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equals(protectedAreaName) ){
				String toChange = ProtectedCuboidsOwners.get(i);
				
				for (String playerName : split){
					if ( (playerName.indexOf("o:") != -1) && toChange.indexOf( " "+playerName.substring(2) )!= -1 ){
						int startIndex = toChange.indexOf( " "+playerName.substring(2));
						int endIndex = toChange.indexOf(" ", startIndex+1 );
						if (endIndex == -1){
							endIndex = toChange.length()-1;
						}
						toChange = toChange.substring(0, startIndex )+toChange.substring( endIndex, toChange.length() );
					}
					if ( toChange.indexOf(" "+playerName) == -1 && toChange.indexOf("o:"+playerName) == -1 ){
						toChange += " "+playerName;
					}
				}
				
				ProtectedCuboidsOwners.set(i, toChange);
				
				File dataSource = new File("protectedCuboids.txt");
				if (dataSource.exists()){
					try {
			            BufferedReader reader = new BufferedReader(new FileReader(dataSource));
			            StringBuilder newFile = new StringBuilder();
			            String line = "";
			            while ((line = reader.readLine()) != null) {
			            	String[] cuboidLine = line.split(",");
			            	if( cuboidLine.length > 7 && !cuboidLine[7].equals(protectedAreaName)){
			            		newFile.append(line).append("\r\n");   
			            	}
			            	else{
			            		newFile.append( ProtectedCuboids.get(i*6)+","+ProtectedCuboids.get(i*6+1)+","
			            				+ProtectedCuboids.get(i*6+2)+","+ProtectedCuboids.get(i*6+3)+","+ProtectedCuboids.get(i*6+4)
			            				+","+ProtectedCuboids.get(i*6+5)+","+ProtectedCuboidsOwners.get(i)+","
			            				+ProtectedCuboidsNames.get(i) ).append("\r\n");
			            	}
			            }
			            reader.close();

			            FileWriter writer = new FileWriter("protectedCuboids.txt");
			            writer.write(newFile.toString());
			            writer.close();
		    
			        } catch (Exception ex) {
			        	CuboidPlugin.log.log(Level.SEVERE, "A problem occured while modifying the cuboid", ex);
			        	return 3;
			        }
			        if (CuboidPlugin.logging)
		        		CuboidPlugin.log.info("Added owners to "+protectedAreaName);
				}
				else{
					CuboidPlugin.log.log(Level.SEVERE,"protectedCuboids.txt seems to have been removed");
					return 2;
				}
				if (returnCode ==1)
					returnCode = 0;
				break;
			}
		}
		return returnCode;
	}
	
	public static byte removePlayer( String[] split, String protectedAreaName ){
		byte returnCode = 1;
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equals(protectedAreaName) ){
				String toChange = ProtectedCuboidsOwners.get(i);
				
				for (String playerName : split){
					if ( toChange.indexOf(" "+playerName) != -1 ){	// si existant
						int startIndex = toChange.indexOf(" "+playerName);
						int endIndex = toChange.indexOf(" ", startIndex+1 );
						if (endIndex == -1){
							endIndex = toChange.length()-1;
						}
						toChange = toChange.substring(0, startIndex )+toChange.substring( endIndex, toChange.length() );
					}
				}
				ProtectedCuboidsOwners.set(i, toChange);
				
				File dataSource = new File("protectedCuboids.txt");
				if (dataSource.exists()){
					try {
			            BufferedReader reader = new BufferedReader(new FileReader(dataSource));
			            StringBuilder newFile = new StringBuilder();
			            String line = "";
			            while ((line = reader.readLine()) != null) {
			            	String[] cuboidLine = line.split(",");
			            	if( cuboidLine.length > 7 && !cuboidLine[7].equals(protectedAreaName)){
			            		newFile.append(line).append("\r\n");   
			            	}
			            	else{
			            		newFile.append( ProtectedCuboids.get(i*6)+","+ProtectedCuboids.get(i*6+1)+","
			            				+ProtectedCuboids.get(i*6+2)+","+ProtectedCuboids.get(i*6+3)+","+ProtectedCuboids.get(i*6+4)
			            				+","+ProtectedCuboids.get(i*6+5)+","+ProtectedCuboidsOwners.get(i)+","
			            				+ProtectedCuboidsNames.get(i) ).append("\r\n");
			            	}
			            }
			            reader.close();

			            FileWriter writer = new FileWriter("protectedCuboids.txt");
			            writer.write(newFile.toString());
			            writer.close();
		    
			        } catch (Exception ex) {
			        	CuboidPlugin.log.log(Level.SEVERE, "A problem occured while modifying the cuboid file", ex);
			        	return 3;
			        }
			        if (CuboidPlugin.logging)
		        		CuboidPlugin.log.info("Added owners to "+protectedAreaName);
				}
				else{
					CuboidPlugin.log.log(Level.SEVERE,"protectedCuboids.txt seems to have been removed");
					return 2;
				}
				if (returnCode ==1)
					returnCode = 0;
				break;
			}
		}
		return returnCode;
	}

}
