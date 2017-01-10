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

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import org.osumercury.controlcenter.CompetitionState;
import org.osumercury.controlcenter.Config;
import org.osumercury.controlcenter.ControlCenter;
import org.osumercury.controlcenter.Log;
import org.osumercury.controlcenter.Score;
import org.osumercury.controlcenter.SessionState;
import org.osumercury.controlcenter.Team;

/**
 * A bunch of Java Graphics voodoo.
 *
 * @author wira
 */
public class DisplayFrame extends JFrame {
    
    private final CompetitionState competition;
    private final ControlCenter cc;
    private int mode;
    private int W;
    private int H;
    private int digitW;
    private int digitH;
    private int smallW;
    private int smallH;
    private int charW;
    private int charH;
    private BufferedImage displayImage;
    private BufferedImage thumbnailImage;
    private BufferedImage scaledLogo;
    private BufferedImage scaledBannerImage;
    private BufferedImage scaledBackgroundImage;
    private BufferedImage[] scaledBlueDigits = new BufferedImage[13];
    private BufferedImage[] scaledRedDigits = new BufferedImage[13];
    private BufferedImage[] scaledSmallDigits = new BufferedImage[13];
    private BufferedImage[] scaledAlphabet = new BufferedImage[26];
    private BufferedImage[] scaledNonAlphabet = new BufferedImage[19];
    private BufferedImage[] scaledWhiteAlphabet = new BufferedImage[26];
    private BufferedImage[] scaledWhiteNonAlphabet = new BufferedImage[19];
    private HashMap<Integer, BufferedImage> teamBadges;
    private DisplayCanvas canvas;
    private int rankStart = 1;
    private int nextTeamID = -1;
    private int thumbIntervalCount = 0;
    private ArrayList<String[]> classificationRows;
    private double[] scores;
    private int[] scoreDigits;
    private int[] scoreDecimal;
    private Score currentScore;
    private Font systemFont;
    private String systemFontName;                
    private HashMap<String, String> text;
    private int spacingXSPx;
    private int spacingSPx;
    private int spacingMPx;
    private int spacingLPx;
    private int spacingXLPx;
    private int clockMarginPx;
    private int timeBarHPx;
    private int horizBarHPx;
    private double currentScoreVal;
    private double bestScoreVal;
    
    private long beginTime = -1;
    private long renderTime = 0;
    private long renderedFrames = 0;
    private boolean drawing = false;
    
    public static final int OUTPUT_LOGO = 0;
    public static final int OUTPUT_RUN_STATUS = 1;
    public static final int OUTPUT_CLASSIFICATION = 2;
    
    public static final int COLON = 10;
    public static final int PERIOD = 11;
    public static final int DASH = 12;
    
    // static options (common for all displays) and default values
    public static String BANNER_FILE;
    
    public static int SCORE_FIELD_DIGITS = 2;
    public static int SCORE_FIELD_DECIMAL = 2;
    
    public static double TEAM_BADGE_HEIGHT_RATIO = 0.55;
    
    public static int BG_RED = 0;
    public static int BG_GREEN = 0;
    public static int BG_BLUE = 0;
    public static Color BG_COLOR;
    public static String BG_IMAGE;
    public static int BG_ALIGNMENT = 0;
    public static int BG_SCALING = 0;
    
    public static int PRIMARY_RED = 0x20;
    public static int PRIMARY_GREEN = 0xbb;
    public static int PRIMARY_BLUE = 0xff;
    public static Color PRIMARY_COLOR;
    
    public static int SECONDARY_RED = 0xff;
    public static int SECONDARY_GREEN = 0x7e;
    public static int SECONDARY_BLUE = 0x30;
    public static Color SECONDARY_COLOR;
    
    public static int ALT_RED = 0xff;
    public static int ALT_GREEN = 0xff;
    public static int ALT_BLUE = 0xff;
    public static Color ALT_COLOR;
    
    public static int TABLE_BG_RED = 0x05;
    public static int TABLE_BG_GREEN = 0x20;
    public static int TABLE_BG_BLUE = 0x50;
    public static Color TABLE_BG_COLOR;
     
    public static float LOGO_HEIGHT_PROPORTION = 0.4f;
    public static float LOGO_Y_POSITION_PROPORTION = 0.4f;
    public static boolean DRAW_RENDER_TIME = false;
    public static boolean ALIGN_CLOCK_LEFT = false;
    public static boolean SHOW_BANNER = true;
    public static boolean GENERATE_THUMBNAIL = false;
    public static float POSITION_RIGHT_RECORDED_SCORES = 0.5f;
    public static float POSITION_LEFT_RECORDED_SCORES = 0.3f;
    public static int THUMB_WIDTH = 300;
    public static int THUMB_INTERVAL = 2;
    
    public static float DIGITS_H = 0.10f;
    public static float DIGITS_SMALL_H = 0.07f;
    public static float TEXT_H = 0.06f;
    
    public static float SPACING_XS = 0.0066f;
    public static float SPACING_S  = 0.0131f;
    public static float SPACING_M  = 0.0197f;
    public static float SPACING_L  = 0.0262f;
    public static float SPACING_XL = 0.0522f;
    public static float CLOCK_MARGIN = 0.0740f;
    
    public static float PAUSE_BAR_H = 0.1f;
    public static float TIME_BAR_H = 0.008f;
    public static float HORIZ_BAR_H = 0.003f;
    
    public DisplayFrame(ControlCenter cc, String nativeFont) {
        this.cc = cc;
        this.competition = cc.getCompetitionState();
        mode = 0;        
        systemFontName = nativeFont;
    }
    
