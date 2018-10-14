package com.github.margeobur.noirpvp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * A class encapsulating the information relevant to the PVP status of a Player. This class is also an encapsulation
 * of the static list of such information for all online players.
 */
public class PVPPlayer {

    // The list of all players on the server
    private static ArrayList<PVPPlayer> players = new ArrayList<>();

    private UUID playerID;

    private UUID attackerID;
    private boolean lastDamagePVP = false;
    private LocalDateTime lastPVP;
    private LocalDateTime lastDeath;

    private enum DeathState { CLEAR, PROTECTED, COOLDOWN } // used in determining whether the user can /back atm
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
        if(deathState.equals(DeathState.CLEAR)) {
            deathState = DeathState.PROTECTED;
        } else if(deathState.equals(DeathState.PROTECTED)) {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime protectionDeactivation = lastDeath.plusSeconds(30);
            LocalDateTime coolDownDeactivation = lastDeath.plusMinutes(5);
            if(currentTime.isAfter(protectionDeactivation) && currentTime.isBefore(coolDownDeactivation)) {
                deathState = DeathState.COOLDOWN;
            }
        }
        System.out.println("\t new state is: " + deathState.name());
    }

    public boolean canBack() {
        if(deathState.equals(DeathState.COOLDOWN)) {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime coolDownDeactivation = lastDeath.plusMinutes(5);
            if(currentTime.isBefore(coolDownDeactivation)) {
                return false;
            } else {
                deathState = DeathState.CLEAR;
            }
        }
        return true;
    }

    public boolean canBeHurt() {
        if(!deathState.equals(DeathState.PROTECTED)) {
            return true;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime protectionDeactivation = lastDeath.plusSeconds(30);
        LocalDateTime coolDownDeactivation = lastDeath.plusMinutes(5);
        if(currentTime.isBefore(protectionDeactivation)) {
            return false;
        } else {
            if(currentTime.isAfter(protectionDeactivation) && currentTime.isBefore(coolDownDeactivation)) {
                deathState = DeathState.COOLDOWN;
            }
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
