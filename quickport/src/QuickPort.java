import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

/*
 * QUICKPORT v0.2.0
 */

public class QuickPort extends Plugin
{
    private QuickPortListener listener = new QuickPortListener();
    private Hashtable playerSettings = new Hashtable();
    private enum Mode { SELF, TUNNEL, SELECT, PLAYER };

    public void enable()
    {
    }

    public void disable()
    {
    }
    
    public void initialize()
    {
        etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
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
    
    public class QuickPortSettings
    {
        public Mode mode = Mode.SELF;
        public Player targetPlayer;
        public boolean firstSelect;
        
        public boolean modeIs(Mode in_mode)
        {
            
            return (mode == in_mode);
        }
        
        public void setMode(Mode in_mode)
        {
            mode = in_mode;
        }
        
        public void switchMode(Player player)
        {
            switch (mode)
            {
                case SELF:
                    mode = Mode.TUNNEL;
                    player.sendMessage("QuickPort Mode: " + Colors.LightGreen + "Tunnel");
                    break;
                case TUNNEL:
                    if (player.canUseCommand("/QuickPortOther") && etc.getServer().getPlayerList().size() > 1)
                    {
                        mode = Mode.SELECT;
                        firstSelect = true;
                        player.sendMessage("QuickPort Mode: " + Colors.LightPurple + "Select Player");
                    }
                    else
                    {
                        mode = Mode.SELF;
                        player.sendMessage("QuickPort Mode: " + Colors.LightGray + "Normal");
                    }
                    break;
                case SELECT:
                    if (firstSelect || !player.canUseCommand("/QuickPortOther")) //neurotic
                    {
                        mode = Mode.SELF;
                        player.sendMessage("QuickPort Mode: " + Colors.LightGray + "Normal");
                    }
                    else
                    {
                        mode = Mode.PLAYER;
                        player.sendMessage("QuickPort Mode: " + Colors.LightBlue + " Target (" + Colors.White + targetPlayer.getName() + Colors.LightBlue + ")");
                    }
                    break;
                default:
                    mode = Mode.SELF;
                    player.sendMessage("QuickPort Mode: " + Colors.LightGray + "Normal");
                    break;
            }
        }
        
        public void selectPlayer(Player player)
        {
            if (firstSelect && targetPlayer != null && etc.getServer().getPlayerList().contains(targetPlayer))
            {
                firstSelect = false;
                player.sendMessage("QuickPort Target: " + targetPlayer.getName());
            }
            else
            {
                List<Player> players = etc.getServer().getPlayerList();
                int index = -1;
                
                if (targetPlayer != null)
                    index = players.indexOf(targetPlayer);
                    
                //Loop around the top
                if (index + 1 >= players.size())
                    targetPlayer = players.get(0);
                else
                    targetPlayer = players.get(index + 1);

                //Skip self
                if (targetPlayer == player)
                {
                    index = players.indexOf(targetPlayer);
                    if (index + 1 >= players.size())
                        targetPlayer = players.get(0);
                    else
                        targetPlayer = players.get(index + 1);
                }
				player.sendMessage("QuickPort Target: " + targetPlayer.getName());
            }
        }
        
        
    }
    
    public class QuickPortListener extends PluginListener
    {

        public void onArmSwing(Player player) {
            if ((player.canUseCommand("/QuickPort") || player.canUseCommand("/QuickPortNormal")) && (player.getItemInHand() == 345))
            {
                QuickPortSettings settings = getSettings(player);

                Location playerLoc;
                if (settings.modeIs(Mode.PLAYER))
					playerLoc = settings.targetPlayer.getLocation();
                else
                    playerLoc = player.getLocation();
                
                if (settings.modeIs(Mode.SELF) || settings.modeIs(Mode.PLAYER))
                {
                    HitBlox blox = new HitBlox(player, 300, 0.3);
                    if (blox.getTargetBlock() != null)
                    {
                        for(int i = 0; i<100; i++)
                        {
                            int cur = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i, blox.getCurBlock().getZ()).getType();
                            int above = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i + 1, blox.getCurBlock().getZ()).getType();
                            if (cur == 0 && above == 0)
                            {
                                playerLoc.x = blox.getCurBlock().getX() + .5;
                                playerLoc.y = blox.getCurBlock().getY() + i;
                                playerLoc.z = blox.getCurBlock().getZ() + .5;
                                
                                if (settings.modeIs(Mode.PLAYER))
									settings.targetPlayer.teleportTo(playerLoc);
                                else
                                    player.teleportTo(playerLoc);
                                    
                                settings.setMode(Mode.SELF);
                                i = 100;
                            }
                        }
                    }
                }
                else if (settings.modeIs(Mode.TUNNEL))
                {
                    HitBlox blox = new HitBlox(player, 300, 0.3);
                    while ((blox.getNextBlock() != null) && ((blox.getCurBlock().getType() != 0) || ((blox.getLastBlock().getType() == 0))));
                    if (blox.getCurBlock() != null)
                    {
                        for(int i = 0; i > -100; i--)
                        {
                            int below = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i - 1, blox.getCurBlock().getZ()).getType();
                            int cur = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i, blox.getCurBlock().getZ()).getType();
                            int above = etc.getServer().getBlockAt(blox.getCurBlock().getX(), blox.getCurBlock().getY() + i + 1, blox.getCurBlock().getZ()).getType();
                            if (below != 0 && cur == 0 && above == 0)
                            {
                                playerLoc.x = blox.getCurBlock().getX() + .5;
                                playerLoc.y = blox.getCurBlock().getY() + i;
                                playerLoc.z = blox.getCurBlock().getZ() + .5;

                                player.teleportTo(playerLoc);

                                settings.targetPlayer = null;
                                i = -100;
                            }
                        }
                    }
                }
                else if (settings.modeIs(Mode.SELECT))
                {
                    settings.selectPlayer(player);
                }
            }
        }
        
        //Toggles through players on server as targets and tunnel mode
        public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand)
        {
            if ((player.canUseCommand("/QuickPort")) && (itemInHand == 345))
            {
                QuickPortSettings settings = getSettings(player);
                settings.switchMode(player);    
            }
            return(false);
        }

    }

}