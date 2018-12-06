package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.tools.TimeTracker;
import com.github.margeobur.noirpvp.trials.JailTrial;
import com.github.margeobur.noirpvp.trials.Trial;
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
        ConfigurationSerialization.registerClass(Trial.class, "Trial");
        ConfigurationSerialization.registerClass(JailTrial.class, "JailTrial");
        ConfigurationSerialization.registerClass(TimeTracker.class, "Timer");
        reloadDatabase();

        scheduler = new SaveScheduler();
        int startDelayTicks = 5 * 60 * 20;
        int savePeriodTicks = SAVE_PERIOD * 60 * 20;
        scheduler.runTaskTimer(NoirPVPPlugin.getInstance(), startDelayTicks, savePeriodTicks);
    }

    public PVPPlayer getPlayerPVPbyUUID(UUID playerID) {
        if(database.contains(playerID.toString())) {
            return (PVPPlayer) database.get(playerID.toString());
        }
        return null;
    }

    public void savePlayerPVP(PVPPlayer player) {
        database.set(player.getID().toString(), player);
        saveDatabase();
    }

    public List<PVPPlayer> getAllPlayers() {
        List<PVPPlayer> players = new ArrayList<>();
        Set<String> keys = database.getKeys(false);
        for(String key: keys) {
            try {
                UUID.fromString(key);
            } catch(IllegalArgumentException e) {
                continue; // if the key isn't a UUID, just skip it
            }
            PVPPlayer pvpPlayer = (PVPPlayer) database.get(key);
            players.add(pvpPlayer);
        }
        return players;
    }

    public void saveJailTrials(List<JailTrial> trials, String path) {
        database.set(path, trials);
        saveDatabase();
    }

    public List<JailTrial> getJailTrials(String path) {
        List<JailTrial> trials = (List<JailTrial>) database.get(path);
        database.set(path, null);
        return trials;
    }

    public void saveTrials(List<Trial> trials, String path) {
        database.set(path, trials);
        saveDatabase();
    }

    public List<Trial> getTrials(String path) {
        List<Trial> trials = (List<Trial>) database.get(path);
        database.set(path, null);
        return trials;
    }

    public void reloadDatabase() {
        if (databaseFile == null) {
            databaseFile = new File(NoirPVPPlugin.getInstance().getDataFolder(), dbFilename);
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
            InputStream inStream = NoirPVPPlugin.getInstance()
                    .getResource("pvpPlayers.yml");
            if(inStream == null) {
                NoirPVPPlugin.getInstance().getLogger().log(Level.SEVERE, "Could not retrieve PVP player database");
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

    private FileConfiguration getDatabase() {
        if (database == null) {
            reloadDatabase();
        }
        return database;
    }

    private void saveDatabase() {
        if (database == null || databaseFile == null) {
            return;
        }
        try {
            getDatabase().save(databaseFile);
        } catch (IOException ex) {
            NoirPVPPlugin.getInstance().getLogger().log(Level.SEVERE, "Could not save config to " + databaseFile, ex);
        }
    }

    private class SaveScheduler extends BukkitRunnable {

        @Override
        public void run() {
            PVPPlayer.saveAllPVPData();
            saveDatabase();
        }
    }
}
