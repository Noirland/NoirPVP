package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.trials.TrialManager;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;

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

/**
 * Listens to and handles events directly related to PVP combat that has yet to result in a fatality.
 */
public class PlayerCombatListener implements Listener {

    private GriefPrevention gp;

    public PlayerCombatListener(GriefPrevention gp) {
        this.gp = gp;
    }

    /**
     * If a player is attacked by another player, determine if it should be allowed and flag the player as having
     * had PVP damage.
     * PVP is allowed outside of GriefPrevention claims, or if it is tick damage.
     */
    // always have the last say, but ignore cancelled events
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Player) {
            PVPPlayer attackerPVP = PVPPlayer.getPlayerByUUID(event.getDamager().getUniqueId());
            attackerPVP.setLastHitCancelled(false);
        }

        Player attacker;
        // Deal with the special case of projectiles
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

        PVPPlayer attackerPVP = PVPPlayer.getPlayerByUUID(attacker.getUniqueId());
        Player victim = (Player) event.getEntity();
        PVPPlayer victimPVP = PVPPlayer.getPlayerByUUID(victim.getUniqueId());

        if(victimPVP.isJailed() || attackerPVP.isJailed()) {
            event.setCancelled(true);
            attacker.sendMessage("Guilty players cannot fight!");
            attackerPVP.setLastHitCancelled(true);
            return;
        } else if(TrialManager.getInstance().currentDefendant() != null) {
            if(TrialManager.getInstance().currentDefendant().equals(victimPVP)
                || TrialManager.getInstance().currentDefendant().equals(attackerPVP)) {
                event.setCancelled(true);
                attacker.sendMessage("Players on trial cannot fight!");
                attackerPVP.setLastHitCancelled(true);
                return;
            }
        }

        // check if either player is in a claim
        PlayerData victimData = gp.dataStore.getPlayerData(victim.getUniqueId());
        PlayerData attackerData = gp.dataStore.getPlayerData(attacker.getUniqueId());
        Claim claimVicIn = gp.dataStore.getClaimAt(victim.getLocation(), false, victimData.lastClaim);
        Claim claimAttIn = gp.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
        if(!(claimVicIn == null && claimAttIn == null)) {
            // Either player is standing in a claim.
            if(isWrongDamageType(event.getCause())) {
                event.setCancelled(true);
                attacker.sendMessage(NoirPVPConfig.CLAIM_DENY_MESSAGE);
                attackerPVP.setLastHitCancelled(true);
                return;
            }
        }

        if(!victimPVP.canBeHurt()) {
            event.setCancelled(true);
            attacker.sendMessage(NoirPVPConfig.PROTECTED_DENY_MESSAGE);
            attackerPVP.setLastHitCancelled(true);
            return;
        }
        if(!attackerPVP.canBeHurt()) {
            event.setCancelled(true);
            attacker.sendMessage(NoirPVPConfig.SELF_COOLDOWN_DENY_MESSAGE);
            attackerPVP.setLastHitCancelled(true);
            return;
        }

        // The damage is allowed through because either both players are outside a claim or it is tick damage
        // This will flag the current time internally, to track the 5-sec effect
        victimPVP.setLastDamagePVP(true, attacker.getUniqueId());
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
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId());

        if(playerPVP.lastDamagePVP()) {
            if(isArmourOrWeapon(event.getItem(), player.getInventory())) {
                event.setCancelled(true);
            }
        } else if(playerPVP.lastHitWasCancelled()){
            if(event.getItem().equals(player.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
                playerPVP.setLastHitCancelled(false);
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
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(player.getUniqueId()); {
            playerPVP.setLastDamagePVP(false, null);
        }
    }
}
