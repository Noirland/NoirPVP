package com.github.margeobur.noirpvp.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A runnable that gets scheduled to run every X seconds. TemperatureChecker adjusts a player's hunger level
 * based on the temperature of the area surrounding them.
 */
public class TemperatureChecker extends BukkitRunnable {

    private Map<UUID, Boolean> playerHungerRates = new HashMap<>();

    @Override
    public void run() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for(Player player: onlinePlayers) {
            double temperature = player.getLocation().getBlock().getTemperature();

        }
    }
}
