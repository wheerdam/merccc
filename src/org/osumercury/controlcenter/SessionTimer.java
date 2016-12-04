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

/**
 *
 * @author wira
 */
public class SessionTimer extends Thread {
    private boolean stop = false;
    private final SessionState r;
    private final CompetitionState c;
    
    private static final long REFRESH_RATE_MS = 50;
    private static final long MIN_CPU_SLEEP_MS = 5;
    
    public SessionTimer(CompetitionState c) {
        this.c = c;
        this.r = c.getSession();
    }
    
    @Override
    public void run() {
        Log.d(0, "SessionTimer: run");
        long startLoopTime, loopTimeUsed;
        while(!stop) {
            startLoopTime = System.currentTimeMillis();
            if(r.getRunNumber() == 0 && r.isSetupFinished()) {
                c.setState(CompetitionState.RUN);
            }
            if(r.isFinished()) {
                stop = true;
                c.setState(CompetitionState.POST_RUN);
                SoundPlayer.play("window-end.wav");
            }
            loopTimeUsed = System.currentTimeMillis() - startLoopTime;
            try {
                if((loopTimeUsed + MIN_CPU_SLEEP_MS) < REFRESH_RATE_MS) {
                    Thread.sleep(REFRESH_RATE_MS - loopTimeUsed);
                } else {
                    Thread.sleep(MIN_CPU_SLEEP_MS);
                }
            } catch(Exception e) {
                
            }
        }
        Log.d(0, "SessionTimer: exit");
    }
    
    public void stopTimer() {
        Log.d(0, "SessionTimer.stopTimer: called");
        stop = true;
    }
}
