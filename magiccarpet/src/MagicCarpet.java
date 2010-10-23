import java.util.Hashtable;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MagicCarpet extends Plugin
{
	private static Logger a = Logger.getLogger("Minecraft");
	private Hashtable carpets = new Hashtable();
	private Listener l = new Listener(this);

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
				if (fibers[i].imadeit) etc.getServer().setBlockAt(0, (int)Math.floor(currentLoc.x) + fibers[i].x, (int)Math.floor(currentLoc.y) - fibers[i].y, (int)Math.floor(currentLoc.z) + fibers[i].z);
				fibers[i].imadeit = false;
			}
		}
		public void drawCarpet() {
			for(int i = 0; i < fibers.length; i++)
			{
				if (!fibers[i].destroyed && etc.getServer().getBlockAt((int)Math.floor(currentLoc.x) + fibers[i].x, (int)Math.floor(currentLoc.y) - fibers[i].y, (int)Math.floor(currentLoc.z) + fibers[i].z).getType() == 0) {
					fibers[i].imadeit = true;
					int blocktypeunder = etc.getServer().getBlockIdAt((int)Math.floor(currentLoc.x) + fibers[i].x, (int)Math.floor(currentLoc.y) - fibers[i].y-1, (int)Math.floor(currentLoc.z) + fibers[i].z);
					if (blocktypeunder!=67 && blocktypeunder!=53) {
						etc.getServer().setBlockAt(fibers[i].type, (int)Math.floor(currentLoc.x) + fibers[i].x, (int)Math.floor(currentLoc.y) - fibers[i].y, (int)Math.floor(currentLoc.z) + fibers[i].z);
					}
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
		etc.getInstance().addCommand("/magiccarpet", "Take yourself wonder by wonder");
	}
	
	public void disable()
	{
		etc.getInstance().removeCommand("/magiccarpet");
		Enumeration e = carpets.elements();
		//iterate through Hashtable keys Enumeration
		while(e.hasMoreElements()) {
			Carpet c = (Carpet)e.nextElement();
			c.removeCarpet();
		}
		carpets.clear();
	}
	
	public void initialize()
	{
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, l, this, PluginListener.Priority.MEDIUM);
	}


	public class Listener extends PluginListener {
	private MagicCarpet plugin;
	Listener(MagicCarpet pl)
	{
		plugin = pl;
	}
	public boolean onCommand(Player player, String[] split)
	{
		try {
			if ((split[0].equalsIgnoreCase("/magiccarpet")||split[0].equalsIgnoreCase("/mc")) && player.canUseCommand("/magiccarpet")) {
				Carpet carpet = (Carpet)carpets.get(player.getName());
				if (carpet == null)
				{
					if(player.canUseCommand("/adult_language"))
						player.sendMessage("You are now on a carpet! OH SHIT!");
					else
						player.sendMessage("My goodness dear chap, it would appear you are on some sort of magical carpet capable of arial travel!");
					carpets.put(player.getName(), new Carpet());
					return true;
				}
				else
				{
					if(player.canUseCommand("/adult_language"))
						player.sendMessage("Hot damn! The carpet disappears!");
					else
						player.sendMessage("... And as quickly as it arrived, the magically imbued decorative rug leaves you.");
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
	
	public void onPlayerMove(Player player, Location from, Location to)
	{
		Carpet carpet = (Carpet)carpets.get(player.getName());
		if (carpet == null)
			return;
		carpet.removeCarpet();
		to.y = to.y-1;
		if(player.getPitch() >= 80)
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
}
