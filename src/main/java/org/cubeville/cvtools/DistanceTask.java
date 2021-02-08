package org.cubeville.cvtools;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import org.cubeville.commons.utils.BlockUtils;
import org.cubeville.cvtools.CVTools;

public class DistanceTask {

    CVTools plugin;
    List<Player> playerList;
    BukkitTask task;
    boolean taskRunning;

    public DistanceTask(CVTools plugin) 
    {
        this.plugin = plugin;
        playerList = new ArrayList<>();
        playerList.clear();
        taskRunning = false;
    }

    public boolean TogglePLayer(Player player)
    {
        boolean isEnabled = false;
        if (playerList.remove(player))
            {
                if (playerList.isEmpty())
                    {
                        StopTask();
                        player.sendTitle("", "", 0, 10, 0);
                    }
            }
        else
            {
                try 
                    {
                        BlockUtils.getWESelection(player);

                        playerList.add(player);
                        StartTask();
                        isEnabled = true;
                    } 
                catch (Exception e) 
                    {
                        player.sendTitle("",  "You must make a region selection", 0, 200, 5);
                    }
            }
        return isEnabled;
    }

    private void StartTask()
    {
        if (!taskRunning)
            {
                Runnable runnable = new Runnable() {
                        public void run() {
                            ProcessTask();
                        }
                    };
                task = Bukkit.getServer().getScheduler().runTaskTimer(plugin, runnable, 1, 20);
                taskRunning = true;
            }
    }

    private void StopTask()
    {
        if (taskRunning)
            {
                task.cancel();
                taskRunning = false;
            }
    }

    public void ProcessTask()
    {
        int fadeIn = 0;
        int fadeOut = 0;
        int stay = 200;
        String title = "";
        String subtitle = "";

        ListIterator<Player> iter = playerList.listIterator();

        while(iter.hasNext())
            {
                Player player = iter.next();
                if (player.isOnline())
                    {

                        try
                            {
                                Location loc = player.getLocation();
                                Location locMin = BlockUtils.getWESelectionMin(player);
                                Location locMax = BlockUtils.getWESelectionMax(player);
                                locMax.add(1.0, 0.0, 1.0);
                                Location delta = loc.clone();

                                if (loc.getX() < locMin.getX())
                                    {
                                        delta.setX(locMin.getX());
                                    }
                                else if (loc.getX() > locMax.getX())
                                    {
                                        delta.setX(locMax.getX());
                                    }

                                if (loc.getZ() < locMin.getZ())
                                    {
                                        delta.setZ(locMin.getZ());
                                    }
                                else if (loc.getZ() > locMax.getZ())
                                    {
                                        delta.setZ(locMax.getZ());
                                    }

                                long dist = (long) Math.floor(delta.distance(loc));
                                String color = "§e";
                                if(dist < 40) color = "§c";
                                else if (dist >= 70) color = "§a";
                                subtitle = color + "Distance to selection is " + String.format("%d ",dist); 
                                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);

                                // player.spawnParticle(Particle.END_ROD, locMax, 100);
                            }
                        catch (IllegalArgumentException e)
                            {
                                // There is no valid WE selection
                                subtitle = "You must make a region selection";
                                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                            }
                    }
                else
                    {
                        // The player is not online
                        iter.remove();
                    }
            }
        if (playerList.isEmpty())
            {
                // No players in the list, halt the task.
                StopTask();
            }
    }

}
