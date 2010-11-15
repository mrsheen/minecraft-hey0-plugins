import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;

public class CuboidProtection {
	// Version 14 : 11/11 15h30 GMT+1
	// for servermod 123-125+
	
	// TODO : gestion par Octree

	static int addedHeight = 0;
	static boolean protectionWarn = false;
	static boolean toggle = true;
	static boolean newestHavePriority = true;
	static String[] restrictedGroups;
	static ArrayList<Integer> ProtectedCuboids = new ArrayList<Integer>();
	static ArrayList<String> ProtectedCuboidsNames = new ArrayList<String>();
	static ArrayList<ArrayList<String>> ProtectedCuboidsOwners = new ArrayList<ArrayList<String>>();
	
	public static void loadProtectedAreas(){
		File dataSource = new File("cuboids/protectedCuboids.txt");
		if (!dataSource.exists()){
			FileWriter writer = null;
            try {
                writer = new FileWriter("cuboids/protectedCuboids.txt", true);
                writer.write("#The data about protected Cuboids will go there\r\n");
                writer.close();
            } catch (Exception e) {
                CuboidPlugin.log.log(Level.SEVERE, "Exception while creating protectedCuboids.txt");
            }
		}
		else{
			try {
					ProtectedCuboids = new ArrayList<Integer>();
					ProtectedCuboidsOwners = new ArrayList<ArrayList<String>>();
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
			                
						for (short i=0; i<6; i++){	// corners coordinates
							ProtectedCuboids.add( Integer.parseInt(donnees[i]) );
						}
						
						 // owners reading
						String[] ownersTab = donnees[6].trim().split(" ");
						ArrayList<String> owners = new ArrayList<String>();
						for (String owner : ownersTab){
							owners.add(owner);
						}

						ProtectedCuboidsOwners.add(owners);
						ProtectedCuboidsNames.add(donnees[7]);	// name
					}
					scanner.close();
					CuboidPlugin.log.info("CuboidPlugin : protected cuboids loaded.");
			} catch (Exception e) {
				CuboidPlugin.log.log(Level.SEVERE, "Cuboid plugin : Error while reading protectedCuboids.txt", e);
			}
			
		}
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
	
	////////////////////////////
	////    DATA SENDING    ////
	////////////////////////////
	
