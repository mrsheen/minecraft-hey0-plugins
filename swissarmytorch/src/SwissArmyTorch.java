import java.util.Hashtable;
import java.util.ArrayList;

public class SwissArmyTorch extends Plugin
{
	private enum Mode { OFF, SNIPE, STICK, LASER, BEAM }
	private Hashtable playerSettings = new Hashtable();
	
	//Class for storing Swiss Army Torch mode, id
	private class SwissSettings
	{
		public Mode mode = Mode.OFF;
		public int type = 0;
		
		public Mode switchMode()
		{
			switch (mode)
			{
			 case OFF:
				mode = Mode.SNIPE;
				break;
			 case SNIPE:
				mode = Mode.STICK;
				break;
			 case STICK:
				mode = Mode.LASER;
				break;
			 case LASER:
				mode = Mode.BEAM;
				break;
			 case BEAM:
				mode = Mode.OFF;
				break;
			 default:
				mode = Mode.OFF;
			}
			return(mode);
		}
	}
	
	//Returns a SwissSettings for the player, making a new one if it has to
	public SwissSettings getSettings(Player player)
	{
		SwissSettings settings = (SwissSettings)playerSettings.get(player.getName());
		if (settings == null)
		{
			playerSettings.put(player.getName(), new SwissSettings());
			settings = (SwissSettings)playerSettings.get(player.getName());
		}

		return(settings);
	}
	
	public void enable()
	{
	}

	public void disable()
	{
	}
	
	//Cleans up settings when player leaves
	public void onDisconnect(Player player) {
		SwissSettings settings = (SwissSettings)playerSettings.get(player.getName());
		if (settings == null)
			return;
		playerSettings.remove(player.getName());
	}

	//Basic command handling
	public boolean onCommand(Player player, String[] split)
	{
		if(split[0].equalsIgnoreCase("/swiss") && player.canUseCommand("/swiss"))
		{
			SwissSettings settings = getSettings(player);
			if (split.length > 1)
			{
				if (split[1].equalsIgnoreCase("help"))
				{
					player.sendMessage("USAGE: /swiss [type]");
					return(true);
				}
				else
				{
					settings.type = Integer.parseInt(split[1]);

					player.sendMessage("SwissArmyTorch  --  Mode: " + settings.mode.name() + "  ID: " + settings.type);
				}
			}
			else
			{
				settings.switchMode();
				player.sendMessage("SwissArmyTorch  --  Mode: " + settings.mode.name() + "  ID: " + settings.type);
			}
			return(true);
		}
		return(false);
	}

	//Hooking on arm animation to fire torch
	public void onArmSwing(Player player)
	{
		if (player.canUseCommand("/swiss"))
		{
			SwissSettings settings = getSettings(player);
		
		    if ((settings.mode != Mode.OFF) && player.getItemInHand() == 76)
			{
				HitBlox blox = new HitBlox(player, 100, 0.1);
				
				switch (settings.mode)
					{
					// SNIPE mode replaces the block you are aimed at
					 case SNIPE:
						while ((blox.getNextBlock() != null) && (blox.getCurBlock().getType() == 0));
						if (blox.getCurBlock() != null)
							blox.setCurBlock(settings.type);
						break;
					// STICK mode replaces the block that protrudes from the face of the block you are aimed at
					 case STICK:
						while ((blox.getNextBlock() != null) && (blox.getCurBlock().getType() == 0));
						if (blox.getCurBlock() != null)
							blox.setLastBlock(settings.type);
						break;
					// LASER replaces all air along the path to the block you are aimed at
					// LASER replaces all non-air in a line behind the block you are aimed at until it hits air or range
					 case LASER:
						if (settings.type == 0)
						{
							//while not at limit AND either current is solid or last is air
							while ((blox.getNextBlock() != null) && ((blox.getCurBlock().getType() != 0) || ((blox.getLastBlock().getType() == 0))))
							{
								blox.setLastBlock(settings.type);
							}
						}
						else
						{
							while ((blox.getNextBlock() != null) && (blox.getCurBlock().getType() == 0))
							{
								blox.setLastBlock(settings.type);
							}
						}
						break;
					// BEAM is idenitcal to LASER, but zaps a 3x3x3 for each block that LASER would.
					 case BEAM:
						ArrayList<Block> blocks = new ArrayList<Block>();
						if (settings.type == 0)
						{
							//while not at limit AND either current is solid or last is air
							while ((blox.getNextBlock() != null) && ((blox.getCurBlock().getType() != 0) || ((blox.getLastBlock().getType() == 0))))
							{
								blocks.add(blox.getLastBlock());
							}
						}
						else
						{
							while ((blox.getNextBlock() != null) && (blox.getCurBlock().getType() == 0))
							{
								blocks.add(blox.getLastBlock());
							}
						}
						
						for (Block b : blocks)
						 for (int tx = -1; tx <= 1; tx++)
						  for (int ty = -1; ty <= 1; ty++)
						   for (int tz = -1; tz <= 1; tz++)
						    etc.getServer().setBlockAt(settings.type, b.getX() + tx, b.getZ() + ty, b.getZ() + tz);

						break;

					}
			}
			//SETTING ID USING REGULAR TORCHES
			else if (player.getItemInHand() == 50)
			{
				HitBlox blox = new HitBlox(player, 100, 0.1);
				
				while ((blox.getNextBlock() != null) && (blox.getCurBlock().getType() == 0));
				if (blox.getCurBlock() == null)
				{
					if (settings.mode == Mode.OFF)
					{
						settings.switchMode();
						settings.type = 0;
					}
					else if (settings.type == 0)
					{
						settings.switchMode();
					}
					else
					{
						settings.type = 0;
					}
				}
				else
				{
					if  (settings.mode == Mode.OFF)
					{
						settings.switchMode();
						settings.type = blox.getCurBlock().getType();
					}
					else if (settings.type == blox.getCurBlock().getType())
					{
						settings.switchMode();
					}
					else
					{
						settings.type = blox.getCurBlock().getType();
					}
				}
				
				player.sendMessage("SwissArmyTorch  --  Mode: " + settings.mode.name() + "  ID: " + settings.type);
			}
		}
	}
	
