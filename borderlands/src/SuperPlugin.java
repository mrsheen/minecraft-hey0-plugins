import java.util.logging.Logger;



public abstract class SuperPlugin extends Plugin
{
	
  
  public final SuperPlugin.ReloadListener reloader = new SuperPlugin.ReloadListener();
  protected PropertiesFile config;
  protected final Logger log = Logger.getLogger("Minecraft");
  protected String name;

  public SuperPlugin(String name)
  {
    this.config = new PropertiesFile(name + ".txt");
    this.name = name;
    reloadConfig();
  }

  public void enableExtra()
  {
  }

  public void disableExtra()
  {
  }

  public void reloadExtra()
  {
  }

  public boolean extraCommand(Player player, String[] split)
  {
    return false;
  }
  public void initializeExtra() {
  }
  public void initialize() {
    etc.getLoader().addListener(PluginLoader.Hook.COMMAND, this.reloader, this, PluginListener.Priority.LOW);
    initializeExtra();
  }

  public void enable() {
    enableExtra();
    this.log.info(this.name + " was enabled.");
  }

  public void disable() {
    disableExtra();
    this.log.info(this.name + " was disabled.");
  }

  private void reloadConfig() {
    this.config.load();
    reloadExtra();
  }

  public void broadcast(String message)
  {
    for (Player p : etc.getServer().getPlayerList())
      p.sendMessage(message); 
  }
  
  private class ReloadListener extends PluginListener {
      private ReloadListener() {
    }

    public boolean onCommand(Player player, String[] split) {
      if ((player.canUseCommand("/reload")) && 
        (split[0].equalsIgnoreCase("/reload"))) {
        SuperPlugin.this.reloadConfig();
      }
      return SuperPlugin.this.extraCommand(player, split);
    }
  }
  
}