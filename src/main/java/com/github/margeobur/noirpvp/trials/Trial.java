package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a trial in the NoirPVP judicial system. It has a defendant and a set of victims.
 */
@SerializableAs("Trial")
public class Trial implements ConfigurationSerializable {

    protected PVPPlayer defendant;

    private Set<UUID> victims = new HashSet<>();
    private LocalDateTime initiatedTime;
    protected enum TrialState { PENDING, IN_PROGRESS, COMPLETED }

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

    public Trial(PVPPlayer attacker, LocalDateTime initTime) {
        initiatedTime = initTime;
        stateOfTrial = TrialState.COMPLETED;

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

    public Set<UUID> getVictims() {
        return victims;
    }

    void start() {
        stateOfTrial = TrialState.IN_PROGRESS;
        TrialEvent trialStartingEvent = new TrialEvent(TrialEvent.TrialEventType.START, this);
        Bukkit.getServer().getPluginManager().callEvent(trialStartingEvent);
    }

    void reset() {
        stateOfTrial = TrialState.PENDING;
    }

    void end() {
        stateOfTrial = TrialState.COMPLETED;

        applyVerdict();

        TrialEvent trialEndingEvent = new TrialEvent(TrialEvent.TrialEventType.FINISH, this);
        Bukkit.getServer().getPluginManager().callEvent(trialEndingEvent);
    }

    void applyVerdict() {
        isGuilty = guiltyVotes > innocentVotes;
        if(isGuilty) {
            defendant.findGuilty(false);
        } else {
            defendant.findInnocent(false);
        }
    }

    void releasePlayer() {
        defendant.releaseFromJail();

        TrialEvent trialResolvedEvent = new TrialEvent(TrialEvent.TrialEventType.RELEASE, this);
        Bukkit.getServer().getPluginManager().callEvent(trialResolvedEvent);
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

    public Trial(Map<String, Object> serialMap) {
        if(serialMap.containsKey("defendant")) {
            UUID defendantID = UUID.fromString((String) serialMap.get("defendant"));
            defendant = PVPPlayer.getPlayerByUUID(defendantID);
        }
        if(serialMap.containsKey("victims")) {
            List<String> idStrings = (List<String>) serialMap.get("victims");
            for(String idStr: idStrings) {
                victims.add(UUID.fromString(idStr));
            }
        }
        if(serialMap.containsKey("initiated-time"))
            initiatedTime = LocalDateTime.parse((String) serialMap.get("initiated-time"));
        if(serialMap.containsKey("guiltyVotes"))
            guiltyVotes = (int) serialMap.get("guiltyVotes");
        if(serialMap.containsKey("innocentVotes"))
            innocentVotes = (int) serialMap.get("innocentVotes");
        if(serialMap.containsKey("isGuilty"))
            isGuilty = (boolean) serialMap.get("isGuilty");

        if(serialMap.containsKey("voteMap")) {
            List<String> voteStrs = (List<String>) serialMap.get("voteMap");
            for(String encodedVote: voteStrs) {
                int firstUUIDChar = encodedVote.indexOf("{") + 1;
                int colon = encodedVote.indexOf(":");
                int closingBrace = encodedVote.indexOf("}");
                UUID victimID = UUID.fromString(encodedVote.substring(firstUUIDChar, colon));
                boolean voteOption = Boolean.valueOf(encodedVote.substring(colon + 1, closingBrace));
                voteMap.put(victimID, voteOption);
            }
        }

        if(serialMap.containsKey("state")) {
            stateOfTrial = TrialState.valueOf((String) serialMap.get("state"));
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> serialMap = new HashMap<>();

        serialMap.put("defendant", defendant.getID().toString());
        List<String> victimIDs = new ArrayList<>();
        for(UUID vicID: victims) {
            victimIDs.add(vicID.toString());
        }
        serialMap.put("victims", victimIDs);
        serialMap.put("initiated-time", initiatedTime.toString());
        serialMap.put("guiltyVotes", guiltyVotes);
        serialMap.put("innocentVotes", innocentVotes);
        if(isComplete())
            serialMap.put("isGuilty", isGuilty);

        List<String> voteStrs = new ArrayList<>();
        for(Map.Entry<UUID, Boolean> vote: voteMap.entrySet()) {
            String encodedVote = "{" + vote.getKey() + ":" +
                    vote.getValue() + "}";
            voteStrs.add(encodedVote);
        }
        if(!voteStrs.isEmpty()) {
            serialMap.put("voteMap", voteStrs);
        }

        serialMap.put("state", stateOfTrial.name());

        return serialMap;
    }
}