	//Stop from placing redstone torches when enabled
	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
	{
		if (player.canUseCommand("/swiss"))
		{
			SwissSettings settings = getSettings(player);
		    if ((settings.mode != Mode.OFF) && itemInHand == 76)
				return(true);
		}
		return(false);
	}
	
	//This class allows us to step through each block along the
	// line of sight of the player.
	class HitBlox
	{
		private Location player_loc;
		private double rot_x, rot_y, view_height;
		
		private double length, h_length, step;
		private int range;
		
		private double x_offset, y_offset, z_offset;
		private int    last_x,   last_y,   last_z;
		private int    target_x, target_y, target_z, target_type;

		
		
		public HitBlox(Player player, int in_range, double in_step)
		{
			player_loc = player.getLocation();
			range = in_range;
			step = in_step;
			length = 0;
			rot_x = (player_loc.rotX+90) % 360;
			rot_y = player_loc.rotY * -1;
			
		/*	if (player.isCrouching()) //Hopefully we can find this!
				view_height = 1.45;
			else */
				view_height = 1.65;
			
			target_x = (int) (player_loc.x);
			target_y = (int) (player_loc.y + view_height);
			target_z = (int) (player_loc.z);
			last_x = target_x;
		    last_y = target_y;
		    last_z = target_z;
		}
		
		public Block getNextBlock()
		{
			last_x = target_x;
		    last_y = target_y;
		    last_z = target_z;
			
			do
			{
				length += step;
				
				h_length = (length * Math.cos(Math.toRadians(rot_y)));
				y_offset = (length * Math.sin(Math.toRadians(rot_y)));
				x_offset = (h_length * Math.cos(Math.toRadians(rot_x)));
				z_offset = (h_length * Math.sin(Math.toRadians(rot_x)));
				
				target_x = (int) (x_offset + player_loc.x);
				target_y = (int) (y_offset + player_loc.y + view_height);
				target_z = (int) (z_offset + player_loc.z);
				
				
			} while ((length <= range) && ((target_x == last_x) && (target_y == last_y) && (target_z == last_z)));
			
			if(length > range)
				return null;
			
			return etc.getServer().getBlockAt(target_x, target_y, target_z);
		}
		
		public Block getCurBlock()
		{
			if(length > range)
				return null;
			else
				return etc.getServer().getBlockAt(target_x, target_y, target_z);
		}
		
		public void setCurBlock(int type)
		{
			if(length <= range)
				etc.getServer().setBlockAt(type, target_x, target_y, target_z);
		}
		
		public Block getLastBlock()
		{
			return etc.getServer().getBlockAt(last_x, last_y, last_z);
		}
		
		public void setLastBlock(int type)
		{
			etc.getServer().setBlockAt(type, last_x, last_y, last_z);
		}
	}
}
