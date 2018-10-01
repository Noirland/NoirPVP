package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.Listeners.PlayerServerListener;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.margeobur.noirpvp.Listeners.PlayerCombatListener;

import java.util.ArrayList;

/**
 * The NoirPVP plugin class. Here you'll find command and event registration, initialisation of
 * database and config, etc...
 */
public class NoirPVPPlugin extends JavaPlugin {

    private ArrayList<PVPPlayer> _players = new ArrayList<PVPPlayer>();

    @Override
    public void onEnable() {
        getLogger().info("onEnable is called!");

        GriefPrevention gp = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");
        getServer().getPluginManager().registerEvents(new PlayerServerListener(_players), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(gp, _players), this);
    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
    }
}
