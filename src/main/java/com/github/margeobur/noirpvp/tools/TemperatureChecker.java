package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * A runnable that gets scheduled to run every X seconds. TemperatureChecker adjusts a player's hunger level
 * based on the temperature of the area surrounding them.
 */
public class TemperatureChecker extends BukkitRunnable {

    private static final int SECONDS_PER_CHECK = 4;
    private static final int HUNGER_TICKS = 10;

    private Map<UUID, Integer> playerHungerRates = new HashMap<>();

    public void start() {
        int ticksDelay = 20 * SECONDS_PER_CHECK;
        this.runTaskTimer(NoirPVPPlugin.getInstance(), 0, ticksDelay);
        new HungerTick().runTaskTimer(NoirPVPPlugin.getInstance(), 0, HUNGER_TICKS);
    }

    @Override
    public void run() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        synchronized (playerHungerRates) {
            playerHungerRates.clear();
            for(Player player: onlinePlayers) {
                double temperature = player.getLocation().getBlock().getTemperature();

                double ticksUntilStarve;
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

                int weatherArmourFlags = 0; // each bit is a boolean set to true if the corresponding armour type is
                // present on the player

                if(player.getInventory().getBoots()!= null &&
                        player.getInventory().getBoots().getType().equals(Material.LEATHER_BOOTS)) {
                    ItemMeta bootsMeta = player.getInventory().getBoots().getItemMeta();
                    if(bootsMeta.hasLore()) {
                        List<String> lore = bootsMeta.getLore();
                        if(lore.size() > 0 &&
                                (lore.get(0).equalsIgnoreCase("protects against the heat."))) {
                            weatherArmourFlags |= 0b00000001;
                        } else if(lore.get(0).equalsIgnoreCase("protects against the cold.")) {
                            weatherArmourFlags |= 0b00010000;
                        }
                    }
                }

                if(player.getInventory().getLeggings()!= null &&
                    player.getInventory().getLeggings().getType().equals(Material.LEATHER_LEGGINGS)) {
                    ItemMeta leggingsMeta = player.getInventory().getLeggings().getItemMeta();
                    if(leggingsMeta.hasLore()) {
                        List<String> lore = leggingsMeta.getLore();
                        if(lore.size() > 0 &&
                                (lore.get(0).equalsIgnoreCase("protects against the heat."))) {
                            weatherArmourFlags |= 0b00000010;
                        } else if(lore.get(0).equalsIgnoreCase("protects against the cold.")) {
                            weatherArmourFlags |= 0b00100000;
                        }
                    }
                }

                if(player.getInventory().getChestplate()!= null &&
                        player.getInventory().getChestplate().getType().equals(Material.LEATHER_CHESTPLATE)) {
                    ItemMeta chestPlateMeta = player.getInventory().getChestplate().getItemMeta();
                    if(chestPlateMeta.hasLore()) {
                        List<String> lore = chestPlateMeta.getLore();
                        if(lore.size() > 0 &&
                                (lore.get(0).equalsIgnoreCase("protects against the heat."))) {
                            weatherArmourFlags |= 0b00000100;
                        } else if(lore.get(0).equalsIgnoreCase("protects against the cold.")) {
                            weatherArmourFlags |= 0b01000000;
                        }
                    }
                }

                if(player.getInventory().getHelmet()!= null &&
                        player.getInventory().getHelmet().getType().equals(Material.LEATHER_HELMET)) {
                    ItemMeta hatMeta = player.getInventory().getHelmet().getItemMeta();
                    if(hatMeta.hasLore()) {
                        List<String> lore = hatMeta.getLore();
                        if(lore.size() > 0 &&
                                (lore.get(0).equalsIgnoreCase("protects against the heat."))) {
                            weatherArmourFlags |= 0b00001000;
                        } else if(lore.get(0).equalsIgnoreCase("protects against the cold.")) {
                            weatherArmourFlags |= 0b10000000;
                        }
                    }
                }

                if(weatherArmourFlags == 240) {
                    if(temperature <= 0.8) {
                        ticksUntilStarve = 0;
                    }
                } else if(weatherArmourFlags == 15) {
                    if(temperature >= 0.8) {
                        ticksUntilStarve = 0;
                    }
                }

                if(player.getGameMode().equals(GameMode.CREATIVE) || player.getGameMode().equals(GameMode.CREATIVE)) {
                    ticksUntilStarve = 0;
                }

                int ticksPerHungerIncrease = (int) ticksUntilStarve / 20;
                playerHungerRates.put(player.getUniqueId(), ticksPerHungerIncrease);
            }
        }
    }

    private class HungerTick extends BukkitRunnable {

        private Map<UUID, Integer> hungerAmounts = new HashMap<>();

        @Override
        public void run() {
            List<UUID> toRemove = new ArrayList<>();
            synchronized (playerHungerRates) {
                for(UUID playerID: playerHungerRates.keySet()) {
                    Player player = Bukkit.getPlayer(playerID);
                    if(player == null || !player.isOnline()) {
                        toRemove.add(playerID);
                        continue;
                    }
                    if(hungerAmounts.containsKey(playerID)) {
                        int currentTicksOfHunger = hungerAmounts.get(playerID);
                        int ticksPerHungerIncrease = playerHungerRates.get(playerID);
                        if(ticksPerHungerIncrease == 0) {
                            continue;
                        }
                        if(currentTicksOfHunger >= ticksPerHungerIncrease) {
                            currentTicksOfHunger = 0;
                            if(player.getFoodLevel() > 0) {
                                player.setFoodLevel(player.getFoodLevel() - 1);
                            }
                        } else {
                            currentTicksOfHunger += HUNGER_TICKS;
                        }
                        hungerAmounts.put(playerID, currentTicksOfHunger);
                    } else {
                        hungerAmounts.put(playerID, 0);
                    }
                }
                for(UUID playerID: toRemove) {
                    playerHungerRates.remove(playerID);
                }
            }
        }
    }

}
