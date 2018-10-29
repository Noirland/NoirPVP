package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Singleton class that manages queuing and scheduling of Trials.
 */
public class TrialManager {

    private static TrialManager instance;
    public enum VoteResult { NO_TRIAL , ALREADY_VOTED , NOT_ALLOWED, SUCCESS }

    private Deque<Trial> trials = new ArrayDeque<>();

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
                tryDoNextTrial();
            }
        };

        int durationInTicks = 20 * NoirPVPConfig.TRIAL_DURATION;
        trialEndTask.runTaskLater(NoirPVPPlugin.getPlugin(), durationInTicks);
    }

    public boolean trialInProgress() {
        if(trials.isEmpty()) {
            return false;
        }
        return trials.peekFirst().isInProgress();
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
}
