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

import org.osumercury.controlcenter.gui.ControlFrame;
import org.osumercury.controlcenter.gui.DisplayFrame;
import org.osumercury.controlcenter.gui.ThumbnailFrame;

/**
 *
 * @author wira
 */
public class RefreshThread extends Thread {
    private boolean stop = false;
    private final boolean on = false;
    private final long delay;
    private final ControlCenter cc;
    
    public RefreshThread(ControlCenter cc, long delay) {
        this.delay = delay;
        this.cc = cc;
    }
    
    public void stopThread() {
        stop = true;
    }
    
    public boolean getStatus() {
        return on;
    }
    
    @Override
    public void run() {
        long render;
        long delay_ns = delay * 1000000;
        long original_delay_ns = delay_ns;
        Log.d(0, "RefreshThread.run: refresh rate = " +
                delay_ns + " nanoseconds");
        ControlCenter.beginTime = System.nanoTime();
        DisplayFrame display = cc.getDisplayFrame();
        ControlFrame control = cc.getControlFrame();
        ThumbnailFrame thumb = cc.getThumbnailFrame();
        while(!stop) {
            if(display.isDrawing()) {
                Log.d(0, "RefreshThread.run: display frame not ready");
            } else {
                display.repaint();
            }
            control.repaintDisplay();
            thumb.repaint();
            try {
                render = display.getRenderTime() + control.getRenderTime() +
                        thumb.getRenderTime();
                if(render < delay_ns/2 && delay_ns/2 >= original_delay_ns) {                    
                    delay_ns /= 2;
                    Log.d(0, "RefreshThread.run: decreasing redraw delay to " + delay_ns + " ns");
                } else if(render > delay_ns) {
                    Log.d(0, "RefreshThread.run: render time was too long (" + render + " ns)");
                    delay_ns *= 2;
                    Log.d(0, "RefreshThread.run: increasing redraw delay to " + delay_ns + " ns");
                }
                Thread.sleep(delay_ns/1000000);
            } catch(Exception e) {

            }
        }
        Log.d(0, "RefreshThread.run: exiting");
    }
}