    public void init() {
        Log.d(0, "DisplayFrame: init");
        recolor();
        canvas = new DisplayCanvas();
        add(canvas);
        classificationRows = new ArrayList();
        scores = new double[Score.getFields().size()];
        scoreDigits = new int[Score.getFields().size()];
        scoreDecimal = new int[Score.getFields().size()];
        teamBadges = new HashMap();
        systemFont = new Font(Font.MONOSPACED, Font.BOLD, 12);
        text = new HashMap();
        
        // default string (English)
        text.put("TEAM", "TEAM");
        text.put("SETUP_PERIOD", "SETUP PERIOD");
        text.put("TIME_LEFT", "TIME LEFT");
        text.put("SETUP_TIME_LEFT", "SETUP TIME LEFT");
        text.put("RUN", "RUN");
        text.put("OF", "OF");
        text.put("NEXT", "NEXT");
        text.put("CURRENT", "CURRENT");
        text.put("NO_SCORE", "NO SCORE");
        text.put("BEST_SCORE", "BEST SCORE");
        text.put("CLASSIFICATION", "CLASSIFICATION");
        text.put("POSITION", "P.");
        text.put("ID", "ID");
        text.put("SCORE", "SCORE");
        
        ArrayList<String> keys = Config.getKeysInOriginalOrder("localization");
        if(keys != null) {
            for(String key : keys) {
                localizeText(key, Config.getValue("localization", key));
            }
        }
        
        newScore();
        if(cc.getControlFrame() != null) {
            cc.getControlFrame().addScoreChangedHook(
                    (String key, int scoreID, String scoreValue) -> {
                        scores[scoreID] = Double.parseDouble(scoreValue);
                        currentScore.setValue(key, scores[scoreID]);
                        currentScoreVal = currentScore.getScore();
                    }
            );
            
            cc.getControlFrame().addUserEventHook((int ID, Object param) -> {
                CompetitionState c = cc.getCompetitionState();
                if(c.getState() == CompetitionState.IDLE ||
                   c.getState() == CompetitionState.SETUP) {
                    return;
                }
                switch(ID) {
                    case UserEvent.STATE_CHANGE_RUN:
                    case UserEvent.SESSION_ATTEMPT_COMMITTED:
                    case UserEvent.DATA_ADDED:
                    case UserEvent.DATA_CHANGED:
                    case UserEvent.DATA_CLEARED:
                    case UserEvent.DATA_IMPORTED:
                    case UserEvent.DATA_RECORD_EXPUNGED:
                        SessionState s = c.getSession();
                        Team t = s.getActiveTeam();
                        if(!t.hasScore()) {
                            return;
                        }
                        bestScoreVal = s.getActiveTeam().getBestScore().getScore();
                        break;
                }
            });
        }
        String val;
        if((val = Config.getValue("display", "score_field_digits")) != null) {
            try {
                SCORE_FIELD_DIGITS = Integer.parseInt(val);
            } catch(NumberFormatException nfe) {
                System.err.println("DisplayFrame.init: failed to parse " +
                        "score_field_digits, using default value 2");
                SCORE_FIELD_DIGITS = 2;
            }
        }
        if((val = Config.getValue("display", "score_field_decimal")) != null) {
            try {
                SCORE_FIELD_DECIMAL = Integer.parseInt(val);
            } catch(NumberFormatException nfe) {
                System.err.println("DisplayFrame.init: failed to parse " +
                        "score_field_decimal, using default value 2");
                SCORE_FIELD_DECIMAL = 2;
            }
        }
        
        // parse score field display format, if defined under [display]
        int i = 0;
        String[] tokens;
        for(String key : Config.getKeysInOriginalOrder("fields")) {
            String entry = Config.getValue("display", key);
            try {
                if(entry != null) {
                    tokens = entry.trim().split(",");
                    scoreDigits[i] = Integer.parseInt(tokens[0].trim());
                    scoreDecimal[i] = Integer.parseInt(tokens[1].trim());
                } else {
                    scoreDigits[i] = SCORE_FIELD_DIGITS;
                    scoreDecimal[i] = SCORE_FIELD_DECIMAL;
                }
            } catch(Exception e) {
                System.err.println("DisplayFrame.init: failed to parse display" +
                        " format for field \"" + key + "\"");
                scoreDigits[i] = SCORE_FIELD_DIGITS;
                scoreDecimal[i] = SCORE_FIELD_DECIMAL;
            }
            i++;
        }        
    }
    
    public void localizeText(String key, String localizedText) {
        if(!text.containsKey(key)) {
            Log.d(0, "DisplayFrame.localizeText: \"" + key + "\" is not a valid" +
                    " string token");
        } else {
            text.put(key, localizedText);
        }
    }
    
    public void setClassificationData(ArrayList<Team> classification) {
        Log.d(3, "DisplayFrame.setClassificationData: new classification of " +
                "size " + classification.size());
        classificationRows = new ArrayList();
        int rank = 1;
        for(Team t : classification) {
            String[] row = { ""+rank, t.getName(), ""+t.getNumber(),
                String.format("%.2f",t.getBestScore().getScore()) };
            classificationRows.add(row);
            rank++;
        }
    }
    
    public void newScore() {
        currentScore = new Score();
        int i = 0;
        for(String key : Config.getKeysInOriginalOrder("fields")) {
            scores[i] = Score.getDefaultValue(key);
            currentScore.setValue(key, Score.getDefaultValue(key));
            i++;
        }
        currentScoreVal = currentScore.getScore();
    }
    
    public void setScore(String key, int id, double value) {
        if(currentScore == null)
            return;
        
        scores[id] = value;
        currentScore.setValue(key, value);
        currentScoreVal = currentScore.getScore();
    }
    
