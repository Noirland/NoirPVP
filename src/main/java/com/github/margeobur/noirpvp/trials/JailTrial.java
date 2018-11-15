package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.configuration.serialization.SerializableAs;

import java.time.LocalDateTime;
import java.util.*;

@SerializableAs("JailTrial")
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
            if(voteCounts[i] > voteCounts[maxInd]) {
                maxInd = i;
            }
        }

        switch(maxInd) {
            case 0:
                result = JailTrialResult.INNOCENT;
                defendant.findInnocent(true);
                break;
            case 1:
                result = JailTrialResult.JAIL;
                defendant.findGuilty(true);
                break;
            case 2:
                result = JailTrialResult.KICK;
                break;
            case 3:
                result = JailTrialResult.BAN;
        }
    }

    @Override
    public void addVote(UUID voterID, boolean isGuiltyVote) {
        if(!voteMap.containsKey(voterID)) {
            if(isGuiltyVote) {
                voteMap.put(voterID, JailTrialResult.JAIL);
            } else {
                voteMap.put(voterID, JailTrialResult.INNOCENT);
            }
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

    @Override
    public int getJailTimeSeconds() {
        return NoirPVPConfig.ADMIN_JAIL_TIME * 60;
    }

    public JailTrialResult getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public JailTrial(Map<String, Object> serialMap) {
        super(serialMap);

        if(serialMap.containsKey("reason")) {
            reason = (String) serialMap.get("reason");
        }
        if(serialMap.containsKey("result")) {
            result = JailTrialResult.valueOf((String) serialMap.get("result"));
        }
        if(serialMap.containsKey("votes")) {
            List<String> voteStrings = (List<String>) serialMap.get("votes");
            for(String encodedVote: voteStrings) {
                int firstUUIDChar = encodedVote.indexOf("{") + 1;
                int colon = encodedVote.indexOf(":");
                int closingBrace = encodedVote.indexOf("}");
                UUID victimID = UUID.fromString(encodedVote.substring(firstUUIDChar, colon));
                JailTrialResult voteOption = JailTrialResult.valueOf(encodedVote.substring(colon + 1, closingBrace));
                voteMap.put(victimID, voteOption);
            }
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> serialMap = super.serialize();

        serialMap.put("reason", reason);
        if(result != null) {
            serialMap.put("result", result.name());
        }

        List<String> voteStrings = new ArrayList<>();
        Set<Map.Entry<UUID, JailTrialResult>> votes = voteMap.entrySet();
        for(Map.Entry<UUID, JailTrialResult> vote: votes) {
            String encodedVote = "{" + vote.getKey().toString() + ":"
                    + vote.getValue().name() + "}";
            voteStrings.add(encodedVote);
        }
        return serialMap;
    }
}
