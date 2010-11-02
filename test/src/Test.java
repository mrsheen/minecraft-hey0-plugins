import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Test extends Plugin {

	static final Logger log = Logger.getLogger("Minecraft");
    static String LOG_PREFIX = "[Portal Test] : ";
	static Server server = etc.getServer();
	Date date = new Date();
	
	private int[][][] NameSignLocs = {{{-1,3,0},{1,3,0}},{{0,3,-1},{0,3,1}}};
    private int[][][] DestSignLocs = {{{1,0,1},{1,0,-1},{1,1,1},{1,1,-1},{1,2,1},{1,2,-1}},{{-1,0,1},{1,0,1},{-1,1,1},{1,1,1},{-1,2,1},{1,2,1}}};
	
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Integer> fireblockLoc = new ArrayList<Integer>();
	static ArrayList<Long> fireblockTimestamps = new ArrayList<Long>();
	
	
	static ArrayList<String> portalNames = new ArrayList<String>();
	static ArrayList<Integer> protectedSigns = new ArrayList<Integer>();
    
	private ArrayList<PluginRegisteredListener> listeners = new ArrayList<PluginRegisteredListener>();
	
    public void enable() {
		log.info(LOG_PREFIX+"Mod Enabled.");
		//Load portal data
        int portalsAdded = 0;
        
        String portals = etc.getDataSource().getPortalNames();
        for (String portalName : portals.split(" ")) {
            Portal portal = etc.getDataSource().getPortal(portalName);
            if (portal != null) {
                addPortal(null, portal);
                portalsAdded++;
            }
        }
        
        log.info(LOG_PREFIX+"Loaded "+portalsAdded+" portals");
        
    }

    public void disable() {
		
        
		PluginLoader loader = etc.getLoader();
		for (PluginRegisteredListener rl : listeners)
			loader.removeListener(rl);
		listeners.clear();
        
        log.info(LOG_PREFIX+"Mod Disabled");
    }
    
    public void initialize() {
    	Listener l = new Listener();

        listeners.add(etc.getLoader().addListener(PluginLoader.Hook.PORTALWARP, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(PluginLoader.Hook.PORTALCREATE, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(PluginLoader.Hook.PORTALDESTROY, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, l, this, PluginListener.Priority.LOW));
        listeners.add(etc.getLoader().addListener(PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.LOW));
    }
    
    public boolean addPortal(Player player, Portal portal) {
        if(portal.loc1.x==0 && portal.loc1.y==128 && portal.loc1.z==0) {
            log.info(LOG_PREFIX+"Blank portal passed");
            return true;
        }
        
        if(player!=null) {
        	player.sendMessage("You did it!");
        	log.info("Portal created by "+player.getName());
        } else {
        	log.info("Portal created by a null player");
        }
        
        Sign namingsign = getNameSign(portal);
        String portalname = "";
        if(namingsign != null) {
            for(int i=0;i<4;i++) {
                if(namingsign.getText(i).length()!=0) {
                    if(portalname=="") {
                        portalname = namingsign.getText(i).toLowerCase();
                    } else {
                        log.info(LOG_PREFIX+"Portal name signs must only have one non-blank line");
                        return true;
                    }
                }
            }
            if(portalname.split(" ").length==1) {
                if(portalname.split(":").length!=1) {
                    log.info(LOG_PREFIX+"Portal names may not have : in them");
                    return true;
                }
            } else {
                log.info(LOG_PREFIX+"Portal names may not have spaces in them");
                return true;
            }
        } else {
            log.info(LOG_PREFIX+"Naming sign incorrectly positioned.");
            log.info(LOG_PREFIX+"Place a single sign on the top row of obsidian");
            return true;
        }
        
        //check if the portal already exists
        if(getPortalIndex(portalname)!=-1) {
        	log.info(LOG_PREFIX+"Portal "+portalname+" is already created somewhere.");
        	return true;
        }
        	
        // Have correct name, save data
        portal.setName(portalname);
        
        portalNames.add(portal.Name);
        protectedSigns.add(namingsign.getX());
        protectedSigns.add(namingsign.getY());
        protectedSigns.add(namingsign.getZ());
        log.info(LOG_PREFIX+"Portal "+portalname+" has been created.");
        
        // write data to the storage file
                
        return false;
    }
    
    private class Listener extends PluginListener {
		public boolean onPortalWarp(Player player, Portal portal) {
			int portalIndex = getPortalIndex(portal.Name);
			if(portalIndex == -1) {
				log.info(LOG_PREFIX+player.getName()+" is using an unnamed portal");
				return false;
			}
            //check that there is only one non-blank sign attached to the side of portalFrom
			Sign destsign = getDestSign(portal);
			String destportalname = "";
            //check that the portalTo name is valid
			if(destsign != null) {
				for(int i=0;i<4;i++) {
					if(destsign.getText(i).length()!=0) {
						if(destportalname=="") {
							destportalname = destsign.getText(i).toLowerCase();
						} else {
							log.info(LOG_PREFIX+"Portal name signs must only have one non-blank line");
							return true;
						}
					}
				}
				if(destportalname.split(" ").length==1) {
					if(destportalname.split(":").length!=1) {
						log.info(LOG_PREFIX+"Portal names may not have : in them");
						return true;
					}
				} else {
					log.info(LOG_PREFIX+"Portal names may not have spaces in them");
					return true;
				}
			} else {
				log.info(LOG_PREFIX+"Naming sign incorrectly positioned.");
				log.info(LOG_PREFIX+"Please a single sign one of the side columns of the portal");
            	return true;
			}
			int destportalIndex = getPortalIndex(destportalname);
			if(destportalIndex == -1) {
				player.sendMessage("There is no portal with the name "+destportalname);
				return true;
			}
			
			Portal destPortal = etc.getDataSource().getPortal(destportalname);
			
            if (destPortal == null)
                return true;
            
			double destx = 0;
			if(destPortal.loc1.x == destPortal.loc2.x) {
				destx = destPortal.loc1.x + 0.5D;
			} else if(destPortal.loc1.x<destPortal.loc2.x) {
				destx = (destPortal.loc2.x-destPortal.loc1.x)/2 + destPortal.loc1.x;
			} else {
				destx = (destPortal.loc1.x-destPortal.loc2.x)/2 + destPortal.loc2.x;
			}
			
			double destz = 0;
			if(destPortal.loc1.z == destPortal.loc2.z) {
				destz = destPortal.loc1.z + 0.5D;
			} else if(destPortal.loc1.z<destPortal.loc2.z) {
				destz = (destPortal.loc2.z-destPortal.loc1.z)/2 + destPortal.loc1.z;
			} else {
				destz = (destPortal.loc1.z-destPortal.loc2.z)/2 + destPortal.loc2.z;
			}
			
            //translate player from portalFrom to portalTo
			player.teleportTo(destx,destPortal.loc1.y,destz,player.getRotation(), player.getPitch());
			
            player.sendMessage("Warping from portal "+portalNames.get(portalIndex)+" to portal "+destportalname);
			
			
			
			
			//blank the sign
			return true;
		}
        
        public boolean onPortalCreate(Portal portal) {
        	Player player=null;
        	long curtime = date.getTime();
        	if(playerList.size()>0) {
        		for(int i=playerList.size()-1;i>=0;i--) {
        			if (fireblockLoc.get(i*3) == (int)Math.floor(portal.loc1.x) && 
        				fireblockLoc.get(i*3+1) == (int)Math.floor(portal.loc1.y) && 
        				fireblockLoc.get(i*3+2) == (int)Math.floor(portal.loc1.z)) {
        					log.info("Found a player");
        					player=server.getPlayer(playerList.get(i));
        				}
        			if((curtime - fireblockTimestamps.get(i))>60) {
        				playerList.remove(i);
        				fireblockTimestamps.remove(i);
        				fireblockLoc.remove(i*3);
        				fireblockLoc.remove(i*3);
        				fireblockLoc.remove(i*3);
        			}
        		}
        	}
			return addPortal(player,portal);
		}
        
        public boolean onPortalDestroy(Portal portal) {
            int portalIndex = getPortalIndex(portal.Name);
			if(portalIndex == -1) {
				log.info(LOG_PREFIX+"An unnamed portal is being destroyed");
				return true;
			}
			
			portalNames.remove(portalIndex);
			
			protectedSigns.remove(portalIndex*3);
			protectedSigns.remove(portalIndex*3);
			protectedSigns.remove(portalIndex*3);
			
			//delete data from file
			log.info(LOG_PREFIX+"Destroyed portal: " + portal.Name);
			return false;
		}
        
        public boolean onBlockDestroy(Player player, Block block) {
    		if(block.getType() == 68) {
    			for(int i=0;i<protectedSigns.size()/3;i++) {
    				if(block.getX() == protectedSigns.get(i*3) &&
    				block.getY() == protectedSigns.get(i*3+1) &&
    				block.getZ() == protectedSigns.get(i*3+2)) {
    				return true;
    				}
    			}
    		}
    		return false;
    	}
        
        public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
    		 if(blockPlaced.getType() == 51) {
    		 	if(server.getBlockIdAt(blockPlaced.getX(),blockPlaced.getY()-1,blockPlaced.getZ())==49) {
    		 		if(playerList.size()>0) {
        				for(int i=playerList.size()-1;i>=0;i--) {
        					if (fireblockLoc.get(i*3) == blockPlaced.getX() && 
        						fireblockLoc.get(i*3+1) == blockPlaced.getY() && 
        						fireblockLoc.get(i*3+2) == blockPlaced.getZ()) {
        						playerList.set(i,player.getName());
        						fireblockTimestamps.set(i,date.getTime());
        						return false;
        					}
        				}
    		 		}
    		 		player.sendMessage("Fire on obsidian");
    		 		playerList.add(player.getName());
    		 		fireblockLoc.add(blockPlaced.getX());
    		 		fireblockLoc.add(blockPlaced.getY());
    		 		fireblockLoc.add(blockPlaced.getZ());
    		 		fireblockTimestamps.add(date.getTime());
    		 	}
    		 }
    		return false;
    	}
        
        public boolean onCommand(Player player, String[] split) {
            if (!player.canUseCommand("/listportals"))
				return false;
			
			String cmd = split[0].toLowerCase();
			
			if (cmd.equals("/listportals")) {
				String portals = etc.getDataSource().getPortalNames();
				
				player.sendMessage(Colors.Rose+"Portals: " + Colors.White + portals);
                return true;
            }
            return false; 
        }
    }
    
	private static int getPortalIndex(String portalName) {
		boolean inList = false;
		for (String p : portalNames) {
			if (p.equalsIgnoreCase(portalName))
				inList = true;
		}
		if (!inList) {
			return -1;
		}
		return portalNames.indexOf(portalName);
	}
    
    public Sign getNameSign(Portal portal) {
    	Sign returnsign = null;
    	int[] locarray = {(int)Math.floor(portal.loc1.x),
						(int)Math.floor(portal.loc1.y),
						(int)Math.floor(portal.loc1.z),
						(int)Math.floor(portal.loc2.x),
						(int)Math.floor(portal.loc2.y),
						(int)Math.floor(portal.loc2.z)};
    	
    	int facing = 1;
    	if((locarray[0]-locarray[3]) == 0) {
    		facing = 0;
    	}
		for(int i=0;i<2;i++) {
			for(int j=0;j<2;j++) {
				if(server.getBlockIdAt(locarray[j*3]+NameSignLocs[facing][i][0],
										locarray[j*3+1]+NameSignLocs[facing][i][1], 
										locarray[j*3+2]+NameSignLocs[facing][i][2]) == 68) {
					if(returnsign != null) {
						return null;
					}
					returnsign = (Sign) server.getComplexBlock(locarray[j*3]+NameSignLocs[facing][i][0],
																locarray[j*3+1]+NameSignLocs[facing][i][1], 
																locarray[j*3+2]+NameSignLocs[facing][i][2]);
				}
			}
		}
    	return returnsign;	
    }
    
    public Sign getDestSign(Portal portal) {
    	Sign returnsign = null;
    	int[] locarray = {(int)Math.floor(portal.loc1.x),
						(int)Math.floor(portal.loc1.y),
						(int)Math.floor(portal.loc1.z),
						(int)Math.floor(portal.loc2.x),
						(int)Math.floor(portal.loc2.y),
						(int)Math.floor(portal.loc2.z)};
    	if(portal.loc2.x<portal.loc1.x || portal.loc2.z<portal.loc1.z) {
    		locarray[0] = (int)Math.floor(portal.loc2.x);
			locarray[1] = (int)Math.floor(portal.loc2.y);
			locarray[2] = (int)Math.floor(portal.loc2.z);
			locarray[3] = (int)Math.floor(portal.loc1.x);
			locarray[4] = (int)Math.floor(portal.loc1.y);
			locarray[5] = (int)Math.floor(portal.loc1.z);	
    	}
    	int facing = 0;
    	if((locarray[0]-locarray[3]) == 0) {
    		facing = 1;
    	}
    	int k = 0;
    	for (int j=0;j<2;j++) {
    		if(j==0) {
    			k=-1;
    		} else {
    			k=1;
    		}
    		for (int i=0;i<6;i++) {
                if(server.getBlockIdAt(locarray[j*3]+DestSignLocs[facing][i][0]*k,
										locarray[j*3+1]+DestSignLocs[facing][i][1], 
										locarray[j*3+2]+DestSignLocs[facing][i][2]*k) == 68) {
					if(returnsign != null) {
						return null; // If we get multiple matches, return none of them
					}
					returnsign = (Sign) server.getComplexBlock(locarray[j*3]+DestSignLocs[facing][i][0]*k,
																locarray[j*3+1]+DestSignLocs[facing][i][1], 
																locarray[j*3+2]+DestSignLocs[facing][i][2]*k);
				}
    		}
    	}
    	return returnsign;
    }
}
