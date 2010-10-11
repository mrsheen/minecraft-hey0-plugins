import java.util.logging.*;
import java.util.Date;

public class StatsLogRecord extends LogRecord {

	public String table;

	public String key, playerName;
	public Date timestamp;
	public String[] command;
	public String chatMessage, reason;
	
	public Block blockDestroyed;
	
	public Block blockPlaced, blockClicked;
	
	public int itemInHand;
	
	public double x,y,z,distance;
	
	public Location from, to;

	public StatsLogRecord() {
		this(Level.INFO, "unknown");
	}
	
	public StatsLogRecord(Level level, String msg)
	{
		super(level, msg);
	}

	public StatsLogRecord(String name) {
		this(Level.INFO, name);
		playerName = name;
		timestamp = new java.util.Date();
	}
	
	// ACTIONS
    public void setDestroy(Block block) {
		this.key = "DESTROY";
		this.table = "actions";
		this.blockDestroyed = block;
    }		
	
    public void setCreate(Block blockPlaced, Block blockClicked, int item) {
		this.key = "CREATE";
		this.table = "actions";
		this.blockPlaced = blockPlaced;
		this.blockClicked = blockClicked;
		this.itemInHand = item;
    }
	
	// MOVEMENTS
	public void setMovement(Location from, Location to, double distance) {
		this.key = "MOVEMENT";
		this.table = "movements";
		this.from = from;
		this.to = to;
		this.distance = distance;
    }
	
	// CONNECTIONS
    public void setKick(String reason) {
		this.key = "KICK";
		this.table = "connections";
		this.reason = reason;
    }		
	
	
    public void setIpBan(String reason) {
		this.key = "IPBAN";
		this.table = "connections";
		this.reason = reason;
    }	
	
	
    public void setBan(String reason) {
		this.key = "BAN";
		this.table = "connections";
		this.reason = reason;
    }	
	
	
    public void setCommand(String[] command) {
		this.key = "COMMAND";
		this.table = "connections";
		this.command = command;
    }	
	
		
    public void setChat(String chatMessage) {
		this.key = "CHAT";
		this.table = "connections";
		this.chatMessage = chatMessage;
    }	
	
	
    public void setLogin() {
		this.key = "Login";
		this.table = "connections";
		
    }	
	
	
    public void setDisconnect() {
		this.key = "DISCONNECT";
		this.table = "connections";
		
    }	
	
  
}