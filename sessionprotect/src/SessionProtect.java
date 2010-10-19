/**
 * SessionProtect.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 */
public class SessionProtect extends SuperPlugin {
	public final SessionListener listener = new SessionListener();
	private boolean kickDoppelgangers;
	private boolean lastInvSlotDelete;

	public SessionProtect() { super("sessionProtect"); }

	public void reloadExtra() {
		kickDoppelgangers = config.getBoolean("kickDoppelgangers", true);
		lastInvSlotDelete = config.getBoolean("lastInvSlotDelete", true);	
	}

	public void initializeExtra() {
		if (kickDoppelgangers)
			etc.getLoader().addListener(PluginLoader.Hook.LOGINCHECK, listener, this, PluginListener.Priority.HIGH);
		if (lastInvSlotDelete)
			etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW);
	}

	private class SessionListener extends PluginListener {
		public String onLoginChecks(String user) {
			Player ghost = null;
			for (Player p : etc.getServer().getPlayerList()) {
				if (p.getName().equalsIgnoreCase(user)) {
					ghost = p;
					break;
				}
			}
			if (ghost != null) {
				String name = ghost.getName();
				ghost.kick("You logged-in somewhere else!");
				for (Player p : etc.getServer().getPlayerList()) {
					p.sendMessage(Colors.LightGray + name + " timed out...");
				}
			}
			return null;
		}

		public void onLogin(Player player) { 
			Inventory bag = player.getInventory();
			bag.removeItem(35);
		}
	}
}
