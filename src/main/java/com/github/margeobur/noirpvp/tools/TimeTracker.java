package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * This class represents an abstracted timer (stopwatch) that uses a database to persistently keep time.
 * TimeTrackers can be serialised as Strings for easy storage as elements of a wider database structure.
 */
@SerializableAs("Timer")
public class TimeTracker implements ConfigurationSerializable {

    private Plugin thePlugin;
    private long milisecondsAlreadyRun = 0; // the value of milisecondsPreviousSessions is included in here

    private boolean paused;
    private LocalDateTime firstStart;
    private LocalDateTime lastUpdate;
    private Map<String, CallbackTask> waitingCallbacks = new HashMap<>();

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
    public void registerTimer(TimerCallback callback, int secondsAfterStart, String title) {
        CallbackTask task = new CallbackTask(title, callback, secondsAfterStart * 1000);
        if(!paused) {
            updateMilisecondsRun();
            task.startExecution();
        }

        waitingCallbacks.put(task.key, task);
    }

    /**
     *
     * @param callback The callback to call at the time of
     * @param secondsFromNow The number of seconds to wait before invoking the callback, measured
     *                       from the time this method is invoked
     */
    public void registerTimerFromNow(TimerCallback callback, int secondsFromNow, String title) {
        if(!paused) {
            updateMilisecondsRun();
        }
        long milisAfterStart = milisecondsAlreadyRun + secondsFromNow * 1000;
        registerTimer(callback, (int) milisAfterStart/1000, title);
    }

    /**
     * Pauses the timer.
     */
    public void pause() {
        updateMilisecondsRun();
        paused = true;
        for(Map.Entry<String, CallbackTask> timerTask: waitingCallbacks.entrySet()) {
            timerTask.getValue().setCancelled();
        }
    }

    /**
     * Resumes the timer. All bukkitrunnables are started again
     */
    public void resume() {
        paused = false;
        Map<String, CallbackTask> toRemove = new HashMap<>(waitingCallbacks);
        for(Map.Entry<String, CallbackTask> timerTask: toRemove.entrySet()) {
            waitingCallbacks.remove(timerTask.getKey());
            CallbackTask newTask = new CallbackTask(timerTask.getKey(),
                    timerTask.getValue().callback, timerTask.getValue().milisecsAfterStart);
            waitingCallbacks.put(timerTask.getKey(), newTask);
            newTask.startExecution();
        }
    }

    /**
     * Reset the state of this timer, essentially building a new one
     */
    public void reset() {
        for(Map.Entry<String, CallbackTask> timerTask: waitingCallbacks.entrySet()) {
            timerTask.getValue().cancel();
        }
        waitingCallbacks.clear();
        lastUpdate = LocalDateTime.now();
        firstStart = lastUpdate;
        paused = false;
        milisecondsAlreadyRun = 0;
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
     * Called before any of the operations that use its value, this method updates the value of
     * milisecondsAlreadyRun to the instant the method is called.
     */
    private void updateMilisecondsRun() {
        if(paused) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Duration timeOnline = Duration.between(now, lastUpdate);
        long milisecsServedNow = Math.abs(timeOnline.toMillis());
        milisecondsAlreadyRun += milisecsServedNow;
        lastUpdate = now;
    }

    private class CallbackTask extends BukkitRunnable {

        String key;
        TimerCallback callback;
        long milisecsAfterStart;
        boolean cancelled = true;
        private long milisecs;

        CallbackTask(String key, TimerCallback callback, long miliseconds) {
            int i = 0;
            while(waitingCallbacks.containsKey(key)) {
                key = key + i++;
            }
            this.key = key;
            this.callback = callback;
            this.milisecsAfterStart = miliseconds;
        }

        void startExecution() {
            milisecs = milisecsAfterStart - milisecondsAlreadyRun;
            cancelled = false;
            runTaskAsynchronously(thePlugin);
        }

        void setCancelled() {
            cancelled = true;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(milisecs);
            } catch (InterruptedException e) {
                return;
            }
            if(paused || cancelled) {
                return;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(waitingCallbacks.containsKey(key)) {
                        waitingCallbacks.remove(key);
                    } else {
                        System.out.println("The callback wasn't present while trying to remove it");
                        System.out.println(key + ": " + CallbackTask.this);
                    }
                    callback.onTimerEnd();
                }
            }.runTask(thePlugin);
        }
    };

    public TimeTracker(Map<String, Object> serialMap) {
        if(serialMap.containsKey("milisecsAlreadyRun"))
            milisecondsAlreadyRun = (int) serialMap.get("milisecsAlreadyRun");

        if(serialMap.containsKey("firstStart")) {
            firstStart = LocalDateTime.parse((String) serialMap.get("firstStart"));
        }
        lastUpdate = LocalDateTime.now();
        paused = true;
        thePlugin = NoirPVPPlugin.getInstance();
    }

    public Map<String, Object> serialize() {
        Map<String, Object> serialMap = new HashMap<>();
        if(!paused) {
            updateMilisecondsRun();
        }

        serialMap.put("milisecsAlreadyRun", milisecondsAlreadyRun);
        serialMap.put("firstStart", firstStart.toString());
        return serialMap;
    }

    public void printoutTasks() {
        System.out.println("Tasks:");
        int i = 0;
        for(Map.Entry<String, CallbackTask> task: waitingCallbacks.entrySet()) {
            System.out.println("\t[" + i++ + ": " + task.getValue().key + "] " + task.toString());
            System.out.println("\tto be called " + task.getValue().milisecsAfterStart/1000 + " s after start.");
        }
    }
}
