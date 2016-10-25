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
package org.osumercury.controlcenter.gui;

import org.osumercury.controlcenter.CompetitionState;

/**
 *
 * @author wira
 */
public class TogglerThread extends Thread {
    private boolean stop = false;
    private boolean on = false;
    private long delay;
    private CompetitionState c;
    
    public TogglerThread(CompetitionState c, long delay) {
        this.delay = delay;
        this.c = c;
    }
    
    public void stopTimer() {
        stop = true;
    }
    
    public boolean getStatus() {
        return on;
    }
    
    @Override
    public void run() {
        long start, elapsed;
        int count = 0;
        while(!stop) {
            start = System.currentTimeMillis();
            if(count == 10) {
                on = !on;
                count = 0;
            };
            count++;
            if(c.getState() == CompetitionState.IDLE ||
                    c.getState() == CompetitionState.POST_RUN) {
                c.repaintFrames();
            }
            elapsed = System.currentTimeMillis() - start;
            if(elapsed < delay/10) {
                try {
                    Thread.sleep(delay/10 - elapsed);
                } catch(Exception e) {
                    
                }
            }
        }
    }
}
