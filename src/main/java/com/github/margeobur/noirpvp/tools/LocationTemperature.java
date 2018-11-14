package com.github.margeobur.noirpvp.tools;

import org.bukkit.Location;
import org.bukkit.block.Biome;

/**
 * Represents the temperature at a location at a given time.
 * The temperature is based off the biome, weather, time of day and elevation.
 * We use it to work out a starve multiplier to apply to players.
 *
 * A LocationTemperature object is intended to be used at the instant it is created -
 * since the temperature is dependant on time and exact elevation, the idea is that
 * you create an LocationTemperature with a player's current location and it provides
 * all the interesting information (e.g. whether the the area is freezing or not)
 */
public class LocationTemperature {

    private double temp;

    public LocationTemperature(Location location) {
        Biome biome = location.getBlock().getBiome();
        temp = 0;
    }

}