    public void setBestScore(Score score) {
        if(score != null) {
            bestScoreVal = score.getScore();
        }
    }
    
    public void setNextTeamID(int id) {
        nextTeamID = id;
    }
    
    public synchronized BufferedImage getThumbnail() {
        return thumbnailImage;
    }
    
    public synchronized void generateThumbnail() {
        int width = THUMB_WIDTH;
        int height = (int)((float)displayImage.getHeight()/displayImage.getWidth()
                * THUMB_WIDTH);
        thumbnailImage = Assets.fastScale(displayImage, width, height);
    }    
    
    public synchronized void setThumbnailWidth(int w) {
        THUMB_WIDTH = w;
    }
    
    private void rescale(int width, int height) {
        if(!isVisible()) {
            return;
        }
        Log.d(0, "DisplayFrame.rescale: rescaling to " + width + "x" + height);        
        Graphics2D g = (Graphics2D) canvas.getGraphics();
        String str = "RESCALING DISPLAY UI";
        int strWidth = getTextWidth(str);
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(SECONDARY_COLOR);
        g.drawRect(canvas.getWidth()/2-strWidth/2-20, 
            canvas.getHeight()/2-charH/2-20, 40+strWidth, 40+charH
        );
        drawText(g, str, canvas.getWidth()/2-strWidth/2, canvas.getHeight()/2-charH/2, false);        
        digitH = (int)(DIGITS_H*height);
        smallH = (int)(DIGITS_SMALL_H*height);
        charH = (int)(TEXT_H*height);
        scaledLogo = Assets.getLogoH((int)(LOGO_HEIGHT_PROPORTION*height));
        scaledBlueDigits = Assets.scaleDigitsH(digitH, false);
        scaledRedDigits = Assets.scaleDigitsH(digitH, true);
        scaledSmallDigits = Assets.scaleDigitsH(smallH, true);
        digitW = scaledBlueDigits[0].getWidth();
        smallW = scaledSmallDigits[0].getWidth();
        spacingXSPx = (int)(SPACING_XS * height);
        spacingSPx  = (int)(SPACING_S * height);
        spacingMPx  = (int)(SPACING_M * height);
        spacingLPx  = (int)(SPACING_L * height);
        spacingXLPx = (int)(SPACING_XL * height);
        clockMarginPx = (int)(CLOCK_MARGIN * width);
        timeBarHPx = (int)(TIME_BAR_H * height);
        horizBarHPx = (int)(HORIZ_BAR_H * height);
               
        if(systemFontName != null) {
            int fontSize = 0;
            do {            
                fontSize++;
                systemFont = new Font(systemFontName, Font.BOLD, fontSize);
            } while(g.getFontMetrics(systemFont).getHeight() < charH);
            charW = g.getFontMetrics(systemFont).stringWidth("A");
            Log.d(3, "DisplayFrame.rescale: font size="+fontSize);
        } else {
            scaledAlphabet = Assets.scaleFontH(0, charH);
            scaledNonAlphabet = Assets.scaleFontH(1, charH);
            scaledWhiteAlphabet = Assets.scaleFontH(2, charH);
            scaledWhiteNonAlphabet = Assets.scaleFontH(3, charH);
            charW = scaledAlphabet[0].getWidth();
        }
        
        // background image scaling
        if(BG_IMAGE != null && Assets.doesAssetExist(BG_IMAGE)) {
            BufferedImage bgImage = Assets.getAsset(BG_IMAGE);
            switch(BG_SCALING) {
                // no scaling
                case 0:
                    scaledBackgroundImage = bgImage;
                    break;
                // fit width
                case 1:
                    scaledBackgroundImage = Assets.scale(bgImage, width,
                            (int)((float)width/bgImage.getWidth()*bgImage.getHeight()));
                    break;
                // fit height
                case 2:
                    scaledBackgroundImage = Assets.scale(bgImage,
                            (int)((float)height/bgImage.getHeight()*bgImage.getWidth()),
                            height);
                    break;
            }
        }
        
        // banner image scaling
        if(BANNER_FILE != null && Assets.doesAssetExist(BANNER_FILE)) {
            int horizontalSpace = (int)(0.5*width-spacingLPx);
            int verticalSpace = height - 
                    (int)(LOGO_Y_POSITION_PROPORTION*height+scaledLogo.getHeight()/2+digitH+spacingXLPx);
            BufferedImage sponsorsImage = Assets.getAsset(BANNER_FILE);
            // try to fit the height first
            Log.d(1, "DisplayFrame.rescale: fitting banner height");
            int fitImageHeight = verticalSpace;
            int fitImageWidth = (int)((float)fitImageHeight/sponsorsImage.getHeight() * 
                    sponsorsImage.getWidth());

            if(fitImageWidth > horizontalSpace) {
                Log.d(1, "DisplayFrame.rescale: fitting banner width");
                // no good, scale down to fit width
                fitImageWidth = horizontalSpace;
                fitImageHeight = (int)((float)fitImageWidth/sponsorsImage.getWidth() *
                       sponsorsImage.getHeight());
            }
            if(fitImageWidth <= 0 || fitImageHeight <= 0) {
                Log.d(1, "DisplayFrame.rescale: unable to fit banner");
            } else {
                scaledBannerImage = Assets.scale(sponsorsImage, fitImageWidth,
                        fitImageHeight);
            }
        }
        
        for(Team t : competition.getTeams()) {
            int teamID = t.getNumber();
            str = t.getLogoFileName();
            if(Assets.doesAssetExist(str)) {
                BufferedImage logo = Assets.getAsset(str);
                int logoScaledWidth =
                        (int)((double)(height*TEAM_BADGE_HEIGHT_RATIO)/logo.getHeight()*logo.getWidth());
                
                // if scaled width is less than half the screen width, use these
                // dimensions
                if(logoScaledWidth < (int)(0.5 * width)) {
                    Log.d(2, "DisplayFrame.rescale: teamBadges[" + teamID + "]: " +
                            "scaled by height");
                    teamBadges.put(teamID, Assets.scale(logo,
                            logoScaledWidth,
                            (int)(height*TEAM_BADGE_HEIGHT_RATIO))
                    );
                    
                // otherwise scale to half screen width so the clock won't be
                // covered by the team badge
                } else {
                    Log.d(2, "DisplayFrame.rescale: teamBadges[" + teamID + "]: " +
                            "scaled by width");
                    teamBadges.put(teamID, Assets.scale(logo,
                            (int)(0.5*width),
                            (int)(0.5*width/logo.getWidth()*logo.getHeight()))
                    );
                }
            } 
        }
    }
    
