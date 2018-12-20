package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.tools.DelayedMessager;
import com.github.margeobur.noirpvp.tools.TimeTracker;
import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
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
@SerializableAs("PVPPlayer")
public class PVPPlayer implements ConfigurationSerializable {

    // The list of all players on the server
    private static ArrayList<PVPPlayer> players = new ArrayList<>();

    private UUID playerID;
    private UUID attackerID;

    private boolean lastDamagePVP = false;
    private LocalDateTime lastPVP;

    private TimeTracker pvpDeathTimer;
    private enum DeathState { CLEAR, PROTECTED, COOLDOWN, PROTECTED_COOLDOWN;} // used in determining whether the user can /back atm
    private DeathState deathState = DeathState.CLEAR;

    private boolean lastDeathRecent = false;
    private TimeTracker regDeathTimer;

    private LocalDateTime logOffTime;
    private LocalDateTime logOnTime;

    private boolean lastHitCancelled;
    private int crimeMarks = 0;
    private Set<UUID> victims = new HashSet<>();
    private enum LegalState { CLEAN, INNOCENT, GUILTY }
    private LegalState legalState = LegalState.CLEAN;
    private LocalDateTime lastConviction;
    private boolean nonMurderConvict = false;

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

    public boolean lastHitWasCancelled() { return lastHitCancelled; }

    public void setLastHitCancelled(boolean hitWasPVP) { lastHitCancelled = hitWasPVP; }

    public LocalDateTime getLastPVP() {
        return lastPVP;
    }

    public UUID getLastAttackerID() {
        return attackerID;
    }

    public void doPvPDeath() {
        if(deathState.equals(DeathState.CLEAR)) {
            deathState = DeathState.PROTECTED;
            if(pvpDeathTimer == null) {
                pvpDeathTimer = new TimeTracker(NoirPVPPlugin.getInstance());
            }
            pvpDeathTimer.registerTimer(() -> {
                doProtectionEnd();
            }, NoirPVPConfig.PROTECTION_DURATION, "protection");
            pvpDeathTimer.registerTimer(() -> {
                doCooldownEnd();
            }, NoirPVPConfig.PROTECTION_DURATION + NoirPVPConfig.COOLDOWN_DURATION, "cooldown");

        } else if(deathState.equals(DeathState.COOLDOWN)) {
            deathState = DeathState.PROTECTED_COOLDOWN;
            if(pvpDeathTimer == null) {
                pvpDeathTimer = new TimeTracker(NoirPVPPlugin.getInstance());
            }
            pvpDeathTimer.registerTimer(() -> {
                doDoubleEnd();
            }, NoirPVPConfig.DOUBLE_PROTECTION_DURATION, "protect_cooldown");
        }
        pvpDeathTimer.resume();
        doRegularDeath();
    }

    public void doRegularDeath() {
        lastDeathRecent = true;
        regDeathTimer = new TimeTracker(NoirPVPPlugin.getInstance());
        regDeathTimer.registerTimer(() -> { lastDeathRecent = false;
                                            regDeathTimer.reset();}, NoirPVPConfig.BACK_DURATION, "death");
        regDeathTimer.resume();
    }

    private void doProtectionEnd() {
        Player player = Bukkit.getPlayer(playerID);
        if(player == null || !player.isOnline()) {
            return;
        }
        player.sendMessage(NoirPVPConfig.PLAYER_PROTECTION_END);
        deathState = DeathState.COOLDOWN;
    }

    private void doCooldownEnd() {
        deathState = DeathState.CLEAR;
        pvpDeathTimer.reset();
    }

    private void doDoubleEnd() {
        Player player = Bukkit.getPlayer(playerID);
        if(player == null || !player.isOnline()) {
            return;
        }
        player.sendMessage(NoirPVPConfig.PLAYER_DOUBLE_END);
        deathState = DeathState.CLEAR;
        pvpDeathTimer.reset();
    }

    /**
     * Determines if a player can use the /back command.
     */
    public boolean canBack() {
        if(deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            return lastDeathRecent;
        }
    }

