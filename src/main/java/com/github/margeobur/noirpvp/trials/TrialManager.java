package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Singleton class that manages queuing and scheduling of Trials.
 */
public class TrialManager {

    private static TrialManager instance;
    private TrialManager() {}

    private static final int TRIAL_DURATION_SECS = 60;
    public enum VoteResult { NO_TRIAL , ALREADY_VOTED , NOT_ALLOWED, SUCCESS }

    private Deque<Trial> trials = new ArrayDeque<>();

    public static TrialManager getInstance() {
        if(instance == null) {
            instance = new TrialManager();
        }
        return instance;
    }

    public void dispatchNewTrial(PVPPlayer _attacker) {
        Trial newTrial = new Trial(_attacker);
        trials.addLast(newTrial);
        tryDoNextTrial();
    }

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

        int durationInTicks = 20 * TRIAL_DURATION_SECS;
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
