import java.util.Hashtable;
import java.util.List;

public class QuickPort extends Plugin
{
	private Hashtable playerSettings = new Hashtable();
	
	private class QuickPortSettings
	{
		public Player targetPlayer;
	}
	
	//Returns a QuickPortSettings for the player, making a new one if it has to
	public QuickPortSettings getSettings(Player player)
	{
		QuickPortSettings settings = (QuickPortSettings)playerSettings.get(player.getName());
		if (settings == null)
		{
			playerSettings.put(player.getName(), new QuickPortSettings());
			settings = (QuickPortSettings)playerSettings.get(player.getName());
		}

		return(settings);
	}
	
	public void enable()
	{
	}

	public void disable()
	{
	}
	
	//Hooking on arm animation to fire torch
	public void onArmSwing(Player player)
	{
		if ((player.canUseCommand("/QuickPort")) && (player.getItemInHand() == 345))
		{
			QuickPortSettings settings = getSettings(player);
			
			Location playerLoc;
			if (settings.targetPlayer == null)
				playerLoc = player.getLocation();
			else
				playerLoc = settings.targetPlayer.getLocation();
			
			HitBlox blox = new HitBlox(player, 300, 0.3);
			while ((blox.getNextBlock() != null) && (blox.getCurBlock().getType() == 0));
				if (blox.getCurBlock() != null)
				{
						for(int i = 0; i<100; i++)
						{
							int cur = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i, blox.getCurBlock().getZ()).getType();
							int above = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i + 1, blox.getCurBlock().getZ()).getType();
							if (cur == 0 && above == 0)
							{
								playerLoc.x = blox.getCurBlock().getX();
								playerLoc.y = blox.getCurBlock().getY() + i;
								playerLoc.z = blox.getCurBlock().getZ();
								
								if (settings.targetPlayer == null)
									player.teleportTo(playerLoc);
								else
									settings.targetPlayer.teleportTo(playerLoc);
									
								settings.targetPlayer = null;
								i = 100;
							}
						}
				}

		}
	}
	

	public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
	{
		if ((player.canUseCommand("/QuickPort")) && (itemInHand == 345))
		{
			QuickPortSettings settings = getSettings(player);
			List<Player> players = etc.getServer().getPlayerList();
			int index = -1;
			
			if (settings.targetPlayer == null)
				index = -1;
			else
				index = players.indexOf(settings.targetPlayer);
				
			if ((index + 1 >= players.size()) || (players.get(index + 1) == player && index + 2 >= players.size()))
				settings.targetPlayer = null;
			else if (players.get(index + 1) == player && index + 2 < players.size())
				settings.targetPlayer = players.get(index + 2);
			else
				settings.targetPlayer = players.get(index + 1);

			if (settings.targetPlayer == null)
				player.sendMessage("QuickPort Target: [Self]");
			else
				player.sendMessage("QuickPort Target: " + settings.targetPlayer.getName());
					
					
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
