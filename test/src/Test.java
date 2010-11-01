import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Test extends Plugin {

	static final Logger log = Logger.getLogger("Minecraft");
	static Server server = etc.getServer();
	
	private int[][][] NameSignLocs = {{{-1,3,0},{1,3,0}},{{0,3,-1},{0,3,1}}};
    private int[][][] DestSignLocs = {{{1,0,1},{1,0,-1},{1,1,1},{1,1,-1},{1,2,1},{1,2,-1}},{{-1,0,1},{1,0,1},{-1,1,1},{1,1,1},{-1,2,1},{1,2,1}}};
	
	
	static ArrayList<String> playerList = new ArrayList<String>();
	static ArrayList<Integer> fireblockLoc = new ArrayList<Integer>();
	
	
	static ArrayList<String> portalNames = new ArrayList<String>();
	static ArrayList<Integer> protectedSigns = new ArrayList<Integer>();
	
	
	
	PluginRegisteredListener onportalentranceListener;
	onportalentranceListener listener_portenter = new onportalentranceListener();
	
	PluginRegisteredListener onportalcreateListener;
	onportalcreateListener listener_portcreate = new onportalcreateListener();
	
	PluginRegisteredListener onportaldestroyListener;
	onportaldestroyListener listener_portdestroy = new onportaldestroyListener();
	
	PluginRegisteredListener blockdestroyListener;
	blockdestroyListener listener_blockdestroy = new blockdestroyListener();
    
    PluginRegisteredListener blockcreateListener;
	blockcreateListener listener_blockcreate = new blockcreateListener();
	
    public void enable() {
		log.info("[Portal Test] Mod Enabled.");
		//Load portal data
    }

    public void disable() {
		log.info("[Portal Test] Mod Disabled");
		etc.getLoader().removeListener(onportalentranceListener);
		etc.getLoader().removeListener(onportalcreateListener);
		etc.getLoader().removeListener(onportaldestroyListener);
		etc.getLoader().removeListener(blockdestroyListener);
        etc.getLoader().removeListener(blockcreateListener);
    }
    
    public void initialize() {
    	onportalentranceListener = etc.getLoader().addListener(PluginLoader.Hook.PORTALWARP, listener_portenter, this, PluginListener.Priority.MEDIUM);
    	onportalcreateListener = etc.getLoader().addListener(PluginLoader.Hook.PORTALCREATE, listener_portcreate, this, PluginListener.Priority.MEDIUM);
    	onportaldestroyListener = etc.getLoader().addListener(PluginLoader.Hook.PORTALDESTROY, listener_portdestroy, this, PluginListener.Priority.MEDIUM);
    	blockdestroyListener = etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener_blockdestroy, this, PluginListener.Priority.MEDIUM);
        blockcreateListener = etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED,listener_blockcreate, this, PluginListener.Priority.MEDIUM);
    }
    
    public class onportalentranceListener extends PluginListener {
		public boolean onPortalWarp(Player player, Portal portal) {
			int portalIndex = getPortalIndex(portal.Name);
			if(portalIndex == -1) {
				log.info("ERROR: "+player.getName()+" is trying to use a portal unnamed by plugin");
				return true;
			}
			Sign destsign = getDestSign(portal);
			String destportalname = "";
			if(destsign != null) {
				for(int i=0;i<4;i++) {
					if(destsign.getText(i).length()!=0) {
						if(destportalname=="") {
							destportalname = destsign.getText(i).toLowerCase();
						} else {
							log.info("Portal name signs must only have one non-blank line");
							return true;
						}
					}
				}
				if(destportalname.split(" ").length==1) {
					if(destportalname.split(":").length!=1) {
						log.info("Portal names may not have : in them");
						return true;
					}
				} else {
					log.info("Portal names may not have spaces in them");
					return true;
				}
			} else {
				log.info("Naming sign incorrectly positioned.");
				log.info("Please a single sign one of the side columns of the portal");
            	return true;
			}
			int destportalIndex = getPortalIndexFromPluginName(destportalname);
			if(destportalIndex == -1) {
				player.sendMessage("There is no portal with the name "+destportalname);
				return true;
			}
			
			Portal destportal = etc.getDataSource().getPortal(portalNames.get(destportalIndex*2));
			
			double destx = 0;
			if(destportal.loc1.x == destportal.loc2.x) {
				destx = destportal.loc1.x + 0.5D;
			} else if(destportal.loc1.x<destportal.loc2.x) {
				destx = (destportal.loc2.x-destportal.loc1.x)/2 + destportal.loc1.x;
			} else {
				destx = (destportal.loc1.x-destportal.loc2.x)/2 + destportal.loc2.x;
			}
			
			double destz = 0;
			if(destportal.loc1.z == destportal.loc2.z) {
				destz = destportal.loc1.z + 0.5D;
			} else if(destportal.loc1.z<destportal.loc2.z) {
				destz = (destportal.loc2.z-destportal.loc1.z)/2 + destportal.loc1.z;
			} else {
				destz = (destportal.loc1.z-destportal.loc2.z)/2 + destportal.loc2.z;
			}
			
			player.teleportTo(destx,destportal.loc1.y,destz,player.getRotation(), player.getPitch());
			
            player.sendMessage("Warping from portal "+portalNames.get(portalIndex*2+1)+" to portal "+destportalname);
			//check that there is only one non-blank sign attached to the side of portalFrom
			//check that the portalTo name is valid
			//get portalTo
			//translate player from portalFrom to portalTo
			//blank the sign
			return false;
		}
    }
    
	private static int getPortalIndex(String portalName) {
		boolean inList = false;
		for (String p : portalNames) {
			if (p==portalName)
				inList = true;
		}
		if (!inList) {
			return -1;
		}
		return portalNames.indexOf(portalName)/2;
	}
    
	private static int getPortalIndexFromPluginName(String portalName) {
		boolean inList = false;
		for (String p : portalNames) {
			if (p==portalName)
				inList = true;
		}
		if (!inList) {
			return -1;
		}
		return (portalNames.indexOf(portalName)-1)/2;
	}
    
    
    
    public class onportalcreateListener extends PluginListener {
		public boolean onPortalCreate(Portal portal) {
			if(portal.loc1.x==0 && portal.loc1.y==128 && portal.loc1.z==0) {
				log.info("ERROR: Blank portal passed");
				return true;
			}
			Sign namingsign = getNameSign(portal);
			String portalname = "";
			if(namingsign != null) {
				for(int i=0;i<4;i++) {
					if(namingsign.getText(i).length()!=0) {
						if(portalname=="") {
							portalname = namingsign.getText(i).toLowerCase();
						} else {
							log.info("Portal name signs must only have one non-blank line");
							return true;
						}
					}
				}
				if(portalname.split(" ").length==1) {
					if(portalname.split(":").length!=1) {
						log.info("Portal names may not have : in them");
						return true;
					}
				} else {
					log.info("Portal names may not have spaces in them");
					return true;
				}
			} else {
				log.info("Naming sign incorrectly positioned.");
				log.info("Place a single sign on the top row of obsidian");
            	return true;
			}
			
			portalNames.add(portal.Name);
			portalNames.add(portalname);
			protectedSigns.add(namingsign.getX());
			protectedSigns.add(namingsign.getY());
			protectedSigns.add(namingsign.getZ());
			log.info("Portal "+portalname+" has been created.");
			
						// write data to the storage file
						
			return false;
		}
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
    	int facing = 1;
    	if((locarray[0]-locarray[3]) == 0) {
    		facing = 0;
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
						return null;
					}
					returnsign = (Sign) server.getComplexBlock(locarray[j*3]+DestSignLocs[facing][i][0]*k,
																locarray[j*3+1]+DestSignLocs[facing][i][1], 
																locarray[j*3+2]+DestSignLocs[facing][i][2]*k);
				}
    		}
    	}
    	return returnsign;
    }
    
    public class onportaldestroyListener extends PluginListener {
		public boolean onPortalDestroy(Portal portal) {
			int portalIndex = getPortalIndex(portal.Name);
			if(portalIndex == -1) {
				log.info("ERROR: An unnamed portal is being destroyed");
				return true;
			}
			String portalPluginName = portalNames.get(portalIndex*2+1);
			
			portalNames.remove(portalIndex*2);
			portalNames.remove(portalIndex*2);
			
			protectedSigns.remove(portalIndex*3);
			protectedSigns.remove(portalIndex*3);
			protectedSigns.remove(portalIndex*3);
			
			//delete data from file
			log.info("Destroyed portal: " + portalPluginName);
			return false;
		}
    }
    
    public class blockdestroyListener extends PluginListener {
    	public boolean onBlockDestroy(Player player, Block block) {
    		if(block.getType() == 68) {
    			for(int i=0;i<protectedSigns.size()/3;i++) {
    				if(block.getX() == protectedSigns.get(i*3) ||
    				block.getY() == protectedSigns.get(i*3+1) ||
    				block.getZ() == protectedSigns.get(i*3+2)) {
    				return true;
    				}
    			}
    		}
    		return false;
    	}
    }
    
    public class blockcreateListener extends PluginListener {
    	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
    		 if(blockPlaced.getType() == 51) {
                // This player may have just created a portal, add to list
                // to check for subsequent onPortalCreate calls
                
    		 }
    		return false;
    	}
    }
}