    /**
     * Determines if the player can be hurt. If they can't be hurt, then they can't hurt others.
     */
    public boolean canBeHurt() {
        if(deathState.equals(DeathState.PROTECTED) || deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            return true;
        }
    }

    public void pauseCooldowns() {
        if(regDeathTimer != null) {
            regDeathTimer.pause();
        }
        if(pvpDeathTimer != null) {
            pvpDeathTimer.pause();
        }
    }

    public void resumeCooldowns() {
        //getPlayer().sendMessage("resuming state is " + deathState);
        if(regDeathTimer != null) {
            regDeathTimer.resume();
        }
        if(pvpDeathTimer != null) {
            pvpDeathTimer.resume();
        }s
    }

    /**
     * Increments the number of crime marks and dispatches a new trial if the player has reached a multiple of 5.
     */
    public void doMurder(PVPPlayer victim) {
        crimeMarks++;
        victims.add(victim.playerID);
        if(crimeMarks % 5 == 0) {
            TrialManager.getInstance().dispatchNewTrial(this);
        }
    }

    public void findGuilty(boolean nonMurder) {
        legalState = LegalState.GUILTY;
        lastConviction = LocalDateTime.now();
    }

    public void findInnocent(boolean penalise) {
        if(penalise) {
            crimeMarks += 25;
        }
        legalState = LegalState.INNOCENT;
        crimeMarks -= 5;
        victims.clear();
    }

    public boolean isJailed() {
        return legalState.equals(LegalState.GUILTY);
    }

    public void releaseFromJail() {
        legalState = LegalState.CLEAN;
        secondsAlreadyServed = 0;
    }

    public boolean wasAdminJailed() {
        return nonMurderConvict;
    }

    /**
     * Returns a copy of the set of victims
     */
    public Set<UUID> getVictims() {
        Set<UUID> victimsCopy = new HashSet<>(victims);
        return victimsCopy;
    }

    public LocalDateTime getLastConviction() {
        return lastConviction;
    }

    public int getCrimeMarks() {
        return crimeMarks;
    }

    public long getTimeAlreadyServed() { return secondsAlreadyServed; }

    /* ---------- Serialisation and Deserialisation ---------- */

