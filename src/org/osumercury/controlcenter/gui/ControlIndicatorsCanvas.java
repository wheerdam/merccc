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
import java.awt.image.*;
import javax.swing.*;
import java.util.Calendar;
import org.osumercury.controlcenter.CompetitionState;
import org.osumercury.controlcenter.Score;
import org.osumercury.controlcenter.SessionState;

/**
 *
 * @author wira
 */
public class ControlIndicatorsCanvas extends JPanel {
    private SessionState session;
    private CompetitionState c;
    private ControlFrame f;
    private BufferedImage[] scaledBlueDigits = new BufferedImage[14];
    private BufferedImage[] scaledRedDigits = new BufferedImage[14];
    private BufferedImage[] smallDigits = new BufferedImage[14];
    private int W, H;
    private int digitH;
    private int digitW;
    private int smallH;
    private int smallW;
    private int yOffset;
    
    public static final int COLON = 10;
    public static final int PERIOD = 11;
    public static final int DASH = 12;
    
    public long renderTime = 0;
    
    public ControlIndicatorsCanvas(CompetitionState c, ControlFrame f) {
        this.c = c;
        this.f = f;
    }
    
    public void set(SessionState session) {
        this.session = session;
        W = getWidth();
        H = getHeight();
    }
    
    public void rescaleDigits() {
        digitW = (int)(40);
        smallW = (int)(0.7*digitW);
        scaledBlueDigits = Assets.scaleDigitsW(digitW, false);
        scaledRedDigits = Assets.scaleDigitsW(digitW, true);
        smallDigits = Assets.scaleDigitsW(smallW, true);
        digitH = scaledBlueDigits[0].getHeight(this);
        smallH = (int)(0.7*digitH);
        //System.out.println("DigitW=" + scaledBlueDigits[0].getWidth(null) + " DigitH=" + digitH);
        scaledBlueDigits[COLON] = Assets.scaleDigitH(COLON, digitH, false);
        scaledBlueDigits[PERIOD] = Assets.scaleDigitH(PERIOD, digitH, false);
        scaledBlueDigits[DASH] = Assets.scaleDigitH(DASH, digitH, false);
        scaledRedDigits[COLON] = Assets.scaleDigitH(COLON, digitH, true);
        scaledRedDigits[PERIOD] = Assets.scaleDigitH(PERIOD, digitH, true);
        scaledRedDigits[DASH] = Assets.scaleDigitH(DASH, digitH, true);        
        smallDigits[COLON] = Assets.scaleDigitH(COLON, smallH, true);
        smallDigits[PERIOD] = Assets.scaleDigitH(PERIOD, smallH, true);
        smallDigits[DASH] = Assets.scaleDigitH(DASH, smallH, true);  
    }
    
