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

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.imgscalr.Scalr;
import org.osumercury.controlcenter.Config;
import org.osumercury.controlcenter.Log;

/**
 *
 * @author wira
 */
public class Assets {
    private static HashMap<String, BufferedImage> rImages = new HashMap<>();
    private static HashMap<String, String> rSounds = new HashMap<>();
    private static BufferedImage[] blueDigits = new BufferedImage[14];
    private static BufferedImage[] redDigits = new BufferedImage[14];
    private static BufferedImage missingAsset;
    private static BufferedImage[] alphabetBlue = new BufferedImage[26];
    private static BufferedImage[] alphabetWhite = new BufferedImage[26];
    private static BufferedImage[] nonAlphabetBlue = new BufferedImage[19];
    private static BufferedImage[] nonAlphabetWhite = new BufferedImage[19];
    private static BufferedImage emptyDigit;
    private static BufferedImage mercuryLogo;
    
    public static void loadInternalAssets() {
        BufferedImage digitsImg;
        BufferedImage fontImg;
        try {
            missingAsset = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/missing-asset.png"));
            digitsImg = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/digits.png"));
            fontImg = ImageIO.read(Assets.class.getResource("/org/osumercury/controlcenter/gui/font.png"));
            
            populateDigits(digitsImg, 204, 250, 2038, 54, 2124, 32, 2202, 204);
            populateFont(fontImg, 96, 176, 0, 176, 351, 527);
        } catch(Exception e) {
            // This should NOT happen
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
                        Log.d(0, "Assets.load: [img] " + fileList[i]);
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
                    Log.d(0, "Assets.load: [wav] " + fileList[i]);
                    rSounds.put(fileList[i], resourcePath + "/" + fileList[i]);
                }
            }
        }

        return true;
    }
    
    public static void theme() {
        if(!Config.hasSection("theme")) {
            return;
        }
        
        Log.d(0, "Assets.theme: Checking for user-defined theme definitions");
        
        String val;
        val = Config.getValue("theme", "digitfont");
        if(val != null) {
            try {
                Log.d(0, "Assets.theme: loading custom digits font " + val);
                BufferedImage digitFont = getAsset(val);
                int digitW = Integer.parseInt(Config.getValue("theme", "digitW"));
                int digitH = Integer.parseInt(Config.getValue("theme", "digitH"));
                int colonX = Integer.parseInt(Config.getValue("theme", "colonX"));
                int colonW = Integer.parseInt(Config.getValue("theme", "colonW"));
                int periodX = Integer.parseInt(Config.getValue("theme", "periodX"));
                int periodW = Integer.parseInt(Config.getValue("theme", "periodW"));
                int dashX = Integer.parseInt(Config.getValue("theme", "dashX"));
                int dashW = Integer.parseInt(Config.getValue("theme", "dashW"));
                populateDigits(digitFont, digitW, digitH,
                        colonX, colonW, periodX, periodW, dashX, dashW);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to load custom digit font");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        val = Config.getValue("theme", "font");
        if(val != null) {
            try {
                Log.d(0, "Assets.theme: loading custom text font " + val);
                BufferedImage font = getAsset(val);
                int runeW = Integer.parseInt(Config.getValue("theme", "runeW"));
                int runeH = Integer.parseInt(Config.getValue("theme", "runeH"));
                int row0 = Integer.parseInt(Config.getValue("theme", "row0"));
                int row1 = Integer.parseInt(Config.getValue("theme", "row1"));
                int row2 = Integer.parseInt(Config.getValue("theme", "row2"));
                int row3 = Integer.parseInt(Config.getValue("theme", "row3"));
                populateFont(font, runeW, runeH, row0, row1, row2, row3);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to load custom text font");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String primaryColor = Config.getValue("theme", "primarycolor");
        if(primaryColor != null) {
            try {
                String[] tokens = primaryColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.PRIMARY_RED = R;
                DisplayFrame.PRIMARY_GREEN = G;
                DisplayFrame.PRIMARY_BLUE = B;
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String secondaryColor = Config.getValue("theme", "secondarycolor");
        if(secondaryColor != null) {
            try {
                String[] tokens = secondaryColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.SECONDARY_RED = R;
                DisplayFrame.SECONDARY_GREEN = G;
                DisplayFrame.SECONDARY_BLUE = B;
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String altColor = Config.getValue("theme", "altcolor");
        if(altColor != null) {
            try {
                String[] tokens = altColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.ALT_RED = R;
                DisplayFrame.ALT_GREEN = G;
                DisplayFrame.ALT_BLUE = B;
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String bgColor = Config.getValue("theme", "bgcolor");
        if(bgColor != null) {
            try {
                String[] tokens = bgColor.trim().split(",");
                int R = Integer.parseInt(tokens[0].trim());
                int G = Integer.parseInt(tokens[1].trim());
                int B = Integer.parseInt(tokens[2].trim());
                DisplayFrame.BG_RED = R;
                DisplayFrame.BG_GREEN = G;
                DisplayFrame.BG_BLUE = B;
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse color information");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
        
        String bgImageFile = Config.getValue("theme", "bgimage");
        if(bgImageFile != null) {
            DisplayFrame.BG_IMAGE = getAsset(bgImageFile);
        }
        
        String bgAlignment = Config.getValue("theme", "bgalignment");
        if(bgAlignment != null) {
            try {
                DisplayFrame.BG_ALIGNMENT = Integer.parseInt(bgAlignment);
            } catch(Exception e) {
                System.err.println("Assets.theme: failed to parse bg alignment");
                System.err.println("Assets.theme: " + e.toString());
            }
        }
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
    
    public static void populateDigits(BufferedImage img, 
            int digitW, int digitH,
            int colonX, int colonW,
            int periodX, int periodW,
            int dashX, int dashW) {
        Log.d(0, "Assets.populateDigits: clipping digits");
        for(int i = 0; i < 10; i++) {
            blueDigits[i] = img.getSubimage(i*digitW, 0, digitW, digitH);
            redDigits[i] = img.getSubimage(i*digitW, digitH, digitW, digitH);
        }

        blueDigits[10] = img.getSubimage(colonX, 0, colonW, digitH);
        redDigits[10] = img.getSubimage(colonX, digitH, colonW, digitH);
        blueDigits[11] = img.getSubimage(periodX, 0, periodW, digitH);
        redDigits[11] = img.getSubimage(periodX, digitH, periodW, digitH);
        blueDigits[12] = img.getSubimage(dashX, 0, dashW, digitH);
        redDigits[12] = img.getSubimage(dashX, digitH, dashW, digitH);

        emptyDigit = img.getSubimage(0, 2*digitH, digitW, digitH);
        blueDigits[13] = emptyDigit;
        redDigits[13] = emptyDigit;
    }
    
    public static void populateFont(BufferedImage img,
            int runeW, int runeH, int row0, int row1, int row2, int row3) {
        Log.d(0, "Assets.populateFont: clipping font runes");

        int i;

        for(i = 0; i < 26; i++) {
            alphabetBlue[i] = img.getSubimage(i*runeW, row0, runeW, runeH);
            alphabetWhite[i] = img.getSubimage(i*runeW, row2, runeW, runeH);
        }

        for(i = 0; i < 19; i++) {
            nonAlphabetBlue[i] = img.getSubimage(i*runeW, row1, runeW, runeH);
            nonAlphabetWhite[i] = img.getSubimage(i*runeW, row3, runeW, runeH);
        }
    }
    
    public static BufferedImage[] scaleFontH(int row, int height) {
        BufferedImage[] scaled = null;
        int width = (int)((double)height/alphabetBlue[0].getHeight()*alphabetBlue[0].getWidth());
        int i;
        switch(row) {
            case 0:
                scaled = new BufferedImage[26];
                for(i = 0; i < 26; i++) {
                    scaled[i] = scale(alphabetBlue[i], width, height);
                }
                
                break;
            case 1:
                scaled = new BufferedImage[19];
                for(i = 0; i < 19; i++) {
                    scaled[i] = scale(nonAlphabetBlue[i], width, height);
                }
                
                break;
            case 2:
                scaled = new BufferedImage[26];
                for(i = 0; i < 26; i++) {
                    scaled[i] = scale(alphabetWhite[i], width, height);
                }
                
                break;
            case 3:
                scaled = new BufferedImage[19];
                for(i = 0; i < 19; i++) {
                    scaled[i] = scale(nonAlphabetWhite[i], width, height);
                }
                
                break;
        }
        
        return scaled;
    }
    
    public static BufferedImage getLogoW(int width) {
        BufferedImage img = rImages.get("logo.png");
        if(img == null) {
            img = missingAsset;
        }
        
        return scale(img, width, (int)((double)width/img.getWidth()*img.getHeight()));
    }
    
    public static BufferedImage getLogoH(int height) {
        BufferedImage img = rImages.get("logo.png");
        if(img == null) {
            img = missingAsset;
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
    
    public static BufferedImage scaleDigitW(int index, int width, boolean red) {
        BufferedImage src = !red ? blueDigits[index] : redDigits[index];
        return scale(src, width, (int)((double)width/src.getWidth()*src.getHeight()));
    }
    
    public static BufferedImage scaleDigitH(int index, int height, boolean red) {
        BufferedImage src = !red ? blueDigits[index] : redDigits[index];
        return scale(src, (int)((double)height/src.getHeight()*src.getWidth()), height);
    }
    
    public static BufferedImage scale(BufferedImage src, int width, int height) {
        // we're for sure don't want an image with 0 dimension
        width = width == 0 ? 1 : width;
        height = height == 0 ? 1 : height;
        BufferedImage result = Scalr.resize(src, Scalr.Method.ULTRA_QUALITY, width, height,
                Scalr.OP_ANTIALIAS);
        /*
        BufferedImage result = new BufferedImage(width, height,
                src.getType());
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, width, height, 0, 0, src.getWidth(),
            src.getHeight(), null);
        g.dispose();
        */
        return result;
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