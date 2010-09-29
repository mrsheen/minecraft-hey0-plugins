import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GriefAlertData {
	// Version 3 : 28/09 09h00 GMT+2
	// for servermod100
	
	static final Logger log = Logger.getLogger("Minecraft");
	static ArrayList<Integer> useWatchIDs = new ArrayList<Integer>();
	static ArrayList<Integer> breakWatchIDs = new ArrayList<Integer>();
	static ArrayList<String> useWatchNames = new ArrayList<String>();
	static ArrayList<String> breakWatchNames = new ArrayList<String>();
		
	GriefAlertData(){
			useWatchIDs = new ArrayList<Integer>();
			breakWatchIDs = new ArrayList<Integer>();
			useWatchNames = new ArrayList<String>();
			breakWatchNames = new ArrayList<String>();
			
			File dataSource = new File("watchedBlocks.txt");
			
			if (!dataSource.exists()) {
	            FileWriter writer = null;
	            try {
	                writer = new FileWriter(dataSource);
	                writer.write("#Add the blocs to be watched here (without the #).\r\n");
	                writer.write("#Fromat is : onUse|onBreak:blocID:displayed name(:color)\r\n");
	                writer.write("#Here are some examples :\r\n");
	                writer.write("#onUse:327:lava bucket\r\n");
	                writer.write("#onBreak:57:diamond block:3\r\n");
	            } catch (Exception e) {
	                log.log(Level.SEVERE, "Exception while creating watchedBlocks.txt");
	            } finally {
	                try {
	                    if (writer != null) {
	                        writer.close();
	                    }
	                } catch (IOException e) {
	                    log.log(Level.SEVERE, "Exception while closing writer for watchedBlocks.txt");
	                }
	            }
	        }
			
			try {
				Scanner scanner = new Scanner(dataSource);
				String[] tempdata = {""};
		        while (scanner.hasNextLine()) {
		        	String line = scanner.nextLine();
		        	if (line.startsWith("#") || line.equals("")) {
		                    continue;
		                }
		                tempdata = line.split(":");
		                //	TODO : color support
		                if ( tempdata[0].startsWith("onUse") ){
			                useWatchIDs.add( Integer.parseInt(tempdata[1]) );
			                useWatchNames.add( tempdata[2] );
		                }
		                else if ( tempdata[0].startsWith("onBreak") ){
		                	breakWatchIDs.add( Integer.parseInt(tempdata[1]) );
		                	breakWatchNames.add( tempdata[2] );
		                }
		            }
		            scanner.close();
		            log.info("Antigrief : sucessfuly loaded");
		        } catch (Exception e) {
		        	log.log(Level.SEVERE, "Antigrief plugin : exception while loading", e);
		        }
	}
	
	public static short isUseWatched(int blocID){
		short iterator = 0;
		for  (int b : useWatchIDs ) {
			if (b == blocID)
				return iterator;
			iterator++;
		}
		return -1;
	}
	
	public static short isBreakWatched(int blocID){
		short iterator = 0;
		for  (int b : breakWatchIDs ) {
			if (b == blocID)
				return iterator;
			iterator++;
		}
		return -1;
	}
}
