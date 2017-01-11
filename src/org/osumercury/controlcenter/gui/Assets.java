/*
    Copyright 2016-2017 Wira Mulia

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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Image;
import org.imgscalr.Scalr;
import org.osumercury.controlcenter.Log;

/**
 *
 * @author wira
 */
public class Assets {
    private static HashMap<String, BufferedImage> rImages = new HashMap<>();
    private static HashMap<String, String> rSounds = new HashMap<>();
    private static BufferedImage[] primaryColorDigits = new BufferedImage[14];
    private static BufferedImage[] secondaryColorDigits = new BufferedImage[14];
    private static BufferedImage[] blackDigits = new BufferedImage[14];
    private static BufferedImage missingAsset;
    private static BufferedImage[] blackAlphabet = new BufferedImage[26];
    private static BufferedImage[] blackNonAlphabet = new BufferedImage[19];
    private static BufferedImage[] primaryColorAlphabet = new BufferedImage[26];
    private static BufferedImage[] alternativeColorAlphabet = new BufferedImage[26];
    private static BufferedImage[] primaryColorNonAlphabet = new BufferedImage[19];
    private static BufferedImage[] alternativeColorNonAlphabet = new BufferedImage[19];
    private static BufferedImage emptyDigit;
    private static BufferedImage mercuryLogo;
    private static BufferedImage digitsImg;
    private static BufferedImage classicDigitsImg;
    private static BufferedImage fontImg;
    
    public static final int DEFAULT_BG_FADE = 10;
    public static final int DEFAULT_BG_COLOR = 0xffffff;
    
    public static final int DIGITS_MODERN = 0;
    public static final int DIGITS_CLASSIC = 1;
    
    public static void loadInternalAssets() {
        try {
            missingAsset = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/missing-asset.png"));
            digitsImg = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/digits-black.png"));
            classicDigitsImg = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/digits-classic.png"));
            fontImg = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/font-black.png"));
            
            populateDigits(digitsImg, DEFAULT_BG_FADE, DEFAULT_BG_COLOR);
            populateFont(fontImg, 96, 176, 0, 176);
        } catch(Exception e) {
            // This should NOT happen
            if(Log.debugLevel > 0) {
                e.printStackTrace();
            }
            Log.fatal(4, "Assets.load: failed to initialize internal assets: " + e.toString());
        }                
        
        try {
            mercuryLogo = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/logo.png"));
        } catch(Exception e) {
            // This should NOT happen
            Log.fatal(6, "Assets.load: can't find \"logo.png\" in jar file");
        }
    }

    public static boolean load(String resourcePath) {
        Log.d(0, "Assets.load: loading assets from '" + resourcePath +"'...");
        String fileList[];
        File d = new File(resourcePath);
        if(!d.exists()) {
            Log.d(0, "Assets.load: " + resourcePath + " does not exist.");
            return false;
        } else if(!d.isDirectory()) {
            Log.d(0, "Assets.load: " + resourcePath + " is not a directory.");
            return false;
        }

        Log.d(0, "Assets.load: enumerating resources...");
        fileList = d.list();
        for(int i = 0; i < fileList.length; i++) {
            if(fileList[i].endsWith(".png") || fileList[i].endsWith(".jpg")) {
                if(rImages.containsKey(fileList[i])) {
                    Log.d(0, "Assets.load: asset '" + fileList[i] + "' is already defined.");
                } else {
                    try {
                        Log.d(1, "Assets.load: [img] " + fileList[i]);
                        rImages.put(fileList[i], ImageIO.read(new File(resourcePath + "/" + fileList[i])));
                    } catch(IOException ioe) {
                        Log.d(0, "Assets.load: I/O exception, the resource is not loaded: " + ioe.getMessage());
                    }
                }
            }
            else if(fileList[i].endsWith(".wav")) {
                if(rSounds.containsKey(fileList[i])) {
                    Log.d(0, "Assets.load: asset '" + fileList[i] + "' is already defined.");
                } else {
                    Log.d(1, "Assets.load: [wav] " + fileList[i]);
                    rSounds.put(fileList[i], resourcePath + "/" + fileList[i]);
                }
            }
        }
        
        if(!doesAssetExist("logo.png")) {
            Log.d(0, "Assets.load: logo.png not found, using default Mercury logo");
        }

        return true;
    }
    