    @Override
    public synchronized void paint(Graphics _g) {
        long startTime = System.nanoTime();
        Graphics2D g = (Graphics2D) _g;
        if(getWidth() != W || getHeight() != H) {
            W = getWidth();
            H = getHeight();
            rescaleDigits();
        } 
        
        yOffset = 0;
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        boolean timerActive = c.getState() == 0 || c.getState() == 1;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        
        int hours;
        int minutes;
        int x, y, i;
        String str;
       
        g.setColor(Color.WHITE);
        g.setFont(new Font("Verdana", Font.BOLD, 16));
        
        str = "CURRENT TIME";
        y = yOffset+g.getFontMetrics().getHeight();                
        g.drawString(str, centeredX(g.getFontMetrics().stringWidth(str)), y);
        yOffset += y+8;
        hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        minutes = Calendar.getInstance().get(Calendar.MINUTE);
        drawDigits(g, hours, minutes, false, true);
        if((Calendar.getInstance().get(Calendar.SECOND) % 2) != 1) {
            g.drawImage(scaledBlueDigits[COLON],
            centeredX(scaledRedDigits[COLON].getWidth(null)), yOffset-digitH, this);
        }
        
        yOffset += 12;

        switch(c.getState()) {
            case CompetitionState.IDLE:                
                
                drawStatusFlags(g, false, false, false, false);
                break;
                
            case CompetitionState.SETUP:
                str = (session.isPaused() ? "SETUP PAUSED" : "SETUP TIME");
                yOffset += g.getFontMetrics().getHeight();                
                g.drawString(str, centeredX(g.getFontMetrics().stringWidth(str)), yOffset);
                yOffset += 8;
                drawDigits(g, (int)(session.getSecondsLeft()+1)/60, (int)(session.getSecondsLeft()+1)%60, 
                        session.getMinutesLeft() < 2, false);
                g.drawImage(session.getMinutesLeft() < 2 ? scaledRedDigits[COLON] : scaledBlueDigits[COLON],
                    centeredX(scaledRedDigits[COLON].getWidth(null)), yOffset-digitH, this);
                drawStatusFlags(g, true, true, session.isPaused(), false);
                break;
                
            case CompetitionState.RUN:
                str = (session.isPaused() ? "TIME PAUSED" : "TIME LEFT");
                yOffset += g.getFontMetrics().getHeight();                    
                g.drawString(str, centeredX(g.getFontMetrics().stringWidth(str)), yOffset);
                yOffset += 8;
                drawDigits(g, (int)(session.getSecondsLeft()+1)/60, (int)(session.getSecondsLeft()+1)%60, 
                        session.getMinutesLeft() < 2, false);
                g.drawImage(session.getMinutesLeft() < 2 ? scaledRedDigits[COLON] : scaledBlueDigits[COLON],
                    centeredX(scaledRedDigits[COLON].getWidth(null)), yOffset-digitH, this);
                yOffset += 12;
                str = ("SCORES");
                y = g.getFontMetrics().getHeight();                
                g.drawString(str, centeredX(g.getFontMetrics().stringWidth(str)), yOffset+y);
                yOffset += y+9;
                for(i = 0; i < session.getMaxAttempts(); i++) {
                    if(i == session.getRunNumber()-1) {
                        g.setColor(Color.WHITE);
                    } else if(i < session.getRunNumber()-1) {
                        g.setColor(new Color(0x20, 0xbb, 0xff));
                    } else {
                        g.setColor(new Color(55, 55, 55));
                    }
                    g.fillRect(15, yOffset, 8, smallH);
                    Score s = i < c.getSession().getActiveScoreList().size() ? c.getSession().getActiveScoreList().get(i) : null;
                    int score;
                    if(s != null) {
                        score = (int) (s.getScore()*100);
                        if(score > 99999 | score < 0) {
                            drawDashes(g);
                        } else {
                            if(score / 10000 != 0) {
                                g.drawImage(smallDigits[score / 10000 % 10], 28, yOffset, this);
                            } else {
                                g.drawImage(smallDigits[13], 28, yOffset, this);
                            }
                             
                            if((score / 10000 != 0) || score / 1000 % 10 != 0) {
                                g.drawImage(smallDigits[score / 1000 % 10], 28+smallW, yOffset, this);
                            } else {
                                g.drawImage(smallDigits[13], 28+smallW, yOffset, this);
                            }
                            g.drawImage(smallDigits[score / 100 % 10], 28+2*smallW, yOffset, this);
                            g.drawImage(smallDigits[PERIOD], 28+3*smallW, yOffset, this);
                            g.drawImage(smallDigits[score / 10 % 10], 28+3*smallW+smallDigits[11].getWidth(), yOffset, this);
                            g.drawImage(smallDigits[score % 10], 28+4*smallW+smallDigits[11].getWidth(), yOffset, this);
                        }
                    } else if(i < session.getRunNumber()-1) {
                        drawDashes(g);
                    }
                    yOffset += 8 + smallH;
                }
                drawStatusFlags(g, !c.redFlagged() && !session.isPaused(), false, session.isPaused(), c.redFlagged());
                break;
                
            case CompetitionState.POST_RUN:
                str = "OUTTA TIME";
                yOffset += g.getFontMetrics().getHeight();                               
                g.drawString(str, centeredX(g.getFontMetrics().stringWidth(str)), yOffset);
                yOffset += 8;
                drawDigits(g, 0, 0, true, false);
                g.drawImage(scaledRedDigits[COLON],
                    centeredX(scaledRedDigits[COLON].getWidth(null)), yOffset-digitH, this);
                yOffset += 12;
                str = ("SCORES");
                y = g.getFontMetrics().getHeight();                
                g.drawString(str, centeredX(g.getFontMetrics().stringWidth(str)), yOffset+y);
                yOffset += y+9;
                for(i = 0; i < session.getMaxAttempts(); i++) {
                    if(i == session.getRunNumber()-1) {
                        g.setColor(Color.WHITE);
                    } else if(i < session.getRunNumber()-1) {
                        g.setColor(new Color(0x20, 0xbb, 0xff));
                    } else {
                        g.setColor(new Color(55, 55, 55));
                    }
                    g.fillRect(15, yOffset, 8, smallH);
                    Score s = i < c.getSession().getActiveScoreList().size() ? c.getSession().getActiveScoreList().get(i) : null;
                    int score;
                    if(s != null) {
                        score = (int) (s.getScore()*100);
                        if(score > 99999 | score < 0) {
                            drawDashes(g);
                        } else {
                            if(score / 10000 != 0) {
                                g.drawImage(smallDigits[score / 10000 % 10], 28, yOffset, this);
                            } else {
                                g.drawImage(smallDigits[13], 28, yOffset, this);
                            } 
                            if((score / 10000 != 0) || score / 1000 % 10 != 0) {
                                g.drawImage(smallDigits[score / 1000 % 10], 28+smallW, yOffset, this);
                            } else {
                                g.drawImage(smallDigits[13], 28, yOffset, this);
                            }
                            g.drawImage(smallDigits[score / 100 % 10], 28+2*smallW, yOffset, this);
                            g.drawImage(smallDigits[PERIOD], 28+3*smallW, yOffset, this);
                            g.drawImage(smallDigits[score / 10 % 10], 28+3*smallW+smallDigits[11].getWidth(), yOffset, this);
                            g.drawImage(smallDigits[score % 10], 28+4*smallW+smallDigits[11].getWidth(), yOffset, this);
                        }
                    } else {
                        drawDashes(g);
                    }
                    yOffset += 8 + smallH;
                }
                drawStatusFlags(g, false, false, false, false);
                break;
        }
        renderTime = System.nanoTime() - startTime;
    }
    
