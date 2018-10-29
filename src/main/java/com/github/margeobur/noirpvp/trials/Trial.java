package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a trial in the NoirPVP judicial system. It has a defendant and a set of victims.
 */
public class Trial {

    private PVPPlayer defendant;

    private Set<UUID> victims;
    private LocalDateTime initiatedTime;
    private enum TrialState { PENDING, IN_PROGRESS, COMPLETED }

    private int guiltyVotes = 0;
    private int innocentVotes = 0;
    private Map<UUID, Boolean> voteMap = new HashMap<>();

    // The verdict. true if the defendant was found guilty
    private boolean isGuilty;

    private TrialState stateOfTrial;
    public Trial(PVPPlayer attacker) {
        initiatedTime = LocalDateTime.now();
        stateOfTrial = TrialState.PENDING;

        defendant = attacker;
        victims = attacker.getVictims();

        TrialEvent trialStartingEvent = new TrialEvent(TrialEvent.TrialEventType.INIT, this);
        Bukkit.getServer().getPluginManager().callEvent(trialStartingEvent);
    }

    public boolean isInProgress() {
        return stateOfTrial.equals(TrialState.IN_PROGRESS);
    }

    public boolean isComplete() {
        return stateOfTrial.equals(TrialState.COMPLETED);
    }

    public PVPPlayer getDefendant() {
        return defendant;
    }

    public Set<UUID> getVictims() {
        return victims;
    }

    void start() {
        stateOfTrial = TrialState.IN_PROGRESS;
        TrialEvent trialStartingEvent = new TrialEvent(TrialEvent.TrialEventType.START, this);
        Bukkit.getServer().getPluginManager().callEvent(trialStartingEvent);
    }

    void end() {
        stateOfTrial = TrialState.COMPLETED;
        isGuilty = guiltyVotes > innocentVotes;

        if(isGuilty) {
            defendant.findGuilty();
        } else {
            defendant.findInnocent();
        }

        TrialEvent trialEndingEvent = new TrialEvent(TrialEvent.TrialEventType.FINISH, this);
        Bukkit.getServer().getPluginManager().callEvent(trialEndingEvent);
    }

    public boolean getIsGuiltyVerdict() {
        return isGuilty;
    }

    public int getJailTimeSeconds() {
        return (defendant.getCrimeMarks() / 5) * NoirPVPConfig.CRIME_MARK_MULTIPLIER * 60;
    }

    public boolean playerHasVoted(UUID voterID) {
        return voteMap.containsKey(voterID);
    }

    public void addVote(UUID voterID, boolean isGuiltyVote) {
        if(!voteMap.containsKey(voterID)) {
            voteMap.put(voterID, isGuiltyVote);
            if(isGuiltyVote) {
                guiltyVotes++;
            } else {
                innocentVotes++;
            }
        }
    }
}
