package com.github.margeobur.noirpvp;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles the storage of NoirPVP data. Data is stored on the disk in YAML.
 * The methods {@link FSDatabase#reloadDatabase()}, {@link FSDatabase#getDatabase()} and {@link FSDatabase#saveDatabase()}
 * were all taken from https://bukkit.gamepedia.com/Configuration_API_Reference#Advanced_Topics
 */
public class FSDatabase {

    private static final String dbFilename = "pvpPlayers.yml";
    private static final int SAVE_PERIOD = 5;

    private static FSDatabase instance;
    private FileConfiguration database = null;
    private File databaseFile = null;
    private SaveScheduler scheduler;

    public static FSDatabase getInstance() {
        if(instance == null) {
            instance = new FSDatabase();
        }
        return instance;
    }

    private FSDatabase() {
        ConfigurationSerialization.registerClass(PVPPlayer.class, "PVPPlayer");
        reloadDatabase();

        scheduler = new SaveScheduler();
        int startDelayTicks = 5 * 60 * 20;
        int savePeriodTicks = SAVE_PERIOD * 60 * 20;
        scheduler.runTaskTimer(NoirPVPPlugin.getPlugin(), startDelayTicks, savePeriodTicks);
    }

    public PVPPlayer getPlayerPVPbyUUID(UUID playerID) {
        if(database.contains(playerID.toString())) {
            return (PVPPlayer) database.get(playerID.toString());
        }
        return null;
    }

    public void savePlayerPVP(PVPPlayer player) {
        database.set(player.getID().toString(), player);
    }

    public Map<UUID, Integer> getJailShortlist() {
        if(database.contains("jail-shortlist")) {
            Map<UUID, Integer> convictIDs = new HashMap<>();
            Map<String, Object> convictIDStrs =
                    (Map<String, Object>) database.getConfigurationSection("jail-shortlist").getValues(false);
            Set<String> idStrs = convictIDStrs.keySet();
            for(String idStr: idStrs) {
//                System.out.println("convict found: " + idStr);
                convictIDs.put(UUID.fromString(idStr), (Integer) convictIDStrs.get(idStr));
            }
            return convictIDs;
        } else {
            return new HashMap<>();
        }
    }

    public void saveJailShortlist(Map<UUID, Integer> convictIDs) {
        Map<String, Integer> convictIDStrs = new HashMap<>();
        Set<UUID> playerIDs = convictIDs.keySet();
        for(UUID convictID: playerIDs) {
            convictIDStrs.put(convictID.toString(), convictIDs.get(convictID));
        }
        database.createSection("jail-shortlist", convictIDStrs);
    }

    public void reloadDatabase() {
        if (databaseFile == null) {
            databaseFile = new File(NoirPVPPlugin.getPlugin().getDataFolder(), dbFilename);
            if(!databaseFile.exists()) {
                try {
                    databaseFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        database = YamlConfiguration.loadConfiguration(databaseFile);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
            InputStream inStream = NoirPVPPlugin.getPlugin()
                    .getResource("pvpPlayers.yml");
            if(inStream == null) {
                NoirPVPPlugin.getPlugin().getLogger().log(Level.SEVERE, "Could not retrieve PVP player database");
                return;
            }
            defConfigStream = new InputStreamReader(inStream,"UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            database.setDefaults(defConfig);
        }
    }

    public FileConfiguration getDatabase() {
        if (database == null) {
            reloadDatabase();
        }
        return database;
    }

    public void saveDatabase() {
        if (database == null || databaseFile == null) {
            return;
        }
        try {
            getDatabase().save(databaseFile);
        } catch (IOException ex) {
            NoirPVPPlugin.getPlugin().getLogger().log(Level.SEVERE, "Could not save config to " + databaseFile, ex);
        }
    }

    private class SaveScheduler extends BukkitRunnable {

        @Override
        public void run() {
            saveDatabase();
        }
    }
}
