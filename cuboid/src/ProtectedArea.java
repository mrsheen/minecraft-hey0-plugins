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
	
	public static void loadProtectedAreas(){
		File dataSource = new File("protectedCuboids.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("protectedCuboids.txt", true);
                writer.append("#The data about protected Cuboids will go there\r\n");
                writer.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception while creating watchedBlocks.txt");
            } finally {
            	try{
            		writer.close();
            	}
            	catch(IOException e){
            		 log.log(Level.SEVERE, "Exception while closing loadProtectedAreas writer", e);
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
					log.info("Cuboid plugin : successfuly loaded.");
			} catch (Exception e) {
				log.log(Level.SEVERE, "Cuboid plugin : Error while reading protectedCuboids.txt", e);
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
			
		int[] firstPoint = Cuboid.getPoint(playerName, false);
		int[] secondPoint = Cuboid.getPoint(playerName, true);

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
			log.log(Level.SEVERE, "Cuboid plugin : Error while writing protectedCuboids.txt", e);
			return 2;
		}
		log.info(playerName+" created a new protected cuboid named "+cuboidName);
		return 0;
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
	            log.log(Level.SEVERE, "A problem occured during cuboid removal", ex);
	            return 2;
	        }
		}
		else{
			log.log(Level.SEVERE,"protectedCuboids.txt seems to have been removed");
			return 3;
		}
		
		log.info(playerName+" removed a protected cuboid named "+cuboidName);
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
