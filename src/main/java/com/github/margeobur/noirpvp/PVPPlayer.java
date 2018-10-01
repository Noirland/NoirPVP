package com.github.margeobur.noirpvp;

import java.util.ArrayList;
import java.util.UUID;

public class PVPPlayer {

    private boolean _lastDamagePVP = false;
    private UUID _playerID;

    public PVPPlayer(UUID playerID) {
        _playerID = playerID;
    }

    public UUID getID() {
        return _playerID;
    }

    public boolean lastDamagePVP() {
        return _lastDamagePVP;
    }

    public void setLastDamagePVP(boolean value) {
        _lastDamagePVP = value;
    }

    public static PVPPlayer getPlayerByUUID(UUID id, ArrayList<PVPPlayer> players) {
        for(PVPPlayer player: players) {
            if(player.getID().equals(id)) {
                return player;
            }
        }
        return null;
    }
}
