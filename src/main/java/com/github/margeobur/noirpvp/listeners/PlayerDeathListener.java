package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.LocalDateTime;

/**
 * Listens to and handles events directly related to PVP deaths
 */
public class PlayerDeathListener implements Listener {

    /**
     * Listens for player deaths, determines if they are PVP related and handles them accordingly.
     * Any death < 5 seconds after a player damages another player is regarded as a PVP death.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDie(PlayerDeathEvent event) {
        PVPPlayer playerInfo = PVPPlayer.getPlayerByUUID(event.getEntity().getUniqueId());
        if(playerInfo == null) {    // shouldn't happen
            return;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime pvpDeactivationTime = playerInfo.getLastPVP().plusSeconds(5);
        if(currentTime.isBefore(pvpDeactivationTime)) {
            doPVPDeath(event, playerInfo);
        }
    }

    /**
     * Handles a PVP death
     */
    private void doPVPDeath(PlayerDeathEvent event, PVPPlayer playerInfo) {
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        playerInfo.doDeath();
        //Player attacker = Bukkit.getPlayer(playerInfo.getLastAttackerID());
    }

}