    public static void theme(HashMap<String, String> theme) {
        if(theme == null) {
            return;
        }
        
        Log.d(1, "Assets.theme: Applying user theme definitions");
        
        String val;
        int digitFade = DEFAULT_BG_FADE;
        int digitFadeColor = DEFAULT_BG_COLOR;
        val = theme.get("digitfade");
        if(val != null) {
            try {
                long intVal = Long.parseLong(val, 16);
                digitFade = (int)(intVal & 0xffL);
                digitFadeColor = (int)((intVal >> 8) & 0xffffffL);
                populateDigits(digitsImg, digitFade, digitFadeColor);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse digit fade");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("digitfont");
        if(val != null) {
            try {
                Log.d(1, "Assets.theme: loading custom digits font " + val);
                BufferedImage digitFont = getAsset(val);
                int digitW = Integer.parseInt(theme.get("digitW"));
                int digitH = Integer.parseInt(theme.get("digitH"));
                int colonX = Integer.parseInt(theme.get("colonX"));
                int colonW = Integer.parseInt(theme.get("colonW"));
                int periodX = Integer.parseInt(theme.get("periodX"));
                int periodW = Integer.parseInt(theme.get("periodW"));
                int dashX = Integer.parseInt(theme.get("dashX"));
                int dashW = Integer.parseInt(theme.get("dashW"));
                populateDigits(digitFont, digitW, digitH,
                        colonX, colonW, periodX, periodW, dashX, dashW,
                        digitFade, digitFadeColor);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to load custom digit font");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("font");
        if(val != null) {
            try {
                Log.d(1, "Assets.theme: loading custom text font " + val);
                BufferedImage font = getAsset(val);
                int runeW = Integer.parseInt(theme.get("runeW"));
                int runeH = Integer.parseInt(theme.get("runeH"));
                int row0 = Integer.parseInt(theme.get("row0"));
                int row1 = Integer.parseInt(theme.get("row1"));
                populateFont(font, runeW, runeH, row0, row1);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to load custom text font");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String primaryColor = theme.get("primarycolor");
        if(primaryColor != null) {
            try {
                String[] tokens = primaryColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.PRIMARY_RED = R;
                DisplayFrame.PRIMARY_GREEN = G;
                DisplayFrame.PRIMARY_BLUE = B;
                // colorize our primary color digits and text font
                Color color = new Color(R, G, B);
                DisplayFrame.PRIMARY_COLOR = color;
                colorizePrimaryDigits(color, digitFade, digitFadeColor);
                colorize(color, null, blackAlphabet, primaryColorAlphabet);
                colorize(color, null, blackNonAlphabet, primaryColorNonAlphabet);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
                e.printStackTrace();
            }
        }
        
        String secondaryColor = theme.get("secondarycolor");
        if(secondaryColor != null) {
            try {
                String[] tokens = secondaryColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.SECONDARY_RED = R;
                DisplayFrame.SECONDARY_GREEN = G;
                DisplayFrame.SECONDARY_BLUE = B;
                // colorize our secondary color digits
                Color color = new Color(R, G, B);
                DisplayFrame.SECONDARY_COLOR = color;
                colorizeSecondaryDigits(color, digitFade, digitFadeColor);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
                e.printStackTrace();
            }
        }
        
        String altColor = theme.get("altcolor");
        if(altColor != null) {
            try {
                String[] tokens = altColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.ALT_RED = R;
                DisplayFrame.ALT_GREEN = G;
                DisplayFrame.ALT_BLUE = B;
                // colorize our alternative text font
                Color color = new Color(R, G, B);
                DisplayFrame.ALT_COLOR = color;
                colorize(color, null, blackAlphabet, alternativeColorAlphabet);
                colorize(color, null, blackNonAlphabet, alternativeColorNonAlphabet);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
                e.printStackTrace();
            }
        }
        
        String bgColor = theme.get("bgcolor");
        if(bgColor != null) {
            try {
                String[] tokens = bgColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.BG_RED = R;
                DisplayFrame.BG_GREEN = G;
                DisplayFrame.BG_BLUE = B;
                ControlIndicatorsCanvas.BG_COLOR = new Color(R, G, B);
                DisplayFrame.BG_COLOR = new Color(R, G, B);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String tableBgColor = theme.get("tablebgcolor");
        if(tableBgColor != null) {
            try {
                String[] tokens = tableBgColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.TABLE_BG_RED = R;
                DisplayFrame.TABLE_BG_GREEN = G;
                DisplayFrame.TABLE_BG_BLUE = B;
                DisplayFrame.TABLE_BG_COLOR = new Color(R, G, B);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("bgimage");
        if(val != null) {
            DisplayFrame.BG_IMAGE = val;
        }
        
        val = theme.get("bgscaling");
        if(val != null) {
            try {
                DisplayFrame.BG_SCALING = Integer.parseInt(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse bg scaling");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("bgalignment");
        if(val != null) {
            try {
                DisplayFrame.BG_ALIGNMENT = Integer.parseInt(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse bg alignment");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("logoheight");
        if(val != null) {
            try {
                DisplayFrame.LOGO_HEIGHT_PROPORTION = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse logo height");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("logoposition");
        if(val != null) {
            try {
                DisplayFrame.LOGO_Y_POSITION_PROPORTION = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse logo position");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("digitsheight");
        if(val != null) {
            try {
                DisplayFrame.DIGITS_H = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse digits height");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("smalldigitsheight");
        if(val != null) {
            try {
                DisplayFrame.DIGITS_SMALL_H = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse small digits height");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("textheight");
        if(val != null) {
            try {
                DisplayFrame.TEXT_H = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse text height");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("banner");
        if(val != null) {
            DisplayFrame.BANNER_FILE = val;
        }
        
        val = theme.get("pausebarheight");
        if(val != null) {
            try {
                DisplayFrame.PAUSE_BAR_H = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse pause bar height");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("clockmargin");
        if(val != null) {
            try {
                DisplayFrame.CLOCK_MARGIN = Float.parseFloat(val);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse clock margin");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("spacing");
        if(val != null) {
            try {
                String[] tokens = val.split(",");
                DisplayFrame.SPACING_XS = Float.parseFloat(tokens[0]);
                DisplayFrame.SPACING_S = Float.parseFloat(tokens[1]);
                DisplayFrame.SPACING_M = Float.parseFloat(tokens[2]);
                DisplayFrame.SPACING_L = Float.parseFloat(tokens[3]);
                DisplayFrame.SPACING_XL = Float.parseFloat(tokens[4]);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse spacing");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = theme.get("digitsstyle");
        if(val != null) {
            try {
                setDigitsStyle(Integer.parseInt(val), digitFade, digitFadeColor);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse digits style");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
    }
    
    public static void colorize(Color color, BufferedImage bg,
                                BufferedImage[] black, BufferedImage[] target) {
        int i = 0;
        for(BufferedImage image : target) {
            colorize(color, bg, black[i], image);
            i++;
        }
    }
    
    public static void colorize(Color color, BufferedImage bg,
                                BufferedImage black, BufferedImage image) {
        BufferedImage fg = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = fg.createGraphics();
        g.drawImage(black, null, 0, 0);
        g.setXORMode(color);
        g.drawImage(black, null, 0, 0);
        g.dispose();
        g = image.createGraphics();
        if(bg != null) {
            if(bg.getWidth() == image.getWidth() &&
               bg.getHeight() == image.getHeight()) {
                g.drawImage(bg, null, 0, 0);
            } else {
                Log.d(0, "Assets.colorize: background dimensions mismatch");
            }
        }
        g.drawImage(fg, null, 0, 0);
        g.dispose();
    }
 
    public static BufferedImage getMercuryLogo(int height) {
        if (mercuryLogo == null) {
            try {
                mercuryLogo = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/logo.png"));
            } catch(Exception e) {
                // This should NOT happen
                Log.fatal(6, "Assets.load: can't find \"logo.png\" in jar file");
            }
        }
        
        return scale(mercuryLogo, (int)((double)height/mercuryLogo.getHeight()*mercuryLogo.getWidth()), height);
    }

    public static boolean doesAssetExist(String key) {
        return rImages.containsKey(key);
    }

    public static BufferedImage getAsset(String key, double width) {
        BufferedImage img = rImages.get(key);
        if(img == null) {
            Log.d(0, "Assets.getAsset: asset not found: '" + key + "'");
            return scale(missingAsset, (int)width, (int)(width/missingAsset.getWidth()*missingAsset.getHeight()));
        }
        return scale(img, (int)width, (int)(width/img.getWidth()*img.getHeight()));
    }

    public static Image getAsset(String key, double width, double height) {
        BufferedImage img = rImages.get(key);
        if(img == null) {
            Log.d(0, "Assets.getAsset: asset not found: '" + key + "'");
            return (Image) missingAsset.getScaledInstance((int)width, (int)height, Image.SCALE_SMOOTH);
        }
        return (Image) img.getScaledInstance((int)width, (int)height, Image.SCALE_SMOOTH);
    }

    public static BufferedImage getAsset(String key) {
        BufferedImage img = rImages.get(key);
        if(img == null) {
            Log.d(0, "Assets.getAsset: asset not found: '" + key + "'");
            return missingAsset;
        }
        return img;
    }

    public static String getSoundAssetPath(String key) {
        return rSounds.get(key);
    }
    
    // populate digits clipped with default font dimensions
    public static void populateDigits(BufferedImage img, int bgFadeIntensity, int bgFadeColorHexRGB) {
        populateDigits(img, 204, 250, 2040, 70, 2110, 38, 2190, 204, bgFadeIntensity, bgFadeColorHexRGB);
    }
    
    public static void populateDigits(BufferedImage img,
            int digitW, int digitH,
            int colonX, int colonW,
            int periodX, int periodW,
            int dashX, int dashW,
            int bgFadeIntensity, int bgFadeColorHexRGB) {
        Log.d(1, "Assets.populateDigits: clipping digits (bg=" + bgFadeIntensity + ", " +
                 String.format("%06X", bgFadeColorHexRGB) + ")");
        for(int i = 0; i < 10; i++) {
            blackDigits[i] = img.getSubimage(i*digitW, 0*digitH, digitW, digitH);
            primaryColorDigits[i] = new BufferedImage(digitW, digitH, BufferedImage.TYPE_INT_ARGB);
            secondaryColorDigits[i] = new BufferedImage(digitW, digitH, BufferedImage.TYPE_INT_ARGB);
        }

        blackDigits[10] = img.getSubimage(colonX, 0*digitH, colonW, digitH);
        blackDigits[11] = img.getSubimage(periodX, 0*digitH, periodW, digitH);
        blackDigits[12] = img.getSubimage(dashX, 0*digitH, dashW, digitH);
        
        primaryColorDigits[10] = new BufferedImage(colonW, digitH, BufferedImage.TYPE_INT_ARGB);
        primaryColorDigits[11] = new BufferedImage(periodW, digitH, BufferedImage.TYPE_INT_ARGB);
        primaryColorDigits[12] = new BufferedImage(dashW, digitH, BufferedImage.TYPE_INT_ARGB);
        
        secondaryColorDigits[10] = new BufferedImage(colonW, digitH, BufferedImage.TYPE_INT_ARGB);
        secondaryColorDigits[11] = new BufferedImage(periodW, digitH, BufferedImage.TYPE_INT_ARGB);
        secondaryColorDigits[12] = new BufferedImage(dashW, digitH, BufferedImage.TYPE_INT_ARGB);

        emptyDigit = img.getSubimage(0, 1*digitH, digitW, digitH);
        blackDigits[13] = emptyDigit;
        primaryColorDigits[13] = new BufferedImage(digitW, digitH, BufferedImage.TYPE_INT_ARGB);
        secondaryColorDigits[13] = new BufferedImage(digitW, digitH, BufferedImage.TYPE_INT_ARGB);
        
        Color primaryColor = new Color(
                DisplayFrame.PRIMARY_RED, DisplayFrame.PRIMARY_GREEN,
                DisplayFrame.PRIMARY_BLUE, 0
        );
        Color secondaryColor = new Color(
                DisplayFrame.SECONDARY_RED, DisplayFrame.SECONDARY_GREEN,
                DisplayFrame.SECONDARY_BLUE, 0
        );
        colorizePrimaryDigits(primaryColor, bgFadeIntensity, bgFadeColorHexRGB);
        colorizeSecondaryDigits(secondaryColor, bgFadeIntensity, bgFadeColorHexRGB);
    }
    
    public static void colorizePrimaryDigits(Color primaryColor,
                                             int bgFadeIntensity,
                                             int bgFadeColorHexRGB) {
        // let's make a background 8
        BufferedImage background = null;
        if(bgFadeIntensity > 0) {
            background = getBackgroundDigit(bgFadeIntensity, bgFadeColorHexRGB, 
                                            blackDigits[8]);
        }
        for(int i = 0; i < 10; i++) {
            colorize(primaryColor, background, blackDigits[i], primaryColorDigits[i]);
        }
        
        // colorize period, colon, and dash
        colorize(primaryColor, null, blackDigits[10], primaryColorDigits[10]);
        colorize(primaryColor, null, blackDigits[11], primaryColorDigits[11]);
        background = bgFadeIntensity == 0 ? null :
                     getBackgroundDigit(bgFadeIntensity, bgFadeColorHexRGB,
                                        blackDigits[8]);
        colorize(primaryColor, background, blackDigits[12], primaryColorDigits[12]);
        background = bgFadeIntensity == 0 ? null :
                     getBackgroundDigit(bgFadeIntensity, bgFadeColorHexRGB,
                                        blackDigits[8]);
        colorize(primaryColor, background, blackDigits[13], primaryColorDigits[13]);
    }
    
    public static void colorizeSecondaryDigits(Color secondaryColor,
                                               int bgFadeIntensity,
                                               int bgFadeColorHexRGB) {
        // let's make a background 8
        BufferedImage background = null;
        if(bgFadeIntensity > 0) {
            background = getBackgroundDigit(bgFadeIntensity, bgFadeColorHexRGB, 
                                            blackDigits[8]);
        }
        for(int i = 0; i < 10; i++) {
            colorize(secondaryColor, background, blackDigits[i], secondaryColorDigits[i]);
        }
        
        // colorize period, colon, and dash
        colorize(secondaryColor, null, blackDigits[10], secondaryColorDigits[10]);
        colorize(secondaryColor, null, blackDigits[11], secondaryColorDigits[11]);
        background = bgFadeIntensity == 0 ? null :
                     getBackgroundDigit(bgFadeIntensity, bgFadeColorHexRGB,
                                        blackDigits[8]);
        colorize(secondaryColor, background, blackDigits[12], secondaryColorDigits[12]);
        background = bgFadeIntensity == 0 ? null :
                     getBackgroundDigit(bgFadeIntensity, bgFadeColorHexRGB,
                                        blackDigits[8]);
        colorize(secondaryColor, background, blackDigits[13], secondaryColorDigits[13]);
    }
    
    public static BufferedImage getBackgroundDigit(int bgFadeIntensity, 
                                                   int colorHexRGB,
                                                   BufferedImage bg) {
        BufferedImage background = new BufferedImage(bg.getWidth(), bg.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) background.createGraphics();
        try {
            float intensity = bgFadeIntensity/255f;
            Log.d(3, "Assets.colorize: setting background at " + bgFadeIntensity + " (" + intensity + ")");
            int pixel;
            int srcAlpha;
            int c;
            for(int y = 0; y < bg.getHeight(); y++) {
                for(int x = 0; x < bg.getWidth(); x++) {
                    pixel = bg.getRGB(x, y);
                    srcAlpha = (pixel >> 24) & 0xff;
                    if((pixel & 0x00ffffffL) == 0 && srcAlpha > 0) {
                        c = (int)(((int)(intensity*srcAlpha)<<24L)+(colorHexRGB & 0xffffff));
                        background.setRGB(x, y, c);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        g.dispose();
        if(Log.debugLevel > 3) {
            printImageToConsole(background, background.getWidth()/4, background.getHeight()/8);
        }
        return background;
    }
    
    public static void printImageToConsole(BufferedImage img, int w, int h) {
        int imgW = img.getWidth();
        int imgH = img.getHeight();
        w = w == 0 ? 1 : w;
        h = h == 0 ? 1 : h;
        int xInterval = imgW / w;
        int yInterval = imgH / h;
        int xIndex = 0;
        int yIndex = 0;
        int alpha;
        Log.d(0, "img:" + imgW + "x" + imgH + " out:" + w + "x" + h);
        Log.d(0, "xInt:" + xInterval + " yInt:" + yInterval);
        for(int y = 0; y < imgH; y++) {
            for(int x = 0; x < imgW; x++) {
                alpha = (img.getRGB(x, y) >> 24) & 0xff;
                xIndex++;
                if(xIndex >= xInterval && yIndex == 0) {
                    if(alpha == 0) {
                        System.out.print(" ");
                    } else if(alpha < 50) {
                        System.out.print(".");
                    } else if(alpha < 100) {
                        System.out.print("o");
                    } else if(alpha < 150) {
                        System.out.print("O");
                    } else if(alpha < 200) {
                        System.out.print("H");
                    } else {
                        System.out.print("8");
                    }
                    
                    xIndex = 0;
                }
            }
            yIndex++;
            if(yIndex >= yInterval) {
                System.out.println();
                yIndex = 0;
            }
        }
        System.out.println();
    }
    
    public static void populateFont(BufferedImage img,
            int runeW, int runeH, int row0, int row1) {
        Log.d(1, "Assets.populateFont: clipping font runes");

        int i;

        for(i = 0; i < 26; i++) {
            blackAlphabet[i] = img.getSubimage(i*runeW, row0, runeW, runeH);
            primaryColorAlphabet[i] = new BufferedImage(runeW, runeH, BufferedImage.TYPE_INT_ARGB);
            alternativeColorAlphabet[i] = new BufferedImage(runeW, runeH, BufferedImage.TYPE_INT_ARGB);
        }

        for(i = 0; i < 19; i++) {
            blackNonAlphabet[i] = img.getSubimage(i*runeW, row1, runeW, runeH);
            primaryColorNonAlphabet[i] = new BufferedImage(runeW, runeH, BufferedImage.TYPE_INT_ARGB);
            alternativeColorNonAlphabet[i] = new BufferedImage(runeW, runeH, BufferedImage.TYPE_INT_ARGB);
        }
        
        Color primaryColor = new Color(
                DisplayFrame.PRIMARY_RED, DisplayFrame.PRIMARY_GREEN,
                DisplayFrame.PRIMARY_BLUE, 0
        );
        Color altColor = new Color(
                DisplayFrame.ALT_RED, DisplayFrame.ALT_GREEN,
                DisplayFrame.ALT_BLUE, 0
        );
        colorize(primaryColor, null, blackAlphabet, primaryColorAlphabet);
        colorize(altColor, null, blackAlphabet, alternativeColorAlphabet);
        colorize(primaryColor, null, blackNonAlphabet, primaryColorNonAlphabet);
        colorize(altColor, null, blackNonAlphabet, alternativeColorNonAlphabet);
    }
    
    public static void setDigitsStyle(int style, int fade, int fadeColor) {
        switch(style) {
            case Assets.DIGITS_CLASSIC:
                populateDigits(classicDigitsImg, 
                               204, 250, 2036, 56, 2110, 38, 2190, 204, 
                               fade, fadeColor);
                break;
            default:
                populateDigits(digitsImg, fade, fadeColor);
        }
    }
    
    public static BufferedImage[] scaleFontH(int row, int height) {
        BufferedImage[] scaled = null;
        int width = (int)((double)height/primaryColorAlphabet[0].getHeight()*primaryColorAlphabet[0].getWidth());
        int i;
        switch(row) {
            case 0:
                scaled = new BufferedImage[26];
                for(i = 0; i < 26; i++) {
                    scaled[i] = scale(primaryColorAlphabet[i], width, height);
                }
                
                break;
            case 1:
                scaled = new BufferedImage[19];
                for(i = 0; i < 19; i++) {
                    scaled[i] = scale(primaryColorNonAlphabet[i], width, height);
                }
                
                break;
            case 2:
                scaled = new BufferedImage[26];
                for(i = 0; i < 26; i++) {
                    scaled[i] = scale(alternativeColorAlphabet[i], width, height);
                }
                
                break;
            case 3:
                scaled = new BufferedImage[19];
                for(i = 0; i < 19; i++) {
                    scaled[i] = scale(alternativeColorNonAlphabet[i], width, height);
                }
                
                break;
        }
        
        return scaled;
    }
    
    public static BufferedImage getLogoW(int width) {
        BufferedImage img = rImages.get("logo.png");
        if(img == null) {
            img = mercuryLogo;
        }
        
        return scale(img, width, (int)((double)width/img.getWidth()*img.getHeight()));
    }
    
    public static BufferedImage getLogoH(int height) {
        BufferedImage img = rImages.get("logo.png");
        if(img == null) {
            img = mercuryLogo;
        }
        
        return scale(img, (int)((double)height/img.getHeight()*img.getWidth()), height);
    }
    
    public static BufferedImage[] scaleDigitsW(int width, boolean red) {
        BufferedImage[] scaled = new BufferedImage[14];
        for(int i = 0; i < 14; i++) {           
            scaled[i] = scaleDigitW(i, width, red);
        }
        return scaled;
    }
    
    public static BufferedImage[] scaleDigitsH(int height, boolean red) {
        BufferedImage[] scaled = new BufferedImage[14];
        for(int i = 0; i < 14; i++) {           
            scaled[i] = scaleDigitH(i, height, red);
        }
        return scaled;
    }
    
    public static BufferedImage scaleDigitW(int index, int width, boolean secondary) {
        BufferedImage src = !secondary ? primaryColorDigits[index] : secondaryColorDigits[index];
        return scale(src, width, (int)((double)width/src.getWidth()*src.getHeight()));
    }
    
    public static BufferedImage scaleDigitH(int index, int height, boolean secondary) {
        BufferedImage src = !secondary ? primaryColorDigits[index] : secondaryColorDigits[index];
        return scale(src, (int)((double)height/src.getHeight()*src.getWidth()), height);
    }
    
    public static BufferedImage scale(BufferedImage src, int width, int height) {
        // we're for sure don't want an image with 0 dimension
        width = width == 0 ? 1 : width;
        height = height == 0 ? 1 : height;
        BufferedImage result = Scalr.resize(src, Scalr.Method.ULTRA_QUALITY, width, height,
                Scalr.OP_ANTIALIAS);

        return result;
    }
    
    public static BufferedImage fastScale(BufferedImage src, int width, int height) {
        width = width == 0 ? 1 : width;
        height = height == 0 ? 1 : height;
        BufferedImage result = Scalr.resize(src, Scalr.Method.SPEED, width, height);

        return result;
    }
    
    public static Color parseHexColor(String hex) {
        try {
            if(hex.startsWith("#")) {
                hex = hex.substring(1, hex.length());
            }
            Color c = null;
            if(hex.length() == 6) {
                c = new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16)            
                );
            } else if(hex.length() == 8) {
                c = new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16),
                        Integer.parseInt(hex.substring(6, 8), 16)
                );
            }

            return c;
        } catch(Exception e) {
            // just return black if we failed to parse
            return Color.BLACK;
        }
    }
    
    public static String getAssetInfo() {
        String ret = "Image assets:\n" +
                     "-------------\n";
        for(String key : rImages.keySet()) {
            ret += key + "\n";
        }
        
        ret += "\nAudio assets (load-on-use):\n" +
                 "---------------------------\n";
        for(Map.Entry<String, String> entry : rSounds.entrySet()) {
            ret += entry.getValue() + "\n";
        }
        
        return ret;
    }
}
