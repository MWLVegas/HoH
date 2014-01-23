package com.ivalicemud.hoh;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.ivalicemud.hoh.HoH;
import com.ivalicemud.hoh.HoH.Group;

public class HoHListeners implements Listener
{
	public final HoH plugin;

	public HoHListeners(HoH plugin) {
		super();
		this.plugin = plugin;
	}
	 
    public void onEnable()
    {
    	plugin.debug("Listeners enabled!");
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onCharacterMove(PlayerMoveEvent event )
    {
    	if ( !event.getPlayer().hasMetadata("hoh.group") )
    		return;

		List<Entity> l = event.getPlayer().getNearbyEntities(1, 1, 1);
		if (l.size() != 0) {
			plugin.debug("Nearby entities!");
			Iterator<Entity> e = l.iterator();
			while (e.hasNext()) {
				Entity a = e.next();
				double dist = a.getLocation().distance(event.getTo());
				plugin.debug(dist);
				if (dist < 0.5) {
					//plugin.msg(event.getPlayer(), "That spot is too crowded!");

					event.getPlayer().teleport(event.getFrom());
					event.setCancelled(true);
					return;
				}
			}
		}
    	
		Group g = plugin.findGroup(event.getPlayer());

    	if ( !plugin.safeHouse(event.getPlayer(), event.getTo() ) )
    	{
    		if ( g.p1 == event.getPlayer() )
        	{
        		g.p1count = 0;
        	}
    		
    		if ( event.getPlayer().hasMetadata("hoh.safehouse"))
    		{
    			event.getPlayer().removeMetadata("hoh.safehouse", plugin);
    			event.getPlayer().removePotionEffect(PotionEffectType.WITHER);
    			plugin.msg(event.getPlayer(),"Leaving the safehouse!");
    		
				final Player p = event.getPlayer();
		    	p.setMetadata("hoh.hassafehousetimer", new FixedMetadataValue(plugin,true));
				plugin.getServer().getScheduler()
						.scheduleSyncDelayedTask(plugin, new Runnable() {
							public void run() {
								p.removeMetadata("hoh.hassafehousetimer", plugin);
								plugin.msg(p,"You can once again enter a safehouse.");
							}
						}, plugin.getConfig().getInt("HoH.timing.safehouseDelay") *20L);
				//event.setCancelled(true);
    			return;
    		}
    		return;
    	}
    	else // you ARE in a safehouse
    	{
    	
    	if (event.getPlayer() == g.p2)
    	{
    		if ( event.getPlayer().hasMetadata("hoh.safehouse"))
    			return;
    		
    		if ( event.getPlayer().hasMetadata("hoh.hassafehousetimer"))
    		{
    			plugin.msg(event.getPlayer(),"You cannot enter another safehouse yet!");
        		g.p2.teleport(event.getFrom() );
        		event.setCancelled(true);
        		return;
    		}
    		
         	PotionEffectType pt = PotionEffectType.WITHER;
         	int power = plugin.getConfig().getInt("HoH.safeHouseWitherStrength") -1;
         	int duration = 9999;
            event.getPlayer().addPotionEffect(new PotionEffect(pt,duration,power));
            event.getPlayer().damage(2);
    		plugin.msg(g.p2,"You are entering a safehouse!");
    		event.getPlayer().setMetadata("hoh.safehouse", new FixedMetadataValue(plugin,true));
    		return;
    	}
    	else if ( g.p1 == event.getPlayer() )
    	{
    		plugin.msg(g.p1,"You cannot enter a safehouse!");
    		g.p1count += 1;
    		event.setCancelled(true);
    		
    		if ( g.p1count >= 15 )
    		{
    			plugin.msg(g.p1,"You appear to be stuck in a safehouse! Resetting your position!");
    			g.p1count = 0;
    			plugin.hunterTP(g.p1,g);
    			return;
    		}
    		g.p1.teleport(event.getFrom() );
    		return;
    	}
    	}
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event)
    {
    	if ( event.getCause() != DamageCause.WITHER && event.getEntity().hasMetadata("hoh.safehouse") )
    	{
    		plugin.debug("Unknown Damage: " + event.getCause() );
    		event.setCancelled(true);
    		return;
    	}
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {

    	if ( !(event.getEntity() instanceof Player)  || !(event.getDamager() instanceof Player ) )
    	{
    		plugin.debug("No Entity Player, or No Player Damager");
    		return;
    	}
    	
    	Player target = (Player) event.getEntity();
    	Player attacker = (Player) event.getDamager();
    	
    	if ( attacker.hasMetadata("hoh.safehouse"))
    	{
    		plugin.msg(attacker,"You cannot attack from inside the safe house!");
    		event.setCancelled(true);
    		return;
    	}
    	
    	if ( target.hasMetadata("hoh.safehouse"))
    	{
    		plugin.msg(attacker,"You cannot attack them while they are inside the safe house!");
    		event.setCancelled(true);
    		return;
    	}
    	
    	
    	if ( target.hasMetadata("hoh.group") || attacker.hasMetadata("hoh.group"))
    	{
    	
    		String targ = "";
    		String att = "";
    		
    		if ( target.hasMetadata("hoh.group"))
    			targ = ((MetadataValue) target.getMetadata("hoh.group").get(0)).asString();
    			
    		
    		if ( attacker.hasMetadata("hoh.group"))
    			att = ((MetadataValue) attacker.getMetadata("hoh.group").get(0)).asString();
    		
    		if ( targ.equals(att) )
    		{
    			//if ( (target.getHealth() - event.getDamage() ) <= 0 )
    			//	plugin.debug("death!!");
    			
    			return;
    		}
    		
    		plugin.msg(attacker,target.getName() + " is not your HoH partner!");
    		event.setCancelled(true);
    		return;
    	}
    }
    
    @EventHandler
    public void onInteract( PlayerInteractEvent event )
    {
    	
    	if ( !event.getPlayer().hasMetadata("hoh.group"))
    		return;

		final Player p = event.getPlayer();

		if ( p.getItemInHand().getType() == Material.COMPASS)
		{
			Group g = plugin.findGroup(p);

			if (g.p2 == p) 
			{
				if ( plugin.safeHouse(p, p.getLocation() ) )
				{
					if ( p.hasMetadata("hoh.teleport") )
					{
						plugin.msg(p,"You cannot teleport yet!");
						event.setCancelled(true);
						return;
					}
					
					Random generator = new Random();
				    Location loc = null;
					int num = 0;
					
				    while (num < 30) {
				    	int randomNum = generator.nextInt(plugin.safeHouses.size());
				    	loc = plugin.safeHouses.get(randomNum);
				    	num++;
				    	if ( p.getLocation().distance(loc) <= 5 )
				    	{
				    		plugin.debug("Same safehouse!");
				    		loc = null;
				    	}
				    	break;
					}
				    
				    if ( loc == null || num >= 30)
				    {
				    	plugin.msg(p, "A new safehouse was not located! Try again!");
				    	return;
				    }
				    
					plugin.debug("Teleporting!");
					p.teleport(loc);
			    	p.setMetadata("hoh.teleport", new FixedMetadataValue(plugin,true));
					
					plugin.getServer().getScheduler()
							.scheduleSyncDelayedTask(plugin, new Runnable() {
								public void run() {
									p.removeMetadata("hoh.teleport", plugin);
								}
							}, plugin.getConfig().getInt("HoH.timing.teleportTimeout") *20);
					event.setCancelled(true);

					return;
				}
			
			}
		}
    }
    
    @EventHandler
    public void onPlayerCommandPreprocess( org.bukkit.event.player.PlayerCommandPreprocessEvent event)
    {
			Player p = event.getPlayer();
			if (p.hasMetadata("hoh.group")) {
		    	String cmd = event.getMessage().length() >= 1 ? event.getMessage().substring(1) : "";
		    	if ( cmd.contains(" ")) 
		    	{
		    		String cmds[] = cmd.split(" ");
		    		cmd = cmds[0];
		    	}

		    	
				if (!plugin.allowedCommand(cmd.toLowerCase(), event.getPlayer() )) {
					p.sendMessage("That command is not allowed while playing HoH!");
					event.setCancelled(true);
				}
			}
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
    	if ( !(event.getEntity() instanceof Player) )
    		return;

    	if ( !event.getEntity().hasMetadata("hoh.group"))
    		return;

		plugin.debug("HoH death!");
		Player p = (Player) event.getEntity();
		Group g = plugin.findGroup(p);

		if (g.p2 == p) // Hunted was Killed!
		{
	
			for (Iterator<ItemStack> iter = event.getDrops().listIterator(); iter
					.hasNext();) {
				ItemStack item = iter.next();
				if (item == null)
					continue;
				iter.remove();
			}
			plugin.gmsg(g, "The Hunted has been killed!");
			
			p.setHealth(20);
			p.setFoodLevel(20);
			event.setDeathMessage("");
			event.setKeepLevel(true);
			plugin.huntedTP(p);
			plugin.endRound(g, 0);

		}
		else if ( g.p1 == p ) // Hunter died
		{
			
			for (Iterator<ItemStack> iter = event.getDrops().listIterator(); iter
					.hasNext();) {
				ItemStack item = iter.next();
				if (item == null)
					continue;
				iter.remove();
			}
			
			p.setHealth(20);
			p.setFoodLevel(20);
			event.setDeathMessage("");
			event.setKeepLevel(true);
			
			plugin.hunterTP(p,g);
		}

    }
    
    @EventHandler
    public void onItemDrop (PlayerDropItemEvent event) {
    	
    	if ( event.getPlayer().hasMetadata("hoh.group"))
    	{
    		plugin.msg(event.getPlayer(),"You cannot drop that here!");
    		event.setCancelled(true);
    		return;
    	}
    	
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event ) {
       
    	plugin.recoverInventory(event.getPlayer(), true);
    	

    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

    	if ( event.getPlayer().hasMetadata("hoh.waiting"))
    	{
    		event.getPlayer().removeMetadata("hoh.waiting", plugin);

    		Iterator<Player> it = plugin.ready.iterator();
        	while ( it.hasNext() )
        	{
        		Player p = it.next();
        		if ( p == event.getPlayer() )
        		{
        			it.remove();
        			break;
        		}
       		}
    	}
    	if ( event.getPlayer().hasMetadata("hoh.group"))
    	{
    		Group group = plugin.findGroup(event.getPlayer());
    		if ( group != null )
    			plugin.endGame(group);
    		
    		plugin.recoverInventory(event.getPlayer(), false);
    	}
    
    }

}