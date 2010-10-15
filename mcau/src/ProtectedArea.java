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

public class ProtectedArea {
	// Version 4.2 : 29/09 14h00 GMT+2
	// for servermod100
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	static boolean toggle = true;
	static ArrayList<Integer> ProtectedCuboids = new ArrayList<Integer>();
	static ArrayList<String> ProtectedCuboidsNames = new ArrayList<String>();
	static ArrayList<String> ProtectedCuboidsOwners = new ArrayList<String>();
	static ArrayList<String> ClaimNames = new ArrayList<String>();
	
	
	public static void loadProtectedAreas(){
		File dataSource = new File("protectedCuboids.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("protectedCuboids.txt", true);
                writer.append("#The data about protected Cuboids will go there\r\n");
                writer.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "[CuboidPlugin] : Exception while creating watchedBlocks.txt");
            } finally {
            	try{
            		writer.close();
            	}
            	catch(IOException e){
            		 log.log(Level.SEVERE, "[CuboidPlugin] : Exception while closing loadProtectedAreas writer", e);
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
					log.info("[CuboidPlugin] : Successfuly loaded.");
			} catch (Exception e) {
				log.log(Level.SEVERE, "[CuboidPlugin] : Error while reading protectedCuboids.txt", e);
			}
			
		}
	}
	
	public static void loadClaimedAreas(){
		File dataSource = new File("protectedCuboids_claims.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("protectedCuboids_claims.txt", true);
                writer.append("#The data about protected Cuboid claims will go there\r\n");
                writer.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "[CuboidPlugin] : Exception while creating protectedCuboids_claims.txt");
            } finally {
            	try{
            		writer.close();
            	}
            	catch(IOException e){
            		 log.log(Level.SEVERE, "[CuboidPlugin] : Exception while closing loadClaimedAreas writer", e);
            	}
            }
		}
		else{
			try {
					ClaimNames = new ArrayList<String>();
					Scanner scanner = new Scanner(dataSource);
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						if (line.startsWith("#") || line.equals("")) {
							continue;
						}
						String[] donnees = line.split(",");
						if (donnees.length < 10) {
							continue;
						}
						
						Cuboidold.setPoint(donnees[8], Integer.parseInt(donnees[0]), Integer.parseInt(donnees[1]), Integer.parseInt(donnees[2]), Integer.parseInt(donnees[6]));
						Cuboidold.setPoint(donnees[8], Integer.parseInt(donnees[3]), Integer.parseInt(donnees[4]), Integer.parseInt(donnees[5]), Integer.parseInt(donnees[7]));
						Cuboidold.setClaimName(donnees[8], donnees[9]);
						ClaimNames.add(donnees[9]);
						
					}
					scanner.close();
					log.info("[CuboidPlugin] : Successfuly loaded claims.");
			} catch (Exception e) {
				log.log(Level.SEVERE, "[CuboidPlugin] : Error while reading protectedCuboids_claims.txt", e);
			}
			
		}
	}
		
	public static short protegerCuboid(String playerName, String ownersList, String cuboidName){
		
		//	Check chevauchements
		
		for(String test : ProtectedCuboidsNames){
			if(test.contains(cuboidName)){
				log.info(playerName+" failed to create a protected cuboid named "+cuboidName+" (aleady used)");
				return 1;
			}
		}
			
		int[] firstPoint = Cuboidold.getPoint(playerName, false);
		int[] secondPoint = Cuboidold.getPoint(playerName, true);

		ProtectedCuboids.add(firstPoint[0]);	// Method plus elegant
		ProtectedCuboids.add(firstPoint[1]);
		ProtectedCuboids.add(firstPoint[2]);
		ProtectedCuboids.add(secondPoint[0]);
		ProtectedCuboids.add(secondPoint[1]);
		ProtectedCuboids.add(secondPoint[2]);
		ProtectedCuboidsOwners.add(ownersList);
		ProtectedCuboidsNames.add(cuboidName);
			
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("protectedCuboids.txt", true));
			String newProtectedCuboid = firstPoint[0]+","+firstPoint[1]+","+firstPoint[2]+","+secondPoint[0]+","+secondPoint[1]+","+secondPoint[2]+","+ownersList+","+cuboidName;
			writer.append(newProtectedCuboid);
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "[CuboidPlugin] : Error while writing protectedCuboids.txt", e);
			return 2;
		}
		log.info(playerName+" created a new protected cuboid named "+cuboidName);
		return 0;
	}
	
	public static short stakeClaim(String playerName, String cuboidName){
		
		//	Check chevauchements
		
		for(String test : ClaimNames){
			if(test.contains(cuboidName)){
				log.info(playerName+" failed to stake a claim named "+cuboidName+" (aleady used)");
				return 2;
			}
		}
		ClaimNames.add(cuboidName);
		
		Cuboidold.setClaimName(playerName, cuboidName);
		int[] firstPoint = Cuboidold.getPoint( playerName, true);
		int[] secondPoint = Cuboidold.getPoint( playerName,false);
		
		int[] blocks = Cuboidold.getBlocks(playerName);
		
		boolean removedClaim = false;
		
		// Remove claim from file, and from memory
		removedClaim = removeClaim(playerName);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("protectedCuboids_claims.txt", true));
			String newClaim = firstPoint[0]+","+firstPoint[1]+","+firstPoint[2]+","+secondPoint[0]+","+secondPoint[1]+","+secondPoint[2]+","+blocks[0]+","+blocks[1]+","+playerName+","+cuboidName;
			writer.append(newClaim);
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "[CuboidPlugin] : Error while writing protectedCuboids_claims.txt", e);
			return -1;
		}
		log.info(playerName+" created a new claim named "+cuboidName);
		if (removedClaim) {
			return 1;
		}
		
		return 0;
		
	}
	
	
	public static boolean removeClaim(String playerName){
	
		boolean removedClaim = false;
		
		File dataSource = new File("protectedCuboids_claims.txt");
		if (dataSource.exists()){
			try {
	            BufferedReader reader = new BufferedReader(new FileReader(dataSource));
	            StringBuilder newFile = new StringBuilder();
	            String line = "";
	            while ((line = reader.readLine()) != null) {
	            	String[] split = line.split(",");
	            	if( split.length == 10 )
					{
						if (!split[8].contains(playerName)){
							newFile.append(line).append("\r\n");   
						}
						else
						{
							removedClaim = true;
						}
	            	}
	            }
	            reader.close();

	            FileWriter writer = new FileWriter("protectedCuboids_claims.txt");
	            writer.write(newFile.toString());
	            writer.close();
    
	        } catch (Exception ex) {
	            log.log(Level.SEVERE, "[CuboidPlugin] : A problem occured during claim removal", ex);
	            return false;
	        }
		}
		else{
			log.log(Level.SEVERE,"[CuboidPlugin] : protectedCuboids_claims.txt seems to have been removed");
			return false;
		}
		
		
		
		return removedClaim;
	}
		
	
	public static short removeProtectedZone(String playerName, String cuboidName){
		
		ArrayList<String> oldProtectedCuboidsNames = ProtectedCuboidsNames;	        	
		ProtectedCuboidsNames = new ArrayList<String>();
		int indexToDel = -1;
		int oldNumber = oldProtectedCuboidsNames.size();
        for (int i = 0; i < oldNumber; i++){
        	if( oldProtectedCuboidsNames.get(i).contains(cuboidName) ){
        		indexToDel=i;
        	}
        	else{
        		ProtectedCuboidsNames.add(oldProtectedCuboidsNames.get(i));
        	}
        }
        
        if( indexToDel==-1 ){
        	log.info(playerName+" failed to remove a protected cuboid named "+cuboidName+" (not found)");
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
	            	if( split.length > 7 && !split[7].contains(cuboidName)){
	            		newFile.append(line).append("\r\n");   
	            	}
	            }
	            reader.close();

	            FileWriter writer = new FileWriter("protectedCuboids.txt");
	            writer.write(newFile.toString());
	            writer.close();
    
	        } catch (Exception ex) {
	            log.log(Level.SEVERE, "[CuboidPlugin] : A problem occured during cuboid removal", ex);
	            return 2;
	        }
		}
		else{
			log.log(Level.SEVERE,"[CuboidPlugin] : protectedCuboids.txt seems to have been removed");
			return 3;
		}
		
		log.info("[CuboidPlugin] : "+playerName+" removed a protected cuboid named "+cuboidName);
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
	
	public static String listerCuboids(String playerName){
		// !TODO!only return player cuboids
		String cuboidsList = "";
		String tempCuboid = "";
		int size = ProtectedCuboidsNames.size();
		if (size > 0){
			for( int i = 0; i<size; i++){
				tempCuboid = ProtectedCuboidsNames.get(i);
				if (tempCuboid.startsWith(playerName)){
					cuboidsList += " "+tempCuboid;
				}
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
		int pt1, pt2;
		for ( int i = 0; i<ProtectedCuboidsOwners.size(); i++ ){
			pt1 = ProtectedCuboids.get(i*6+0);	// X
			pt2 = ProtectedCuboids.get(i*6+3);
			if ( isBetween(X, pt1, pt2) ){
				pt1 = ProtectedCuboids.get(i*6+2);	// Z = real Y
				pt2 = ProtectedCuboids.get(i*6+5);
				if ( isBetween(Z, pt1, pt2) ){
					pt1 = ProtectedCuboids.get(i*6+1);	// Y = real Z
					pt2 = ProtectedCuboids.get(i*6+4);
					if ( isBetween(Y, pt1, pt2) ){
						return ProtectedCuboidsOwners.get(i);
					}
				}
			}
		}
		return null;
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
	
}
