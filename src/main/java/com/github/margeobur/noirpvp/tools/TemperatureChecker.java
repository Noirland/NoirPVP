package com.github.margeobur.noirpvp.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * A runnable that gets scheduled to run every X seconds. TemperatureChecker adjusts a player's hunger level
 * based on the temperature of the area surrounding them.
 */
public class TemperatureChecker extends BukkitRunnable {

    @Override
    public void run() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
    }
}
