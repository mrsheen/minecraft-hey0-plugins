import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;

@SuppressWarnings("serial")
public class CuboidData implements Serializable {
	// Version 14 : 11/11 15h30 GMT+1
	// for servermod 123-125+
	
	public String owner = "";
	private String name ="";
	private int[][][] cuboidData;
	public byte loadReturnCode;
	
	CuboidData( int Xsize, int Ysize, int Zsize ){
		for (int i = 0; i<Xsize; i++){
			this.cuboidData[i] = new int[Ysize][];
			for (int j = 0; j < Ysize; ++j) {
				this.cuboidData[i][j] = new int[Zsize];
			}
		}
	}
	
	CuboidData( String owner, String name, int[][][] tableau ){
		this.owner = owner;
		this.name = name;
		this.cuboidData = tableau;
	}
	
	CuboidData ( String owner, String name ){
		this.owner = owner;
		this.name = name;
		this.loadReturnCode = this.load();
	}
		
	public int[][][] getData(){
		return this.cuboidData;	
	}
	
	public byte save(){
		
		File cuboidFolder = new File("cuboids");
        try {
            if (!cuboidFolder.exists()) {
            	cuboidFolder.mkdir();
            }
            File ownerFolder = new File("cuboids/"+owner);
            try {
	            if (!ownerFolder.exists()){
	            	ownerFolder.mkdir();
	            }
            }
            catch( Exception e){
            	e.printStackTrace();
            	return 1;
            }
        }
        catch( Exception e){
        	e.printStackTrace();
        	return 1;
        }
		
		try {
			ObjectOutputStream oos = new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream(  new File("cuboids/"+owner+"/"+this.name+".cuboid") ) ) );
	        oos.writeObject(this.cuboidData);
	        oos.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			return 2;
	    }  
		CuboidPlugin.log.info("New saved cuboid : "+this.name);
		return 0;
	}
	
	private byte load(){
		try {
			ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream( new FileInputStream( new File("cuboids/"+owner+"/"+this.name+".cuboid") ) ) );
	        try {
	        	this.cuboidData = (int[][][])( ois.readObject() );
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        	return 3;
	        }
	        ois.close();
		}
		catch (FileNotFoundException e) {
				e.printStackTrace();
				return 1;
	    }
		catch (IOException e) {
			e.printStackTrace();
			return 2;
		}
		CuboidPlugin.log.info("Loaded cuboid : "+this.name);
		return 0;
	}
	
	public static boolean copyFile(File sourceFile, File destFile) {
		boolean returnStatus = true;
		if(!destFile.exists()) {
			try{
				destFile.createNewFile();
			}
			catch(Exception e){
				returnStatus = false;
			}
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		catch (Exception e){
			returnStatus = false;
		}
		finally {
			if(source != null) {
				try{
					source.close();
				}
				catch(Exception e){
					returnStatus = false;
				}
			}
			if(destination != null) {
				try{
					destination.close();
				}
				catch(Exception e){
					returnStatus = false;
				}
			}
		}
		return returnStatus;
	}
	
}
