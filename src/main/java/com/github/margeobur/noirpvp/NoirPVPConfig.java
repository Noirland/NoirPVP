package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.trials.JailCell;
import com.github.margeobur.noirpvp.trials.JailTrial;
import com.github.margeobur.noirpvp.trials.Trial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.List;
import java.util.logging.Level;

/**
 * Holds all the logic for retrieving the plugin config and store
 */
public class NoirPVPConfig {

    public static String CLAIM_DENY_MESSAGE;
    public static String PROTECTED_DENY_MESSAGE;
    public static String SELF_COOLDOWN_DENY_MESSAGE;
    public static String PLAYER_PROTECTION_END = ChatColor.RED + "You are no longer protected from PVP";
    public static String PLAYER_DOUBLE_END = ChatColor.RED + "You are no longer protected from PVP";

    public static int PROTECTION_DURATION;
    public static int COOLDOWN_DURATION;
    public static int DOUBLE_PROTECTION_DURATION;
    public static int CRIME_MARK_MULTIPLIER = 10;
    public static int TRIAL_DURATION;
    public static int ADMIN_JAIL_TIME;

    private Location courtDock;
    private Location releasePoint;

    private static NoirPVPConfig instance;

    public static NoirPVPConfig getInstance() {
        if(instance == null) {
            instance = new NoirPVPConfig();
        }
        return instance;
    }

    public void initConfig() {
        NoirPVPPlugin.getInstance().saveDefaultConfig();
        ConfigurationSerialization.registerClass(JailCell.class, "JailCell");
        FileConfiguration config = NoirPVPPlugin.getInstance().getConfig();

        PROTECTION_DURATION = config.getInt("first-protection-duration");
        COOLDOWN_DURATION = config.getInt("cooldown-duration");
        DOUBLE_PROTECTION_DURATION = config.getInt("second-protection-duration");
        TRIAL_DURATION = config.getInt("trial-duration");

        CLAIM_DENY_MESSAGE = config.getString("claim-deny-message");
        PROTECTED_DENY_MESSAGE = config.getString("protected-deny-message");
        SELF_COOLDOWN_DENY_MESSAGE = config.getString("self-protected-deny-message");

        CRIME_MARK_MULTIPLIER = config.getInt("crime-mark-multiplier");
        ADMIN_JAIL_TIME = config.getInt("admin-init-jailtime");

        if(config.contains("jail-locations")) {
            try {
                List<JailCell> cells = (List<JailCell>) config.getList("jail-locations");
                JailCell.setCells(cells);
            } catch (ClassCastException e) {
                NoirPVPPlugin.getInstance().getLogger().log(Level.SEVERE, "Could not get list of Jail cells");
            }
        }

        if(config.contains("court-dock")) {
            courtDock = (Location) config.get("court-dock");
        }
        if(config.contains("release-point")) {
            releasePoint = (Location) config.get("release-point");
        }
    }

    public void addJailCell(Location cellLocation) {
        FileConfiguration config = NoirPVPPlugin.getInstance().getConfig();
        JailCell.addNewCell(cellLocation);

        config.set("jail-locations", JailCell.getCells());
        NoirPVPPlugin.getInstance().saveConfig();
    }

    public void saveCells() {
        FileConfiguration config = NoirPVPPlugin.getInstance().getConfig();

        config.set("jail-locations", JailCell.getCells());
        NoirPVPPlugin.getInstance().saveConfig();
    }

    public Location getCourtDock() {
        if(courtDock == null) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        } else {
            return courtDock;
        }
    }

    public void setCourtDock(Location courtDock) {
        this.courtDock = courtDock;
        NoirPVPPlugin.getInstance().getConfig().set("court-dock", courtDock);
        NoirPVPPlugin.getInstance().saveConfig();
    }

    public Location getReleasePoint() {
        if(releasePoint == null) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        } else {
            return releasePoint;
        }
    }

    public void setReleasePoint(Location releasePoint) {
        this.releasePoint = releasePoint;
        NoirPVPPlugin.getInstance().getConfig().set("release-point", releasePoint);
        NoirPVPPlugin.getInstance().saveConfig();
    }
}
