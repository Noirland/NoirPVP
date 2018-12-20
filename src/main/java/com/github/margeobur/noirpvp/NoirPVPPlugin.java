package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.commands.AdminCommands;
import com.github.margeobur.noirpvp.commands.JudicialCommands;
import com.github.margeobur.noirpvp.commands.OverrideCommands;
import com.github.margeobur.noirpvp.listeners.*;
import com.github.margeobur.noirpvp.tools.Recipes;
import com.github.margeobur.noirpvp.tools.TemperatureChecker;
import com.github.margeobur.noirpvp.trials.JailCell;
import com.github.margeobur.noirpvp.trials.TrialEventListener;
import com.github.margeobur.noirpvp.trials.TrialManager;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * The NoirPVP plugin class. Here you'll find command and event registration, initialisation of
 * database and config, etc...
 */
public class NoirPVPPlugin extends JavaPlugin {

    private static NoirPVPPlugin instance;
    private TemperatureChecker tempChecker;

    public static NoirPVPPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        NoirPVPConfig.getInstance().initConfig();
        GriefPrevention gp = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");

        // ---------- listeners ----------
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(gp), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(), this);
        getServer().getPluginManager().registerEvents(new TrialEventListener(), this);
        getServer().getPluginManager().registerEvents(new JailedPlayerListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCraftListener(), this);

        JudicialCommands commandHandler = new JudicialCommands();
        getCommand("innocent").setExecutor(commandHandler);
        getCommand("guilty").setExecutor(commandHandler);
        getCommand("crime").setExecutor(commandHandler);
        AdminCommands adminCH = new AdminCommands();
        getCommand("njail").setExecutor(adminCH);
        getCommand("nkick").setExecutor(adminCH);
        getCommand("nban").setExecutor(adminCH);
        getCommand("setdock").setExecutor(adminCH);
        getCommand("setrelease").setExecutor(adminCH);
        getCommand("nunjail").setExecutor(adminCH);
        //this.getCommand("jail").setExecutor(commandHandler);
        getCommand("noirpvp").setExecutor(new OverrideCommands());

        if(TrialManager.getInstance() == null) {
            getLogger().log(Level.SEVERE, "Could not resume trials");
        }

        Recipes.addRecipes();

        tempChecker = new TemperatureChecker();
        tempChecker.start();
    }

    @Override
    public void onDisable() {
        JailCell.saveCells();
        TrialManager.getInstance().pauseAllTrials();
        PVPPlayer.pauseAllCooldowns();
        PVPPlayer.saveAllPVPData();
    }
}
