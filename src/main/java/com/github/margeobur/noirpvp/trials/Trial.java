package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a trial in the NoirPVP judicial system. It has a defendant and a set of victims.
 */
public class Trial {

    private PVPPlayer defendant;

    private Set<PVPPlayer> victims;
    private LocalDateTime initiatedTime;
    private enum TrialState { PENDING, IN_PROGRESS, COMPLETED }

    private final int CRIME_MARK_MULTIPLIER = 10;
    private int guiltyVotes = 0;
    private int innocentVotes = 0;

    // The verdict. true if the defendant was found guilty
    private boolean isGuilty;

    private TrialState stateOfTrial;
    public Trial(PVPPlayer attacker) {
        initiatedTime = LocalDateTime.now();
        stateOfTrial = TrialState.PENDING;

        defendant = attacker;
        victims = attacker.getVictims();
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

    public Set<PVPPlayer> getVictims() {
        return victims;
    }

    public void start() {
        TrialEvent trialStartingEvent = new TrialEvent(TrialEvent.TrialEventType.START, this);
        Bukkit.getServer().getPluginManager().callEvent(trialStartingEvent);
    }

    public void end() {
        stateOfTrial = TrialState.COMPLETED;
        isGuilty = guiltyVotes > innocentVotes;

        TrialEvent trialEndingEvent = new TrialEvent(TrialEvent.TrialEventType.FINISH, this);
        Bukkit.getServer().getPluginManager().callEvent(trialEndingEvent);
    }

    public boolean getIsGuiltyVerdict() {
        return isGuilty;
    }

    public int getJailTimeMinutes() {
        return (defendant.getCrimeMarks() / 5) * CRIME_MARK_MULTIPLIER;
    }
}