	public static String displayProtectedCuboids(){
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
	
	private static String ownersString( int index ){
		String owners = "";
		for ( String owner : ProtectedCuboidsOwners.get(index) ){
			owners += " " + owner;
		}	
		return owners.trim();
	}
	
	private static String ownersString( ArrayList<String> list ){
		String owners = "";
		
		for ( String owner : list ){
			owners += " " + owner;
		}
		return owners.trim();
	}
	
	public static String ownersString( String protectedAreaName ){
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equalsIgnoreCase(protectedAreaName) ){				
				return " " + ownersString(i);
			}
		}	
		return null;
	}
			
	public static ArrayList<String> ownersTab(Block block){
		ArrayList<String> lastEntry = null;
		int pt1, pt2;
		int X = block.getX();
		int Y = block.getY();
		int Z = block.getZ();
		
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
						if ( newestHavePriority ){
							lastEntry = ProtectedCuboidsOwners.get(i);
						}
						else{
							return ProtectedCuboidsOwners.get(i);
						}
					}
				}
			}
		}
		return lastEntry;
	}
	
	public static String areaInfo(int X, int Y, int Z){
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
						if ( newestHavePriority ){
							lastEntry = "name = " + ProtectedCuboidsNames.get(i)+ " owners = " + ownersString(i);
						}
						else{
							return "name = " + ProtectedCuboidsNames.get(i)+ " owners = " + ownersString(i);
						}
					}
				}
			}
		}
		return lastEntry;
	}

	public static boolean isAllowed(Player player, ComplexBlock block, boolean warn){
		Block simpleBlock = new Block();
		simpleBlock.setX( block.getX() );
		simpleBlock.setY( block.getY() );
		simpleBlock.setZ( block.getZ() );
		return isAllowed(player, simpleBlock, warn);
	}
	
	public static boolean isAllowed(Player player, Block block, boolean warn){
		ArrayList<String> owners = ownersTab(block);
		
		if (owners == null){	// Si ce block n'est pas protégé
			String[] playerGroups = player.getGroups();
			for (String playerGroup : playerGroups){
				for (String group : restrictedGroups){
					if ( playerGroup.equalsIgnoreCase(group) ){
						return false;
					}
				}
			}
			return true;
		}
		
		String playerName = player.getName().toLowerCase();
		for (String owner : owners){
			if ( owner.equalsIgnoreCase(playerName) || owner.equalsIgnoreCase("o:"+playerName) ){
				return true;
			}
			if ( owner.startsWith("g:") && player.isInGroup(owner.substring(2)) ){
				return true;
			}
		}
		
		if (warn && protectionWarn){
			player.sendMessage(Colors.Rose+"This block is protected !" );
		}
		return false;
	}
	
	public static boolean isOwner( Player player, String protectedAreaName ){
		boolean found = false;
		String playerName = "o:" + player.getName();
				
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equalsIgnoreCase(protectedAreaName) ){		
				found = true;
				for( String owner : ProtectedCuboidsOwners.get(i) ){
					if ( owner.equalsIgnoreCase(playerName) ){
						return true;
					}
				}
			}
		}
		
		if (!found){
			player.sendMessage(Colors.Rose+"Could not find any protected area named "+protectedAreaName);
		}
		
		return false;
	}
	
	//////////////////////////////
	////    DATA TREATMENT    ////
	//////////////////////////////
			
	public static byte protegerCuboid(String playerName, ArrayList<String> ownersList, String cuboidName, boolean highProtect){
				
		for(String test : ProtectedCuboidsNames){
			if(test.equals(cuboidName)){
				if (CuboidPlugin.logging)
					CuboidPlugin.log.info(playerName+" failed to create a protected cuboid named "+cuboidName+" (aleady used)");
				return 1;
			}
		}
			
		int[] firstPoint = Cuboid.getPoint(playerName, false);
		int[] secondPoint = Cuboid.getPoint(playerName, true);
		
		if ( highProtect ){
			firstPoint[1] = 0;
			secondPoint[1] = 128;
		}
		else if( firstPoint[1] == secondPoint[1] ){	// s'ils sont à la même hauteur
			firstPoint[1]-=addedHeight;
			secondPoint[1]+=addedHeight;
		}
		
		ProtectedCuboids.add(firstPoint[0]);	// Méthode plus élégante
		ProtectedCuboids.add(firstPoint[1]);
		ProtectedCuboids.add(firstPoint[2]);
		ProtectedCuboids.add(secondPoint[0]);
		ProtectedCuboids.add(secondPoint[1]);
		ProtectedCuboids.add(secondPoint[2]);
		ProtectedCuboidsOwners.add(ownersList);
		ProtectedCuboidsNames.add(cuboidName);
			
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("cuboids/protectedCuboids.txt", true));
			String newProtectedCuboid = firstPoint[0]+","+firstPoint[1]+","+firstPoint[2]+","+secondPoint[0]+","+secondPoint[1]
			       +","+secondPoint[2]+","+ownersString(ownersList)+","+cuboidName;
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
		
		File dataSource = new File("cuboids/protectedCuboids.txt");
		if (dataSource.exists()){
			try {
	            StringBuilder newFile = new StringBuilder();
	            
	            for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
	            	if(ProtectedCuboidsNames.get(i).equals(cuboidName)) {
	            		newFile.append( firstPoint[0]+","+firstPoint[1]+","+firstPoint[2]+","+secondPoint[0]+","+secondPoint[1]
	            	    +","+secondPoint[2]+","+ownersString(i)+","+ProtectedCuboidsNames.get(i) ).append("\r\n");
	            	} else {
	            		newFile.append( ProtectedCuboids.get(i*6)+","+ProtectedCuboids.get(i*6+1)+","+ProtectedCuboids.get(i*6+2)
	            				+","+ProtectedCuboids.get(i*6+3)+","+ProtectedCuboids.get(i*6+4)+","+ProtectedCuboids.get(i*6+5)
	            				+","+ownersString(i)+","+ProtectedCuboidsNames.get(i) ).append("\r\n");
	            	}
	            }

	            FileWriter writer = new FileWriter("cuboids/protectedCuboids.txt");
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
        
        ArrayList<ArrayList<String>> oldProtectedCuboidsOwners = ProtectedCuboidsOwners;
        ProtectedCuboidsOwners = new ArrayList<ArrayList<String>>();
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
		
		File dataSource = new File("cuboids/protectedCuboids.txt");
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

	            FileWriter writer = new FileWriter("cuboids/protectedCuboids.txt");
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
	
	public static byte addPlayer( String[] split, String protectedAreaName ){
		byte returnCode = 1;
		for (int i = 0; i<ProtectedCuboidsNames.size(); i++){
			if( ProtectedCuboidsNames.get(i).equals(protectedAreaName) ){
				
				ArrayList<String> allowedList = ProtectedCuboidsOwners.get(i);
				
				for (String newPlayer : split){
					boolean done = false;
					boolean newIsOwner = false;
					if ( newPlayer.startsWith("o:") ){
						newPlayer = newPlayer.substring(2);	
						newIsOwner = true;
					}
					
					for ( int j = 0; j < allowedList.size() && !done; j++ ){
						String allowedPlayer = allowedList.get(j);
						
						if ( allowedPlayer.equalsIgnoreCase(newPlayer) ){
							if ( newIsOwner ){ // on corrige s'il faut
								allowedList.set(j, "o:" + newPlayer);
							}
							done = true;
						}
						
						if ( allowedPlayer.equalsIgnoreCase("o:" + newPlayer) ){
							done = true;
						}	
					}
					
					if ( !done ){
						allowedList.add( ((newIsOwner) ? "o:" : "") + newPlayer );
					}
				}
				
				ProtectedCuboidsOwners.set(i, allowedList);
				
				File dataSource = new File("cuboids/protectedCuboids.txt");
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
			            				+","+ProtectedCuboids.get(i*6+5)+","+ownersString(i)+","
			            				+ProtectedCuboidsNames.get(i) ).append("\r\n");
			            	}
			            }
			            reader.close();

			            FileWriter writer = new FileWriter("cuboids/protectedCuboids.txt");
			            writer.write(newFile.toString());
			            writer.close();
		    
			        } catch (Exception ex) {
			        	CuboidPlugin.log.log(Level.SEVERE, "A problem occured while modifying the cuboid", ex);
			        	return 3;
			        }
			        
			        Cuboid.updateChestsState( ProtectedCuboids.get(i*6), ProtectedCuboids.get(i*6+1), ProtectedCuboids.get(i*6+2),
			        		ProtectedCuboids.get(i*6+3), ProtectedCuboids.get(i*6+4), ProtectedCuboids.get(i*6+5) );
			        
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
				ArrayList<String> allowedList = ProtectedCuboidsOwners.get(i);

				for (String playerName : split){
					allowedList.remove(playerName);
				}

				ProtectedCuboidsOwners.set(i, allowedList);
				
				File dataSource = new File("cuboids/protectedCuboids.txt");
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
			            				+","+ProtectedCuboids.get(i*6+5)+","+ownersString(i)+","
			            				+ProtectedCuboidsNames.get(i) ).append("\r\n");
			            	}
			            }
			            reader.close();

			            FileWriter writer = new FileWriter("cuboids/protectedCuboids.txt");
			            writer.write(newFile.toString());
			            writer.close();
		    
			        } catch (Exception ex) {
			        	CuboidPlugin.log.log(Level.SEVERE, "A problem occured while modifying the cuboid file", ex);
			        	return 3;
			        }
			        
			        Cuboid.updateChestsState( ProtectedCuboids.get(i*6), ProtectedCuboids.get(i*6+1), ProtectedCuboids.get(i*6+2),
			        		ProtectedCuboids.get(i*6+3), ProtectedCuboids.get(i*6+4), ProtectedCuboids.get(i*6+5) );
			        
			        if (CuboidPlugin.logging)
		        		CuboidPlugin.log.info("Removed owners from "+protectedAreaName);
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
