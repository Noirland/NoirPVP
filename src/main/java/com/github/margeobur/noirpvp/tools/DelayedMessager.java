package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DelayedMessager {

    public void scheduleMessage(Player player, String message, int seconds) {
        int ticks = seconds * 20;

        SendMessageTask task = new SendMessageTask(player, message);
        task.runTaskLater(NoirPVPPlugin.getPlugin(), ticks);
    }

    private class SendMessageTask extends BukkitRunnable {
        Player _player;
        String _message;

        SendMessageTask(Player player, String message) {
            _player = player;
            _message = message;
        }

        @Override
        public void run() {
            _player.sendMessage(_message);
        }
    }
}
