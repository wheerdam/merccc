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
    
    private CompetitionState c;
    private int mode;
    private int W;
    private int H;
    private int digitW;
    private int digitH;
    private int smallW;
    private int smallH;
    private int charW;
    private int charH;
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
    private RefreshThread refresh;
    private int rankStart = 1;
    private ArrayList<String[]> classificationRows;
    private double[] scores;
    private int[] scoreDigits;
    private int[] scoreDecimal;
    private Score currentScore;
    private Font nativeFont;
    
    public static final int OUTPUT_LOGO = 0;
    public static final int OUTPUT_RUN_STATUS = 1;
    public static final int OUTPUT_CLASSIFICATION = 2;
    
    public static final int COLON = 10;
    public static final int PERIOD = 11;
    public static final int DASH = 12;
    
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
     
    public static final int DISPLAY_REFRESH_RATE_MS = 50;
    public static boolean DRAW_RENDER_TIME = false;
    public static boolean ALIGN_CLOCK_LEFT = false;
    public static float POSITION_RIGHT_RECORDED_SCORES = 0.5f;
    public static float POSITION_LEFT_RECORDED_SCORES = 0.3f;
    private static String NATIVE_FONT;
    
    private static long renderTime = -1;
    
    public DisplayFrame(String nativeFont) {
        this.c = ControlCenter.competition;
        mode = 0;        
        NATIVE_FONT = nativeFont;
    }
    
    public void init() {
        Log.d(0, "DisplayFrame: init");
        PRIMARY_COLOR = new Color(PRIMARY_RED, PRIMARY_GREEN, PRIMARY_BLUE);
        SECONDARY_COLOR = new Color(SECONDARY_RED, SECONDARY_GREEN, SECONDARY_BLUE);
        ALT_COLOR = new Color(ALT_RED, ALT_GREEN, ALT_BLUE);
        BG_COLOR = new Color(BG_RED, BG_GREEN, BG_BLUE);
        canvas = new DisplayCanvas();
        add(canvas);
        refresh = new RefreshThread(this);
        refresh.start();
        classificationRows = new ArrayList();
        scores = new double[Score.fields.size()];
        scoreDigits = new int[Score.fields.size()];
        scoreDecimal = new int[Score.fields.size()];
        teamBadges = new BufferedImage[c.getTeams().size()];
        nativeFont = new Font(Font.MONOSPACED, Font.BOLD, 12);
        newScore();
        if(ControlCenter.control != null) {
            ControlCenter.control.addScoreChangedHook((String key, int scoreID, String scoreValue) -> {
                scores[scoreID] = Double.parseDouble(scoreValue);
                currentScore.setValue(key, scores[scoreID]);
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
        for(int i = 0; i < scores.length; i++) {
            for(String key : Config.getKeysInOriginalOrder("fields")) {
                scores[i] = Score.defaultValue.get(key);
                currentScore.setValue(key, Score.defaultValue.get(key));
            }
        }
    }
    
    public void setScore(String key, int id, double value) {
        if(currentScore == null)
            return;
        
        scores[id] = value;
        currentScore.setValue(key, value);
    }
    
    private void rescale(int width, int height) {
        Log.d(0, "DisplayFrame.rescale: rescaling");        
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
               
        if(NATIVE_FONT != null) {
            int fontSize = 0;
            do {            
                fontSize++;
                nativeFont = new Font(NATIVE_FONT, Font.BOLD, fontSize);
            } while(g.getFontMetrics(nativeFont).getHeight() < charH);
            charW = g.getFontMetrics(nativeFont).stringWidth("A");
            Log.d(3, "DisplayFrame.rescale: font size="+fontSize);
        } else {
            scaledAlphabet = Assets.scaleFontH(0, charH);
            scaledNonAlphabet = Assets.scaleFontH(1, charH);
            scaledWhiteAlphabet = Assets.scaleFontH(2, charH);
            scaledWhiteNonAlphabet = Assets.scaleFontH(3, charH);
            charW = scaledAlphabet[0].getWidth();
        }
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
        refresh.stopThread();
    }
    
    private int getTextWidth(String str) {
        if(NATIVE_FONT != null) {
            return canvas.getGraphics().getFontMetrics(nativeFont).stringWidth(str);
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
        if(NATIVE_FONT != null) {
            FontMetrics m = g.getFontMetrics(nativeFont);
            int baseY = y + m.getHeight() - m.getDescent();
            g.setColor(white ? Color.WHITE : PRIMARY_COLOR);            
            g.setFont(nativeFont);
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
            Graphics2D g = (Graphics2D) _g;
            if(W != getWidth() || H != getHeight()) {
                W = getWidth();
                H = getHeight();
                rescale(W, H);
            }
            int x, y, i;
            
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
            SessionState s = c.getSession();
            
            int teamID = -1;
            String teamName = "";
            String institution = "";
            if(s != null && s.getActiveTeam() != null) {
                teamID = s.getActiveTeam().getNumber();
                teamName = s.getActiveTeam().getName();
                institution = s.getActiveTeam().getInstitution();
            }
            int yOffset = 0;
            OUTER:
            switch (mode) {
                case OUTPUT_RUN_STATUS:
                    if (c.getState() != CompetitionState.IDLE) {
                        if(s.isPaused() && System.currentTimeMillis()/500%2 == 1) {
                            g.setColor(new Color(0xff, 0xff, 0x00));
                        } else if(c.redFlagged() && System.currentTimeMillis()/500%2 == 1) {
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
                        if(c.getState() != CompetitionState.POST_RUN) {
                            double ratio = (double)s.getElapsedTimeMilliseconds()/
                                    (c.getState() == CompetitionState.SETUP ? 
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
                                            W(1)-10-2*digitW-10,
                                    H(0.9)-15-charH-20-digitH
                            );
                            g.drawImage(s.getMinutesLeft() < 2 ? scaledRedDigits[COLON] : scaledBlueDigits[COLON],
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10-scaledRedDigits[COLON].getWidth()/2 :
                                            W(1)-10-2*digitW-10-scaledRedDigits[COLON].getWidth()/2,
                                    H(0.9)-15-charH-20-digitH, this);
                        } else {
                            drawClock(g,
                                    0, 0, true,
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10 : 
                                            W(1)-10-2*digitW-10,
                                    H(0.9)-15-charH-20-digitH
                            );
                            g.drawImage(scaledRedDigits[COLON],
                                    ALIGN_CLOCK_LEFT ? 10+2*digitW+10-scaledRedDigits[COLON].getWidth()/2 :
                                            W(1)-10-2*digitW-10-scaledRedDigits[COLON].getWidth()/2,
                                    H(0.9)-15-charH-20-digitH, this);
                        }
                        ArrayList<Score> activeScores;
                        int numScores, scoreWidth;
                        scoreWidth = 6*smallW+scaledSmallDigits[PERIOD].getWidth();                          
                        switch (c.getState()) {
                            case CompetitionState.SETUP:
                                str = "SETUP PERIOD";
                                y = H(0.9)-15-charH;
                                drawText(g, str, 
                                        ALIGN_CLOCK_LEFT ? 10 : W(1)-10-getTextWidth(str),
                                        y, false);
                                str = c.getTeamByID(teamID).getLogoFileName();
                                if(teamBadges[teamID] == null &&
                                        Assets.doesAssetExist(str)) {
                                    BufferedImage logo = Assets.getAsset(str);
                                    teamBadges[teamID] = Assets.scale(logo,
                                            (int)((double)H(TEAM_BADGE_HEIGHT_RATIO)/logo.getHeight()*logo.getWidth()),
                                            H(TEAM_BADGE_HEIGHT_RATIO));
                                } 
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
                                str2 = "" + c.getSession().getRunNumber() + " of " + c.getSession().getMaxAttempts();
                                y = H(0.9)-15-charH;
                                drawText(g, str, 
                                        ALIGN_CLOCK_LEFT ? 10 : W(1)-10-
                                                getTextWidth(str)-getTextWidth(str2),
                                        y, false);                                
                                drawText(g, str2,
                                        ALIGN_CLOCK_LEFT ? 10+getTextWidth(str)
                                                 : W(1)-10-getTextWidth(str2),
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
                                Team t = c.getSession().getActiveTeam();
                                Score highestScore = t.getBestScore();                                
                                if(highestScore != null) {
                                    str = "BEST SCORE";
                                    drawText(g, str,
                                            ALIGN_CLOCK_LEFT ? W(1)-getTextWidth(str)-10 : 10,
                                            H(0.9)-15-charH, false);
                                    drawScore(g, highestScore, 
                                            ALIGN_CLOCK_LEFT ?
                                                    W(1)-scoreWidth-10 :
                                                    10,
                                            H(0.9)-15-charH-20-smallH);
                                } else {
                                    str = "NO SCORE";
                                    drawText(g, str,
                                            ALIGN_CLOCK_LEFT ? W(1)-getTextWidth(str)-10 :
                                                    10,
                                            H(0.9)-15-charH, true);
                                }
                                break OUTER;
                            default:
                                break;
                        }
                    } else if(ControlCenter.control != null) {
                        int nextTeamID = ControlCenter.control.getTeamSelectIndex();
                        if(nextTeamID >= 0) {
                            Team t = c.getTeamByID(nextTeamID);
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
                            g.setColor(new Color(0x05, 0x20, 0x50));
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
                    
                    if(c.getState() == CompetitionState.SETUP ||
                            c.getState() == CompetitionState.RUN) {
                        g.setColor(BG_COLOR);
                        g.fillRect(0, H(1)-5-charH-5, W, charH+10);
                        g.setColor(ALT_COLOR);
                        g.fillRect(0, H(1)-5-charH-5-2, W, 2);
                        str = c.getState() == CompetitionState.SETUP ? "SETUP TIME LEFT " :
                                "TIME LEFT ";
                        int prevWidth = getTextWidth(str);
                        drawText(g, str, 5, H(1)-5-charH, false);
                        str = ((c.getSession().getSecondsLeft()+1)/60) + ":" + 
                                String.format("%02d", ((c.getSession().getSecondsLeft()+1) % 60));
                        drawText(g, str, 5+prevWidth, H(1)-5-charH, true);
                    } 
                    
                    break;
            }
            
            if(DRAW_RENDER_TIME) {
                g.setColor(Color.BLACK);
                str = "" + renderTime + " ms";
                g.fillRect(0, H(1)-charH-10, getTextWidth(str)+10, 10+charH);
                drawText(g, str, 5, H(1)-charH-5, false);
            }
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
            int normalizedScore = (int)(score*Math.pow(10,decimal));;
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
    
    class RefreshThread extends Thread {
        private boolean stop = false;
        private DisplayFrame f;
        
        public RefreshThread(DisplayFrame f) {
            this.f = f;
        }
        
        @Override
        public void run() {
            long renderStart;
            Log.d(0, "DisplayFrame.RefreshThread: started");
            while(!stop) {
                renderStart = System.currentTimeMillis();
                f.repaint();
                renderTime = System.currentTimeMillis() - renderStart;
                if(renderTime < DISPLAY_REFRESH_RATE_MS) {
                    try{
                        Thread.sleep(DISPLAY_REFRESH_RATE_MS - renderTime);
                    } catch(Exception e) {
                        
                    }
                }
            }
            Log.d(0, "DisplayFrame.RefreshThread: exit");
        }
        
        public void stopThread() {
            stop = true;
        }
    }
}
