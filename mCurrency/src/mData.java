/**
 * mCurrency plugin 1.0 
 * @author The Maniac
 */

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class mData implements Serializable
{
    // The logger
    protected static final Logger log = Logger.getLogger("Minecraft");
    
    private Hashtable   balances;       // Player balances
    private Hashtable   chests;         // Player chests (not used in mCurrency)
    private String      file;           // Location of data file
    private int         startBalance;   // Starting balance for players
        
    /**
     * Constructor
     *
     * @param fileName          Location of data file
     * @param startingBalance   Starting balance for new players
     */
    public mData(String fileName, int startingBalance) throws IOException, ClassNotFoundException
    {    
        mData data = null;
        file = fileName;
        startBalance = startingBalance;
        
        try
        {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream inStream = new ObjectInputStream(fis);        
            data = (mData)inStream.readObject();
            inStream.close();
            fis.close();
        }
        catch (Exception e)
        {
            data = null;
        }
        
        if (data == null)
        {
            balances = new Hashtable();
            chests = new Hashtable();
            write();
        }
        else
        {
            balances = data.balances;
            chests = data.chests;
        }        
    }
    
    /**
     * Writes data file to disc
     */
    public void write()
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream outStream = new ObjectOutputStream(fos);
            outStream.writeObject(this);
            outStream.close();
            fos.close();
        }
        catch (Exception e)
        {
            log.severe("[mData] Critical error while writing data: ");
            e.printStackTrace();
        }            
    }
    
    /**
     * Returns players balance
     *
     * @param player    Player to look up
     */
    public int getBalance(String player)
    {        
        if (balances.get(player) == null)
        {
            balances.put(player, startBalance);
            return startBalance;
        }
        else
        {
            return (Integer)balances.get(player);
        }
    }
    
    /**
     * Sets a players balance
     *
     * @param player    Player to set
     * @param balance   Player's new balance
     */
    public void setBalance(String player, int balance)
    {
        balances.put(player, balance);
    }
    
    /**
     * Returns a player's chest
     *
     * @param player    Name of player to look up
     */
    public String getChest(String player)
    {
        if (chests.get(player) != null)
        {
            return (String)chests.get(player);
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Sets a chest's owner
     *
     * @param player        Player to look up
     * @param chestBlock    Block of chest to attach to player
     */
    public void setChestOwner(String player, Block chestBlock)
    {   
        chests.put(player, getChestName(chestBlock));
    }
        
    /**
     * Returns name of chest (string created from block position)
     *
     * @param chestBlock      
     */
    public String getChestName(Block chestBlock)
    {
        return Integer.toString(chestBlock.getX()) + Integer.toString(chestBlock.getY())
            + Integer.toString(chestBlock.getZ());
    }
    
    /**
     * Returns true if player has access to chest
     *
     * @param player    
     * @param chestBlock
     */
    public boolean hasChestAccess(String player, Block chestBlock)
    {
        log.info("Player: " + player + ", Chestblock:" + chestBlock);
        String chestName = getChestName(chestBlock);
        return !chests.contains(chestName) || getChest(player).equals(chestName);
    }
}