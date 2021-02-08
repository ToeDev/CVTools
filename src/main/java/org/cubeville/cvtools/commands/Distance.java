package org.cubeville.cvtools.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Particle;
import org.bukkit.plugin.PluginBase;

import com.sk89q.worldedit.regions.Region;

import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandResponse;
import org.cubeville.commons.utils.BlockUtils;
import org.cubeville.cvtools.DistanceTask;

public class Distance extends Command {
	
	private DistanceTask distTask;

	public Distance(DistanceTask distTask) {
		super("distance");
		this.distTask = distTask;
		setSilentConsole();
	}
	@Override
	public CommandResponse execute(Player player, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters)
	{
		String retVal;
		
		if (distTask.TogglePLayer(player))
		{
			retVal = "Distance measurement enabled.";
		}
		else
		{
			retVal = "Distance measurement disabled.";
		}

		return new CommandResponse(retVal);
	}

}
