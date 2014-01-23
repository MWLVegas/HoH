package com.ivalicemud.hoh;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;


public class HoH extends JavaPlugin implements Listener
{
	public ArrayList<Group> groups = new ArrayList<Group>();
	public ArrayList<Player> ready = new ArrayList<Player>	();
	public ArrayList<InventorySaver> is = new ArrayList<InventorySaver>	();
	public ArrayList<Location> safeHouses = new ArrayList<Location>();
	public ArrayList<Location> startPoints = new ArrayList<Location>();
	
	 private File dir;

	 public Random generator = new Random();
    static int gameScheduler;
	static ScoreboardManager manager;
	static boolean tagAPIenabled = false;
	boolean setup;
	
	class Group
	{
		Player p1;
		Player p2;
		int round;
		long time;
		int num;
		Player op1;
		Player op2;
		Location p1loc;
		Location p2loc;
		int p1count;
		
		Scoreboard board;
		Team team; 
		Objective objective;
		Score p1s;
		Score p2s;
		
		public Group ( Player player1, Player player2 )
		{
		p1 = player1;
		p2 = player2;
		round = 1;
		time = System.currentTimeMillis();
		num = -1;
		board = manager.getNewScoreboard();
		
		team = board.registerNewTeam("HoH");
		team.addPlayer(p1);
		team.addPlayer(p2);
		team.setAllowFriendlyFire(true);
		
		objective = board.registerNewObjective(p1.getName(), "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName("Charges");
		
		op1 = p1;
		op2 = p2;
		p1loc = op1.getLocation();
		p2loc = op2.getLocation();

		p1s = objective.getScore(Bukkit.getOfflinePlayer(p1.getName()));
		p2s = objective.getScore(Bukkit.getOfflinePlayer(p2.getName()));
		p1s.setScore(0);
		p2s.setScore(0);
		p1.setScoreboard(board);
		p2.setScoreboard(board);
		
		
		
		}
	}

	
	class InventorySaver {
		 Player p;
		 
		ItemStack[] i;
		ItemStack[] eq;
		
		public InventorySaver ( Player pl )
		{
			p = pl;
			
			i = p.getInventory().getContents();
			eq = p.getInventory().getArmorContents();
		}
		 
		}
	
    public static HoH plugin;

    public HoH()
    {
    }
 
    public void error(Object msg )
    {
				Bukkit.getServer().getLogger().severe("HoH Error: " + msg.toString() );
			
    }
 
    public void info(Object msg )
    {
				Bukkit.getServer().getLogger().info("HoH: " + msg.toString() );
			
    }
    
    public void debug(Object msg )
    {
			if ( plugin.getConfig().getBoolean("general.Debug") == true )
			{
				Bukkit.getServer().getLogger().info("HoH DEBUG: " + msg.toString() );
			}
    }

    public void onDisable()
    {
    	Iterator<Group> it = groups.iterator();
    	while ( it.hasNext() )
    	{
    		Group group = it.next();
    		endGame(group);
    		
    	}
    }
    
    public void onEnable()
    {
    	setup = false;

		getServer().getScheduler().scheduleSyncDelayedTask(this,
				new Runnable() {
					public void run() {	
						enablePlugin();
					}
				}, 20L);
    }
    
    public void enablePlugin()
    {
    	plugin = this;
    	manager = Bukkit.getScoreboardManager();
    	
    	loadConfiguration();
        
        	gameScheduler = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        	{ 
        		@Override
                public void run() {
        			gameUpdate();
                }
        	}	, 0, getConfig().getInt("general.gameUpdate") * (20) );

        	loadSafeHouses();
        	loadStartPoints();
        
        	dir = new File(getDataFolder(), "eq-backup");
        	
            if (getServer().getPluginManager().getPlugin("TagAPI") == null) {
			info("TagAPI is not installed - There will be no color nametag support!\nDownload TagAPI at http://dev.bukkit.org/bukkit-plugins/tag/");
    		}
    		else
    		{
    			info("TagAPI is installed - using colored nametags!");
    			getServer().getPluginManager().registerEvents(new TagApiListener(this), this);
    		}
    		
        	getServer().getPluginManager().registerEvents(new HoHListeners(this), this);
        	setup = true;


    }
    
    public void setConfig(String line, Object set)
    {
        if(!getConfig().contains(line))
            getConfig().set(line, set);
    }

    public void loadConfiguration()
    {
    	setConfig("general.GameDisabled",false);
    	setConfig("general.Debug",false);
    	setConfig("general.gameUpdate",5);
    	setConfig("general.announceJoin",true);
    	setConfig("general.announceJoinspamTimer",60);
    	setConfig("general.announceGameStart",true);
    	setConfig("general.mainWorld","world");
    	
    	setConfig("HoH.allowRegen",false);
    	setConfig("HoH.timing.roundLength",5);
    	setConfig("HoH.timing.waitLength",5);
    	setConfig("HoH.timing.safehouseDelay",10);
    	
    	setConfig("HoH.timing.teleportTimeout",10);
    	
    	setConfig("HoH.safeHouseRadius",5);
    	setConfig("HoH.safeHouseWitherStrength",1);
    	setConfig("HoH.points.win",2);
    	setConfig("HoH.points.lose",0.5);
    	
    	List<String> inv;
    	setConfig("HoH.kits.hunter.head",310);
    	setConfig("HoH.kits.hunter.chest",311);
    	setConfig("HoH.kits.hunter.boots",313);
    	setConfig("HoH.kits.hunter.legs",312);
    	inv = Arrays.asList("373:8262", "276");
    	setConfig("HoH.kits.hunter.inventory",inv);

    	setConfig("HoH.kits.hunted.head",298);
    	setConfig("HoH.kits.hunted.chest",299);
    	setConfig("HoH.kits.hunted.boots",300);
    	setConfig("HoH.kits.hunted.legs",301);
    	inv = Arrays.asList("272", "373:8226");
    	setConfig("HoH.kits.hunted.inventory",inv);
    	
    	inv = Arrays.asList("who","list","broadcast","bc");
    	setConfig("HoH.whitelistCommands",inv);

    	if ( !getConfig().contains("prizes"))
    	{
    		setConfig("prizes.1.item",264);
    		setConfig("prizes.1.amt",1);
    		setConfig("prizes.1.cost",5);
    		
    		setConfig("prizes.2.item",388);
    		setConfig("prizes.2.amt",2);
    		setConfig("prizes.2.cost",5);
    		
    	}
    	saveConfig();
    }
    
