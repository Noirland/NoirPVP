package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Singleton class that manages queuing and scheduling of Trials.
 */
public class TrialManager {

    private static TrialManager instance;
    public enum VoteResult { NO_TRIAL , ALREADY_VOTED , NOT_ALLOWED, SUCCESS }

    private Deque<Trial> trials = new ArrayDeque<>();
    private List<Trial> releaseTrials = new ArrayList<>();

    public static TrialManager getInstance() {
        if(instance == null) {
            instance = new TrialManager();
        }
        return instance;
    }

    private TrialManager() { }

    public void dispatchNewTrial(PVPPlayer attacker) {
        Trial newTrial = new Trial(attacker);
        trials.addLast(newTrial);

        // Try start the trial after a 5 second wait period
        new BukkitRunnable() {
            @Override
            public void run() {
                tryDoNextTrial();
            }
        }.runTaskLater(NoirPVPPlugin.getPlugin(), 5 * 20);
    }

//    public void jailPlayer(PVPPlayer player) {
//
//    }

    private void tryDoNextTrial() {
        if(trials.isEmpty()) {
            return;
        }
        Trial mostRecentTrial = trials.getFirst();
        if(mostRecentTrial.isInProgress()) {
            return;
        } else if(mostRecentTrial.isComplete()) {
            trials.remove(mostRecentTrial);
            mostRecentTrial = trials.getFirst();
        }
        // if the trial at the HEAD of the queue is neither in progress nor complete, then
        // it must be pending, so we can start it and schedule its completion.

        mostRecentTrial.start();
        scheduleTrialEnd();
    }

    private void scheduleTrialEnd() {
        BukkitRunnable trialEndTask = new BukkitRunnable() {
            @Override
            public void run() {
                Trial currentTrial = trials.peekFirst();
                currentTrial.end();
                trials.remove(currentTrial);
                if(currentTrial.getIsGuiltyVerdict()) {
                    scheduleJailRelease(currentTrial, 0);
                }
                tryDoNextTrial();
            }
        };

        int durationInTicks = 20 * NoirPVPConfig.TRIAL_DURATION;
        trialEndTask.runTaskLater(NoirPVPPlugin.getPlugin(), durationInTicks);
    }

    private void scheduleJailRelease(Trial finishedTrial, int alreadyServed) {
        BukkitRunnable jailReleaseTask = new BukkitRunnable() {
            @Override
            public void run() {
                finishedTrial.releasePlayer();
                releaseTrials.remove(finishedTrial);
            }
        };

        int delayInTicks = 20 * finishedTrial.getJailTimeSeconds() - alreadyServed;
        jailReleaseTask.runTaskLater(NoirPVPPlugin.getPlugin(), delayInTicks);
    }

    public PVPPlayer currentDefendant() {
        if(!trials.isEmpty() && trials.peekFirst().isInProgress()) {
            return trials.peekFirst().getDefendant();
        }
        return null;
    }

    public VoteResult addVoteToCurrentTrial(UUID voterID, boolean voteIsGuilty) {
        Trial currentTrial = trials.peekFirst();
        if(currentTrial == null || !currentTrial.isInProgress()) {
            return VoteResult.NO_TRIAL;
        }

        if(currentTrial.playerHasVoted(voterID)) {
            return VoteResult.ALREADY_VOTED;
        }

        if(voterID.equals(currentTrial.getDefendant().getPlayer().getUniqueId())) {
            return VoteResult.NOT_ALLOWED;
        }

        currentTrial.addVote(voterID, voteIsGuilty);
        return VoteResult.SUCCESS;
    }

    public void rescheduleTrialPotentially(UUID playerID) {
        JailCell.refreshJailShortlist();
        List<UUID> convictIDs = JailCell.getJailShortlist();
        for(UUID convictID: convictIDs) {
            if(convictID.equals(playerID)) { // should be unnecessary
                PVPPlayer convictPVP = PVPPlayer.getPlayerByUUID(playerID);
                if(convictPVP.lastLoggedOff() == null || !convictPVP.isJailed()) {
                    return;
                }

                LocalDateTime lastConviction = convictPVP.getLastConviction();
                Trial finishedTrial = new Trial(convictPVP, lastConviction.minusMinutes(1));
                releaseTrials.add(finishedTrial);

                scheduleJailRelease(finishedTrial, (int) convictPVP.getTimeAlreadyServed());
            }
        }
    }
}
