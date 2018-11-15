package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.tools.DelayedMessager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.logging.Level;

/**
 * The purpose of this class is to respond to trial events and perform all the necessary actions.
 * It broadcasts information about trials to the server and teleports guilty players where they need to be.
 */
public class TrialEventListener implements Listener {

    private static final String BROADCAST_PREFIX =
            ChatColor.GOLD + "[" + ChatColor.RED  + "Trial Announcement" + ChatColor.GOLD + "] " + ChatColor.RESET;
    private static final int NUM_NAMES_DISPLAYED = 4;

    @EventHandler
    public void onTrialChange(TrialEvent event) {
        Trial trial = event.getTrial();
        boolean isJailTrial = false;
        JailTrial jTrial = null;
        if(trial instanceof JailTrial) {
            isJailTrial = true;
            jTrial = (JailTrial) trial;
        }
        Player defendant = trial.getDefendant().getPlayer();
        if(defendant == null) {
            return;
        }

        if(event.getType().equals(TrialEvent.TrialEventType.INIT)) {
            defendant.sendMessage("You have been put on trial. " +
                    "You may not leave until your trial has seen completion.");

            Location cellLocation = JailCell.getVacantCellFor(defendant.getUniqueId());
            if(cellLocation == null) {
                NoirPVPPlugin.getInstance().getLogger().log(Level.WARNING,
                        "Player was sent to holding but no jail is set");
                cellLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            defendant.teleport(cellLocation);

        } else if(event.getType().equals(TrialEvent.TrialEventType.START)) {
            String broadcast;
            if(isJailTrial) {
                broadcast = buildJailMessage(jTrial);
            } else {
                broadcast = buildTrialMessage(trial);
            }
            Bukkit.getServer().broadcastMessage(BROADCAST_PREFIX + broadcast);

            JailCell.releasePlayer(defendant.getUniqueId());
            defendant.teleport(NoirPVPConfig.getInstance().getCourtDock());

        } else if(event.getType().equals(TrialEvent.TrialEventType.FINISH)) {
            if(!trial.getIsGuiltyVerdict()) {
                if(trial == null) {
                    NoirPVPPlugin.getInstance().getLogger().log(Level.SEVERE, "trial is null");
                    return;
                } else if(trial.getDefendant() == null) {
                    NoirPVPPlugin.getInstance().getLogger().log(Level.SEVERE, "defendant is null");
                    return;
                } else if(trial.getDefendant().getPlayer() == null) {
                    NoirPVPPlugin.getInstance().getLogger().log(Level.SEVERE, "player is null");
                    return;
                }
                String broadcast = trial.getDefendant().getPlayer().getDisplayName() + ChatColor.GOLD +
                        ChatColor.GOLD + " has been found " + ChatColor.WHITE + "INNOCENT" + ChatColor.GOLD + " and has been released.";
                Bukkit.getServer().broadcastMessage(BROADCAST_PREFIX + broadcast);

                Location releaseLocation = NoirPVPConfig.getInstance().getReleasePoint();
                defendant.teleport(releaseLocation);
            } else if(!isJailTrial || jTrial.getResult().equals(JailTrial.JailTrialResult.JAIL)) {
                String timeStr = DelayedMessager.formatTimeString(trial.getJailTimeSeconds());

                String broadcast = defendant.getDisplayName() +
                        ChatColor.GOLD + " has been found " + ChatColor.DARK_RED + "GUILTY" + ChatColor.GOLD + " and will spend " + timeStr + " in jail.";
                Bukkit.getServer().broadcastMessage(BROADCAST_PREFIX + broadcast);

                Location cellLocation = JailCell.getVacantCellFor(defendant.getUniqueId());
                if(cellLocation == null) {
                    NoirPVPPlugin.getInstance().getLogger().log(Level.WARNING,
                            "Player was sent to holding but no jail is set");
                    cellLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
                }
                defendant.teleport(cellLocation);
            } else if(isJailTrial) {
                if(jTrial.getResult().equals(JailTrial.JailTrialResult.KICK)) {
                    String broadcast = defendant.getDisplayName() +
                            ChatColor.GOLD + " has been found " + ChatColor.DARK_RED + "GUILTY" + ChatColor.GOLD + " and will be kicked.";
                    Bukkit.getServer().broadcastMessage(BROADCAST_PREFIX + broadcast);

                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            "kick " + defendant.getName() + " " + jTrial.getReason());
                } else if(jTrial.getResult().equals(JailTrial.JailTrialResult.BAN)) {
                    String broadcast = defendant.getDisplayName() +
                            ChatColor.GOLD + " has been found " + ChatColor.DARK_RED + "GUILTY" + ChatColor.GOLD + " and will be banned.";
                    Bukkit.getServer().broadcastMessage(BROADCAST_PREFIX + broadcast);

                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            "ban " + defendant.getName() + " " + jTrial.getReason());
                }
            }
        } else if(event.getType().equals(TrialEvent.TrialEventType.RELEASE)) {
            defendant.sendMessage("You are free to go.");

            JailCell.releasePlayer(defendant.getUniqueId());
            defendant.teleport(NoirPVPConfig.getInstance().getReleasePoint());
        }
    }

    private String buildJailMessage(JailTrial trial) {
        StringBuilder broadcast = new StringBuilder();
        broadcast.append(trial.getDefendant().getPlayer().getDisplayName())
                .append(ChatColor.GOLD)
                .append(" is on trial for the reason: ")
                .append(ChatColor.WHITE)
                .append(trial.getReason())
                .append(ChatColor.GOLD);

        broadcast.append(" Vote on the action that should be taken with /innocent, /njail, /nkick or /nban.");
        return broadcast.toString();
    }

    private String buildTrialMessage(Trial trial) {
        StringBuilder broadcast = new StringBuilder();
        broadcast.append(trial.getDefendant().getPlayer().getDisplayName())
                .append(ChatColor.GOLD)
                .append(" is on trial for the murder of ");

        // Get the names of all the players
        Set<UUID> victimIDs = trial.getVictims();
        List<String> victimNames = new ArrayList<>();
        for(UUID id: victimIDs) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                victimNames.add(player.getDisplayName());
            }
        }

        // We only want to display up to NUM_NAMES_DISPLAYED names, so that we don't have too long a message.
        // Thus we step through an append names only until we have that many
        String victimName = "";
        for(int i = 0; i < victimNames.size();) {
            victimName = victimNames.get(i);
            if(i == NUM_NAMES_DISPLAYED || i == victimNames.size() - 1) {
                break;
            }
            broadcast.append(victimName).append(", ");
            i++;
        }

        // Finish off the list of names depending on how many there were.
        if(victimNames.size() == 0) {
            broadcast.append("several. ");  // this should never happen
        } else if(victimNames.size() == 1) {
            broadcast.append(victimName).append(". ");
        } else if(victimNames.size() > 1 && victimNames.size() <= NUM_NAMES_DISPLAYED) {
            broadcast.append("and ").append(victimName).append(". ");
        } else {
            broadcast.append("and ").append(victimNames.size() - NUM_NAMES_DISPLAYED).append(" others. ");
        }

        broadcast.append(trial.getDefendant().getCrimeMarks()).append(" counts all in all.");
        broadcast.append(" Vote /guilty or /innocent now!");
        return broadcast.toString();
    }

}
