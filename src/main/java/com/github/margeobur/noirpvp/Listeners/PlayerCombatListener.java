package com.github.margeobur.noirpvp.Listeners;

import com.github.margeobur.noirpvp.PVPPlayer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;

/**
 * Listens to and handles events directly related to PVP combat.
 */
public class PlayerCombatListener implements Listener {

    private final String COMBAT_DENY_MESSAGE = ChatColor.RED + "You can't hurt a player in a claim.";

    private GriefPrevention _gp;
    private ArrayList<PVPPlayer> _players;

    public PlayerCombatListener(GriefPrevention gp, ArrayList<PVPPlayer> players) {
        _gp = gp;
        _players = players;
    }

    /**
     * If a player is attacked by another player, determine if it should be allowed and flag the player as having
     * had PVP damage.
     * PVP is allowed outside of GriefPrevention claims, or if it is tick damage.
     */
    @EventHandler(priority = EventPriority.HIGHEST)     // always have the last say, but ignore cancelled events
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        Player attacker;
        // First, deal with the special case of projectiles
        if(event.getDamager() instanceof Projectile) {
            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if(!(source instanceof Player) ||
                !(event.getEntity() instanceof Player)) {
                return;
            }
            attacker = (Player) source;
        } else if( !(event.getEntity() instanceof Player) ||
            !(event.getDamager() instanceof Player)) {
            return;
        } else {
            attacker = (Player) event.getDamager();
        }

        Player victim = (Player) event.getEntity();
        PlayerData victimData = _gp.dataStore.getPlayerData(victim.getUniqueId());
        PlayerData attackerData = _gp.dataStore.getPlayerData(attacker.getUniqueId());

        Claim claimVicIn = _gp.dataStore.getClaimAt(victim.getLocation(), false, victimData.lastClaim);
        Claim claimAttIn = _gp.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
        if(!(claimVicIn == null && claimAttIn == null)) {
            // Either player is standing in a claim.
            if(isWrongDamageType(event.getCause())) {
                event.setCancelled(true);
                attacker.sendMessage(COMBAT_DENY_MESSAGE);
                return;
            }
        }
        // The damage is allowed through because either both players are outside a claim or it is tick damage
        PVPPlayer victimPVP = PVPPlayer.getPlayerByUUID(victim.getUniqueId(), _players);
        victimPVP.setLastDamagePVP(true);
    }

    private boolean isWrongDamageType(DamageCause cause) {
        return (cause.equals(DamageCause.ENTITY_ATTACK) ||
                cause.equals(DamageCause.ENTITY_SWEEP_ATTACK) ||
                cause.equals(DamageCause.PROJECTILE) ||
                cause.equals(DamageCause.MAGIC));
    }

    /**
     * Detects item damage. If a player has been flagged as having been hit in PVP, the damage is cancelled
     * on equipped armour, weapons or shield.
     * Item damage events related to PVP are always dispatched after damage has been delt to the player,
     * so this method will see the flag for PVP.
     */
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId(), _players);

        if(playerPVP.lastDamagePVP()) {
            if(isArmourOrWeapon(event.getItem(), player.getInventory())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isArmourOrWeapon(ItemStack item, PlayerInventory inventory) {
        return (item.equals(inventory.getHelmet()) ||
                item.equals(inventory.getChestplate()) ||
                item.equals(inventory.getLeggings()) ||
                item.equals(inventory.getBoots()) ||
                item.equals(inventory.getItemInMainHand()) ||
                item.equals(inventory.getItemInOffHand()));
    }

    /**
     * Detects non-PVP damage of players and flags the player as having taken non-PVP damage.
     */
    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId(), _players); {
            playerPVP.setLastDamagePVP(false);
        }
    }
}
