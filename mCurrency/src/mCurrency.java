/**
 * mCurrency plugin 1.0 
 * @author The Maniac
 */

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class mCurrency extends Plugin 
{
    // The logger
    protected static final Logger log = Logger.getLogger("Minecraft");
    
    private PropertiesFile  props;              // Server properties file
    
    private mData    data;                      // Data storage
    private LinkedList<String> rankedList;      // Player list, sorted richest->poorest
    
    private int moneyTick = 0;                  // Money over time amount
    private int moneyTickInterval = 0;          // Frequency of money tick
    private Timer moneyTicker;                  // Timer class that handles money tick
    
    private String moneyName;                   // Name of money
    
    /**
     * Constructor
     */
    public mCurrency() 
    {
        props = null;
        data = null;
        rankedList = null;
        moneyTicker = null;
    }
    
    /**
     * Enable override.
     */
    public void enable() 
    {
        if (load()) 
        {
            log.info("[mCurrency] Plugin Enabled.");
        } 
        else 
        {
            log.info("[mCurrency] Plugin failed to load.");
        }
        
        etc.getInstance().addCommand("/deposit", "[amount] <player> - Adds money to account");
        etc.getInstance().addCommand("/debit", "[amount] <player> - Subtracts money from account");
        etc.getInstance().addCommand("/pay", "[amount] [player] - Gives money to player");
        etc.getInstance().addCommand("/money", "- Displays current balance");
        etc.getInstance().addCommand("/rank", "- Displays rank");
        etc.getInstance().addCommand("/top5", "- Displays 5 richest people on the server");        
    }    
    
    /**
     * Disable override.
     */
    public void disable() 
    {
        etc.getInstance().removeCommand("/deposit");
        etc.getInstance().removeCommand("/debit");
        etc.getInstance().removeCommand("/pay");
        etc.getInstance().removeCommand("/money");
        etc.getInstance().removeCommand("/rank");
        etc.getInstance().removeCommand("/top5");
        
        if (moneyTicker != null)
            moneyTicker.cancel();
        
        log.info("[mCurrency] Plugin Disabled.");
    }
    
    /**
     * OnLogin override.
     */
    public void onLogin(Player player) 
    {
        updateState(player, true);
    }
        
    /**
     * OnCommand override.
     */
    public boolean onCommand(Player player, String[] split)
    {        
        if (!isEnabled())
            return false;
        if (!player.canUseCommand(split[0]))
            return false;
            
        if (split[0].equalsIgnoreCase("/deposit"))
        {
            if (split.length < 2 || split.length > 3)
            {
                player.sendMessage(Colors.Rose + "usage: /deposit [amount] <player>");
                return true;
            }
            else
            {
                // Parse recipient
                Player recipient = (split.length == 2 ? player : getPlayer(split[2]));          
                if (recipient == null)
                {
                    player.sendMessage(Colors.Rose + "Player not found: " + split[2]);
                    return true;
                }
                
                // Parse amount
                int amount = 0;
                try
                {  
                    amount = Integer.parseInt(split[1]);
                    if (amount < 1)
                    {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    player.sendMessage(Colors.Rose + "Invalid amount: " + amount);
                    return true;
                }
                
                // Call handler
                deposit(player, recipient, Integer.parseInt(split[1]), true);
            }
            
            return true;
        }
        else if (split[0].equalsIgnoreCase("/debit"))
        {
            if (split.length < 2 || split.length > 3)
            {
                player.sendMessage(Colors.Rose + "usage: /debit [amount] <player>");
                return true;
            }
            else
            {
                // Parse recipient
                Player recipient = (split.length == 2 ? player : getPlayer(split[2]));          
                if (recipient == null)
                {
                    player.sendMessage(Colors.Rose + "Player not found: " + split[2]);
                    return true;
                }
                
                // Parse amount
                int amount = 0;
                try
                {  
                    amount = Integer.parseInt(split[1]);
                    if (amount < 1)
                    {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    player.sendMessage(Colors.Rose + "Invalid amount: " + amount);
                    return true;
                }
                
                // Call handler
                debit(player, recipient, Integer.parseInt(split[1]), true);
            }
            
            return true;
        }
        else if (split[0].equalsIgnoreCase("/pay"))
        {
            if (split.length != 3)
            {
                player.sendMessage(Colors.Rose + "usage: /pay [amount] [player]");
            }
            else
            {         
                // Parse recipient
                Player to = getPlayer(split[2]);                
                if (to == null)
                {
                    player.sendMessage(Colors.Rose + "Player not found: " + split[2]);
                    return true;
                }
                       
                // Parse amount
                int amount = 0;
                try
                {  
                    amount = Integer.parseInt(split[1]);
                    if (amount < 1)
                    {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    player.sendMessage(Colors.Rose + "Invalid amount: " + amount);
                    return true;
                }
                
                // Call handler
                pay(player, to, amount);   
            }       
                
            return true;   
        }
        else if (split[0].equalsIgnoreCase("/money"))
        {
            showBalance(player);                
            return true;
        }
        else if (split[0].equalsIgnoreCase("/rank"))  
        {
            rank(player);
            return true;
        }
        else if (split[0].equalsIgnoreCase("/top5")) 
        {
            top5(player);
            return true;
        }
        
        return false;
    } 
    
    // --------------End of overrides-----------------------------------------//
    
    /**
     * Adds player to ranked list if not already there and writes currency
     * data to disc if write is true
     *
     * @param player    Player to add to list
     * @param write     True if all data is written to disc
     */
    private void updateState(Player player, boolean write)
    {
        String name = player.getName();
        
        // Remove player first, then reinsert to insure list stays sorted.
        rankedList.remove(name);
        insertIntoRankedList(name);
        
        if (write)
            data.write();
    }
    
    /**
     * Loads data from properties file, currency data file, and also starts
     * money tick Timer.
     */
    private boolean load()
    {
        // Load data from properties file
        props = new PropertiesFile("mCurrency.properties");
        int startingBalance = props.getInt("startingBalance", 0);
        String dataFile = props.getString("dataFile", "mCurrency.data");
        moneyTick = props.getInt("moneyTick", 0);
        moneyTickInterval = 1000 * props.getInt("moneyTickInterval", 0);
        moneyName = " " + props.getString("moneyName", "coin");
        
        // Try loading/creating data file
        try
        {
            data = new mData(dataFile, startingBalance);
        }
        catch (Exception e)
        {
            log.severe("[mCurrency] Critical error while loading data:");
            e.printStackTrace();
        }
        
        // Initialize data sets
        rankedList = new LinkedList();
        
        // Start money tick
        if (moneyTickInterval > 0)
        {
            moneyTicker = new Timer();
            moneyTicker.schedule(new MoneyTickerTask(), 0, moneyTickInterval);
        }
        
        return true;
    }

    /**
     * Timer task for money ticker
     */
    private class MoneyTickerTask extends TimerTask
    {
        /**
         * The task
         */
        public void run()
        {
            List<Player> players = etc.getInstance().getServer().getPlayerList();
            for (Player player : players)
            {
                deposit(null, player, moneyTick, false);
            }
        }
    }
    
    /**
     * Deposits money into player's account
     * 
     * @param player    Player to send confirmation message to
     * @param to        Player receiving money
     * @param amount    Amount of money to send
     * @param verbose   True if players are messaged about the transfer    
     */
    private void deposit(Player player, Player to, int amount, boolean verbose)
    {        
        String toName = to.getName();
        
        int balance = data.getBalance(toName);
        balance += amount;
        data.setBalance(toName, balance);
        
        if (verbose)
        {
            to.sendMessage(Colors.Green + "You received " + amount + moneyName);
            showBalance(to);
            
            if (player != null)
            {
                player.sendMessage(Colors.Green + amount + moneyName + " deposited into " 
                    + to.getName() + "'s account");
            }
        }
        
        updateState(to, true);
    }
    
    /**
     * Removes money from players account
     * 
     * @param player    Player to send confirmation message to
     * @param to        Player losing money
     * @param amount    Amount of money to send
     * @param verbose   True if players are messaged about the transfer    
     */
    private void debit(Player player, Player from, int amount, boolean verbose)
    {        
        String fromName = from.getName();
        
        int balance = data.getBalance(fromName);
        
        if (amount > balance)
            amount = balance;
            
        balance -= amount;            
        data.setBalance(fromName, balance);
        
        if (verbose)
        {
            from.sendMessage(Colors.Green + amount + moneyName + " was deducted from your account.");
            showBalance(from);
            
            if (player != null)
            {
                player.sendMessage(Colors.Green + amount + moneyName + " removed from " 
                    + from.getName() + "'s account");
            }
        }
        
        updateState(from, true);
    }
    
    /**
     * Transfers money from one player's account to another
     *
     * @param from      Player to transfer money from
     * @param to        Player to transfer money to
     * @param amount    Amount of money to transfer    
     */
    private void pay(Player from, Player to, int amount)
    {
        String fromName = from.getName();
        String toName = to.getName();
        int fromBalance = data.getBalance(fromName);
        int toBalance = data.getBalance(toName);
        
        if (from.getName().equals(to.getName()))
        {
            from.sendMessage(Colors.Rose + "You cannot send yourself money");
        }
        else if (amount > fromBalance)
        {
            from.sendMessage(Colors.Rose + "You do not have enough money.");
        }
        else
        {
            fromBalance -= amount;
            data.setBalance(fromName, fromBalance);
            
            toBalance += amount;
            data.setBalance(toName, toBalance);
            
            from.sendMessage(Colors.Green + "You have sent " + amount + moneyName
                + " to " + toName);
            
            to.sendMessage(Colors.Green + fromName + " has sent you " + amount + moneyName);            
            
            showBalance(from);
            showBalance(to);
            
            updateState(from, false);
            updateState(to, true);
        }
    }

    /**
     * Displays balance to player
     *
     * @param player    Player to display balance to
     */
    private void showBalance(Player player)
    {
        int balance = data.getBalance(player.getName());
        player.sendMessage(Colors.Green + "Balance: " + balance);
    }
    
    /**
     * Displays rank to player
     *
     * @param player    Player to display rank to    
     */
    private void rank(Player player)
    {        
        if (!rankedList.contains(player.getName()))
        {
            insertIntoRankedList(player.getName());
        }
        
        int playerRank = rankedList.indexOf(player.getName()) + 1;
        
        player.sendMessage(Colors.Green + "Your rank is " + playerRank);
    }
    
    /**
     * Displays top-5 list to player
     *
     * @param player    Player to display list to    
     */
    private void top5(Player player)
    {       
        player.sendMessage(Colors.Green + "Top 5 Richest People:");
        for (int i = 0; i < 5 && i < rankedList.size(); i++)
        {
            String name = rankedList.get(i);
            int rank = i + 1;
            player.sendMessage(Colors.Green + "   " + rank + ". " + name + " - " + data.getBalance(name));
        }
    }
    
    /**
     * Returns player object given a player name
     *
     * @param playerName    Name of player to return
     */
    private Player getPlayer(String playerName)
    {
        return etc.getInstance().getServer().getPlayer(playerName);
    }
       
    /**
     * Inserts player name into ranked list, in the correct spot
     *
     * @param name  Name to insert
     */
    private void insertIntoRankedList(String name)
    {
        int bal = data.getBalance(name);
        int index = 0;
        for (String curr : rankedList)
        {
            if (bal > data.getBalance(curr))
                break;
            
            index++;
        }
        
        rankedList.add(index, name);
    }
}