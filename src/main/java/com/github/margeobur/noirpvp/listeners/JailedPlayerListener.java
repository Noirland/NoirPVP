package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.trials.JailCell;
import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Responsible for listening to player activity and stopping jailed / on trial player from performing
 * prohibited actions.
 */
public class JailedPlayerListener implements Listener {

    private LocalDateTime lastTeleport = LocalDateTime.now();

    /**
     * This method observes movement of the current player on trial and teleports them back to the trial
     * block ("the dock") if they move too far
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        PVPPlayer possibleDefendant = TrialManager.getInstance().currentDefendant();
        if(possibleDefendant == null || !possibleDefendant.getID().equals(playerID)) {
            return;
        }

        Location trialDock = NoirPVPConfig.getInstance().getCourtDock();
        Location playerLocation = event.getTo();
        if(playerLocation.getBlockX() > trialDock.getBlockX() + 1
                || playerLocation.getBlockX() < trialDock.getBlockX() - 1
                || playerLocation.getBlockY() > trialDock.getBlockY() + 1
                || playerLocation.getBlockY() < trialDock.getBlockY() - 1
                || playerLocation.getBlockZ() > trialDock.getBlockZ() + 2 // +2 to allow them to jump
                || playerLocation.getBlockZ() < trialDock.getBlockZ() - 1) {

            if(lastTeleport.plusSeconds(3).isBefore(LocalDateTime.now())) {
                lastTeleport = LocalDateTime.now();
                event.getPlayer().teleport(trialDock);
            }
        }
    }

    /**
     * Stops jailed players from interacting with their inventory
     */
    @EventHandler
    public void onPlayerInteractInventory(InventoryInteractEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if(interactorIsJailedPlayer(entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * If a player interacts with their inventory, we want to check that they aren't jailed or on trial.
     * If they are, we will cancel any inventory interactions, including dropping or picking up items.
     */
    @EventHandler
    public void onPlayerMoveItem(InventoryDragEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if(interactorIsJailedPlayer(entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * @see JailedPlayerListener#onPlayerMoveItem(InventoryDragEvent)
     */
    @EventHandler
    public void onPlayerClickItem(InventoryClickEvent event) {
        HumanEntity entity = event.getWhoClicked();
        if(interactorIsJailedPlayer(entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * @see JailedPlayerListener#onPlayerMoveItem(InventoryDragEvent)
     */
    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if(event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if(interactorIsJailedPlayer(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * @see JailedPlayerListener#onPlayerMoveItem(InventoryDragEvent)
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if(interactorIsJailedPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * A generic helper method for all the inventory listener methods in this class.
     * Determines if the entity is a player and whether or not that player is jailed or on trial.
     * If they are, the method returns true.
     */
    private boolean interactorIsJailedPlayer(HumanEntity entity) {
        if(entity instanceof Player) {
            Player player = (Player) entity;
            PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId());
            PVPPlayer possibleDefendant = TrialManager.getInstance().currentDefendant();
            if(possibleDefendant != null && possibleDefendant.getID().equals(player.getUniqueId())) {
                return true;
            } else if(playerPVP.isJailed() && JailCell.playerOnShortlist(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
