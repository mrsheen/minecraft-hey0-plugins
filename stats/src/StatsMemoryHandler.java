import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Handler;
import java.util.logging.MemoryHandler;
import java.util.logging.Level;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;


/**
 * StatsHandler outputs contents to a the correct file, depending on the logrecord contents
 */
public class StatsMemoryHandler extends MemoryHandler {


    protected static final Logger log = Logger.getLogger("Minecraft");

	private String logAction;
	private int maxLogLines;
	
	private int currentLogLines = 0;
	
	public StatsMemoryHandler(String action, int buffer, Handler handler){
		
		super(handler,buffer+10, Level.OFF);
		logAction = action;
		maxLogLines = buffer;
	}
	
	@Override
	public synchronized void publish(LogRecord record) {
		// Check it is the correct player MemoryHandler
		//log.info("publish1: " + Integer.toString(currentLogLines));
		
		if (!isLoggable(record))
			return;
		super.publish(record);
		currentLogLines++;
		//log.info("publish2: " + Integer.toString(currentLogLines));
		
		if (currentLogLines >= maxLogLines - 1) {
			//log.info("Pushing : " + Integer.toString(currentLogLines) + Integer.toString(maxLogLines));
			log.info("[Stats] : Pushing " + Integer.toString(currentLogLines) + " to database");
			currentLogLines = 0;
			// Condition occurred so dump buffered records
			push();
			
		}
		
	}
	
	public boolean isLoggable(LogRecord record) {
		StatsLogRecord logRecord = (StatsLogRecord)record;
		
		
		if (logRecord.table.equalsIgnoreCase(logAction)) {
			return true;
		}
		else {
			return false;
		} 
		
	}
	
	
}