    public PVPPlayer(Map<String, Object> serialMap) {

        if(serialMap.containsKey("playerID")) { playerID = UUID.fromString((String) serialMap.get("playerID")); }
        if(serialMap.containsKey("lastDamagePVP")) { lastDamagePVP = (Boolean) serialMap.get("lastDamagePVP"); }
        if(serialMap.containsKey("lastPVP")) { lastPVP = LocalDateTime.parse((String) serialMap.get("lastPVP")); }
        if(serialMap.containsKey("deathState")) { deathState = DeathState.valueOf((String) serialMap.get("deathState")); }
        if(serialMap.containsKey("logOffTime")) { logOffTime = LocalDateTime.parse((String) serialMap.get("logOffTime")); }

        if(serialMap.containsKey("pvp-timer")) {
            pvpDeathTimer = (TimeTracker) serialMap.get("pvp-timer");
            if(deathState == DeathState.PROTECTED) {
                pvpDeathTimer.registerTimer(() -> {
                    doProtectionEnd();
                }, NoirPVPConfig.PROTECTION_DURATION, "protection");
                pvpDeathTimer.registerTimer(() -> {
                    doCooldownEnd();
                }, NoirPVPConfig.PROTECTION_DURATION + NoirPVPConfig.COOLDOWN_DURATION, "cooldown");
            } else if(deathState == DeathState.COOLDOWN) {
                pvpDeathTimer.registerTimer(() -> {
                    doCooldownEnd();
                }, NoirPVPConfig.PROTECTION_DURATION + NoirPVPConfig.COOLDOWN_DURATION, "cooldown");
            } else if(deathState == DeathState.PROTECTED_COOLDOWN) {
                pvpDeathTimer.registerTimer(() -> {
                    doDoubleEnd();
                }, NoirPVPConfig.DOUBLE_PROTECTION_DURATION, "protect_cooldown");
            }
        }
        if(serialMap.containsKey("reg-timer")) {
            regDeathTimer = (TimeTracker) serialMap.get("reg-timer");
            if(regDeathTimer.getSecondsElapsed() < 30) {
                doRegularDeath();
            }
        }

        if(serialMap.containsKey("crimeMarks")) { crimeMarks = (Integer) serialMap.get("crimeMarks"); }
        if(serialMap.containsKey("victims")) {
            List<String> victimIDs = (List<String>) serialMap.get("victims");

            for(String victimID: victimIDs) {
                UUID actualID = UUID.fromString(victimID);
                victims.add(actualID);
            }
        }

        if(serialMap.containsKey("legalState")) { legalState = LegalState.valueOf((String) serialMap.get("legalState")); }
        if(serialMap.containsKey("lastConviction")) { lastConviction = LocalDateTime.parse((String) serialMap.get("lastConviction")); }
        if(serialMap.containsKey("secondsAlreadyServed")) { secondsAlreadyServed = (Integer) serialMap.get("secondsAlreadyServed"); }
        if(serialMap.containsKey("nonMurderConvict")) { nonMurderConvict = (Boolean) serialMap.get("nonMurderConvict"); }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialMap = new HashMap<>();

        if(playerID != null)
            serialMap.put("playerID", playerID.toString());
        serialMap.put("lastDamagePVP", lastDamagePVP);
        if(lastPVP != null)
            serialMap.put("lastPVP", lastPVP.toString());
        if(deathState != null)
            serialMap.put("deathState", deathState.name());
        if(logOffTime != null)
            serialMap.put("logOffTime", logOffTime.toString());

        if(pvpDeathTimer != null) {
            serialMap.put("pvp-timer", pvpDeathTimer);
        }
        if(regDeathTimer != null) {
            serialMap.put("reg-timer", regDeathTimer);
        }

        serialMap.put("crimeMarks", crimeMarks);
        List<String> victimIDs = new ArrayList<>();
        if(!(victims == null || victims.isEmpty())) {
            for(UUID victimID: victims) {
                victimIDs.add(victimID.toString());
            }
        }
        serialMap.put("victims", victimIDs);

        if(legalState != null)
            serialMap.put("legalState", legalState.name());
        if(lastConviction != null)
            serialMap.put("lastConviction", lastConviction.toString());
        serialMap.put("secondsAlreadyServed", secondsAlreadyServed);
        serialMap.put("nonMurderConvict", nonMurderConvict);

        return serialMap;
    }

    /* ---------- Managing the static collection of players ---------- */

    public static PVPPlayer getPlayerByUUID(UUID id) {
        for(PVPPlayer player: players) {
            if(player.getID().equals(id)) {
                return player;
            }
        }
        PVPPlayer player = FSDatabase.getInstance().getPlayerPVPbyUUID(id);
        players.add(player);
        return player;
    }

    public static void addIfNotPresent(UUID id) {
        if(getPlayerByUUID(id) == null) {
            PVPPlayer player = FSDatabase.getInstance().getPlayerPVPbyUUID(id);
            if(player == null) {
                players.add(new PVPPlayer(id));
            } else {
                players.add(player);
            }
        }
    }

    public static void removePlayer(UUID id) {
        PVPPlayer player = getPlayerByUUID(id);
        if(player != null) {
            players.remove(player);
        }
    }

    public static List<PVPPlayer> getTopCriminals() {
        saveAllPVPData();
        List<PVPPlayer> allPlayers = FSDatabase.getInstance().getAllPlayers();

        Collections.sort(allPlayers, Comparator.comparingInt(PVPPlayer::getCrimeMarks));
        Collections.reverse(allPlayers);
        return allPlayers;
    }

    public static void pauseAllCooldowns() {
        for(PVPPlayer player: players) {
            if(player.getPlayer() != null && player.getPlayer().isOnline()) {
                player.pauseCooldowns();
            }
        }
    }

    public static void saveAllPVPData() {
        for(PVPPlayer player: players) {
            FSDatabase.getInstance().savePlayerPVP(player);
        }
    }

}
