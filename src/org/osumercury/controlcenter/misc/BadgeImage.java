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
package org.osumercury.controlcenter.misc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.osumercury.controlcenter.gui.Assets;

/**
 *
 * @author wira
 */
public class BadgeImage {
    private BufferedImage unscaledImage;
    private float widthInches;
    private float proportion;
    private int dpi;
    private final String name;
    private final String secondary;
    private final int number;
    private final BufferedImage background;
    private final Color backgroundColor;
    private final Color textBackgroundColor;
    private final Color textColor;
    private int backgroundScaling;
    private float backgroundScalingWidth;
    private float backgroundScalingHeight;
    
    public static final int BACKGROUND_FIT_WIDTH = 0;
    public static final int BACKGROUND_FIT_HEIGHT = 1;
    public static final int BACKGROUND_FILL = 2;
    public static final int BACKGROUND_SCALE = 3;
    
    public static final float DEFAULT_WIDTH_INCHES = 2.25f;
    public static final float DEFAULT_PROPORTION = 1.25f;
    public static final int DEFAULT_DPI = 300;
    
    public BadgeImage(int number, String name, String secondary,
            BufferedImage background, String backgroundColor,
            String textBackgroundColor, String textColor) {
        this.number = number;
        this.name = name;
        this.secondary = secondary;
        this.background = background;
        this.backgroundColor = Assets.parseHexColor(backgroundColor);
        this.textBackgroundColor = Assets.parseHexColor(textBackgroundColor);
        this.textColor = Assets.parseHexColor(textColor);
        backgroundScaling = BACKGROUND_FIT_WIDTH;
        widthInches = DEFAULT_WIDTH_INCHES;
        proportion = DEFAULT_PROPORTION;
        dpi = DEFAULT_DPI;
    }
    
    public void setProportion(float f) {
        this.proportion = f;
    }
    
    public void setDPI(int dpi) {
        this.dpi = dpi;
    }
    
    public void setWidthInches(float f) {
        this.widthInches = f;
    }
    
    public Dimension getResolution() {
        return new Dimension((int)(widthInches*dpi),
                (int)(proportion*widthInches*dpi));
    }
    
    public void setBackgroundFitWidth() {
        backgroundScaling = BACKGROUND_FIT_WIDTH;
    }
    
    public void setBackgroundFitHeight() {
        backgroundScaling = BACKGROUND_FIT_HEIGHT;
    }
    
    public void setBackgroundFill() {
        backgroundScaling = BACKGROUND_FILL;
    }
    
    public void setBackgroundScale(int width, int height) {
        backgroundScaling = BACKGROUND_SCALE;
        backgroundScalingWidth = width;
        backgroundScalingHeight = height;
    }
    
    public BufferedImage getImage() {
        // draw image here
        return unscaledImage;
    }
    
    private void drawNumber(Graphics2D g) {
        
    }
    
    private void drawName(Graphics2D g) {
        
    }
    
    private void drawSecondary(Graphics2D g) {
        
    }
    
    public BufferedImage getImageScaleWidth(float width) {
        return getImageScaleWidth((int)(width*dpi));
    }
    
    public BufferedImage getImageScaleHeight(float height) {
        return getImageScaleHeight((int)(height*dpi));
    }
    
    public BufferedImage getImageScaled(float width, float height) {
        return getImageScaled((int)(width*dpi), (int)(height*dpi));
    }
    
    public BufferedImage getImageScaleWidth(int width) {
        int height = (int)((double)width/unscaledImage.getWidth() *
                unscaledImage.getHeight());
        return Assets.scale(unscaledImage, width, height);
    }
    
    public BufferedImage getImageScaleHeight(int height) {
        int width = (int)((double)height/unscaledImage.getHeight() *
                unscaledImage.getWidth());
        return Assets.scale(unscaledImage, width, height);
    }
    
    public BufferedImage getImageScaled(int width, int height) {
        return Assets.scale(unscaledImage, width, height);
    }
}
