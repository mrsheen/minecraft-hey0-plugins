import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MagicCarpet extends Plugin
{
	private static Logger a = Logger.getLogger("Minecraft");
	private Hashtable carpets = new Hashtable();
	public MagicCarpet()
	{
	}
	public class CarpetFiber
	{
		public CarpetFiber(int x, int y, int z, int type)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.type = type;
		}
		int x,y,z,type = 0;
		boolean destroyed = false;
		boolean imadeit = false;
	}
	public class Carpet
	{
		public CarpetFiber[] fibers = {
			new CarpetFiber(2, 0, 2, 20),
			new CarpetFiber(2, 0, 1, 20),
			new CarpetFiber(2, 0, 0, 20),
			new CarpetFiber(2, 0, -1, 20),
			new CarpetFiber(2, 0, -2, 20),
			new CarpetFiber(1, 0, 2, 20),
			new CarpetFiber(1, 0, 1, 20),
			new CarpetFiber(1, 0, 0, 20),
			new CarpetFiber(1, 0, -1, 20),
			new CarpetFiber(1, 0, -2, 20),
			new CarpetFiber(0, 0, 2, 20),
			new CarpetFiber(0, 0, 1, 20),
			new CarpetFiber(0, 0, 0, 20),
			new CarpetFiber(0, 0, -1, 20),
			new CarpetFiber(0, 0, -2, 20),
			new CarpetFiber(-1, 0, 2, 20),
			new CarpetFiber(-1, 0, 1, 20),
			new CarpetFiber(-1, 0, 0, 20),
			new CarpetFiber(-1, 0, -1, 20),
			new CarpetFiber(-1, 0, -2, 20),
			new CarpetFiber(-2, 0, 2, 20),
			new CarpetFiber(-2, 0, 1, 20),
			new CarpetFiber(-2, 0, 0, 20),
			new CarpetFiber(-2, 0, -1, 20),
			new CarpetFiber(-2, 0, -2, 20)
			};
		Location currentLoc;
		public void removeCarpet() {
			if (currentLoc == null)
				return;
			for(int i = 0; i < fibers.length; i++)
			{
				if (fibers[i].imadeit) etc.getServer().setBlockAt(0, (int)currentLoc.x + fibers[i].x, (int)currentLoc.y - fibers[i].y, (int)currentLoc.z + fibers[i].z);
				fibers[i].imadeit = false;
			}
		}
		public void drawCarpet() {
			for(int i = 0; i < fibers.length; i++)
			{
				if (!fibers[i].destroyed && etc.getServer().getBlockAt((int)currentLoc.x + fibers[i].x, (int)currentLoc.y - fibers[i].y, (int)currentLoc.z + fibers[i].z).getType() == 0) {
					fibers[i].imadeit = true;
					etc.getServer().setBlockAt(fibers[i].type, (int)currentLoc.x + fibers[i].x, (int)currentLoc.y - fibers[i].y, (int)currentLoc.z + fibers[i].z);
				} else {
					fibers[i].imadeit = false;
				}
			}
		}
		public CarpetFiber getCenterFiber()
		{
			for(int i = 0; i < fibers.length; i++)
			{
				if(fibers[i].x == 0 && fibers[i].y == 0 && fibers[i].z == 0)
					return fibers[i];
			}
			return null;
		}
	}
	public void enable()
	{
	}
	
	public void disable()
	{
	}

	public boolean onCommand(Player player, String[] split)
	{
		try {
			if (split[0].equalsIgnoreCase("/magiccarpet") && player.canUseCommand("/magiccarpet")) {
				Carpet carpet = (Carpet)carpets.get(player.getName());
				if (carpet == null)
				{
					player.sendMessage("You are now on a carpet! OH SHIT!");
					carpets.put(player.getName(), new Carpet());
					return true;
				}
				else
				{
					player.sendMessage("Poof! The carpet disappears");
					carpets.remove(player.getName());
					Location from = player.getLocation();
					carpet.removeCarpet();
				}
				return true;
			}
		} catch (Exception ex) {
			a.log(Level.SEVERE, "Exception in command handler (Report this to chrisinajar):", ex);
			return false;
		}
		return false;
	}
	
	public boolean onBlockDestroy(Player player, Block block) {
		Carpet carpet = (Carpet)carpets.get(player.getName());
		if (carpet == null)
			return false;
		if(!carpet.getCenterFiber().imadeit)
			return false;
		Location from = player.getLocation();
		if((int)from.x != block.getX() || ((int)from.y -1) != block.getY() || (int)from.z != block.getZ())
			return false;
			
		carpet.removeCarpet();
		
		return true;
	}

	public void onPlayerMove(Player player, Location from, Location to)
	{
		Carpet carpet = (Carpet)carpets.get(player.getName());
		if (carpet == null)
			return;
		carpet.removeCarpet();
		to.y = to.y-1;
		if(player.getPitch() == 90)
			to.y = to.y-1;
		carpet.currentLoc = to;
		carpet.drawCarpet();

	}
	public void onDisconnect(Player player) {
		Carpet carpet = (Carpet)carpets.get(player.getName());
		if (carpet == null)
			return;
		carpets.remove(player.getName());
		carpet.removeCarpet();
	}
}

