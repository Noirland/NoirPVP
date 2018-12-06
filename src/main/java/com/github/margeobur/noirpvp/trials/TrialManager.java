package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.FSDatabase;
import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.tools.TimeTracker;
import com.github.margeobur.noirpvp.tools.TimerCallback;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;

/**
 * Singleton class that manages queuing and scheduling of Trials.
 */
public class TrialManager {

    private static TrialManager instance;
    public enum VoteResult { NO_TRIAL, ALREADY_VOTED, NOT_ALLOWED, WRONG_TYPE, SUCCESS }

    private Deque<Trial> queuedTrials = new ArrayDeque<>();
    private List<Trial> cancelledSentences = new ArrayList<>();
    private List<Trial> offlineTrials = new ArrayList<>();
    private List<Trial> releaseTrials = new ArrayList<>();

    public static TrialManager getInstance() {
        if(instance == null) {
            instance = new TrialManager();
        }
        return instance;
    }

    private TrialManager() {
        FSDatabase database = FSDatabase.getInstance();

        List<Trial> queuedRegularTrials = database.getTrials("queued-trials");
        List<JailTrial> queuedJailTrials = database.getJailTrials("queued-jail-trials");
        if(queuedRegularTrials != null) {
            for (Trial queuedTrial : queuedRegularTrials) {
                queuedTrials.addLast(queuedTrial);
            }
        }
        if(queuedJailTrials != null) {
            for(Trial queuedTrial: queuedJailTrials) {
                queuedTrials.addLast(queuedTrial);
            }
        }

        List<Trial> offlineRegTrials = database.getTrials("offline-trials");
        List<JailTrial> offlineJailTrials = database.getJailTrials("offline-jail-trials");
        if(offlineRegTrials != null) {
            offlineTrials.addAll(offlineRegTrials);
        }
        if(offlineJailTrials != null) {
            offlineTrials.addAll(offlineJailTrials);
        }

        if(!queuedTrials.isEmpty()) {
            NoirPVPPlugin.getInstance().getLogger().log(Level.INFO, "Found trials");
            tryDoNextTrial();
        }
    }

    public void dispatchNewTrial(PVPPlayer attacker) {
        Trial newTrial = new Trial(attacker);
        queuedTrials.addLast(newTrial);

        // Try start the trial after a 5 second wait period
        new BukkitRunnable() {
            @Override
            public void run() {
                tryDoNextTrial();
            }
        }.runTaskLater(NoirPVPPlugin.getInstance(), 5 * 20);
    }

    public void dispatchNewJailTrial(PVPPlayer player, String reason) {
        JailTrial newTrial = new JailTrial(player, reason);
        queuedTrials.addLast(newTrial);

        new BukkitRunnable() {
            @Override
            public void run() {
                tryDoNextTrial();
            }
        }.runTaskLater(NoirPVPPlugin.getInstance(), 5 * 20);
    }

    private void tryDoNextTrial() {
        if(queuedTrials.isEmpty()) {
            return;
        }
        Trial mostRecentTrial = queuedTrials.getFirst();
        if(mostRecentTrial.isInProgress()) {
            return;
        } else if(mostRecentTrial.isComplete()) {
            queuedTrials.remove(mostRecentTrial);
            mostRecentTrial = queuedTrials.getFirst();
        }
        // if the trial at the HEAD of the queue is neither in progress nor complete, then
        // it must be pending, so we can start it and schedule its completion.

        if(mostRecentTrial.getDefendant().getPlayer() == null || !mostRecentTrial.getDefendant().getPlayer().isOnline()) {
            offlineTrials.add(mostRecentTrial);
            queuedTrials.remove(mostRecentTrial);
            tryDoNextTrial();
        } else {
            mostRecentTrial.start();
            scheduleTrialEnd();
        }
    }

    private void scheduleTrialEnd() {
        TimerCallback trialEndTask = () -> {
            Trial currentTrial = queuedTrials.peekFirst();
            if(currentTrial.getDefendant().getPlayer() == null || !currentTrial.getDefendant().getPlayer().isOnline()) {
                offlineTrials.add(currentTrial);
                queuedTrials.remove(currentTrial);
                currentTrial.reset();
                return;
            }
            currentTrial.end();
            queuedTrials.remove(currentTrial);
            if((currentTrial instanceof JailTrial) &&
                    ((JailTrial) currentTrial).getResult().equals(JailTrial.JailTrialResult.JAIL) ||
               !(currentTrial instanceof JailTrial) &&
                    currentTrial.getIsGuiltyVerdict()) {
                scheduleJailRelease(currentTrial, 0);
                releaseTrials.add(currentTrial);
            }
            tryDoNextTrial();
        };

        TimeTracker timer = new TimeTracker(NoirPVPPlugin.getInstance());
        timer.registerTimer(trialEndTask, NoirPVPConfig.TRIAL_DURATION, "trial");
        timer.resume();
    }

    public void unjailPlayer(PVPPlayer playerPVP) {
        for(Trial trial: releaseTrials) {
            if(trial.getDefendant().equals(playerPVP)) {
                trial.releasePlayer();
                releaseTrials.remove(trial);
                return;
            }
        }
    }

    private void scheduleJailRelease(Trial finishedTrial, int alreadyServed) {
        TimerCallback trialEndTask = () -> {
                if(finishedTrial.getDefendant().getPlayer() == null || !finishedTrial.getDefendant().getPlayer().isOnline()) {
                    //cooldowns and jail time will have been paused when the player went offline
                    return;
                }
                if(finishedTrial.getDefendant().isJailed()) {
                    finishedTrial.releasePlayer();
                    releaseTrials.remove(finishedTrial);
                }
        };

        TimeTracker timer = new TimeTracker(NoirPVPPlugin.getInstance());
        timer.registerTimer(trialEndTask, (finishedTrial.getJailTimeSeconds() - alreadyServed), "trialEnd");
        timer.resume();
        finishedTrial.setReleaseTimer(timer);
    }

