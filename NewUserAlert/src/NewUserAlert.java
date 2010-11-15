import java.util.logging.Logger;

//@author MonkeyCrumpets

public class NewUserAlert extends Plugin  {
	private Listener l = new Listener(this);
	protected static final Logger log = Logger.getLogger("Minecraft");
	private String name = "NewUserAlert";
	private String version = "1.0";
	
		public void enable() {
	}
	
	public void disable() {
	}

	public void initialize() {
		log.info(name + " " + version + " initialized");
		etc.getLoader().addListener( PluginLoader.Hook.BLOCK_DESTROYED, l, this, PluginListener.Priority.MEDIUM);
	}
	
	public class Listener extends PluginListener {
		NewUserAlert p;
		public Listener(NewUserAlert plugin) {
			p = plugin;
		}
		
		public boolean onBlockDestroy(Player player, Block block) {
			if (player.canBuild()==false) {
				if (block.getStatus()==3) {
					player.sendMessage(Colors.LightBlue + "You do not have build access, please post your name at");
					player.sendMessage(Colors.LightBlue + "reddit.com/dowvk then ask an admin to give you access.");
				}
			}
			return false;
		}
	}
}
