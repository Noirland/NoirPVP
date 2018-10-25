package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.listeners.PlayerCommandListener;
import com.github.margeobur.noirpvp.listeners.PlayerDeathListener;
import com.github.margeobur.noirpvp.listeners.PlayerServerListener;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.margeobur.noirpvp.listeners.PlayerCombatListener;

/**
 * The NoirPVP plugin class. Here you'll find command and event registration, initialisation of
 * database and config, etc...
 */
public class NoirPVPPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        GriefPrevention gp = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");

        // ---------- listeners ----------
        getServer().getPluginManager().registerEvents(new PlayerServerListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(gp), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(), this);
    }
    @Override
    public void onDisable() { }
}
