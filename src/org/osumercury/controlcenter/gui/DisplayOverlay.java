/*
    Copyright 2017 Wira Mulia

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

import java.awt.image.BufferedImage;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

/**
 *
 * @author wira
 */
public class DisplayOverlay {
    private BufferedImage img;
    private BufferedImage scaledImg;
    private float xScrRatio;
    private float yScrRatio;
    private final boolean logoMode;
    private final boolean runStatusMode;
    private final boolean classificationMode;
    private boolean visible;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    
    public DisplayOverlay(BufferedImage img, float xScrRatio, float yScrRatio,
                          boolean logoMode, boolean runStatusMode,
                          boolean classificationMode) {
        this.img = img;
        this.scaledImg = img;
        this.xScrRatio = xScrRatio;
        this.yScrRatio = yScrRatio;
        this.logoMode = logoMode;
        this.runStatusMode = runStatusMode;
        this.classificationMode = classificationMode;
        visible = false;
    }
    
    public BufferedImage getImage() {
        return scaledImg;
    }
    
    public float getX() {
        return xScrRatio;
    }
    
    public float getY() {
        return yScrRatio;
    }
    
    public void reposition(float x, float y) {
        xScrRatio = x;
        yScrRatio = y;
    }
    
    public void setVisible(boolean b) {
        this.visible = b;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public ReadLock getReadLock() {
        return lock.readLock();
    }
    
    public void rescaleWidth(int widthPx) {
        lock.writeLock().lock();
        try {
            scaledImg = Assets.scale(img, widthPx, 
                    (int)((double)img.getHeight()/img.getWidth() * widthPx));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void rescaleHeight(int heightPx) {
        lock.writeLock().lock();
        try {
            scaledImg = Assets.scale(img, 
                    (int)((double)img.getWidth()/img.getHeight() * heightPx),
                    heightPx);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean drawableInThisMode(int mode) {
        if(logoMode && mode == DisplayFrame.OUTPUT_LOGO) {
            return true;
        } else if(runStatusMode && mode == DisplayFrame.OUTPUT_RUN_STATUS) {
            return true;
        } else if(classificationMode && mode == DisplayFrame.OUTPUT_CLASSIFICATION) {
            return true;
        }
        return false;
    }
}
