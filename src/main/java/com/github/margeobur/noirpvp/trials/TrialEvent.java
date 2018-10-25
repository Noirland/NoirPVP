package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

/**
 * The primary purpose of this class is to wrap a Trial object in an Event
 */
public final class TrialEvent extends Event {

    private Trial theTrial;
    public enum TrialEventType { START, FINISH }
    private TrialEventType type;

    private static HandlerList _handlers = new HandlerList();

    TrialEvent(TrialEventType type, Trial trial) {
        this.type = type;
        theTrial = trial;
    }

    public TrialEventType getType() {
        return type;
    }

    public Trial getTrial() {
        return theTrial;
    }

    @Override
    public HandlerList getHandlers() {
        return _handlers;
    }

    public static HandlerList getHandlerList() {
        return _handlers;
    }
}
