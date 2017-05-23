/*
    Copyright 2016 Wira Mulia

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package org.osumercury.controlcenter;

import java.util.ArrayList;

/**
 *
 * @author wira
 */
public class SessionState {
    private int runs;
    private final int maxAttempts;
    private final long setupDuration;
    private final long windowDuration;    
    private long timerStart;
    private long timerPauseStarted;
    private long timerPaused;
    private boolean paused;
    private boolean stopped;
    private final Team activeTeam;
    private final ArrayList<Score> activeScoreList;
    private Score currentScore;
    
    public SessionState(Team t, int totalAttempts, long setupDuration, long windowDuration) {
        this.maxAttempts = totalAttempts;
        this.setupDuration = setupDuration;
        this.windowDuration = windowDuration;
        this.activeTeam = t;
        activeScoreList = new ArrayList();
        paused = false;
        stopped = false;
        runs = -1; // staging
        System.out.println("SessionState: new: " + totalAttempts + ", " +
                setupDuration + ", " + windowDuration);
    }
    
    public Team getActiveTeam() {
        return activeTeam;
    }
    
    public ArrayList<Score> getActiveScoreList() {
        return activeScoreList;
    }
    
    private void startTimer() {
        timerStart = System.currentTimeMillis();
        timerPaused = 0;
        paused = false;
    }
    
    public void pauseTimer() {
        if(paused)
            return;
        
        paused = true;
        timerPauseStarted = System.currentTimeMillis();
        ControlCenter.triggerEvent(UserEvent.SESSION_PAUSED, null);
    }
    
    public void resumeTimer() {
        if(!paused)
            return;
        
        timerPaused += (System.currentTimeMillis() - timerPauseStarted);
        paused = false;
        ControlCenter.triggerEvent(UserEvent.SESSION_RESUMED, null);
    }
    
    public long getElapsedTimeMilliseconds() {
        if(!paused) {
            return System.currentTimeMillis() - timerStart - timerPaused;
        } else {
            return timerPauseStarted - timerStart - timerPaused;
        }
    }
    
    public long getRemainingTimeMilliseconds() {
        long remaining = (runs == 0 ? setupDuration : windowDuration) - getElapsedTimeMilliseconds();
        return remaining < 0 ? 0 : remaining;
    }
    
    public long getHoursLeft() {
        return getRemainingTimeMilliseconds() / (60 * 60 * 1000);
    }
    
    public long getMinutesLeft() {
        return getRemainingTimeMilliseconds() / (60 * 1000);
    }
    
    public long getSecondsLeft() {
        return getRemainingTimeMilliseconds() / (1000);
    }
    
    public long getSetupDuration() {
        return setupDuration;
    }
    
    public long getWindowDuration() {
        return windowDuration;
    }
    
    public void addTimeSeconds(long addedTime) {
        timerStart += addedTime*1000;
        ControlCenter.triggerEvent(UserEvent.SESSION_TIME_ADDED, addedTime);
    }
    
    public synchronized void start() {
        runs = 0; // setup phase
        startTimer(); // start timer for SETUP
    }
    
    public synchronized void completeRun(boolean commit) {
        if(runs > maxAttempts) {
            Log.err("SessionState.completeRun: extra call to completeRun\n" + 
                    " - attempts: " + runs);
            return;
        } else if(runs == 0) {
            Log.err("SessionState.completeRun: unable to advance, " + 
                    "still in setup phase");
            return;
        }
        // commit score to database
        try {
            Data.lock().writeLock().lock();
            if(commit) {
                currentScore.setCompleted(true);            
                activeScoreList.add(currentScore);
                activeTeam.addScore(currentScore);
                ControlCenter.triggerEvent(UserEvent.SESSION_ATTEMPT_COMMITTED, 
                        runs);
            } else {
                activeScoreList.add(null);
                ControlCenter.triggerEvent(UserEvent.SESSION_ATTEMPT_DISCARDED, 
                        runs);
            }
        } finally {
            Data.lock().writeLock().unlock();
        }
        currentScore = new Score();
        runs++;
    }
    
    public synchronized void endSetup() {
        if(runs == 0) {
            currentScore = new Score();
            startTimer();
            runs++;
        } else {
            Log.err("SessionState.endSetup: we're not in setup mode!");
        }
    }
    
    public synchronized void modifyCurrentScore(String key, double value) {
        if(currentScore == null) {
            Log.err("SessionState.modifyCurrentScore: no current score available");
        } else {
            currentScore.setValue(key, value);
        }
    }
    
    public synchronized double getCurrentScoreValue(String key) {
        if(currentScore == null) {
            Log.err("SessionState.modifyCurrentScore: no current score available");
            return Double.NaN;
        } else {
            return currentScore.getValue(key);
        }
    }
    
    public synchronized int getRunNumber() {
        return runs;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public boolean isSetupFinished() {
        return stopped | runs > 0 | getRemainingTimeMilliseconds() == 0;
    }
    
    public boolean isFinished() {
        return stopped | getRemainingTimeMilliseconds() == 0 | runs > maxAttempts;
    }
    
    public void end() {
        stopped = true;
    }
}
