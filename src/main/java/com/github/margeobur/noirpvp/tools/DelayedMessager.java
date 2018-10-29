package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Sends a message to a player after a certain amount of time
 */
public class DelayedMessager {

    public BukkitRunnable scheduleMessage(Player player, String message, int seconds) {
        int ticks = seconds * 20;

        SendMessageTask task = new SendMessageTask(player, message);
        task.runTaskLater(NoirPVPPlugin.getPlugin(), ticks);
        return task;
    }

    private class SendMessageTask extends BukkitRunnable {
        Player player;
        String message;

        SendMessageTask(Player player, String message) {
            this.player = player;
            this.message = message;
        }

        @Override
        public void run() {
            player.sendMessage(message);
        }
    }

    /**
     * Formats a string to express a time in an easy human-readable format.
     * Putting this here because it's vaguely cohesive... at any rate it will be used in multiple places.
     * @return A string, in a player-friendly format, ready to be displayed
     */
    public static String formatTimeString(int seconds) {
        int minutes = seconds / 60;
        int hours = minutes / 3600;
        minutes = minutes - 60 * hours;
        seconds = seconds - 60 * minutes;

        String timeStr = "";
        if(hours == 1) {
            timeStr = "1 hour";
        } else if(hours > 1) {
            timeStr = hours + " hours";
        }

        if(minutes > 0) {
            timeStr = timeStr + minutes + " minutes";
        }
        return timeStr;
    }
}