    public boolean allowedCommand( String cmd, Player p)
    {
    	if ( cmd.equalsIgnoreCase("hoh") ) return true;
    	
    	if ( cmd.equalsIgnoreCase("spawn") ) return false;
    	if ( cmd.equalsIgnoreCase("home") ) return false;
    	if ( cmd.equalsIgnoreCase("tpa") ) return false;
    	if ( cmd.equalsIgnoreCase("tp") ) return false;
    	if ( cmd.equalsIgnoreCase("tpo") ) return false;
    	if ( cmd.equalsIgnoreCase("sethome") ) return false;

    	if ( p.hasPermission("HoH.bypassWhitelist") )
    		return true;

    	List<String> cmds = getConfig().getStringList("HoH.whitelistCommands");
    	Iterator<String> it = cmds.iterator();
    	while ( it.hasNext() )
    	{
    		String check = it.next();
    		if ( cmd.equalsIgnoreCase(check) )
    		return true;
    	}
    	return false;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String args[])
    {
		 
    	 if(cmd.getName().equalsIgnoreCase("hoh"))
    	 {
    		 if ( setup == false )
    		 {
    			 sender.sendMessage("Please wait ... HoH is not fully set up yet.");
    			 return true;
    		 }
    		 
    		 ArrayList<String> a = new ArrayList<String>();
     		
     		if ( args.length <= 0 || args[0].equalsIgnoreCase("help") )
     		{
     			sender.sendMessage("Hand of Herobrine, by raum266, Geoso and Stuzz - version "+plugin.getDescription().getVersion());
     			listCommands(sender);
     			return true;
     		}

     		int num = 1;
    		while ( num < args.length )
    		{
    			a.add( args[num]);
    			num++;
    		}
    	
    		switch ( args[0].toLowerCase() )
    		{
    		default: sender.sendMessage("That is an invalid command."); listCommands(sender); break;
    		case "join": joinGame(sender); break;
    		case "reload": reloadConfig(sender); break;
    		case "list": listGames(sender); break;
    		case "cancel": cancelGame(sender,a); break;
    		case "quit": quitGame(sender); break;
    		case "config": changeConfig(sender,a); break;
    		case "safehouse": showSafeHouses(sender); break;
    		case "store": useStore(sender,a); break;
    		case "disable": disableHoH(sender); break;
    		}
    	 }
             return true;
             
    }

    void disableHoH(CommandSender s)
    {
    	if ( !perm(s, "hoh.admin", true )) return;
    	
    	getConfig().set("general.GameDisabled", !getConfig().getBoolean("general.GameDisabled"));
    	
    	s.sendMessage("HoH : Game Disabled set to " + getConfig().getBoolean("general.GameDisabled"));
    	
    	saveConfig();
    }
    
    void useStore(CommandSender s, ArrayList<String> a)
    {
    	if ( a.size() <= 0 || a.get(0).toString().equalsIgnoreCase("list"))
    	{
    		s.sendMessage("Items:");
    		
            for(String prize : getConfig().getConfigurationSection("prizes").getKeys(false) )
            {


            	int num = getConfig().getInt("prizes."+ prize + ".item");
            	int amt = getConfig().getInt("prizes."+ prize + ".amt");
            	double cost = getConfig().getInt("prizes."+ prize + ".cost");
            	
            	String item = Material.getMaterial(num).toString().replace("_"," ");
            	s.sendMessage(prize + ") " + amt + "x " +item + " ["+cost+"]");
            	//ItemStack item = new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunter.legs")),1));
            	//p.getInventory().addItem( new ItemStack(Material.COMPASS,1));
            }


    		s.sendMessage("You have " + getPoints( (Player) s ) + " points to spend.");
    		s.sendMessage("Use /hoh store buy # to make a purchase!");
    		return;
    	}
    	if ( a.size() == 1 && a.get(0).toString().equalsIgnoreCase("buy"))
    	{
    		
    		s.sendMessage("You must provide an item number to buy!");
    		s.sendMessage("Syntax: /hoh store buy #");
    		return;
    	}

    	if ( a.size() >= 2 && a.get(0).toString().equalsIgnoreCase("buy"))
    	{
    		Integer num = 0;
    		try {    		
    			num = Integer.parseInt(a.get(1));
    		} catch(NumberFormatException e) 
    		{
    			s.sendMessage("You must provide an item number to buy!");
        		s.sendMessage("Syntax: /hoh store buy #");
        		return;
    		}

    		if ( !getConfig().isSet("prizes."+num+".item"))
    		{
    			s.sendMessage("That prize does not exist.");
    			return;
    		}

        	int prize = getConfig().getInt("prizes."+ num + ".item");
        	int amt = getConfig().getInt("prizes."+ num + ".amt");
        	double cost = getConfig().getInt("prizes."+ num + ".cost");
        	Player p = (Player) s;
        	if ( cost > getPoints(p))
        	{
        		s.sendMessage("You do not have enough points to purchase that prize!");
        		return;
        	}
        	
        	if ( p.getInventory().firstEmpty() == -1 )
        	{
        		s.sendMessage("You need to have at least 1 empty inventory slot to make a purchase!");
        		return;
        	}
        	setPoints( p, (0 - cost) );
        	ItemStack item = new ItemStack(Material.getMaterial(prize),amt);
        	p.getInventory().addItem(item);
        	s.sendMessage("You purchase " + amt + " " + Material.getMaterial(prize).toString().replace("_"," ") + " for " + cost + " points.");
        	
        	return;

        }

    }
    