    public void rescale() {
        rescale(getWidth(), getHeight());
    }
    
    public void setFont(String fontName) {
        systemFontName = fontName;
        rescale(getWidth(), getHeight());
    }
    
    public void setMode(int mode) {
        this.mode = mode;
    } 
    
    public void recolor() {
        PRIMARY_COLOR = new Color(PRIMARY_RED, PRIMARY_GREEN, PRIMARY_BLUE);
        SECONDARY_COLOR = new Color(SECONDARY_RED, SECONDARY_GREEN, SECONDARY_BLUE);
        ALT_COLOR = new Color(ALT_RED, ALT_GREEN, ALT_BLUE);
        BG_COLOR = new Color(BG_RED, BG_GREEN, BG_BLUE);
        TABLE_BG_COLOR = new Color(TABLE_BG_RED, TABLE_BG_GREEN, TABLE_BG_BLUE);
    }
    
    public int getMode() {
        return mode;
    }
    
    public void setRankStart(int rank) {
        rankStart = rank;
    }    
    
    public int getRankStart() {
        return rankStart;
    }
    
    public void refresh() {
        canvas.repaint();
    }
    
    private int getTextWidth(String str) {
        if(systemFontName != null) {
            return canvas.getGraphics().getFontMetrics(systemFont).stringWidth(str);
        }
        
        int width = 0;
        for(char c : str.toCharArray()) {
            if(c == ' ') {
                width += charW/2;
            } else {
                width += charW;
            }
        }
        return width;
    }
        
    private void drawText(Graphics2D g, String str, int x, int y, boolean white) {
        if(systemFontName != null) {
            FontMetrics m = g.getFontMetrics(systemFont);
            int baseY = y + m.getHeight() - m.getDescent();
            g.setColor(white ? Color.WHITE : PRIMARY_COLOR);            
            g.setFont(systemFont);
            g.drawString(str, x, baseY);            
            return;
        }
        
        for(char c : str.toUpperCase().toCharArray()) {
            switch (c) {
                case '-':
                    g.drawImage(white ? scaledWhiteNonAlphabet[10] : scaledNonAlphabet[10], x, y, this);
                    x += charW;
                    break;
                case ':':
                    g.drawImage(white ? scaledWhiteNonAlphabet[11] : scaledNonAlphabet[11], x, y, this);
                    x += charW;
                    break;
                case ',':
                    g.drawImage(white ? scaledWhiteNonAlphabet[12] : scaledNonAlphabet[12], x, y, this);
                    x += charW;
                    break;
                case '.':
                    g.drawImage(white ? scaledWhiteNonAlphabet[13] : scaledNonAlphabet[13], x, y, this);
                    x += charW;
                    break;
                case '\'':
                    g.drawImage(white ? scaledWhiteNonAlphabet[14] : scaledNonAlphabet[14], x, y, this);
                    x += charW;
                    break;
                case '"':
                    g.drawImage(white ? scaledWhiteNonAlphabet[15] : scaledNonAlphabet[15], x, y, this);
                    x += charW;
                    break;
                case '?':
                    g.drawImage(white ? scaledWhiteNonAlphabet[16] : scaledNonAlphabet[16], x, y, this);
                    x += charW;
                    break;
                case '!':
                    g.drawImage(white ? scaledWhiteNonAlphabet[17] : scaledNonAlphabet[17], x, y, this);
                    x += charW;
                    break;    
                case '#':
                    g.drawImage(white ? scaledWhiteNonAlphabet[18] : scaledNonAlphabet[18], x, y, this);
                    x += charW;
                    break;  
                case ' ':
                    x += charW/2;
                    break;
                default:                                        
                    if(((int)c) < 48) {
                        break;
                    }

                    if(((int)c) > 127) {
                        g.drawImage(white ? scaledWhiteNonAlphabet[13] : scaledNonAlphabet[13], x, y, this);                        
                    } else if(((int)c) >= 65) {
                        g.drawImage(white ? scaledWhiteAlphabet[(int)c-65] : scaledAlphabet[(int)c-65], x, y, this);
                    } else {
                        g.drawImage(white ? scaledWhiteNonAlphabet[(int)c-48] : scaledNonAlphabet[(int)c-48], x, y, this);
                    }
                    x += charW;
                    break;
            }
        }
    }
    
    public long getRenderTime() {
        return renderTime;
    }
    
    public boolean isDrawing() {
        return drawing;
    }
    
    class DisplayCanvas extends JPanel {
        
        public int W(double r) {
            return (int)(r*getWidth());
        }

        public int H(double r) {
            return (int)(r*getHeight());
        }

        private int centeredX(int width) {
            return (int)(W(0.5) - 0.5*width);
        }

        private int centeredY(int height) {
            return (int)(H(0.5) - 0.5*height);
        }        
        
