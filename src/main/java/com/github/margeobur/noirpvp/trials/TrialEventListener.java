package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.Set;

/**
 * The purpose of this class is to respond to trial events and perform all the necessary actions.
 * It broadcasts information about trials to the server and teleports guilty players where they need to be.
 */
public class TrialEventListener implements Listener {

    private static final int NUM_NAMES_DISPLAYED = 4;

    @EventHandler
    public void onTrialChange(TrialEvent event) {
        Trial trial = event.getTrial();
        if(event.getType().equals(TrialEvent.TrialEventType.START)) {
            StringBuilder broadcast = new StringBuilder();
            broadcast.append(trial.getDefendant().getPlayer().getDisplayName())
                    .append(" is on trial for the murder of ");

            int i = 0;
            Set<PVPPlayer> victims = trial.getVictims();
            Iterator<PVPPlayer> iter = victims.iterator();

            if(!iter.hasNext()) {
                return; // this should never happen
            }

            PVPPlayer victim = iter.next();
            for(; iter.hasNext(); victim = iter.next()) {
                if(i == NUM_NAMES_DISPLAYED || i == victims.size() - 1) {
                    break;
                }
                broadcast.append(victim.getPlayer().getDisplayName()).append(", ");
                i++;
            }

            if(victims.size() == 1) {
                broadcast.append(victim.getPlayer().getDisplayName()).append(". ");
            } else if(victims.size() > 1 && victims.size() <= NUM_NAMES_DISPLAYED) {
                broadcast.append("and ").append(victim.getPlayer().getDisplayName()).append(". ");
            } else {
                broadcast.append("and ").append(victims.size() - NUM_NAMES_DISPLAYED).append(" others. ");
            }

            broadcast.append(trial.getDefendant().getCrimeMarks()).append(" counts all in all.");
            broadcast.append(" Vote /guilty or /innocent now!");
            Bukkit.getServer().broadcastMessage(broadcast.toString());

        } else {
            if(!trial.getIsGuiltyVerdict()) {
                String broadcast = trial.getDefendant().getPlayer().getDisplayName() +
                        " has been found INNOCENT and has been released.";
                Bukkit.getServer().broadcastMessage(broadcast);
            } else {
                int minutes = trial.getJailTimeMinutes();
                int hours = minutes / 60;
                minutes = minutes % 60;

                String timeStr = "";
                if(hours == 1) {
                    timeStr = "1 hour";
                } else if(hours > 1) {
                    timeStr = hours + " hours";
                }

                if(minutes > 0) {
                    timeStr = timeStr + minutes + " minutes";
                }

                String broadcast = trial.getDefendant().getPlayer().getDisplayName() +
                        " has been found GUILTY and will spend " + timeStr + " in jail.";
                Bukkit.getServer().broadcastMessage(broadcast);
            }
        }
    }

}