    boolean perm( CommandSender s, String perm, boolean loud )
    {
    	if ( s.hasPermission(perm) )
    		return true;
    	
    	if ( loud) 
    	s.sendMessage("[HoH] You are unable to do that.");
    	return false;
    }
    void reloadConfig( CommandSender s)
    {
    	if ( !perm(s, "hoh.admin", true )) return;
    	
    	getServer().getScheduler().cancelTask(gameScheduler);
    	
    	reloadConfig();
    	loadConfiguration();
     	//resetSafeHouses();
     	//resetStartPoints();
     	
        getServer().getPluginManager().registerEvents(plugin, this);

        	gameScheduler = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        	{ 
        		@Override
                public void run() {
        			gameUpdate();
                }
        	}	, 0, getConfig().getInt("general.gameUpdate") * (20) );

    	
    }
    
    public void changeConfig( CommandSender s, ArrayList<String> a)
    {
    	if ( !perm(s, "hoh.admin", true )) return;
    	
    	if ( !(s instanceof Player )) 
    		return;
    	
    	Player p = (Player) s;
    	
    	if ( a.size() == 0 )
    	{
    		s.sendMessage("You must set a valid configuration option. Valid options are:");
    		return;
    	}
    	
    	String cmd = a.get(0);
    	a.remove(0);
    	
    	switch ( cmd.toLowerCase() )
    	{
    	default: s.sendMessage("That is an invalid configuration option. Valid options are:\n\r" +
    			"sethouse\n\r"+
    			"delhouse\n\r"+
    			"setstart\n\r"+
    			"delstart\n\r" 			);
    	
    	return;
    	case "setcenter": 
    		getConfig().set("HoH.points.centerX",p.getLocation().getBlockX());
    		getConfig().set("HoH.points.centerY",p.getLocation().getBlockY());
    		getConfig().set("HoH.points.centerZ",p.getLocation().getBlockZ());
    		getConfig().set("HoH.points.world",p.getLocation().getWorld().getUID().toString());
    	msg(p, "Center location for HoH set!"); break;
    	case "sethouse": setHouse(p); break;
    	case "delhouse": delHouse(p); break;
    	case "setstart": setStart(p); break;
    	case "delstart": delStart(p, a); break;
    	}
    	
    	saveConfig();
    }
    
    
    public void listCommands( CommandSender s)
    {
    	s.sendMessage("All commands follow /hoh : ");
    	s.sendMessage("Join - Queue for an HoH battle");
    	s.sendMessage("Quit - Exit the Queue");
    	s.sendMessage("Stats - Show your HoH stats (Work in Progress");
    	s.sendMessage("Store - Use your points to buy items");
    	
    	s.sendMessage("SafeHouse - Show all current safehouses");
    	s.sendMessage("Config - Edit config options");
    	s.sendMessage("Reload - Reload the config files");
    	s.sendMessage("List - Show all games in progress");
    	return;
    }
    
    public void listGames( CommandSender s )
    {
    	int num = 0;
    	int active = 0;
    	if ( !(s instanceof Player))
    		return;
    	Player p = (Player) s;
    	
    	Iterator<Group> it = groups.iterator();
    	while ( it.hasNext() )
    	{
    		Group group = it.next();
    		num++;
    		
    		if ( group != null )
    		{
    			msg(p,"Group #"+num+" - Round: "+group.round+" " + group.p1.getName() + " vs " + group.p2.getName() );
    			active++;
    		}
    		
    		if ( active == 0 )
    		{
    			groups.clear();
    		}
    	}
    	
    }
    
    public void quitGame( CommandSender s)
    {
    	if ( !(s instanceof Player))
    		return;
    	Player p = (Player) s;
    	
    	if ( p.hasMetadata("hoh.group"))
    	{
    		msg(p,"You cannot quit a game that is in progress!");
    		return;
    	}
    	
    	if ( p.hasMetadata("hoh.waiting"))
    	{
    		msg(p,"You remove yourself from the HoH queue.");
    		p.removeMetadata("hoh.waiting",plugin);

    		Iterator<Player> it = ready.iterator();
        	while ( it.hasNext() )
        	{
        		Player p2 = it.next();
        		if ( p2 == p )
        		{
        			it.remove();
        			break;
        		}
       		}
    		return;
    	}
    	
		

    }
    
    public void cancelGame( CommandSender s, ArrayList<String> a)
    {
    	if ( !perm(s, "hoh.admin", true )) return;
    	
    	if ( a.size() < 1 )
    	{
    		s.sendMessage("That is an invalid group number.");
    		return;
    	}
    	
    	String chk = a.get(0);
    	Iterator<Group> it = groups.iterator();
    	while ( it.hasNext() )
    	{
    		Group g = it.next();

    		if ( g.p1 == getServer().getPlayer(chk) ||
    				g.p2 == getServer().getPlayer(chk) ||
    				String.valueOf(g.num).equals(chk) )
    		{
    			s.sendMessage("Game found - Cancelling!");
    		endGame(g);
    		return;	
    		}
   		}
    	
    	Player p = getServer().getPlayer(chk);
    	if ( p == null )
    	{
    		s.sendMessage("That game number, or player was not found.");
    		return;
    	}
    	
    	p.removeMetadata("hoh.group", plugin);
    		s.sendMessage("Player information reset.");
    	return;
    }
    
    public boolean isSetup( ) {
    	
    	boolean set = true;
    	if ( safeHouses.size() <= 1 )
    	{
    		error("You must have at least two safe houses set!");
    		set = false;
    	}
    	
    	if ( !getConfig().contains("HoH.points.centerX") )
    	{
    		error("You have no center mark yet!");
    		set = false;
    	}

    	if ( !getConfig().contains("HoH.kits.hunter.inventory") )
    	{
    		error("You have no hunter kit set up!");
    		set = false;
    	}
    	
    	if ( !getConfig().contains("HoH.kits.hunted.inventory") )
    	{
    		error("You have no hunted kit set up!");
    		set = false;
    	}
    	if ( startPoints.size() <= 3 )
    	{
    		error("You must have at least 4 starting points!");
    		set = false;
    	}

    	// At least 1 reward
    	return set;

    	
    }
    public void joinGame(CommandSender s)
    {
       if ( !(s instanceof Player) )
    	   return;

       Player p = (Player) s;
       
       if ( getConfig().getBoolean("general.GameDisabled"))
       {
    	   msg(p,"HoH is currently disabled.");
    	   return;
       }
       
       if ( !isSetup() )
       {
    	   msg(p,"The game has not yet been configured for play. Contact your admin for assistance.");
    	   return;
       }
       if ( p.hasMetadata("hoh.group") )
    		   {
    	   msg(p,"You are already playing Hand of Herobrine!");
    	   return;
    		   }
       if ( p.hasMetadata("hoh.waiting") )
       {
    	   msg(p,"You are already waiting to join Hand of Herobrine!");
    	return;
       }
       
       ready.add(p);
       p.setMetadata("hoh.waiting", new FixedMetadataValue(plugin,true));
       boolean broadcast = false;
       
       msg(p,"You are ready to play Hand of Herobrine ... waiting for a partner!");
       
       if ( getConfig().getBoolean("general.announceJoin") == true
    		   && ready.size() <= 1){
    	
    	   if ( p.hasMetadata("hoh.join")) // Already joined recently - check to announce
    	   {
       			long timer = ((MetadataValue) p.getMetadata("hoh.join").get(0)).asLong();
       			if ( timer + ( getConfig().getInt("general.announceJoinspamTimer") * 1000) <= System.currentTimeMillis() )
       			{
       				broadcast = true;
       				
       			}
       			
       			debug("Time between messages: "+  (System.currentTimeMillis() - (timer + ( getConfig().getInt("general.announceJoinspamTimer") * 1000)))  );
    	   }
    	   else broadcast = true;
    	   
    	   if ( broadcast )
    		   getServer().broadcastMessage(ChatColor.RED + "[HoH] " + ChatColor.WHITE + p.getDisplayName() + " has joined the Hand of Herobrine! Type /hoh join to play!");
    	   else
    		   debug("no broadcast. Timer not expired.");
    		   
    	   p.setMetadata("hoh.join", new FixedMetadataValue(plugin,System.currentTimeMillis()) );
    	   
       }

       checkGameStart();
    }
    
    void checkGameStart()
    {
    	if ( ready.size() < 2 )
    		return;
    	
    	
    	newGame( ready.get(0), ready.get(1) );
    	ready.remove(1);
    	ready.remove(0);
    	
    	checkGameStart();
    }
    

	public void gameUpdate() {
		checkGameStart();

		Iterator<Group> it = groups.iterator();
		int num = -1;
		int active = 0;
		long time = System.currentTimeMillis();
		int roundLength = getConfig().getInt("HoH.timing.roundLength");
		int waitLength = getConfig().getInt("HoH.timing.waitLength");

		while (it.hasNext()) {
			Group group = it.next();

			num++;

			if (group != null) {
				active++;

				if (group.round == 2) // waiting round
				{
					long timer = group.time + (waitLength * 1000);

					if ((timer <= time)) {
						endRound(group, num);
				}

				} else {
					
					long timer = time - group.time;
					timer /= 1000;

					if ( !safeHouse(group.p2,null) )
						group.p1.setCompassTarget( group.p2.getLocation());
					
					if ( getConfig().getBoolean("HoH.allowRegen") == true )
					{
						group.p2.setFoodLevel(20);
						group.p1.setFoodLevel(20);
						
					}
					else
					{
						group.p2.setFoodLevel(19);
						group.p1.setFoodLevel(19);
					}
					
					addScore(group);
					if ((timer / 60) >= roundLength) {
						endRound(group, num);
					}
				}

			}
		}

		if (active == 0) {
			groups.clear();
		}
	}
    
	public void addScore(Group g )
	{
		Double x = getConfig().getDouble("HoH.points.centerX");
		Double y = getConfig().getDouble("HoH.points.centerY");
		Double z = getConfig().getDouble("HoH.points.centerZ");
		World world = getServer().getWorld( UUID.fromString(getConfig().getString("HoH.points.world")));
		Location loc = new Location(world,x, y, z);
		
		double dist = g.p2.getLocation().distance(loc);
		
		if ( !safeHouse(g.p2,null) )
		{
			int bonus = 0;
			int score = g.objective.getScore( getServer().getOfflinePlayer(g.p2.getName())).getScore();
			
			if ( dist > 75 ) bonus = 1;
			else if ( dist > 60 ) bonus = 2;
			else if ( dist > 30 ) bonus = 4;
			else if ( dist > 8 ) bonus = 8;
			else if ( dist >= 0 ) bonus  = 16;
			
			g.objective.getScore( getServer().getOfflinePlayer(g.p2.getName()) ).setScore(score + bonus);
	    	
			
		}
		
	}
	
	public void setHouse( Player p )
	{
		int num = safeHouses.size() + 1;
		
		
		setConfig("HoH.points.houses."+num+".world",p.getLocation().getWorld().getUID().toString());
		setConfig("HoH.points.houses."+num+".centerX",p.getLocation().getBlockX());
		setConfig("HoH.points.houses."+num+".centerY",p.getLocation().getBlockY());
		setConfig("HoH.points.houses."+num+".centerZ",p.getLocation().getBlockZ());
		msg(p, "Center location for house set!"); 
		
		safeHouses.add(p.getLocation() );
		
		
	}
	
	public void delHouse(Player p)
	{

       	Iterator<Location> sh = safeHouses.iterator();
       	int dist = getConfig().getInt("HoH.safeHouseRadius");

       	while ( sh.hasNext() )
       	{
       		Location house = sh.next();
       		
       		if ( p.getLocation().distance(house) <= dist )
       		{
       			msg(p,"This safehouse has been removed!");
       			sh.remove();
       	       	resetSafeHouses();
       			loadSafeHouses();

       			return;
       		}
       	}
       	
       	msg(p,"You are not standing in a safehouse!");


	}
	
	public void resetSafeHouses()
	{
		int num = 1;
		
		 for(String houses : getConfig().getConfigurationSection("HoH.points.houses").getKeys(false) )
		 {
			 debug("Deleting house "+ houses);
			 getConfig().set("HoH.points.houses."+houses,null);
		 }
		 saveConfig();
		
		Iterator<Location> sh1 = safeHouses.iterator();
       	
       	while ( sh1.hasNext() )
       	{
       		debug("Saving Safehouse " + num);
       		Location h = sh1.next();
       	
       	
       		setConfig("HoH.points.houses."+num+".world",h.getWorld().getUID().toString());
       		setConfig("HoH.points.houses."+num+".centerX",h.getBlockX());
       		setConfig("HoH.points.houses."+num+".centerY",h.getBlockY());
       		setConfig("HoH.points.houses."+num+".centerZ",h.getBlockZ());
       		
			num++;
			
       	}
		
	}
	
	public void loadSafeHouses()
	{
		safeHouses.clear();
		if ( !getConfig().contains("HoH.points.houses") )
			return;
		
		 for(String houses : getConfig().getConfigurationSection("HoH.points.houses").getKeys(false) )
		 {
			 
			 World world = getServer().getWorld( UUID.fromString(getConfig().getString("HoH.points.houses."+houses+".world")));
			 Double x = getConfig().getDouble("HoH.points.houses."+houses+".centerX");
			 Double y = getConfig().getDouble("HoH.points.houses."+houses+".centerY");
			 Double z = getConfig().getDouble("HoH.points.houses."+houses+".centerZ");
			 
			 Location loc = null;
			 try {
			 loc = new Location( world, x, y, z);
			 
			 } catch (NullPointerException e)
			 {
				 error("Invalid safehouse .. skipping!");
				 continue;
			 }
			 //debug(world.getName() );
			 safeHouses.add(loc);
			 
		 }
		 
		info("Safe Houses loaded: " + safeHouses.size()); 
		
	}
	
	public boolean safeHouse( Player p, Location loc )
	{
		
       	Iterator<Location> sh = safeHouses.iterator();
       	int dist = getConfig().getInt("HoH.safeHouseRadius");
       	Location chk = p.getLocation();
       	if ( loc != null ) 
       		{
//       		debug("Checking given loc");
       		chk = loc;
       		}
       	
       	while ( sh.hasNext() )
       	{
       		Location house = sh.next();
       		
       		if ( chk.distance(house) <= dist )
       			return true;
       	}

		return false;
	
	}
	

    public void showSafeHouses( CommandSender s)
    {
    	if ( !perm(s, "hoh.admin", true )) return;
    	
    	if ( !(s instanceof Player) )
    		return;

       	debug(safeHouses.get(0).getWorld());

    	Player p = (Player) s;
    	msg (p,"Showing "+ safeHouses.size() + " safe houses.");
    	
    	final int dist = getConfig().getInt("HoH.safeHouseRadius");

    	Iterator<Location> sh = safeHouses.iterator();
       	while ( sh.hasNext() )
       	{
       		Location h = sh.next();
       		final Location house = h.clone();

       		debug("Showing " + house.getBlockX() + " " + house.getBlockY() + " " + house.getBlockZ() );

       		Smoke(house,dist);
       		
       		getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() {Smoke(house, dist); }}, 20L);
       		getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() {Smoke(house, dist); }}, 40L);
       		getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() {Smoke(house, dist); }}, 60L);
       		getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() {Smoke(house, dist); }}, 80L);
       		getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { public void run() {Smoke(house, dist); }}, 100L);
       		

}

       	

     
    }
    
    void Smoke( Location house, int dist )
    {
    	Location b = null;
   		house.getWorld().playEffect( house,  Effect.MOBSPAWNER_FLAMES,1);
   		
   		for ( int a = 0; a < 10; a++)
   		{
   			
   		b = new Location(house.getWorld(),house.getX() - dist,house.getY(),house.getZ() - dist );
   		b.getWorld().playEffect( b, Effect.SMOKE, 1);       	

   		b = new Location(house.getWorld(),house.getX() - dist,house.getY() ,house.getZ() + dist );
   		b.getWorld().playEffect( b, Effect.SMOKE, 1);       	

   		b = new Location(house.getWorld(),house.getX() + dist ,house.getY(),house.getZ() - dist);
   		b.getWorld().playEffect( b, Effect.SMOKE, 1);       	

   		b = new Location(house.getWorld(),house.getX() + dist,house.getY() ,house.getZ() + dist );
   		b.getWorld().playEffect( b, Effect.SMOKE, 1);
   		}

    }
    

	public void setStart( Player p )
	{
		int num = startPoints.size() + 1;
		
		
		setConfig("HoH.points.start."+num+".world",p.getLocation().getWorld().getUID().toString());
		setConfig("HoH.points.start."+num+".centerX",p.getLocation().getBlockX());
		setConfig("HoH.points.start."+num+".centerY",p.getLocation().getBlockY());
		setConfig("HoH.points.start."+num+".centerZ",p.getLocation().getBlockZ());
		
		startPoints.add(p.getLocation() );
		msg(p,"Start point added! There are now " + startPoints.size() + " start points.");		
		saveConfig();
		
	}
	
	public void listStartLocations( Player p )
	{
		int num = 0;
		Iterator<Location> sh1 = startPoints.iterator();
       	
       	while ( sh1.hasNext() )
       	{
       	Location l = sh1.next();
       	msg(p,num+") " +l.getBlockX() +" " +l.getBlockY() +" " +l.getBlockZ() );
       	num++;
       	
       	}
	}
	
	public void delStart(Player p, ArrayList<String> a)
	{
		if ( a.size() == 0 )
		{
		msg(p,"Delete which start spot:");
		listStartLocations(p);
		return;
		}
	
		int house = -1;
		
		try {
		house = Integer.valueOf(a.get(0));
		} catch(NumberFormatException e) {
			msg(p,"Invalid startpoint. Delete which one?");
			listStartLocations(p);
		}

		if ( house == -1 || house > startPoints.size() )
		{
			msg(p,"Invalid startpoint. Delete which one?");
			listStartLocations(p);
		}
			
		startPoints.remove(house);
		resetStartPoints();
		saveConfig();
		msg(p,"Start point removed. There are now " + startPoints.size() + " start points.");
		return;

	}
	
	public void resetStartPoints()
	{
		int num = 1;
		
		 for(String houses : getConfig().getConfigurationSection("HoH.points.start").getKeys(false) )
		 {
			 debug("Deleting Start "+ houses);
			 getConfig().set("HoH.points.start."+houses,null);
		 }
		 saveConfig();
		
		Iterator<Location> sh1 = startPoints.iterator();
       	
       	while ( sh1.hasNext() )
       	{
       		debug("Saving StartPoint " + num);
       		Location h = sh1.next();
       	
       	
       		setConfig("HoH.points.start."+num+".world",h.getWorld().getUID().toString());
       		setConfig("HoH.points.start."+num+".centerX",h.getBlockX());
       		setConfig("HoH.points.start."+num+".centerY",h.getBlockY());
       		setConfig("HoH.points.start."+num+".centerZ",h.getBlockZ());
       		
			num++;
			
       	}
		
	}
	
	public void loadStartPoints()
	{
		startPoints.clear();
		if ( !getConfig().contains("HoH.points.start") )
			return;
		
		 for(String houses : getConfig().getConfigurationSection("HoH.points.start").getKeys(false) )
		 {
			 World world = getServer().getWorld( UUID.fromString(getConfig().getString("HoH.points.start."+houses+".world")));

			 Double x = getConfig().getDouble("HoH.points.start."+houses+".centerX");
			 Double y = getConfig().getDouble("HoH.points.start."+houses+".centerY");
			 Double z = getConfig().getDouble("HoH.points.start."+houses+".centerZ");
			 
			 Location loc = null;
			 try {
			 loc = new Location( world, x, y, z);
			 
			 } catch (NullPointerException e)
			 {
				 error("Invalid startpoint .. skipping!");
				 continue;
			 }
			 
			 startPoints.add(loc);
			 
		 }
		 
		info("Starting Points Loaded: " + startPoints.size()); 
		
	}
	
	
    public void endRound( Group g, int num )
    {
    	g.p1.removeMetadata("hoh.safehouse",plugin);
    	g.p2.removeMetadata("hoh.safehouse",plugin);
    	
    	g.p1.removePotionEffect(PotionEffectType.WITHER);
		g.p1.removePotionEffect(PotionEffectType.SPEED);

		g.p2.removePotionEffect(PotionEffectType.WITHER);
		g.p2.removePotionEffect(PotionEffectType.SPEED);

		
    	if ( g.round == 1 )
    	{

    		Player p1 = g.p1;
    		g.p1 = g.p2;
    		g.p2 = p1;

    		g.round = 2;
    		gmsg(g,"End of round!");
    		
			g.p1.setNoDamageTicks( 1000 ); //no damage while in waiting round
			g.p2.setNoDamageTicks( 1000 );

    		newRound(g);
    	}
    	else if ( g.round == 2 ) // waiting round
    	{
			g.p1.setNoDamageTicks( 0 );
			g.p2.setNoDamageTicks( 0 );
    		
    		g.round = 3;
    		newRound(g);
    	}
    	else if ( g.round == 3 ) // End of game
    	{
    		
    		g.round = 4;
    		
    		g.p1.setHealth(20);
    		g.p2.setHealth(20);
    		g.p1.setFoodLevel(20);
    		g.p2.setFoodLevel(20);
    		
    		endGame(g);
    	}
    }

    public double getPoints(Player p)
    {
    	double points = 0;
    	File dir2 = new File(getDataFolder(),"score" + p.getName().substring(1,1).toLowerCase());
   	 	File file = new File(dir2, p.getName());    
   	 	
   		YamlConfiguration config = new YamlConfiguration();
        try {
			config.load(file);
			points = config.getDouble("score");
		} catch (IOException | InvalidConfigurationException e) {
		}
    	return points;
    }

	public void setPoints(Player p, double points) {
		double point = getPoints(p);
		File dir2 = new File(getDataFolder(), "score"
				+ p.getName().substring(1, 1).toLowerCase());
		File file = new File(dir2, p.getName());
		YamlConfiguration config = new YamlConfiguration();
		point += points;
		config.set("score", point);

		try {
			config.save(file);
		} catch (IOException e) {
			debug("Error saving point file: " + file);
		}
	}

    public void getWinner(Group g)
    {
    	int p1 = g.objective.getScore( getServer().getOfflinePlayer(g.p1.getName()) ).getScore();
    	int p2 = g.objective.getScore( getServer().getOfflinePlayer(g.p2.getName()) ).getScore();
    	
    	gmsg(g,"Final Scores:");
    	gmsg(g,g.p1.getName() + ": "+p1);
    	gmsg(g,g.p2.getName() + ": "+p2);
    	
    	if ( p1 < p2 )
    	{
    		gmsg(g,g.p2.getName() + " wins!");
    		setPoints(g.p2, getConfig().getDouble("HoH.points.win"));
    		setPoints(g.p1, getConfig().getDouble("HoH.points.lose"));
        }
    	else if ( p2 < p1 )
    	{
    		gmsg(g,g.p1.getName() + " wins!");
    		setPoints(g.p1, getConfig().getDouble("HoH.points.win"));
    		setPoints(g.p2, getConfig().getDouble("HoH.points.lose"));
    	}
    	else
    	{
    		gmsg(g,"Tie game!");
    	}
    	
    }
    
    public void endGame(Group g)
    {
    	final Group gr = g;
    	if ( g == null )
    		return;
    	
    	if ( g.round == 4 ) // Actual End
    	{
    		gmsg(g,"Game over!");
    	    getWinner(g);  
    	}
    	else // Premature end
    	{
    		gmsg(g,"The game has been cancelled!");
    	}
    	debug("Teleporting players out ...");
		g.op1.teleport(g.p1loc);
		g.op2.teleport(g.p2loc);
    	debug("Removing metadatas ...");
    	g.p1.removeMetadata("hoh.safehouse",plugin);
    	g.p2.removeMetadata("hoh.safehouse",plugin);

    	g.p1.removeMetadata("hoh.group", plugin);
		g.p2.removeMetadata("hoh.group", plugin);

		debug("Removing potion effects ..");
		g.p1.removePotionEffect(PotionEffectType.WITHER);
		g.p1.removePotionEffect(PotionEffectType.SPEED);

		g.p2.removePotionEffect(PotionEffectType.WITHER);
		g.p2.removePotionEffect(PotionEffectType.SPEED);

		

		debug("Recovering inventories ...");
		plugin.getServer().getScheduler()
		.scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
		    	recoverInventory(gr.p1, false);
		    	recoverInventory(gr.p2, false);

			}
		}, 20);
		
    	g.p1.setNoDamageTicks( 0 );
		g.p2.setNoDamageTicks( 0 );
		
		
		g.team.unregister();
		g.objective.unregister();
		g.board.clearSlot(DisplaySlot.SIDEBAR);

		groups.set(g.num, null);
		g = null;
		
    }
    
    public void hunterTP( final Player p, Group g )
    {
        int randomNum = generator.nextInt(startPoints.size());
        Location loc = startPoints.get(randomNum);
        
        
        if ( loc == null )
        {
        error("Teleport error!");
        msg(p,"There has been a teleport error!");
        return;
        }

        Chunk c = loc.getWorld().getChunkAt(loc);
        
        if ( !c.isLoaded() )
        {
        	c.load();
        }
        
    	if ( loc.distance(g.p2.getLocation())< 40 )
    	{
    		debug("Hunter too close! Moving Hunter!");
    		hunterTP( p, g );
    		return;
    	}
		p.teleport(loc);
		
		plugin.getServer().getScheduler()
		.scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				hunterKit(p);
			}
		}, 10);
		

    }
    
    public void huntedTP( Player p )
    {
		int randomNum = generator.nextInt(startPoints.size());
        Location loc = startPoints.get(randomNum);
        
        if ( loc == null )
        {
        error("Teleport error!");
        msg(p,"There has been a teleport error!");
        return;
        }
 Chunk c = loc.getWorld().getChunkAt(loc);
        
        if ( !c.isLoaded() )
        {
        	c.load();
        }
        
		p.teleport(loc);
		
    }
    public void saveInventory( Player p )
    {

    	InventorySaver inv = new InventorySaver( p );
    	
    	is.add(inv);
    	debug("Inventory saved: " +p.getName());
    	
    	 File file = new File(dir, p.getName());
         YamlConfiguration config = new YamlConfiguration();
         config.set("inv", p.getInventory().getContents() );
         config.set("eq", p.getInventory().getArmorContents());
         try {
			config.save(file);
		} catch (IOException e) {
			debug("Error saving flatfile");
		}
    	
    }
    public void recoverInventory( Player p, boolean quiet )
    {
    	File file = new File(dir, p.getName());
    	boolean found = false;
    	
       	Iterator<InventorySaver> it = is.iterator();
       	
       	while ( it.hasNext() )
       	{
       		InventorySaver inventory = it.next();
       		if ( inventory.p == p )
       		{
       			debug("Found inventory for " + inventory.p.getName() );

       			clearInventory(p);
       			p.getInventory().setContents( inventory.i );
       			p.getInventory().setArmorContents(inventory.eq);
       			debug("Inventory recovered: " + p.getName());

       			it.remove();
       			found = true;
       			break;
       		}
       	}
       	
       	if ( !found )
       	{
       	debug("Not found in memory - checking files..");
       	YamlConfiguration config = new YamlConfiguration();
        try {
			config.load(file);
			List<?> items = config.getList("inv");
            List<?> eq = config.getList("eq");
	            
	        p.getInventory().setContents( items.toArray(new ItemStack[items.size()]));
	        p.getInventory().setArmorContents(eq.toArray(new ItemStack[eq.size()]));
	        found = true;
	            
		} catch (IOException | InvalidConfigurationException e) {
		}
        
       	}
       	
       	if ( !found && !quiet )
			msg(p, "Your inventory could not be recovered!");	
       	
       	file.delete();
       	
       	if ( found && quiet )
       	{
       		debug("Moving to spawn...");
       		World world = getServer().getWorld(getConfig().getString("general.mainWorld"));
       		p.teleport(world.getSpawnLocation());
       	}
       	
       	return;
    }
    public void gmsg( Group g, String msg )
    {
    	g.p1.sendMessage( msg.toString().replaceAll("&([0-9A-Fa-f])", "\u00A7$1") );
    	g.p2.sendMessage( msg.toString().replaceAll("&([0-9A-Fa-f])", "\u00A7$1") );
    }
    
    public void msg( Player p, String msg )
    {
    	p.sendMessage( msg.toString().replaceAll("&([0-9A-Fa-f])", "\u00A7$1") );
    }

    public void clearInventory( Player p )
    {
    	debug("Clearing inventory: " + p.getName());
		p.getInventory().clear();
		p.getInventory().setHelmet(null);
		p.getInventory().setBoots(null);
		p.getInventory().setChestplate(null);
		p.getInventory().setLeggings(null);
		p.getEquipment().clear();
    }
    
    void hunterKit( Player p )
    {

    	clearInventory(p);

    	debug("Giving hunter kit to "+p.getName() );
    	
    	p.getInventory().setHelmet( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunter.head")),1));
    	p.getInventory().setBoots( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunter.boots")),1));
    	p.getInventory().setChestplate( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunter.chest")),1));
    	p.getInventory().setLeggings( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunter.legs")),1));
    	
    	p.getInventory().addItem( new ItemStack(Material.COMPASS,1));
    	
		List<?> inv = getConfig().getList("HoH.kits.hunter.inventory");
		Iterator<?> i = inv.iterator();
		while (i.hasNext()) {
			Object a = i.next();
			if (a.toString().contains(":")) {

				String[] items = a.toString().split(":");
				try {
					Material m = Material.getMaterial(Integer.valueOf(items[0]));
					String data = items[1].trim();
					
					ItemStack item = new ItemStack(m, 1);
					if (item.getType() == Material.POTION)
						item.setDurability(Integer.valueOf(data).shortValue());
					else

					p.getInventory().addItem(item);
				} catch (NullPointerException e) {
					error("Invalid kit item - Hunter: " + a);
					continue;
				}

			} else
				try {
				p.getInventory().addItem(new ItemStack(Material.getMaterial(Integer.valueOf(a.toString())), 1));
				} catch (NullPointerException e) {
					error("Invalid kit item - Hunter: " + a);
					continue;
				}
		}

     	PotionEffectType pt = PotionEffectType.SPEED;
     	int power = 1;
     	int duration = 9999;
        p.addPotionEffect(new PotionEffect(pt,duration,power));
    	debug("Hunter kit finished "+p.getName() );
   
    }
   
    void huntedKit( Player p)
    {
    	debug("Giving hunted kit to "+p.getName() );

    	clearInventory(p);
    	p.getInventory().addItem( new ItemStack(Material.COMPASS,1));

    	p.getInventory().setHelmet( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunted.head")),1));
    	p.getInventory().setBoots( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunted.boots")),1));
    	p.getInventory().setChestplate( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunted.chest")),1));
    	p.getInventory().setLeggings( new ItemStack(Material.getMaterial(getConfig().getInt("HoH.kits.hunted.legs")),1));
    	
		List<?> inv = getConfig().getList("HoH.kits.hunted.inventory");
		Iterator<?> i = inv.iterator();
		while (i.hasNext()) {
			Object a = i.next();
			if (a.toString().contains(":")) {

				String[] items = a.toString().split(":");
				try {
					Material m = Material.getMaterial(Integer.valueOf(items[0]));
					String data = items[1].trim();
					
					ItemStack item = new ItemStack(m, 1);
					if (item.getType() == Material.POTION)
						item.setDurability(Integer.valueOf(data).shortValue());
					else

					p.getInventory().addItem(item);
				} catch (NullPointerException e) {
					error("Invalid kit item - hunted: " + a);
					continue;
				}

			} else
				try {
				p.getInventory().addItem(new ItemStack(Material.getMaterial(Integer.valueOf(a.toString())), 1));
				} catch (NullPointerException e) {
					error("Invalid kit item - hunted: " + a);
					continue;
				}
		}
    }
    
   public void newRound( Group g )
   {
	   clearInventory(g.p1);
	   clearInventory(g.p2);

	    g.p1count = 0;
		g.p1.removeMetadata("hoh.safehouse", plugin);
		g.p1.removePotionEffect(PotionEffectType.WITHER);
		g.p1.removePotionEffect(PotionEffectType.SPEED);

		g.p2.removeMetadata("hoh.safehouse", plugin);
		g.p2.removePotionEffect(PotionEffectType.WITHER);
		g.p2.removePotionEffect(PotionEffectType.SPEED);

		g.p1.setHealth(20);
		g.p2.setHealth(20);
		g.p1.setFoodLevel(20);
		g.p2.setFoodLevel(20);
		
		g.time = System.currentTimeMillis()+100;

		if ( g.round == 2 ) // waiting round
		{
			gmsg(g,"The new round will begin soon ...");
		}
		if ( g.round == 1 || g.round == 3 )
		{

			
			huntedTP(g.p2);
			hunterTP(g.p1, g);

			huntedKit(g.p2);

			msg(g.p1,"You are the HUNTER!");
			msg(g.p2,"You are the HUNTED!");
			
			Double x = getConfig().getDouble("HoH.points.centerX");
			Double y = getConfig().getDouble("HoH.points.centerY");
			Double z = getConfig().getDouble("HoH.points.centerZ");
			World world = getServer().getWorld( UUID.fromString(getConfig().getString("HoH.points.world")));

			Location loc = new Location(world,x, y, z);
			g.p2.setCompassTarget(loc);
		}
		
   }
   
   public int findGroup( Group g )
   {
   	Iterator<Group> it = groups.iterator();
   	int num = 0;
   	while ( it.hasNext() )
   	{
   		Group group = it.next();
   		if ( group == g )
   		{
   			g.num = num;
   			return num;
   		}
   		
   		num++;
   	}
   	
   	return -1;
   }

    public Group findGroup( Player p)
    {
    	if ( p.hasMetadata("hoh.group") )
    	{
    		try {
    			
        		int groupnum = ((MetadataValue) p.getMetadata("hoh.group").get(0)).asInt();
    		
    		if ( groups.get(groupnum) != null )
    			return groups.get(groupnum);
    		}
    		catch ( NumberFormatException e ) {
    			
    		}
    	}
    
    	Iterator<Group> it = groups.iterator();
    	while ( it.hasNext() )
    	{
    		Group group = it.next();
    	
    		if ( group != null )
    		{
    			if ( group.p1 == p || group.p2 == p )
    			{

    				return group;
    			}
    		}
    	}
    	return null;
     }
    
       
    public void newGame( Player p1, Player p2 )
    {
    	Player pt1;
    	Player pt2;

        int randomNum = generator.nextInt(100) + 1;

    	if ( randomNum < 50 )
    	{ pt1 = p1; pt2 = p2; }
    	else
    	{ pt1 = p2; pt2 = p1; }
    	
    	Group group = new Group( pt1, pt2 );
    	groups.add(group);
    	int num = findGroup( group );
		p1.removeMetadata("hoh.waiting", plugin);
		p2.removeMetadata("hoh.waiting", plugin);
		gmsg(group,"A new game of HoH has begun!");
    	
    	pt1.setMetadata("hoh.group", new FixedMetadataValue(plugin,num));
    	pt2.setMetadata("hoh.group", new FixedMetadataValue(plugin,num));
    	int gnum =  findGroup(group);
    	 
		debug("Group set: "+gnum);
		group.p1.setMetadata("hoh.group", new FixedMetadataValue(plugin,gnum));
		group.p2.setMetadata("hoh.group", new FixedMetadataValue(plugin,gnum));
    	
    	// Save Inventories
    	saveInventory(pt1);
    	saveInventory(pt2);
    	
 	   if ( getConfig().getBoolean("general.announceGameStart") == true ) 
		   getServer().broadcastMessage(ChatColor.RED + "[HoH] " + ChatColor.WHITE + "A new game between " + pt1.getDisplayName() + " and " + pt2.getDisplayName() + " has begun!");

    	newRound(group);
    	
    }
    
    public void checkWinner( CommandSender s, ArrayList<String> a)
    {
    	if ( a.size() != 2 )
    	{
    		s.sendMessage("This requires two players.");
    		return;
    	}
    	
    	Player p1 = getServer().getPlayer(a.get(0));
    	Player p2 = getServer().getPlayer(a.get(1));
    	
    	if ( p1 == null || p2 == null)
    	{
    		s.sendMessage("Player(s) not found.");
    		return;
    	}
    	
    	//Scoreboard board = p.getScoreboard();
    	//Team team = board.getPlayerTeam(p);
    	
    	//Objective o = board.getObjective("Charges");
    	 
    }
    
    public void targetCompass(CommandSender s, ArrayList<String> a)
    {
    	Player player = null;
    	Player target = null;
    	
    	if ( !(s instanceof Player) )
    	{
    		if ( a.size() != 2 )
        	{
        		s.sendMessage("Target requires 2 arguments - /fog target (player) (target)");
        		return;
        	}
    		
    		player = getServer().getPlayer(a.get(0));
    		target = getServer().getPlayer(a.get(1));

        		
    	}
    	else
    	{
    		if ( a.size() != 2 )
        	{
        		s.sendMessage("Target requires an argument: /fog target (target)");
        		return;
        	}
    		player = (Player) s;
    		target = getServer().getPlayer(a.get(0));
    	}
    	
    	if ( player == null )
    	{
        	s.sendMessage("FOG: Player not found. Aborting.");
    	}
    	else if ( target == null )
    	{
        	s.sendMessage("FOG: Target not found. Aborting.");
    	}
    	
    	player.setCompassTarget( target.getLocation());
        
    	return;
    }
   
}