package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * This class represents an abstracted timer (stopwatch) that uses a database to persistently keep time.
 * TimeTrackers can be serialised as Strings for easy storage as elements of a wider database structure.
 */
public class TimeTracker {

    private Plugin thePlugin;
    private long milisecondsAlreadyRun = 0; // the value of milisecondsPreviousSessions is included in here

    private boolean paused;
    private LocalDateTime firstStart;
    private LocalDateTime lastUpdate;
    private List<BukkitRunnable> activeTasks = new ArrayList<>();
    private List<CallbackAtTime> waitingCallbacks = new ArrayList<>();

    public TimeTracker(Plugin plugin) {
        thePlugin = plugin;
        lastUpdate = LocalDateTime.now();
        firstStart = lastUpdate;
        paused = false;
    }

    /**
     *
     * @param callback The callback to call at the time of
     * @param secondsAfterStart The number of seconds to wait before invoking the callback, measured
     *                          from the time the timer was first started
     */
    public void registerTimer(TimerCallback callback, int secondsAfterStart) {
        BukkitRunnable runnable;
        if(!paused) {
            updateMilisecondsRun();
            long milisecsToGo = 1000 * secondsAfterStart - milisecondsAlreadyRun;
            runnable = scheduleCallback(callback, milisecsToGo);
        }
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
            }
        };
        activeTasks.add(runnable);
        waitingCallbacks.add(activeTasks.indexOf(runnable), new CallbackAtTime(secondsAfterStart, callback));
    }

    /**
     *
     * @param callback The callback to call at the time of
     * @param secondsFromNow The number of seconds to wait before invoking the callback, measured
     *                       from the time this method is invoked
     */
    public void registerTimerFromNow(TimerCallback callback, int secondsFromNow) {
        BukkitRunnable runnable;
        if(!paused) {
            updateMilisecondsRun();
            runnable = scheduleCallback(callback, secondsFromNow * 1000);
        }
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
            }
        };
        int secondsAfterStart = (int) (milisecondsAlreadyRun/1000 + secondsFromNow);
        activeTasks.add(runnable);
        waitingCallbacks.add(activeTasks.indexOf(runnable), new CallbackAtTime(secondsAfterStart, callback));
    }

    /**
     * Pauses the timer.
     */
    public void pause() {
        updateMilisecondsRun();
        paused = true;
        for(BukkitRunnable timerTask: activeTasks) {
            timerTask.cancel();
            int seconds = waitingCallbacks.get(activeTasks.indexOf(timerTask)).getSecondsAfterStart();
            System.out.println("Pausing " + seconds + " second task with " +
                    (1000 * seconds - milisecondsAlreadyRun)/1000 + " seconds left");
        }
    }

    /**
     * Resumes the timer. All bukkitrunnables are started again
     */
    public void resume() {
        paused = false;
        int i = 0;
        for(CallbackAtTime callbackPair: waitingCallbacks) {
            BukkitRunnable newRunnable = scheduleCallback(callbackPair.getCallback(),
                    1000 * callbackPair.getSecondsAfterStart() - milisecondsAlreadyRun);
            activeTasks.set(i++, newRunnable);
        }
    }

    /**
     * @return the number of seconds for which this timer has run
     */
    public int getSecondsElapsed() {
        if(!paused) {
            updateMilisecondsRun();
        }
        return (int) milisecondsAlreadyRun / 1000;
    }

    /**
     * This method is wrapped by {@link TimeTracker#registerTimer(TimerCallback, int)} and
     * {@link TimeTracker#registerTimerFromNow(TimerCallback, int)}.
     *
     * It runs a wait task asynchronously and then calls a synchronous task that invokes the callback.
     */
    private BukkitRunnable scheduleCallback(TimerCallback callback, long miliseconds) {
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
                } catch (InterruptedException e) {
                    return;
                }
                if(paused) {
                    return;
                }
                runCallback.runTask(NoirPVPPlugin.getInstance());
                waitingCallbacks.remove(activeTasks.indexOf(this));
                activeTasks.remove(this);
            }
        };

        waitForTime.runTaskAsynchronously(thePlugin);
        return waitForTime;
    }

    /**
     * Called before any of the operations that use its value, this method updates the value of
     * milisecondsAlreadyRun to the instant the method is called.
     */
    public void updateMilisecondsRun() {
        if(paused) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        System.out.print("Updating at " + now + " by ");
        Duration timeOnline = Duration.between(now, lastUpdate);
        long milisecsServedNow = Math.abs(timeOnline.toMillis());
        System.out.print(milisecsServedNow + " miliseconds to ");
        milisecondsAlreadyRun += milisecsServedNow;
        System.out.print(milisecondsAlreadyRun + " miliseconds.\n");
        lastUpdate = now;
    }

    private class CallbackAtTime {
        private int secondsAfterStart;
        private TimerCallback callback;

        public CallbackAtTime(int secondsAfterStart, TimerCallback callback) {
            this.secondsAfterStart = secondsAfterStart;
            this.callback = callback;
        }

        public int getSecondsAfterStart() {
            return secondsAfterStart;
        }

        public TimerCallback getCallback() {
            return callback;
        }
    }

    public TimeTracker(Map<String, Object> serialMap) {
        if(serialMap.containsKey("milisecsAlreadyRun"))
            milisecondsAlreadyRun = (int) serialMap.get("milisecsAlreadyRun");

        if(serialMap.containsKey("firstStart")) {
            firstStart = LocalDateTime.parse((String) serialMap.get("firstStart"));
        }
    }

    public void serialize(Map<String, Object> serialMap) {
        if(!paused) {
            updateMilisecondsRun();
        }

        serialMap.put("milisecsAlreadyRun", milisecondsAlreadyRun);
        serialMap.put("firstStart", firstStart.toString());
    }
}
