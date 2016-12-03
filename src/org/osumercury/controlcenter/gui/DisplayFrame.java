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

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;
import java.util.Calendar;
import java.util.ArrayList;
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
    private BufferedImage[] scaledBlueDigits = new BufferedImage[13];
    private BufferedImage[] scaledRedDigits = new BufferedImage[13];
    private BufferedImage[] scaledSmallDigits = new BufferedImage[13];
    private BufferedImage[] scaledAlphabet = new BufferedImage[26];
    private BufferedImage[] scaledNonAlphabet = new BufferedImage[19];
    private BufferedImage[] scaledWhiteAlphabet = new BufferedImage[26];
    private BufferedImage[] scaledWhiteNonAlphabet = new BufferedImage[19];
    private BufferedImage[] teamBadges;
    private DisplayCanvas canvas;
    private int rankStart = 1;
    private int thumbIntervalCount = 0;
    private ArrayList<String[]> classificationRows;
    private double[] scores;
    private int[] scoreDigits;
    private int[] scoreDecimal;
    private Score currentScore;
    private Font systemFont;
    private String systemFontName;                
    
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
    public static int SCORE_FIELD_DIGITS = 2;
    public static int SCORE_FIELD_DECIMAL = 2;
    
    public static double TEAM_BADGE_HEIGHT_RATIO = 0.55;
    
    public static int BG_RED = 0;
    public static int BG_GREEN = 0;
    public static int BG_BLUE = 0;
    public static Color BG_COLOR;
    public static BufferedImage BG_IMAGE;
    public static int BG_ALIGNMENT = 0;
    
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
     
    public static boolean DRAW_RENDER_TIME = false;
    public static boolean ALIGN_CLOCK_LEFT = false;
    public static boolean GENERATE_THUMBNAIL = false;
    public static float POSITION_RIGHT_RECORDED_SCORES = 0.5f;
    public static float POSITION_LEFT_RECORDED_SCORES = 0.3f;
    public static int THUMB_WIDTH = 300;
    public static int THUMB_INTERVAL = 2;
    
    public DisplayFrame(ControlCenter cc, String nativeFont) {
        this.cc = cc;
        this.competition = cc.getCompetitionState();
        mode = 0;        
        systemFontName = nativeFont;
    }
    
    public void init() {
        Log.d(0, "DisplayFrame: init");
        PRIMARY_COLOR = new Color(PRIMARY_RED, PRIMARY_GREEN, PRIMARY_BLUE);
        SECONDARY_COLOR = new Color(SECONDARY_RED, SECONDARY_GREEN, SECONDARY_BLUE);
        ALT_COLOR = new Color(ALT_RED, ALT_GREEN, ALT_BLUE);
        BG_COLOR = new Color(BG_RED, BG_GREEN, BG_BLUE);
        TABLE_BG_COLOR = new Color(TABLE_BG_RED, TABLE_BG_GREEN, TABLE_BG_BLUE);
        canvas = new DisplayCanvas();
        add(canvas);
        classificationRows = new ArrayList();
        scores = new double[Score.getFields().size()];
        scoreDigits = new int[Score.getFields().size()];
        scoreDecimal = new int[Score.getFields().size()];
        teamBadges = new BufferedImage[competition.getTeams().size()];
        systemFont = new Font(Font.MONOSPACED, Font.BOLD, 12);
        newScore();
        if(cc.getControlFrame() != null) {
            cc.getControlFrame().addScoreChangedHook(
                    (String key, int scoreID, String scoreValue) -> {
                        scores[scoreID] = Double.parseDouble(scoreValue);
                        currentScore.setValue(key, scores[scoreID]);
                    }
            );        
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
    }
    
    public void setScore(String key, int id, double value) {
        if(currentScore == null)
            return;
        
        scores[id] = value;
        currentScore.setValue(key, value);
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
        digitH = (int)(0.10*height);
        smallH = (int)(0.07*height);
        charH = (int)(0.06*height);
        scaledLogo = Assets.getLogoH((int)(0.40*height));
        scaledBlueDigits = Assets.scaleDigitsH(digitH, false);
        scaledRedDigits = Assets.scaleDigitsH(digitH, true);
        scaledSmallDigits = Assets.scaleDigitsH(smallH, true);
        digitW = scaledBlueDigits[0].getWidth();
        smallW = scaledSmallDigits[0].getWidth();
               
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
                    teamBadges[teamID] = Assets.scale(logo,
                            logoScaledWidth,
                            (int)(height*TEAM_BADGE_HEIGHT_RATIO));
                    
                // otherwise scale to half screen width so the clock won't be
                // covered by the team badge
                } else {
                    Log.d(2, "DisplayFrame.rescale: teamBadges[" + teamID + "]: " +
                            "scaled by width");
                    teamBadges[teamID] = Assets.scale(logo,
                            (int)(0.5*width),
                            (int)(0.5*width/logo.getWidth()*logo.getHeight()));
                }
            } 
        }
    }
    
    public void setFont(String fontName) {
        systemFontName = fontName;
        rescale(getWidth(), getHeight());
    }
    
    public void setMode(int mode) {
        this.mode = mode;
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
    
    public void stopRefreshThread() {
        // refresh.stopThread();
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
            
            if(BG_IMAGE != null) {
                switch(BG_ALIGNMENT) {
                    case 0:
                        g.drawImage(BG_IMAGE, 0, 0, null);
                        break;
                    case 1:
                        g.drawImage(BG_IMAGE, W(1)-BG_IMAGE.getWidth(), 0, null);
                        break;
                    case 2:
                        g.drawImage(BG_IMAGE, W(1)-BG_IMAGE.getWidth(), H(1)-BG_IMAGE.getHeight(), null);
                        break;
                    case 3:
                        g.drawImage(BG_IMAGE, 0, H(1)-BG_IMAGE.getHeight(), null);
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
                        g.fillRect(0, 0, W, H(0.1));
                        g.fillRect(0, H(0.9), W, H(0.1));
                        yOffset += H(0.1)+15;
                        str = "ROBOT #" + teamID;
                        str2 = " " + teamName;
                        drawText(g, str, 10, yOffset, true);
                        drawText(g, str2, 10+getTextWidth(str), yOffset, false);
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
                            g.fillRect(0, H(0.1), (int)(ratio*W), 6);
                            
                            drawClock(g,
                                    (int)((s.getSecondsLeft()+1)/60),
                                    (int)((s.getSecondsLeft()+1)%60),
                                    s.getMinutesLeft() < 2,
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10 : 
                                            W(1)-100-2*digitW-10,
                                    H(0.9)-15-charH-20-digitH
                            );
                            g.drawImage(s.getMinutesLeft() < 2 ? scaledRedDigits[COLON] : scaledBlueDigits[COLON],
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10-scaledRedDigits[COLON].getWidth()/2 :
                                            W(1)-100-2*digitW-10-scaledRedDigits[COLON].getWidth()/2,
                                    H(0.9)-15-charH-20-digitH, this);
                        } else {
                            drawClock(g,
                                    0, 0, true,
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10 : 
                                            W(1)-100-2*digitW-10,
                                    H(0.9)-15-charH-20-digitH
                            );
                            g.drawImage(scaledRedDigits[COLON],
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10-scaledRedDigits[COLON].getWidth()/2 :
                                            W(1)-100-2*digitW-10-scaledRedDigits[COLON].getWidth()/2,
                                    H(0.9)-15-charH-20-digitH, this);
                        }
                        int scoreWidth;
                        scoreWidth = 6*smallW+scaledSmallDigits[PERIOD].getWidth();                          
                        switch (competition.getState()) {
                            case CompetitionState.SETUP:
                                str = "SETUP PERIOD";
                                y = H(0.9)-15-charH;
                                drawText(g, str, 
                                        ALIGN_CLOCK_LEFT ? 10 : W(1)-100-getTextWidth(str),
                                        y, false);
                                if(teamBadges[teamID] != null) {
                                    g.setColor(PRIMARY_COLOR);
                                    g.drawRect(
                                            ALIGN_CLOCK_LEFT ? W(1)-12-teamBadges[teamID].getWidth() :
                                                    8, 
                                            H(0.9)-12-teamBadges[teamID].getHeight(),
                                            teamBadges[teamID].getWidth()+3, 
                                            teamBadges[teamID].getHeight()+3);
                                    g.drawImage(teamBadges[teamID],
                                            ALIGN_CLOCK_LEFT ? W(1)-10-teamBadges[teamID].getWidth() :
                                                    10,
                                            H(0.9)-10-teamBadges[teamID].getHeight(), this);
                                }

                                break OUTER;
                            case CompetitionState.RUN:
                                str = "RUN ";
                                str2 = "" + competition.getSession().getRunNumber() + " of " + competition.getSession().getMaxAttempts();
                                y = H(0.9)-15-charH;
                                drawText(g, str, 
                                        ALIGN_CLOCK_LEFT ? 10 : W(1)-100-
                                                getTextWidth(str)-getTextWidth(str2),
                                        y, false);                                
                                drawText(g, str2,
                                        ALIGN_CLOCK_LEFT ? 10+getTextWidth(str)
                                                 : W(1)-100-getTextWidth(str2),
                                        y, true);                              
                                i = 0;
                                int colW;
                                int rowH = 40+smallH+10+charH;
                                x = 10;
                                yOffset += 15;
                                for(String key : Config.getKeysInOriginalOrder("fields")) {
                                    int colW_a = 40+scoreDigits[i]*smallW +
                                           (scoreDecimal[i] == 0 ? 0 : 
                                           scoreDecimal[i]*smallW+scaledSmallDigits[PERIOD].getWidth());
                                    int colW_b = 40+getTextWidth(key);
                                    colW = colW_a > colW_b ? colW_a : colW_b;
                                    if(x + colW > W(1) - 20) {
                                        x = 10;
                                        yOffset += rowH;
                                    }                                    
                                    g.setColor(PRIMARY_COLOR);
                                    g.drawRect(x+5, yOffset+5, colW-10, rowH-10);
                                    str = key;
                                    drawText(g, str, x+20, yOffset+20, false);
                                    drawScoreField(g, scores[i],
                                            scoreDigits[i], scoreDecimal[i],
                                            x+20, yOffset+20+charH+10);
                                    i++;
                                    x += colW;
                                }
                                str = "CURRENT ";
                                str2 = String.format("%.2f", Score.calculate(currentScore));
                                x = W(1)-10-getTextWidth(str2);
                                y = H(0.1) + 15;
                                drawText(g, str2, x, y, true);
                                x -= getTextWidth(str);
                                drawText(g, str, x, y, false);
                            case CompetitionState.POST_RUN:
                                Team t = competition.getSession().getActiveTeam();
                                Score highestScore = t.getBestScore();                                
                                if(highestScore != null) {
                                    str = "BEST SCORE";
                                    drawText(g, str,
                                            ALIGN_CLOCK_LEFT ? W(1)-getTextWidth(str)-10 : 100,
                                            H(0.9)-15-charH, false);
                                    drawScore(g, highestScore, 
                                            ALIGN_CLOCK_LEFT ?
                                                    W(1)-scoreWidth-10 :
                                                    100,
                                            H(0.9)-15-charH-20-smallH);
                                } else {
                                    str = "NO SCORE";
                                    drawText(g, str,
                                            ALIGN_CLOCK_LEFT ? W(1)-getTextWidth(str)-10 :
                                                    100,
                                            H(0.9)-15-charH, true);
                                }
                                break OUTER;
                            default:
                                break;
                        }
                    } else if(cc.getControlFrame() != null) {
                        int nextTeamID = cc.getControlFrame().getTeamSelectIndex();
                        if(nextTeamID >= 0) {
                            Team t = competition.getTeamByID(nextTeamID);
                            str = "NEXT:";
                            drawText(g, str, 10, H(1)-15-3*charH, false);
                            str = "#" + nextTeamID + " " + t.getName();
                            drawText(g, str, 10, H(1)-15-2*charH, true);
                            str = t.getInstitution();
                            drawText(g, str, 10, H(1)-15-1*charH, false);
                        }
                    }
                case OUTPUT_LOGO:
                    g.setColor(SECONDARY_COLOR);
                    g.drawImage(scaledLogo, //(int)(System.currentTimeMillis()/500%10)],
                            centeredX(scaledLogo.getWidth()),
                            H(0.4) - scaledLogo.getHeight()/2,
                            this);
                    int hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int minutes = Calendar.getInstance().get(Calendar.MINUTE);
                    y =  H(0.4) + scaledLogo.getHeight()/2 + 10;
                    if((Calendar.getInstance().get(Calendar.SECOND) % 2) != 1) {
                        g.drawImage(scaledBlueDigits[COLON],
                                (int)(W(0.5)-scaledBlueDigits[COLON].getWidth()/2),
                                y, this);
                    }
                    drawClock(g, hours, minutes, false, W(0.5), y);
                        
                    break;
                case OUTPUT_CLASSIFICATION:
                    y = 5;
                    str = "CLASSIFICATION";
                    drawText(g, str, centeredX(getTextWidth(str)), y, false);
                    y += charH + 5;
                    str = "P.";
                    drawText(g, str, 20, y, false);
                    str = "TEAM";
                    drawText(g, str, W(0.1), y, false);
                    str = "ID";
                    drawText(g, str, W(0.6), y, false);
                    str = "SCORE";
                    drawText(g, str, W(0.8), y, false);
                    y += charH + 5;
                    g.setColor(SECONDARY_COLOR);
                    g.fillRect(0, y, W(1), 2);
                    y += 2;
                    boolean background = true;
                    int latchRankStart = rankStart;
                    for(i = 0; i < 10; i++) {
                        if(i+(latchRankStart-1) >= classificationRows.size()) {
                            break;
                        }
                        if(background) {
                            g.setColor(TABLE_BG_COLOR);
                            g.fillRect(0, y, W(1), charH+10);
                        }
                        y += 5;
                        
                        String[] row = classificationRows.get(i+(latchRankStart-1));
                        drawText(g, row[0], 20, y, true);
                        drawText(g, row[1], W(0.1), y, true);
                        drawText(g, row[2], W(0.6), y, true);
                        drawText(g, row[3], W(0.8), y, true);
                        y += charH + 5;
                        background = !background;
                    }
                    
                    if(competition.getState() == CompetitionState.SETUP ||
                            competition.getState() == CompetitionState.RUN) {
                        g.setColor(BG_COLOR);
                        g.fillRect(0, H(1)-5-charH-5, W, charH+10);
                        g.setColor(ALT_COLOR);
                        g.fillRect(0, H(1)-5-charH-5-2, W, 2);
                        str = competition.getState() == CompetitionState.SETUP ? "SETUP TIME LEFT " :
                                "TIME LEFT ";
                        int prevWidth = getTextWidth(str);
                        drawText(g, str, 5, H(1)-5-charH, false);
                        str = ((competition.getSession().getSecondsLeft()+1)/60) + ":" + 
                                String.format("%02d", ((competition.getSession().getSecondsLeft()+1) % 60));
                        drawText(g, str, 5+prevWidth, H(1)-5-charH, true);
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
                String strControlRenderTime = "control: " +
                        String.format("%1$4s", cc.getControlFrame().getRenderTime()/1000000) + " ms";
                
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
                    x-2*digitW-10, y, this);
            g.drawImage(!red ? scaledBlueDigits[digit1] : scaledRedDigits[digit1],
                    x-1*digitW-10, y, this);
            g.drawImage(!red ? scaledBlueDigits[digit2] : scaledRedDigits[digit2],
                    x+10, y, this);
            g.drawImage(!red ? scaledBlueDigits[digit3] : scaledRedDigits[digit3],
                    x+digitW+10, y, this);
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
        
        private int getClockWidth() {
            return 4 * scaledBlueDigits[0].getWidth() + 20;
        }
        
        private void drawScore(Graphics2D g, Score score, int x, int y) {
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
            int scoreInt = (int)(score.getScore()*100);
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
