package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.PVPPlayer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JailTrial extends Trial {

    public enum JailTrialResult { INNOCENT, JAIL, KICK, BAN;}
    private JailTrialResult result;
    private String reason;

    private Map<UUID, JailTrialResult> voteMap = new HashMap<>();

    public JailTrial(PVPPlayer attacker, String reason) {
        super(attacker);
        this.reason = reason;
    }

    public JailTrial(PVPPlayer attacker, String reason, LocalDateTime initTime) {
        super(attacker, initTime);
        this.reason = reason;
    }

    @Override
    public void applyVerdict() {
        int[] voteCounts = new int[4];

        Set<UUID> voters = voteMap.keySet();
        for(UUID voterID: voters) {
            int index = voteMap.get(voterID).ordinal();
            voteCounts[index]++;
        }
        int maxInd = 0;
        for(int i = 0; i < voteCounts.length; i++) {
            if(voteCounts[i] > voteCounts[0]) {
                maxInd = i;
            }
        }

        switch(maxInd) {
            case 0:
                result = JailTrialResult.INNOCENT;
                break;
            case 1:
                result = JailTrialResult.JAIL;
                break;
            case 2:
                result = JailTrialResult.KICK;
                break;
            case 3:
                result = JailTrialResult.BAN;
        }
    }

    public void addVote(UUID voterID, JailTrialResult vote) {
        if(!voteMap.containsKey(voterID)) {
            voteMap.put(voterID, vote);
        }
    }

    @Override
    public boolean playerHasVoted(UUID voterID) {
        return voteMap.containsKey(voterID);
    }

    @Override
    public boolean getIsGuiltyVerdict() {
        return !result.equals(JailTrialResult.INNOCENT);
    }

    public JailTrialResult getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }
}
