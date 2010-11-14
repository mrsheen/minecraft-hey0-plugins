import java.util.logging.Logger;

/**
*
* @author Nijiko
*/
public class Template extends Plugin  {
	private Listener l = new Listener(this);
	protected static final Logger log = Logger.getLogger("Minecraft");
	private String name = "MyPlugin";
	private String version = "1.0";

	public void enable() {
	}
	
	public void disable() {
	}

	public void initialize() {
		log.info(name + " " + version + " initialized");
		// Uncomment as needed.
		//etc.getLoader().addListener( PluginLoader.Hook.ARM_SWING, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.BLOCK_CREATED, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.BLOCK_DESTROYED, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.CHAT, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.COMMAND, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.COMPLEX_BLOCK_CHANGE, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.COMPLEX_BLOCK_SEND, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.DISCONNECT, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.INVENTORY_CHANGE, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.IPBAN, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.KICK, l, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener( PluginLoader.Hook.LOGIN, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.LOGINCHECK, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.NUM_HOOKS, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.PLAYER_MOVE, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.SERVERCOMMAND, l, this, PluginListener.Priority.MEDIUM);
		//etc.getLoader().addListener( PluginLoader.Hook.TELEPORT, l, this, PluginListener.Priority.MEDIUM);
	}

	// Sends a message to all players!
	public void broadcast(String message) {
		for (Player p : etc.getServer().getPlayerList()) {
			p.sendMessage(message);
		}
	}
	
	public class Listener extends PluginListener {
		Template p;
		
		// This controls the accessability of functions / variables from the main class.
		public Listener(Template plugin) {
			p = plugin;
		}
		
		// remove the /* and */ from any function you want to use
		// make sure you add them to the listener above as well!
		
		/*
		public void onPlayerMove(Player player, Location from, Location to) {
		}
		*/

		/*
		public boolean onTeleport(Player player, Location from, Location to) {
			return false;
		}
		*/

		/*
		public String onLoginChecks(String user) {
			return null;
		}
		*/

		public void onLogin(Player player) {
			// Player Message
			player.sendMessage(Colors.Yellow + "Currently running plugin: " + p.name + " v" + p.version + "!");
			
			// Global Message
			p.broadcast(Colors.Green + player.getName() + " has joined the server! Wooo~");
		}

		/*
		public void onDisconnect(Player player) {
		}
		*/

		/*
		public boolean onChat(Player player, String message) {
			return false;
		}
		*/

		/*
		public boolean onCommand(Player player, String[] split) {
			return false;
		}
		*/

		/*
		public boolean onConsoleCommand(String[] split) {
			return false;
		}
		*/

		/*
		public void onBan(Player mod, Player player, String reason) {
		}
		*/

		/*
		public void onIpBan(Player mod, Player player, String reason) {
		}
		*/

		/*
		public void onKick(Player mod, Player player, String reason) {
		}
		*/

		/*
		public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
			return false;
		}
		*/

		/*
		public boolean onBlockDestroy(Player player, Block block) {
			return false;
		}
		*/

		/*
		public void onArmSwing(Player player) {
		}
		*/

		/*
		public boolean onInventoryChange(Player player) {
			return false;
		}
		*/

		/*
		public boolean onComplexBlockChange(Player player, ComplexBlock block) {
			return false;
		}
		*/
		
		/*
		public boolean onSendComplexBlock(Player player, ComplexBlock block) {
			return false;
		}
		*/
	}
}