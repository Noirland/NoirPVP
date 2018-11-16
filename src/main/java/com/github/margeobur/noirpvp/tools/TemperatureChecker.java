package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
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

    private Map<UUID, Integer> playerHungerRates = new HashMap<>();

    public void start() {
        int ticksDelay = 20 * 4;
        this.runTaskTimer(NoirPVPPlugin.getInstance(), 0, ticksDelay);
    }

    @Override
    public void run() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for(Player player: onlinePlayers) {
            double temperature = player.getLocation().getBlock().getTemperature();

            double ticksUntilStarve = 24000;
            if(temperature >= 2) {
                ticksUntilStarve = 200;
            } else if(temperature >= 1.1) {
                ticksUntilStarve = 5714.285714 - (temperature - 1.1)/(2 - 1.1) * (5714.285714 - 200);
            } else if(temperature >= 0.95) {
                ticksUntilStarve = 23529.41176 - (temperature - 0.95)/(1.1 - 0.95) * (23529.41176 - 5714.285714);
            } else if(temperature >= 0.5) {
                ticksUntilStarve = 24000.096 - (temperature - 0.5)/(0.95 - 0.5) * (24000.096 - 23529.41176);
            } else if(temperature >= 0.3) {
                ticksUntilStarve = 20000 + (temperature - 0.3)/(0.5 - 0.3) * (24000.096 - 20000);
            } else if(temperature >= 0.2) {
                ticksUntilStarve = 8000 +  (temperature - 0.2)/(0.3 - 0.2) * (20000 - 8000);
            } else if(temperature >= 0.05) {
                ticksUntilStarve = 4000 + (temperature - 0.05)/(0.2 - 0.05) * (8000 - 4000);
            } else if(temperature >= -0.5) {
                ticksUntilStarve = 200 + (temperature + 0.5)/(0.05 + 0.5) * (4000 - 200);
            } else {
                ticksUntilStarve = 200;
            }

            playerHungerRates.put(player.getUniqueId(), (int) ticksUntilStarve);
        }
    }
}
