package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.*;

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

    private int crimeMarks = 0;
    private Set<PVPPlayer> victims = new HashSet<>();
    private enum LegalState { CLEAN, INNOCENT, GUILTY }
    private LegalState legalState;
    private LocalDateTime lastConviction;

    public PVPPlayer(UUID playerID) {
        this.playerID = playerID;
    }

    public UUID getID() {
        return playerID;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerID);
    }

    /* Victim related methods */

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
        lastDeath = LocalDateTime.now();
        updateState();
        if(deathState.equals(DeathState.CLEAR)) {
            deathState = DeathState.PROTECTED;
        } else if(deathState.equals(DeathState.COOLDOWN)) {
            deathState = DeathState.PROTECTED_COOLDOWN;
        }
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
        LocalDateTime protectionDeactivation = lastDeath.plusSeconds(NoirPVPConfig.PROTECTION_DURATION);
        LocalDateTime coolDownDeactivation = lastDeath.plusSeconds(NoirPVPConfig.COOLDOWN_DURATION);
        LocalDateTime doubleDeathDeactivation = lastDeath.plusSeconds(NoirPVPConfig.DOUBLE_PROTECTION_DURATION);

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

    /**
     * Determines if a player can use the /back command.
     */
    public boolean canBack() {
        updateState();
        if(deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determines if the player can be hurt. If they can't be hurt, then they can't hurt others.
     */
    public boolean canBeHurt() {
        updateState();
        if(deathState.equals(DeathState.PROTECTED) || deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Increments the number of crime marks and dispatches a new trial if the player has reached a multiple of 5.
     */
    public void doMurder(PVPPlayer victim) {
        crimeMarks++;
        victims.add(victim);
        if(crimeMarks % 5 == 0) {
            TrialManager.getInstance().dispatchNewTrial(this);
        }
    }

    public void findGuilty() {
        legalState = LegalState.GUILTY;
        lastConviction = LocalDateTime.now();
    }

    public void findInnocent() {
        legalState = LegalState.INNOCENT;
        crimeMarks -= 5;
    }

    public void releaseFromJail() {
        legalState = LegalState.CLEAN;
    }

    /**
     * Returns a copy of the set of victims
     */
    public Set<PVPPlayer> getVictims() {
        Set<PVPPlayer> victimsCopy = new HashSet<>(victims);
        return victimsCopy;
    }

    public int getCrimeMarks() {
        return crimeMarks;
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

    public static List<PVPPlayer> getTopCriminals() {
        Collections.sort(players, Comparator.comparingInt(PVPPlayer::getCrimeMarks));
        List<PVPPlayer> playersCopy = new ArrayList<>();

        for(int i = 0; i < 10 && i < players.size(); i++) {
            playersCopy.add(players.get(i));
        }
        return playersCopy;
    }
}
