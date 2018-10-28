package com.github.margeobur.noirpvp;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Holds all the logic for retrieving the plugin config and store
 */
public class NoirPVPConfig {

    public static String CLAIM_DENY_MESSAGE;
    public static String PROTECTED_DENY_MESSAGE;
    public static String SELF_COOLDOWN_DENY_MESSAGE;

    public static int PROTECTION_DURATION;
    public static int COOLDOWN_DURATION;
    public static int DOUBLE_PROTECTION_DURATION;

    public static int CRIME_MARK_MULTIPLIER = 10;

    public static void initConfig() {
        NoirPVPPlugin.getPlugin().saveDefaultConfig();
        FileConfiguration config = NoirPVPPlugin.getPlugin().getConfig();

        PROTECTION_DURATION = config.getInt("first-protection-duration");
        COOLDOWN_DURATION = config.getInt("cooldown-duration");
        DOUBLE_PROTECTION_DURATION = config.getInt("second-protection-duration");

        CLAIM_DENY_MESSAGE = config.getString("claim-deny-message");
        PROTECTED_DENY_MESSAGE = config.getString("protected-deny-message");
        SELF_COOLDOWN_DENY_MESSAGE = config.getString("self-protected-deny-message");

        CRIME_MARK_MULTIPLIER = config.getInt("crime-mark-multiplier");
    }


    public static void main(String[] args) {
        System.out.println(CLAIM_DENY_MESSAGE);
    }
}