        @Override
        public void paint(Graphics _g) {
            drawing = true;
                        
            if(beginTime < 0) {
                beginTime = System.nanoTime();
            }
            long startTime = System.nanoTime();
            
            if(W != getWidth() || H != getHeight()) {
                W = getWidth();
                H = getHeight();
                rescale(W, H);
            }
            int x, y, i;
            displayImage = new BufferedImage(
                W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = displayImage.createGraphics();
            
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(BG_COLOR);
            g.fillRect(0, 0, W, H);
            
            if(scaledBackgroundImage != null) {
                switch(BG_ALIGNMENT) {
                    case 0:
                        g.drawImage(scaledBackgroundImage, 0, 0, null);
                        break;
                    case 1:
                        g.drawImage(scaledBackgroundImage, 
                                W(1)-scaledBackgroundImage.getWidth(), 0, null);
                        break;
                    case 2:
                        g.drawImage(scaledBackgroundImage, 
                                W(1)-scaledBackgroundImage.getWidth(), 
                                H(1)-scaledBackgroundImage.getHeight(), null);
                        break;
                    case 3:
                        g.drawImage(scaledBackgroundImage, 0, 
                                H(1)-scaledBackgroundImage.getHeight(), null);
                        break;
                    case 4:
                        g.drawImage(scaledBackgroundImage, 
                                centeredX(scaledBackgroundImage.getWidth()), 
                                0, null);
                        break;
                    case 5:
                        g.drawImage(scaledBackgroundImage, 
                                centeredX(scaledBackgroundImage.getWidth()),
                                H(1)-scaledBackgroundImage.getHeight(), null);
                        break;
                    case 6:
                        g.drawImage(scaledBackgroundImage, 
                                centeredX(scaledBackgroundImage.getWidth()),
                                centeredY(scaledBackgroundImage.getHeight()), null);
                        break;
                }
            }
            
            String str, str2;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
            SessionState s = competition.getSession();
            
            int teamID = -1;
            String teamName = "";
            if(s != null && s.getActiveTeam() != null) {
                teamID = s.getActiveTeam().getNumber();
                teamName = s.getActiveTeam().getName();
            }
            int yOffset = 0;
            OUTER:
            switch (mode) {
                case OUTPUT_RUN_STATUS:
                    if (s != null && competition.getState() != CompetitionState.IDLE) {
                        if(s.isPaused() && System.currentTimeMillis()/500%2 == 1) {
                            g.setColor(new Color(0xff, 0xff, 0x00));
                        } else if(competition.redFlagged() && System.currentTimeMillis()/500%2 == 1) {
                            g.setColor(new Color(0xff, 0x00, 0x00));
                        } else {
                            g.setColor(BG_COLOR);
                        }
                        g.fillRect(0, 0, W, H(PAUSE_BAR_H));
                        g.fillRect(0, H(1-PAUSE_BAR_H), W, H(PAUSE_BAR_H));
                        yOffset += H(PAUSE_BAR_H)+timeBarHPx+spacingSPx;
                        str = text.get("TEAM") + " #" + teamID;
                        str2 = " " + teamName;
                        drawText(g, str, spacingSPx, yOffset, true);
                        drawText(g, str2, spacingSPx+getTextWidth(str), yOffset, false);
                        yOffset += charH;
                        if(competition.getState() != CompetitionState.POST_RUN) {
                            double ratio = (double)s.getElapsedTimeMilliseconds()/
                                    (competition.getState() == CompetitionState.SETUP ? 
                                    s.getSetupDuration() : s.getWindowDuration());
                            ratio = ratio < 0 ? 0 : ratio;
                            ratio = ratio > 1 ? 1 : ratio;
                            g.setColor(new Color(PRIMARY_RED+(int)(ratio*(SECONDARY_RED-PRIMARY_RED)),
                                    PRIMARY_GREEN-(int)(ratio*(PRIMARY_GREEN-SECONDARY_GREEN)),
                                    PRIMARY_BLUE-(int)(ratio*(PRIMARY_BLUE-SECONDARY_BLUE))
                            ));
                            g.fillRect(0, H(PAUSE_BAR_H), (int)(ratio*W), timeBarHPx);
                            
                            drawClock(g,
                                    (int)((s.getSecondsLeft()+1)/60),
                                    (int)((s.getSecondsLeft()+1)%60),
                                    s.getMinutesLeft() < 2,
                                    ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx+2*digitW+spacingSPx : 
                                            W(1)-clockMarginPx-2*digitW-spacingSPx,
                                    H(1-PAUSE_BAR_H)-spacingMPx-charH-spacingLPx-digitH
                            );
                            g.drawImage(s.getMinutesLeft() < 2 ? scaledRedDigits[COLON] : scaledBlueDigits[COLON],
                                    ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx+2*digitW+spacingSPx-scaledRedDigits[COLON].getWidth()/2 :
                                            W(1)-clockMarginPx-2*digitW-spacingSPx-scaledRedDigits[COLON].getWidth()/2,
                                    H(1-PAUSE_BAR_H)-spacingMPx-charH-spacingLPx-digitH, this);
                        } else {
                            drawClock(g,
                                    0, 0, true,
                                    ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx+2*digitW+spacingSPx : 
                                            W(1)-clockMarginPx-2*digitW-spacingSPx,
                                    H(1-PAUSE_BAR_H)-spacingMPx-charH-spacingLPx-digitH
                            );
                            g.drawImage(scaledRedDigits[COLON],
                                    ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx+2*digitW+
                                            spacingSPx-scaledRedDigits[COLON].getWidth()/2 :
                                            W(1)-clockMarginPx-2*digitW-spacingSPx-scaledRedDigits[COLON].getWidth()/2,
                                    H(1-PAUSE_BAR_H)-spacingMPx-charH-spacingLPx-digitH, this);
                        }
                        int scoreWidth;
                        scoreWidth = 6*smallW+scaledSmallDigits[PERIOD].getWidth();                          
                        switch (competition.getState()) {
                            case CompetitionState.SETUP:
                                str = text.get("SETUP_PERIOD");
                                y = H(1-PAUSE_BAR_H)-spacingMPx-charH;
                                drawText(g, str, 
                                        ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx : W(1)-clockMarginPx-getTextWidth(str),
                                        y, false);
                                if(teamBadges.get(teamID) != null) {
                                    g.setColor(PRIMARY_COLOR);
                                    BufferedImage badge = teamBadges.get(teamID);
                                    g.drawRect(
                                            ALIGN_CLOCK_LEFT ? 
                                                    W(1)-spacingSPx-spacingXSPx-badge.getWidth() :
                                                    spacingSPx-spacingXSPx, 
                                            (int)(H(1-PAUSE_BAR_H)-spacingSPx-spacingXSPx-badge.getHeight()),
                                            badge.getWidth()+2*spacingXSPx, 
                                            badge.getHeight()+2*spacingXSPx);
                                    g.drawImage(badge,
                                            ALIGN_CLOCK_LEFT ? W(1)-spacingSPx-badge.getWidth() :
                                                    spacingSPx,
                                            H(1-PAUSE_BAR_H)-spacingSPx-badge.getHeight(), this);
                                }
                                if(SHOW_BANNER && scaledBannerImage != null) {
                                    g.drawImage(scaledBannerImage,
                                            W(1)-spacingSPx-scaledBannerImage.getWidth(),
                                            H(PAUSE_BAR_H)+spacingSPx, null);
                                }

                                break OUTER;
                            case CompetitionState.RUN:
                                str = text.get("RUN") + " ";
                                str2 = "" + competition.getSession().getRunNumber() + 
                                        " " + text.get("OF") + " " + 
                                        competition.getSession().getMaxAttempts();
                                y = H(1-PAUSE_BAR_H)-spacingMPx-charH;
                                drawText(g, str, 
                                        ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx :
                                                W(1)-clockMarginPx-
                                                getTextWidth(str)-getTextWidth(str2),
                                        y, false);                                
                                drawText(g, str2,
                                        ALIGN_CLOCK_LEFT ? clockMarginPx+spacingSPx+getTextWidth(str)
                                                 : W(1)-clockMarginPx-getTextWidth(str2),
                                        y, true);                              
                                i = 0;
                                int colW;
                                int rowH = spacingXLPx+smallH+spacingSPx+charH;
                                x = spacingSPx;
                                yOffset += spacingMPx;
                                for(String key : Config.getKeysInOriginalOrder("fields")) {
                                    int colW_a = spacingXLPx+scoreDigits[i]*smallW +
                                           (scoreDecimal[i] == 0 ? 0 : 
                                           scoreDecimal[i]*smallW+scaledSmallDigits[PERIOD].getWidth());
                                    int colW_b = spacingXLPx+getTextWidth(key);
                                    colW = colW_a > colW_b ? colW_a : colW_b;
                                    if(x + colW > W(1) - spacingLPx) {
                                        x = spacingSPx;
                                        yOffset += rowH;
                                    }                                    
                                    g.setColor(PRIMARY_COLOR);
                                    g.drawRect(x+spacingXSPx, yOffset+spacingXSPx,
                                               colW-spacingSPx, rowH-spacingSPx);
                                    str = key;
                                    drawText(g, str, x+spacingLPx, yOffset+spacingLPx, false);
                                    drawScoreField(g, scores[i],
                                            scoreDigits[i], scoreDecimal[i],
                                            x+spacingLPx, yOffset+spacingLPx+charH+spacingSPx);
                                    i++;
                                    x += colW;
                                }
                                str = text.get("CURRENT") + " ";
                                str2 = String.format("%.2f", currentScoreVal);
                                x = W(1)-spacingSPx-getTextWidth(str2);
                                y = H(PAUSE_BAR_H) + spacingMPx;
                                drawText(g, str2, x, y, true);
                                x -= getTextWidth(str);
                                drawText(g, str, x, y, false);
                            case CompetitionState.POST_RUN:
                                Team t = competition.getSession().getActiveTeam();
                                if(t.hasScore()) {
                                    str = text.get("BEST_SCORE");
                                    drawText(g, str,
                                            ALIGN_CLOCK_LEFT ? W(1)-clockMarginPx-getTextWidth(str)-spacingSPx : 
                                            clockMarginPx,
                                            H(1-PAUSE_BAR_H)-spacingMPx-charH, false);
                                    drawScore(g, bestScoreVal, 
                                            ALIGN_CLOCK_LEFT ?
                                                    W(1)-clockMarginPx-scoreWidth-spacingSPx :
                                                    clockMarginPx,
                                            H(1-PAUSE_BAR_H)-spacingMPx-charH-spacingLPx-smallH);
                                } else {
                                    str = "NO SCORE";
                                    drawText(g, str,
                                            ALIGN_CLOCK_LEFT ? W(1)-clockMarginPx-getTextWidth(str)-spacingSPx :
                                                    clockMarginPx,
                                            H(1-PAUSE_BAR_H)-spacingMPx-charH, true);
                                }
                                break OUTER;
                            default:
                                break;
                        }
                    } else if(cc.getControlFrame() != null || nextTeamID >= 0) {
                        if(cc.getControlFrame() != null) {
                            nextTeamID = cc.getControlFrame().getSelectedTeamID();
                        }
                        if(nextTeamID >= 0) {
                            Team t = competition.getTeamByID(nextTeamID);
                            str = text.get("NEXT") + ":";
                            drawText(g, str, spacingSPx, H(1)-spacingMPx-3*charH, false);
                            str = "#" + nextTeamID + " " + t.getName();
                            drawText(g, str, spacingSPx, H(1)-spacingMPx-2*charH, true);
                            str = t.getInstitution();
                            drawText(g, str, spacingSPx, H(1)-spacingMPx-1*charH, false);
                        }
                    }
                case OUTPUT_LOGO:
                    g.setColor(SECONDARY_COLOR);
                    g.drawImage(scaledLogo, //(int)(System.currentTimeMillis()/500%10)],
                            centeredX(scaledLogo.getWidth()),
                            H(LOGO_Y_POSITION_PROPORTION) - scaledLogo.getHeight()/2,
                            this);
                    int hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int minutes = Calendar.getInstance().get(Calendar.MINUTE);
                    y =  H(LOGO_Y_POSITION_PROPORTION) + scaledLogo.getHeight()/2 + spacingSPx;
                    if((Calendar.getInstance().get(Calendar.SECOND) % 2) != 1) {
                        g.drawImage(scaledBlueDigits[COLON],
                                (int)(W(0.5)-scaledBlueDigits[COLON].getWidth()/2),
                                y, this);
                    }
                    drawClock(g, hours, minutes, false, W(0.5), y);
                    if(SHOW_BANNER && scaledBannerImage != null) {
                        y = H(1)-scaledBannerImage.getHeight()-spacingSPx;
                        g.drawImage(scaledBannerImage, W(1)-spacingSPx-scaledBannerImage.getWidth(), y, null);
                    }
                        
                    break;
                case OUTPUT_CLASSIFICATION:
                    y = 5;
                    str = text.get("CLASSIFICATION");
                    drawText(g, str, centeredX(getTextWidth(str)), y, false);
                    y += charH + spacingXSPx;
                    str = text.get("POSITION");
                    drawText(g, str, spacingLPx, y, false);
                    str = text.get("TEAM");
                    drawText(g, str, W(0.1), y, false);
                    str = text.get("ID");
                    drawText(g, str, W(0.6), y, false);
                    str = text.get("SCORE");
                    drawText(g, str, W(0.8), y, false);
                    y += charH + spacingXSPx;
                    g.setColor(SECONDARY_COLOR);
                    g.fillRect(0, y, W(1), horizBarHPx);
                    y += horizBarHPx;
                    boolean background = true;
                    int latchRankStart = rankStart;
                    for(i = 0; i < spacingSPx; i++) {
                        if(i+(latchRankStart-1) >= classificationRows.size()) {
                            break;
                        }
                        if(background) {
                            g.setColor(TABLE_BG_COLOR);
                            g.fillRect(0, y, W(1), charH+spacingSPx);
                        }
                        y += 5;
                        
                        String[] row = classificationRows.get(i+(latchRankStart-1));
                        drawText(g, row[0], spacingLPx, y, true);
                        drawText(g, row[1], W(0.1), y, true);
                        drawText(g, row[2], W(0.6), y, true);
                        drawText(g, row[3], W(0.8), y, true);
                        y += charH + spacingXSPx;
                        background = !background;
                    }
                    
                    if(competition.getState() == CompetitionState.SETUP ||
                            competition.getState() == CompetitionState.RUN) {
                        g.setColor(BG_COLOR);
                        g.fillRect(0, H(1)-spacingXSPx-charH-spacingXSPx, W, charH+spacingSPx);
                        g.setColor(ALT_COLOR);
                        g.fillRect(0, H(1)-spacingXSPx-charH-spacingXSPx-horizBarHPx, W, horizBarHPx);
                        str = competition.getState() == CompetitionState.SETUP ? 
                                text.get("SETUP_TIME_LEFT") + " " :
                                text.get("TIME_LEFT") + " ";
                        int prevWidth = getTextWidth(str);
                        drawText(g, str, spacingXSPx, H(1)-spacingXSPx-charH, false);
                        str = ((competition.getSession().getSecondsLeft()+1)/60) + ":" + 
                                String.format("%02d", ((competition.getSession().getSecondsLeft()+1) % 60));
                        drawText(g, str, spacingXSPx+prevWidth, H(1)-spacingXSPx-charH, true);
                    } 
                    
                    break;
            }
            
            if(GENERATE_THUMBNAIL) {
                if(thumbIntervalCount == THUMB_INTERVAL) {
                    thumbIntervalCount = 0;
                    generateThumbnail();
                }
                thumbIntervalCount++;
            }
            
            renderedFrames++;
            _g.drawImage(displayImage, 0, 0, null);
            g.dispose();            
            if(DRAW_RENDER_TIME) {                
                String strFPS =
                        String.format("%.2f",
                                (float)renderedFrames/((System.nanoTime()-beginTime)/1000000000.0))
                        + " fps";                
                String strControlRenderTime = 
                        cc.getControlFrame() != null ?
                        ("control: " +
                        String.format("%1$4s", cc.getControlFrame().getRenderTime()/1000000) + " ms")
                        : "";
                
                _g.setFont(new Font("Monospaced", Font.PLAIN, 14));
                int txtHeight = _g.getFontMetrics().getHeight();
                int txtDescent = _g.getFontMetrics().getDescent();
                
                int txtWidth = _g.getFontMetrics().stringWidth(strFPS);
                _g.setColor(Color.BLACK);
                _g.fillRect(W(1)-4-txtWidth, H(1)-txtHeight, txtWidth+4, txtHeight);
                _g.setColor(Color.YELLOW);
                _g.drawString(strFPS, W(1)-2-txtWidth, H(1)-txtDescent);
                
                txtWidth = _g.getFontMetrics().stringWidth(strControlRenderTime);
                _g.setColor(Color.BLACK);
                _g.fillRect(W(1)-4-txtWidth, H(1)-3*txtHeight, txtWidth+4, txtHeight);
                _g.setColor(Color.YELLOW);
                _g.drawString(strControlRenderTime, W(1)-2-txtWidth, H(1)-2*txtHeight-txtDescent);
                
                renderTime = System.nanoTime()-startTime;
                String strRenderTime = "display: " + 
                        String.format("%1$4s", renderTime/1000000) + " ms";
                txtWidth = _g.getFontMetrics().stringWidth(strRenderTime);
                _g.setColor(Color.BLACK);
                _g.fillRect(W(1)-4-txtWidth, H(1)-2*txtHeight, txtWidth+4, txtHeight);
                _g.setColor(Color.YELLOW);
                _g.drawString(strRenderTime, W(1)-2-txtWidth, H(1)-txtHeight-txtDescent);
            }                        
            drawing = false;
        }        
        
        private void drawClock(Graphics2D g, int high, int low, boolean red, int x, int y) {
            int digit0 = high / 10 % 10;
            int digit1 = high % 10;
            int digit2 = low / 10 % 10;
            int digit3 = low % 10;
            
            g.drawImage(!red ? scaledBlueDigits[digit0] : scaledRedDigits[digit0],
                    x-2*digitW-spacingSPx, y, this);
            g.drawImage(!red ? scaledBlueDigits[digit1] : scaledRedDigits[digit1],
                    x-1*digitW-spacingSPx, y, this);
            g.drawImage(!red ? scaledBlueDigits[digit2] : scaledRedDigits[digit2],
                    x+spacingSPx, y, this);
            g.drawImage(!red ? scaledBlueDigits[digit3] : scaledRedDigits[digit3],
                    x+digitW+spacingSPx, y, this);
        }
        
        private void drawScoreField(Graphics2D g, double score, int digits, int decimal, int x, int y) {
            boolean leading = true;
            boolean outOfRange = score < 0 || score >= Math.pow(10,digits);
            int totalDigits = digits + decimal;
            int normalizedScore = (int)(score*Math.pow(10,decimal));
            if((int)score >= Math.pow(10, digits)) {
                normalizedScore %= (int)(Math.pow(10, totalDigits));
            }
            String value = String.format("%0" + totalDigits + "d", normalizedScore);
            int i;
            char c;
            for(i = 0; i < digits; i++) {
                if(outOfRange) {
                    g.drawImage(scaledSmallDigits[DASH], x, y, this);
                } else {
                    c = value.charAt(i);
                    if(leading && c != '0' || i == digits-1) {
                        leading = false;
                    }
                    g.drawImage(scaledSmallDigits[leading ? 13 : c-48], x ,y, this);
                }
                x += smallW;
            }
            if(decimal > 0) {
                g.drawImage(scaledSmallDigits[PERIOD], x, y, this);
                x += scaledSmallDigits[PERIOD].getWidth();
            }
            for(i = digits; i < totalDigits; i++) {
                if(outOfRange) {
                    g.drawImage(scaledSmallDigits[DASH], x, y, this);
                } else {
                    g.drawImage(scaledSmallDigits[value.charAt(i)-48], x ,y, this);
                }
                x += smallW;
            }
        }
        
        private void drawScore(Graphics2D g, Double score, int x, int y) {
            // not gonna bother with rounding
            if(score == null) {
                g.drawImage(scaledSmallDigits[DASH], x, y, this);
                g.drawImage(scaledSmallDigits[DASH], x+smallW, y, this);
                g.drawImage(scaledSmallDigits[DASH], x+2*smallW, y, this);
                g.drawImage(scaledSmallDigits[DASH], x+3*smallW, y, this);
                g.drawImage(scaledSmallDigits[PERIOD], x+4*smallW, y, this);
                g.drawImage(scaledSmallDigits[DASH], x+4*smallW+scaledSmallDigits[PERIOD].getWidth(), y, this);
                g.drawImage(scaledSmallDigits[DASH], x+5*smallW+scaledSmallDigits[PERIOD].getWidth(), y, this);
                return;
            }
            int scoreInt = (int)(score*100);
            if(scoreInt < 0) {
                g.drawImage(scaledSmallDigits[12], x, y, this);
                scoreInt *= -1;
            } else {
                g.drawImage(scaledSmallDigits[13], x, y, this);
            }
            
            int[] digits = new int[5];
            for(int i = 0; i < 5; i++) {
                digits[i] = scoreInt/(int)Math.pow(10,4-i) % 10;                
            }            
            
            g.drawImage(
                    digits[0] == 0 ? scaledSmallDigits[13] : scaledSmallDigits[digits[0]],
                    x+smallW, y, this
            );
            
            g.drawImage(
                    digits[0] == 0 && digits[1] == 0? scaledSmallDigits[13] : scaledSmallDigits[digits[1]],
                    x+2*smallW, y, this
            );
            
            g.drawImage(
                    scaledSmallDigits[digits[2]],
                    x+3*smallW, y, this
            );
            
            g.drawImage(
                    scaledSmallDigits[PERIOD],
                    x+4*smallW, y, this
            );
            
            g.drawImage(
                    scaledSmallDigits[digits[3]],
                    x+4*smallW+scaledSmallDigits[PERIOD].getWidth(), y, this
            );
            
            g.drawImage(
                    scaledSmallDigits[digits[4]],
                    x+5*smallW+scaledSmallDigits[PERIOD].getWidth(), y, this
            );
        }                
    }        
}