    private void drawDashes(Graphics2D g) {
        g.drawImage(smallDigits[DASH], 28, yOffset, this);
        g.drawImage(smallDigits[DASH], 28+smallW, yOffset, this);
        g.drawImage(smallDigits[DASH], 28+2*smallW, yOffset, this);
        g.drawImage(smallDigits[DASH], 28+3*smallW+smallDigits[11].getWidth(), yOffset, this);
        g.drawImage(smallDigits[DASH], 28+4*smallW+smallDigits[11].getWidth(), yOffset, this);
    }
    
    private void drawDigits(Graphics2D g, int upper, int lower, boolean red, boolean leadingZero) {
        int[] index = new int[4];
        index[0] = upper / 10 % 10;
        index[1] = upper % 10;
        index[2] = lower / 10 % 10;
        index[3] = lower % 10;
        
        if(leadingZero || index[0] != 0) {
            g.drawImage(red ? scaledRedDigits[index[0]] : scaledBlueDigits[index[0]],
                    W(0.5)-2*digitW-10, yOffset, this);
        } else {
            g.drawImage(scaledRedDigits[13], W(0.5)-2*digitW-10, yOffset, this);
        }
        g.drawImage(red ? scaledRedDigits[index[1]] : scaledBlueDigits[index[1]],
                W(0.5)-digitW-10, yOffset, this);
        g.drawImage(red ? scaledRedDigits[index[2]] : scaledBlueDigits[index[2]],
                W(0.5)+10, yOffset, this);
        g.drawImage(red ? scaledRedDigits[index[3]] : scaledBlueDigits[index[3]],
                W(0.5)+10+digitW, yOffset, this);        
        
        yOffset += digitH;
    }
    
    private int W(double r) {
        return (int)(r*W);
    }
    
    private int H(double r) {
        return (int)(r*H);
    }
    
    private int centeredX(int width) {
        return (int)(0.5*W - 0.5*width);
    }
    
    private int rightJustify(int width, int margin) {
        return (int)(W-width-margin);
    }
    
    private void drawStatusFlags(Graphics2D g, boolean green, boolean greenBlink, boolean yellow, boolean red) {        
        g.setColor(c.getState() == CompetitionState.IDLE ? 
                Color.DARK_GRAY : new Color(0x20, 0xbb, 0xff));
        g.fillRect(5, H-5-45, W-10, 45);
        if(c.getState() == CompetitionState.SETUP || 
                c.getState() == CompetitionState.RUN) {
            if(c.redFlagged() && System.currentTimeMillis()/500%2==1) {
                g.setColor(new Color(80, 0, 0));
            } else if(c.getSession().isPaused() && System.currentTimeMillis()/500%2==1) {
                g.setColor(new Color(80, 80, 0));
            } else {
                g.setColor(Color.BLACK);                
            }
        } else {
            g.setColor(Color.BLACK);
        }
        
        g.fillRect(7, H-7-41, W-14, 41);
        g.setColor(green ? Color.GREEN : Color.DARK_GRAY);
        if(green && greenBlink) {
            g.setColor(System.currentTimeMillis()/500%2==1 ? Color.GREEN : Color.DARK_GRAY);
        }
        g.fillOval((int)(W/2.0-35/2.0-5-35), H-5-40, 35, 35);
        
        g.setColor(yellow && System.currentTimeMillis()/500%2==1 ? Color.YELLOW : Color.DARK_GRAY);
        g.fillOval((int)(W/2.0-35/2.0), H-5-40, 35, 35);
        
        g.setColor(red && System.currentTimeMillis()/500%2==1 ? Color.RED : Color.DARK_GRAY);
        g.fillOval((int)(W/2.0+35/2.0+5), H-5-40, 35, 35);
    }
}
