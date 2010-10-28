import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.io.*;
import java.util.*;

public class TimeCop extends Plugin {

    private TimeCopListener listener = new TimeCopListener();

	static final Logger log = Logger.getLogger("Minecraft");
	static MinecraftServer mcworld = etc.getMCServer();
	
	 private Timer TimeCopTicker;
	 private int TimeCopTickInterval = 40000;
	 
	 private long timetofreeze = 0;
	 private int curmode = 0;
	 
	 private long loopfrom = 0;
	 private long loopto = 0;
	
    public void enable() {
		log.info("[TimeCop] Mod Enabled.");
		if (TimeCopTickInterval > 0) {
           TimeCopTicker = new Timer();
            TimeCopTicker.schedule(new TimeCopTickerTask(), 400, TimeCopTickInterval);
        }
    }

    public void disable() {
		log.info("[TimeCop] Mod Disabled");
        if (TimeCopTicker != null) {
            TimeCopTicker.cancel();
        }
    }
    
    public void initialize() {
    	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
    	
    }
    
        public class TimeCopListener extends PluginListener {
        	    public boolean onCommand(Player player,String[] split) {
    		if (split[0].equalsIgnoreCase("/enforcetime") && (split.length>1)) {
    			if(split[1].equalsIgnoreCase("day")) {
    				player.sendMessage("Daytime enforced!");
    				timetofreeze = 6000;
    				setthetime(timetofreeze);
    				curmode = 1;
    			} else if(split[1].equalsIgnoreCase("night")) {
    				player.sendMessage("Nighttime enforced!");
    				timetofreeze = 18000;
    				setthetime(timetofreeze);
    				curmode = 1;
    				
    			} else {
    				player.sendMessage("Enforcetime can ony enforce day or night");
    			}
    			return true;
    		} else if (split[0].equalsIgnoreCase("/normaltime")) {
    				player.sendMessage("Timeflow returned to normal!");
    				curmode = 0;
    			return true;
    		} else if (split[0].equalsIgnoreCase("/freezetime") && (split.length>1)) {
    			if(converttime(split[1]) != -1) {
    				timetofreeze = converttime(split[1]);
    				setthetime(timetofreeze);
    				curmode = 1;
    				player.sendMessage("Time Frozen");
    			} else {
    				player.sendMessage(split[1] + " is not a valid input");
    			}
    			return true;
    		} else if (split[0].equalsIgnoreCase("/looptime")) {
    			if(split.length==2) {
    				if(split[1].equalsIgnoreCase("day")) {
    					loopfrom = 24000;
    					loopto = 11000;
    					setthetime(loopfrom);
    					curmode = 2;
    				} else if(split[1].equalsIgnoreCase("night")) {
    					player.sendMessage("You want to loop night? Not implemented yet!");
    					
    				} else {
    					player.sendMessage("You are doing it wrong!");
    				}
    			} else if(split.length>2) {
    				if(converttime(split[1])!=-1 && converttime(split[2])!=-1) {
    					loopfrom = converttime(split[1]);
    					loopto = converttime(split[2]);
    					setthetime(loopfrom);
    					curmode = 2;
    					player.sendMessage("Timeloop started");
    				} else {
    					player.sendMessage("Get the inputs right. Jerk!");
    				}
    			}
    			
    			
    			
    			
    			return true;
    		} else if (split[0].equalsIgnoreCase("/gettime")) {
    			player.sendMessage("The time is " + etc.getServer().getTime());
    			return true;
    	}
    	return false;
   	}
        	
        }
        	

	
    private class TimeCopTickerTask extends TimerTask {
        public void run()
        {
            updateTime();
        }
    }
    
    private void updateTime() {	
    	switch(curmode) {
    		case 1: 
    			setthetime(timetofreeze);
    			break;
    		case 2: 
    			if(loopfrom>loopto) {
    				if(getthetime()<loopfrom && getthetime()>loopto) {
    					setthetime(loopfrom);
    				}
    			} else if (loopto>loopfrom) {
    				if(getthetime()<loopfrom || getthetime()>loopto) {
    					setthetime(loopfrom);
    				}
    			} else {
    				setthetime(loopfrom);
    			}
    			break;
    		
    		default: 
    			break;
    	}
    	return;
    }
    
    private long converttime(String input) {
    	long returnnumber = -1;
    	try {
    		returnnumber = Long.parseLong(input.trim());
    	} catch (NumberFormatException nfe) {
    		if(input.equalsIgnoreCase("noon") || input.equalsIgnoreCase("day")) {
    			returnnumber = 6000;
    		} else if(input.equalsIgnoreCase("midnight") || input.equalsIgnoreCase("night")) {
    			returnnumber = 18000;
    		} else if(input.equalsIgnoreCase("dusk")) {
    			returnnumber = 12500;
    		} else if(input.equalsIgnoreCase("dawn")) {
    			returnnumber = 22500;
    		}
    	}
    	if(returnnumber>24000 || returnnumber<0) {
    		returnnumber = -1;
    	}
    	return returnnumber;
    }
    private void setthetime(long timetoset) {
    	long curtime = getthetime();
    	long curtimelong = etc.getServer().getTime();
    	
    	if(timetoset > curtime) {
    		etc.getServer().setTime(curtimelong - curtime + timetoset);
    	} else if (curtime > timetoset) {
    		etc.getServer().setTime(curtimelong - curtime + timetoset + 24000);
    	}
    }
    
    private long getthetime() {
    	return (long) etc.getServer().getTime()%24000;	
    }
}
