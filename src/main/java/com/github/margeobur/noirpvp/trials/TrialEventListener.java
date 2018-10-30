package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.tools.DelayedMessager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

/**
 * The purpose of this class is to respond to trial events and perform all the necessary actions.
 * It broadcasts information about trials to the server and teleports guilty players where they need to be.
 */
public class TrialEventListener implements Listener {

    private static final int NUM_NAMES_DISPLAYED = 4;

    @EventHandler
    public void onTrialChange(TrialEvent event) {
        Trial trial = event.getTrial();

        if(event.getType().equals(TrialEvent.TrialEventType.INIT)) {
            Player defendant = trial.getDefendant().getPlayer();
            defendant.sendMessage("You have been put on trial for murder. " +
                    "You may not leave until your trial has seen completion.");

            Location cellLocation = JailCell.getVacantCellFor(defendant.getUniqueId());
            defendant.teleport(cellLocation);

        } else if(event.getType().equals(TrialEvent.TrialEventType.START)) {
            // Most of the code here simply serves the purpose of building the broadcast message
            StringBuilder broadcast = new StringBuilder();
            broadcast.append(trial.getDefendant().getPlayer().getDisplayName())
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
            Bukkit.getServer().broadcastMessage(broadcast.toString());

            Player defendant = trial.getDefendant().getPlayer();
            JailCell.releasePlayer(defendant.getUniqueId());
            defendant.teleport(NoirPVPConfig.getInstance().getCourtDock());

        } else if(event.getType().equals(TrialEvent.TrialEventType.FINISH)) {
            if(!trial.getIsGuiltyVerdict()) {
                String broadcast = trial.getDefendant().getPlayer().getDisplayName() +
                        " has been found INNOCENT and has been released.";
                Bukkit.getServer().broadcastMessage(broadcast);

                Player defendant = trial.getDefendant().getPlayer();
                Location releaseLocation = NoirPVPConfig.getInstance().getReleasePoint();
                defendant.teleport(releaseLocation);
            } else {
                String timeStr = DelayedMessager.formatTimeString(trial.getJailTimeSeconds());

                String broadcast = trial.getDefendant().getPlayer().getDisplayName() +
                        " has been found GUILTY and will spend " + timeStr + " in jail.";
                Bukkit.getServer().broadcastMessage(broadcast);

                Player defendant = trial.getDefendant().getPlayer();
                Location cellLocation = JailCell.getVacantCellFor(defendant.getUniqueId());
                defendant.teleport(cellLocation);
            }
        } else if(event.getType().equals(TrialEvent.TrialEventType.RELEASE)) {
            Player defendant = trial.getDefendant().getPlayer();
            defendant.sendMessage("You are free to go.");

            JailCell.releasePlayer(defendant.getUniqueId());
            defendant.teleport(NoirPVPConfig.getInstance().getReleasePoint());
        }
    }

}
