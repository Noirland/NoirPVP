package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.FSDatabase;
import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Responsible for initiating the loading of player data into memory, as well as the saving of player data
 * to the database when they leave.
 */
public class PlayerConnectionListener implements Listener {

    // the time to wait before committing the player data to disk and removing from memory
    private static final int WAIT_TIME_BEFORE_SAVING = 2;

    public PlayerConnectionListener() { }

    /**
     * When a player joins we want to retrieve their PVP-related data from the database and
     * resume any PVP or trial cooldowns.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        // addIfNotPresent makes a call to FSDatabase#getPlayerPVPbyUUID if it needs to
        PVPPlayer.addIfNotPresent(playerID);
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(playerID);
        playerPVP.resumeCooldowns();
        TrialManager.getInstance().rescheduleJailReleasePotentially(playerID);
        TrialManager.getInstance().retryOfflineTrialPotentially(playerID);
    }

    /**
     * When a player leaves we want to save their data to the database and schedule their data
     * for being cleared from memory.
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId());

        playerPVP.pauseCooldowns();

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
        runInfoDel.runTaskLater(NoirPVPPlugin.getInstance(), ticks);
    }
}
