package com.github.margeobur.noirpvp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * A class encapsulating the information relevant to the PVP status of a Player. This class is also an encapsulation
 * of the static list of such information for all online players.
 *
 * PVP cooldowns are handled here. The {@link com.github.margeobur.noirpvp.listeners.PlayerCombatListener} will request
 * the status of a player's ability to do PVP, and will notify the PVPPlayer every time a player dies or does PVP so that
 * this class can update the player's state.
 */
public class PVPPlayer {

    // The list of all players on the server
    private static ArrayList<PVPPlayer> players = new ArrayList<>();

    private UUID playerID;

    private UUID attackerID;
    private boolean lastDamagePVP = false;
    private LocalDateTime lastPVP;
    private LocalDateTime lastDeath;

    private enum DeathState { CLEAR, PROTECTED, COOLDOWN, PROTECTED_COOLDOWN } // used in determining whether the user can /back atm
    private DeathState deathState = DeathState.CLEAR;

    public PVPPlayer(UUID playerID) {
        this.playerID = playerID;
    }

    public UUID getID() {
        return playerID;
    }

    public boolean lastDamagePVP() {
        return lastDamagePVP;
    }

    public void setLastDamagePVP(boolean dmgWasPVP, UUID attacker) {
        lastDamagePVP = dmgWasPVP;
        if(dmgWasPVP) {
            lastPVP = LocalDateTime.now();
            attackerID = attacker;
        }
    }

    public LocalDateTime getLastPVP() {
        return lastPVP;
    }

    public UUID getLastAttackerID() {
        return attackerID;
    }

    public void doDeath() {
        System.out.println("Player died, old state is: " + deathState.name());
        lastDeath = LocalDateTime.now();
        updateState();
        if(deathState.equals(DeathState.CLEAR)) {
            deathState = DeathState.PROTECTED;
        } else if(deathState.equals(DeathState.COOLDOWN)) {
            deathState = DeathState.PROTECTED_COOLDOWN;
        }
        System.out.println("\t new state is: " + deathState.name());
    }

    /*
        This method acts a state transition funciton. It updates the state whenever we need to check it without
        the user dying.
     */
    private void updateState() {
        if(lastDeath == null) {
            return;     // we must be CLEAR
        }
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime protectionDeactivation = lastDeath.plusSeconds(30);
        LocalDateTime coolDownDeactivation = lastDeath.plusMinutes(5);
        LocalDateTime doubleDeathDeactivation = lastDeath.plusMinutes(2);

        if(deathState.equals(DeathState.PROTECTED)) {
            if(currentTime.isAfter(protectionDeactivation) && currentTime.isBefore(coolDownDeactivation)) {
                deathState = DeathState.COOLDOWN;
            } else if(currentTime.isAfter(coolDownDeactivation)) {
                deathState = DeathState.CLEAR;
            } // otherwise still in PROTECTED state
        } else if(deathState.equals(DeathState.COOLDOWN)) {
            if(currentTime.isAfter(coolDownDeactivation)) {
                deathState = DeathState.CLEAR;
            } // otherwise still in COOLDOWN state
        } else if(deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            if(currentTime.isAfter(doubleDeathDeactivation)) {
                deathState = DeathState.CLEAR;
            }
        }   // if in CLEAR state then we will always remain CLEAR (we only leave it via death in doDeath())
    }

    public boolean canBack() {
        updateState();
        if(deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean canBeHurt() {
        updateState();
        if(deathState.equals(DeathState.PROTECTED) || deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            return true;
        }
    }

    /* Managing the static collection of players */

    public static PVPPlayer getPlayerByUUID(UUID id) {
        for(PVPPlayer player: players) {
            if(player.getID().equals(id)) {
                return player;
            }
        }
        return null;
    }

    public static void addIfNotPresent(UUID id) {
        if(getPlayerByUUID(id) == null) {
            players.add(new PVPPlayer(id));
        }
    }
}