    public PVPPlayer currentDefendant() {
        if(!queuedTrials.isEmpty() && queuedTrials.peekFirst().isInProgress()) {
            return queuedTrials.peekFirst().getDefendant();
        }
        return null;
    }

    public VoteResult addVoteToJailTrial(UUID voterID, JailTrial.JailTrialResult vote) {
        Trial cTrial = queuedTrials.peekFirst();
        if(cTrial == null) {
            return VoteResult.NO_TRIAL;
        }
        if(!(cTrial instanceof JailTrial)) {
            return VoteResult.WRONG_TYPE;
        }
        JailTrial currentTrial = (JailTrial) cTrial;

        if(!currentTrial.isInProgress()) {
            return VoteResult.NO_TRIAL;
        }

        if(currentTrial.playerHasVoted(voterID)) {
            return VoteResult.ALREADY_VOTED;
        }

        if(voterID.equals(currentTrial.getDefendant().getPlayer().getUniqueId())) {
            return VoteResult.NOT_ALLOWED;
        }

        currentTrial.addVote(voterID, vote);
        return VoteResult.SUCCESS;
    }

    public VoteResult addVoteToCurrentTrial(UUID voterID, boolean voteIsGuilty) {
        Trial currentTrial = queuedTrials.peekFirst();
        if(currentTrial == null || !currentTrial.isInProgress()) {
            return VoteResult.NO_TRIAL;
        }

        if(currentTrial.playerHasVoted(voterID)) {
            return VoteResult.ALREADY_VOTED;
        }

        if(voterID.equals(currentTrial.getDefendant().getID())) {
            return VoteResult.NOT_ALLOWED;
        }

        currentTrial.addVote(voterID, voteIsGuilty);
        return VoteResult.SUCCESS;
    }

    public void pauseAllTrials() {
        FSDatabase database = FSDatabase.getInstance();
        if(queuedTrials.peekFirst() != null && queuedTrials.peekFirst().isInProgress()) {
            queuedTrials.peekFirst().getReleaseTimer().reset();
            queuedTrials.peekFirst().reset();
        }

        List<Trial> queuedRegularTrials = new ArrayList<>();
        List<JailTrial> queuedJailTrials = new ArrayList<>();
        for(Trial queuedTrial: queuedTrials) {
            if(queuedTrial instanceof JailTrial) {
                queuedJailTrials.add((JailTrial) queuedTrial);
            } else {
                queuedRegularTrials.add(queuedTrial);
            }
        }
        if(!queuedRegularTrials.isEmpty())
            database.saveTrials(queuedRegularTrials, "queued-trials");

        if(!queuedJailTrials.isEmpty())
            database.saveJailTrials(queuedJailTrials, "queued-jail-trials");

        List<Trial> offlineRegTrials = new ArrayList<>();
        List<JailTrial> offlineJailTrials = new ArrayList<>();
        for(Trial offlineTrial: offlineTrials) {
            if(offlineTrial instanceof JailTrial) {
                offlineJailTrials.add((JailTrial) offlineTrial);
            } else {
                offlineRegTrials.add(offlineTrial);
            }
        }
        if(!offlineRegTrials.isEmpty())
            database.saveTrials(offlineRegTrials, "offline-trials");

        if(!offlineJailTrials.isEmpty())
            database.saveJailTrials(offlineJailTrials, "offline-jail-trials");
    }

    public void rescheduleJailReleasePotentially(UUID playerID) {
        JailCell.refreshJailShortlist();
        Map<UUID, Integer> convictIDs = JailCell.getJailShortlist();
        Set<UUID> ids = convictIDs.keySet();
        for(UUID convictID: ids) {
            if(convictID.equals(playerID)) {
                PVPPlayer convictPVP = PVPPlayer.getPlayerByUUID(playerID);
                if(!convictPVP.isJailed()) {
                    return;
                }

                for(Trial trial: releaseTrials) {
                    if(trial.getDefendant().equals(convictPVP)) {
                        scheduleJailRelease(trial, (int) convictPVP.getTimeAlreadyServed());
                        return;
                    }
                }

                LocalDateTime lastConviction = convictPVP.getLastConviction();
                Trial finishedTrial;
                if (convictPVP.wasAdminJailed()) {
                    finishedTrial = new JailTrial(convictPVP, "", lastConviction.minusMinutes(1));
                } else {
                    finishedTrial = new Trial(convictPVP, lastConviction.minusMinutes(1));
                }
                releaseTrials.add(finishedTrial);

                scheduleJailRelease(finishedTrial, (int) convictPVP.getTimeAlreadyServed());
            }
        }
    }

    public void retryOfflineTrialPotentially(UUID playerID) {
        for(Trial unfinishedTrial: offlineTrials) {
            if(unfinishedTrial.getDefendant().getID().equals(playerID)) {
                if(unfinishedTrial.isComplete()) {
                    unfinishedTrial.end();
                    offlineTrials.remove(unfinishedTrial);
                    if((unfinishedTrial instanceof JailTrial) &&
                            ((JailTrial) unfinishedTrial).getResult().equals(JailTrial.JailTrialResult.JAIL) ||
                            !(unfinishedTrial instanceof JailTrial) &&
                                    unfinishedTrial.getIsGuiltyVerdict()) {
                        scheduleJailRelease(unfinishedTrial, 0);
                    }
                } else {
                    offlineTrials.remove(unfinishedTrial);
                    queuedTrials.addLast(unfinishedTrial);
                    tryDoNextTrial();
                }
                return;
            }
        }
    }
}
