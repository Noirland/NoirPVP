package com.github.margeobur.noirpvp;

import com.github.margeobur.noirpvp.tools.DelayedMessager;
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
    private LocalDateTime lastDeath;
    private LocalDateTime lastRegularDeath;
    private enum DeathState { CLEAR, PROTECTED, COOLDOWN, PROTECTED_COOLDOWN } // used in determining whether the user can /back atm
    private DeathState deathState = DeathState.CLEAR;

    private BukkitRunnable lastMessageTask;
    private LocalDateTime logOffTime;
    private LocalDateTime logOnTime;
    private long secondsLoggedOff;

    private boolean lastHitCancelled;
    private int crimeMarks = 0;
    private Set<UUID> victims = new HashSet<>();
    private enum LegalState { CLEAN, INNOCENT, GUILTY }
    private LegalState legalState = LegalState.CLEAN;
    private LocalDateTime lastConviction;
    private long secondsAlreadyServed = 0;
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

    public void doDeath() {
        lastDeath = LocalDateTime.now();
        updateState();
        if(deathState.equals(DeathState.CLEAR)) {
            deathState = DeathState.PROTECTED;
        } else if(deathState.equals(DeathState.COOLDOWN)) {
            deathState = DeathState.PROTECTED_COOLDOWN;
        }

        Player player = Bukkit.getPlayer(playerID);
        DelayedMessager messager = new DelayedMessager();
        if(canBack()) {
            lastMessageTask = messager.scheduleMessage(player, NoirPVPConfig.PLAYER_PROTECTION_END,
                    NoirPVPConfig.PROTECTION_DURATION);
        } else {
            lastMessageTask = messager.scheduleMessage(player, NoirPVPConfig.PLAYER_DOUBLE_END,
                    NoirPVPConfig.DOUBLE_PROTECTION_DURATION);
        }
    }

    public void doRegularDeath() {
        lastRegularDeath = LocalDateTime.now();
    }

    /*
        This method acts a state transition funciton. It updates the state whenever we need to check it without
        the user dying.
     */
    private void updateState() {
        //getPlayer().sendMessage("old state: " + deathState);
        if(lastDeath == null) {
            return;     // we must be CLEAR
        }
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime protectionDeactivation = lastDeath.plusSeconds(NoirPVPConfig.PROTECTION_DURATION + secondsLoggedOff);
        LocalDateTime coolDownDeactivation = lastDeath.plusSeconds(NoirPVPConfig.COOLDOWN_DURATION + secondsLoggedOff);
        LocalDateTime doubleDeathDeactivation = lastDeath.plusSeconds(NoirPVPConfig.DOUBLE_PROTECTION_DURATION + secondsLoggedOff);

        if(deathState.equals(DeathState.PROTECTED)) {
            //getPlayer().sendMessage("next PVP end " + protectionDeactivation.toString() + " seconds: " + secondsLoggedOff);
            if(currentTime.isAfter(protectionDeactivation) && currentTime.isBefore(coolDownDeactivation)) {
                deathState = DeathState.COOLDOWN;
            } else if(currentTime.isAfter(coolDownDeactivation)) {
                deathState = DeathState.CLEAR;
                secondsLoggedOff = 0;
            } // otherwise still in PROTECTED state
        } else if(deathState.equals(DeathState.COOLDOWN)) {
            //getPlayer().sendMessage("next cooldown end " + coolDownDeactivation.toString() + " seconds: " + secondsLoggedOff);
            if(currentTime.isAfter(coolDownDeactivation)) {
                deathState = DeathState.CLEAR;
                secondsLoggedOff = 0;
            } // otherwise still in COOLDOWN state
        } else if(deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            //getPlayer().sendMessage("next backblock end " + doubleDeathDeactivation.toString() + " seconds: " + secondsLoggedOff);
            if(currentTime.isAfter(doubleDeathDeactivation)) {
                deathState = DeathState.CLEAR;
                secondsLoggedOff = 0;
            }
        } else { // if in CLEAR state then we will always remain CLEAR (we only leave it via death in doDeath())
            secondsLoggedOff = 0;
        }
        //getPlayer().sendMessage("new state " + deathState);
    }

    public void pauseCooldowns() {
        if(lastMessageTask != null) {
            lastMessageTask.cancel();
        }
        logOffTime = LocalDateTime.now();

        if(isJailed()) {
            Duration timeOnline = Duration.between(logOnTime, logOffTime);
            long secondsServedNow = Math.abs(timeOnline.getSeconds());
            secondsAlreadyServed += secondsServedNow;
        }
    }

    public void resumeCooldowns() {
        //getPlayer().sendMessage("resuming state is " + deathState);
        logOnTime = LocalDateTime.now();
        if(logOffTime == null || lastDeath == null) {
            return;
        }
        Duration timeLoggedOff = Duration.between(logOnTime, logOffTime);
        secondsLoggedOff += Math.abs(timeLoggedOff.getSeconds());

//        System.out.println("logoff time: " + logOffTime);
//        System.out.println("seconds logged off: " + secondsLoggedOff);

        Duration timeBetweenDeathLogoff = Duration.between(lastDeath, logOffTime);
        long secondsBetweenDeathLogoff = Math.abs(timeBetweenDeathLogoff.getSeconds());

        Player player = Bukkit.getPlayer(playerID);
        DelayedMessager messager = new DelayedMessager();
        if(canBack()) {
            if(secondsBetweenDeathLogoff < NoirPVPConfig.PROTECTION_DURATION) {
                int remainingTime = Math.abs((int) secondsBetweenDeathLogoff - NoirPVPConfig.PROTECTION_DURATION);
                lastMessageTask = messager.scheduleMessage(player, NoirPVPConfig.PLAYER_PROTECTION_END, remainingTime);
            }
        } else {
            if(secondsBetweenDeathLogoff < NoirPVPConfig.DOUBLE_PROTECTION_DURATION) {
                int remainingTime = Math.abs((int) secondsBetweenDeathLogoff - NoirPVPConfig.DOUBLE_PROTECTION_DURATION);
                lastMessageTask = messager.scheduleMessage(player, NoirPVPConfig.PLAYER_DOUBLE_END, remainingTime);
            }
        }
    }

    public LocalDateTime lastLoggedOff() {
        return logOffTime;
    }

    /**
     * Determines if a player can use the /back command.
     */
    public boolean canBack() {
        updateState();
        if(deathState.equals(DeathState.PROTECTED_COOLDOWN)) {
            return false;
        } else {
            LocalDateTime backUseEnd;
            if(lastRegularDeath == null) {
                if(lastDeath == null) {
                    return false;
                } else {
                    backUseEnd = lastDeath.plusSeconds(30);
                }
            } else if(lastDeath == null || lastDeath.isBefore(lastRegularDeath)) {
                backUseEnd = lastRegularDeath.plusSeconds(30);
            } else {
                backUseEnd = lastDeath.plusSeconds(30);
            }

            if(backUseEnd.isBefore(LocalDateTime.now())) {
                return false;
            } else {
                return true;
            }
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
        if(serialMap.containsKey("lastDeath")) { lastDeath = LocalDateTime.parse((String) serialMap.get("lastPVP")); }
        if(serialMap.containsKey("deathState")) { deathState = DeathState.valueOf((String) serialMap.get("deathState")); }
        if(serialMap.containsKey("logOffTime")) { logOffTime = LocalDateTime.parse((String) serialMap.get("logOffTime")); }

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
        if(lastDeath != null)
            serialMap.put("lastDeath", lastDeath.toString());
        if(deathState != null)
            serialMap.put("deathState", deathState.name());
        if(logOffTime != null)
            serialMap.put("logOffTime", logOffTime.toString());

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
