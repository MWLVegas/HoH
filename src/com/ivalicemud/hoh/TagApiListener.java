package com.ivalicemud.hoh;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;

import org.kitteh.tag.PlayerReceiveNameTagEvent;

import com.ivalicemud.hoh.HoH;

public class TagApiListener implements Listener
{

	

	public TagApiListener(final HoH plugin)
	{
		super();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void PlayerNameTag(final PlayerReceiveNameTagEvent event)
	{
		Player target = event.getNamedPlayer();
		Player viewer = event.getPlayer();
		String targ = "";
		String view = "";
		if ( target.hasMetadata("hoh.group") )
		{
			
    			targ = ((MetadataValue) target.getMetadata("hoh.group").get(0)).asString();
    			
    		
    		if ( viewer.hasMetadata("hoh.group"))
    			view = ((MetadataValue) viewer.getMetadata("hoh.group").get(0)).asString();
    		
    		if ( targ.equals(view) )
    			event.setTag(ChatColor.RED + target.getName() );
    		else
    			event.setTag(ChatColor.GREEN + target.getName() );
			
		}
		
	}
}