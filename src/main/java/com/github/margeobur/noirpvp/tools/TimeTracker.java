package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import javafx.util.Callback;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class represents an abstracted timer (stopwatch) that uses a database to persistently keep time.
 * TimeTrackers can be serialised as Strings for easy storage as elements of a wider database structure.
 */
public class TimeTracker {

    private Plugin thePlugin;
    private long milisecondsPreviousSessions = 0;
    private long milisecondsAlreadyRun = 0; // the value of milisecondsPreviousSessions is included in here

    private LocalDateTime lastUpdate;
    private LocalDateTime lastPause;
    private LocalDateTime lastResume;

    public TimeTracker(Plugin plugin) {
        thePlugin = plugin;
        lastUpdate = LocalDateTime.now();
    }

    /**
     *
     * @param callback The callback to call at the time of
     * @param secondsAfterStart The number of seconds to wait before invoking the callback, measured
     *                          from the time the timer was first started
     */
    public void registerTimer(TimerCallback callback, int secondsAfterStart) {
        updateMilisecondsRun();
        long milisecsToGo = 1000 * secondsAfterStart - milisecondsAlreadyRun;
        scheduleCallback(callback, milisecsToGo);
    }

    /**
     *
     * @param callback The callback to call at the time of
     * @param secondsFromNow The number of seconds to wait before invoking the callback, measured
     *                       from the time this method is invoked
     */
    public void registerTimerFromNow(TimerCallback callback, int secondsFromNow) {
        scheduleCallback(callback, secondsFromNow * 1000);
    }

    /**
     * Pauses the timer.
     */
    public void pause() {
        updateMilisecondsRun();

    }

    /**
     * This method is wrapped by {@link TimeTracker#registerTimer(TimerCallback, int)} and
     * {@link TimeTracker#registerTimerFromNow(TimerCallback, int)}.
     *
     * It runs a wait task asynchronously and then calls a synchronous task that invokes the callback.
     */
    private void scheduleCallback(TimerCallback callback, long miliseconds) {
        BukkitRunnable runCallback = new BukkitRunnable() {
            @Override
            public void run() {
                callback.onTimerEnd();
            }
        };

        BukkitRunnable waitForTime = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(miliseconds);
                } catch (InterruptedException e) { }
                runCallback.runTask(NoirPVPPlugin.getInstance());
            }
        };

        waitForTime.runTaskAsynchronously(thePlugin);
    }

    /**
     * Called before any of the operations that use its value, this method updates the value of
     * milisecondsAlreadyRun to the instant the method is called.
     */
    public void updateMilisecondsRun() {
        LocalDateTime now = LocalDateTime.now();
        Duration timeOnline = Duration.between(now, lastUpdate);
        long secondsServedNow = Math.abs(timeOnline.getSeconds());
        milisecondsAlreadyRun += secondsServedNow;
        lastUpdate = now;
    }
}
