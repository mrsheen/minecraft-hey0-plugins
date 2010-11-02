import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public class InvisibilityMode extends Plugin
{
  public void initialize()
  {
    etc.getLoader().addListener(PluginLoader.Hook.COMMAND, new InvisibilityMode.InvisibilityModeListener(), this, PluginListener.Priority.MEDIUM);
  }

  public void enable() {
    gq.invizPlayers = new HashSet();
  }

  public void disable() {
    gq.invizPlayers = null;
  }
  public class InvisibilityModeListener extends PluginListener {
    public InvisibilityModeListener() {
    }
    public boolean onCommand(Player paramPlayer, String[] paramArrayOfString) {
      if ((paramArrayOfString[0].equalsIgnoreCase("/inviz")) && (paramPlayer.canUseCommand("/inviz"))) {
        if (!gq.invizPlayers.contains(paramPlayer.getName().toLowerCase())) {
          gq.invizPlayers.add(paramPlayer.getName().toLowerCase());

          gq localgq = (gq)etc.getServer().getMCServer().k.b.a(paramPlayer.getUser().g);
          if (localgq != null) {
            for (eo localeo : localgq.k) {
				//System.out.println("setting new packet handler for: "+localeo.getPlayer().getName());
              localeo.a.b(new de(localgq.a.g));
            }
			
			
            localgq.k.clear();
          }
          paramPlayer.sendMessage("You are now invisible!");
        } else {
          gq.invizPlayers.remove(paramPlayer.getName().toLowerCase());
		  //System.out.println(paramPlayer.getUser().g);
		  
		  gq localgq = (gq)etc.getServer().getMCServer().k.b.a(paramPlayer.getUser().g);
          if (localgq != null) {
			//System.out.println("non-null gq");
			
			for (int i = 0; i < etc.getServer().getMCServer().f.b.size(); ++i) {
                eo localeo = (eo) etc.getServer().getMCServer().f.b.get(i);
				if (!localeo.getPlayer().getName().equalsIgnoreCase(paramPlayer.getName().toLowerCase())) {
					//System.out.println("added to player: "+localeo.getPlayer().getName());
					localgq.a(localeo);
				}
                
            }
		  
            //localgq.k.clear();
          }
		  
          paramPlayer.sendMessage("You are no longer invisible!");
        }
        return true;
      }
      return false;
    }
  }
}