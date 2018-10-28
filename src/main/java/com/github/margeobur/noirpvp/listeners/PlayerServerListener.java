package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.FSDatabase;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Responsible for initiating the loading of player data into memory, as well as the saving of player data
 * to the disk.
 */
public class PlayerServerListener implements Listener {

    // the time to wait before committing the player data to disk and removing from memory
    private static final int WAIT_TIME_BEFORE_SAVING = 5;

    public PlayerServerListener() { }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // addIfNotPresent makes a call to FSDatabase#getPlayerPVPbyUUID if it needs to
        PVPPlayer.addIfNotPresent(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId());
        FSDatabase.getInstance().savePlayerPVP(playerPVP);


        BukkitRunnable runInfoDel = new BukkitRunnable() {
            @Override
            public void run() {
                if(!player.isOnline()) {
                    FSDatabase.getInstance().savePlayerPVP(playerPVP);
                    PVPPlayer.removePlayer(player.getUniqueId());
                }
            }
        };

        int ticks = WAIT_TIME_BEFORE_SAVING * 60 * 20;
        runInfoDel.runTaskLater(NoirPVPPlugin.getPlugin(), ticks);
    }
}
