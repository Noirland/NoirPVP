package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.tools.DelayedMessager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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
        if (playerInfo.getLastPVP() == null) {
            System.out.println("doing regular death");
            playerInfo.doRegularDeath();
            return;
        }
        LocalDateTime pvpDeactivationTime = playerInfo.getLastPVP().plusSeconds(5);
        if(currentTime.isBefore(pvpDeactivationTime)) {
            doPVPDeath(event, playerInfo);
        } else {
            System.out.println("doing regular death");
            playerInfo.doRegularDeath();
        }

        if(event.getEntity().getLastDamageCause().getCause().equals(EntityDamageEvent.DamageCause.STARVATION)) {
            if(event.getEntity().getLocation().getBlock().getTemperature() >= 1.1) {
                event.setDeathMessage(event.getEntity().getDisplayName() + " succumbed to heat stroke.");
            } else if(event.getEntity().getLocation().getBlock().getTemperature() <=  0.05) {
                event.setDeathMessage(event.getEntity().getDisplayName() + " froze to death.");
            }
        }
    }

    /**
     * Handles a PVP death
     */
    private void doPVPDeath(PlayerDeathEvent event, PVPPlayer playerInfo) {
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        // This will send a message to the player and update their state:
        playerInfo.doDeath();

        Player attacker = Bukkit.getPlayer(playerInfo.getLastAttackerID());
        PVPPlayer attackerPVP = PVPPlayer.getPlayerByUUID(attacker.getUniqueId());
        Player victim = event.getEntity();
        PVPPlayer victimPVP = PVPPlayer.getPlayerByUUID(victim.getUniqueId());

        attackerPVP.doMurder(victimPVP);
    }
}
