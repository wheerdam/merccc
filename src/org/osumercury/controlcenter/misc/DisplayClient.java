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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.osumercury.controlcenter.*;
import org.osumercury.controlcenter.gui.DisplayFrame;
import org.osumercury.controlcenter.gui.Assets;

/**
 *
 * @author wira
 */
public class DisplayClient {
    private static PrintWriter w;
    private static BufferedReader r;
    private static Socket s;
    private static File fetchedResources;
    
    public static String getConfigString(String host, int port, boolean copyResources) {
        StringBuilder str = new StringBuilder();
        try {
            Log.d(0, "- fetching config from " + host + ":" + port);
            String d;
            s = new Socket(host, port);
            r = new BufferedReader(new InputStreamReader(
                    s.getInputStream()
            ));
            w = new PrintWriter(s.getOutputStream());
            Log.d(1, r.readLine()); // shed header
            send("promptoff");
            r.readLine(); // shed prompt off response
            send("config");
            while(!(d = r.readLine()).equals("DONE")) {
                Log.d(1, d);
                str.append(d);
                str.append("\n");
            }
            if(copyResources) {
                Log.d(0, "- fetching resources");
                JOptionPane.showMessageDialog(null, "Choose destination directory " +
                        "for downloaded resources", "Resource directory",
                        JOptionPane.INFORMATION_MESSAGE);
                File f;
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.showOpenDialog(null);
                if(fileChooser.getSelectedFile() != null) {
                    f = new File(fileChooser.getSelectedFile().getAbsolutePath());
                    if(!f.exists()) {
                        f.mkdir();
                    }
                    if(f.exists()) {
                        w.println("resources");
                        w.flush();
                        Sock.get(s, f.getAbsolutePath(), null);
                        fetchedResources = f;
                    }
                } else {
                    Log.d(0, "- failed to download resources");
                }
            }
            s.close();            
            return str.toString();
        } catch(IOException ioe) {
            System.err.println("failed to fetch config: " + ioe);
            return null;
        }
    }
    
    public static void connect(ControlCenter cc, 
            String host, int port, boolean fetchConfig,
            int lockMode) {
        CompetitionState c = cc.getCompetitionState();
        DisplayFrame display = cc.getDisplayFrame();        
        SoundPlayer.setEnabled(false);
        
        try {
            String d;
            Log.d(0, "- connecting to " + host + ":" + port);
            s = new Socket(host, port);
            r = new BufferedReader(new InputStreamReader(
                    s.getInputStream()
            ));
            w = new PrintWriter(s.getOutputStream());
            Log.d(0, "- connection header: " + r.readLine());
            send("promptoff");
            r.readLine(); // shed prompt off response
            
            send("state");
            int state = Integer.parseInt(r.readLine().split(" ")[1]);
            if(state > -1) {
                Log.fatal(104, "Server is not idle. Can only connect to an " +
                        "idle server");
            }
            
            // check if config hashes match
            if(!fetchConfig) {
                send("hash");
                int serverHash = Integer.parseInt(r.readLine().split(" ")[1]);
                int localHash = Config.getConfigString().hashCode();
                Log.d(0, "- server config hash: " + serverHash + " local hash: " +
                        localHash);
                if(serverHash != localHash) {
                    Log.fatal(102, "configuration hashes do not match");
                }                
            } else if(fetchedResources != null) {
                Assets.load(fetchedResources.getAbsolutePath()
                        + "/" + Config.getValue("system", "resourcedir"));
            }
            
            // get current scoring state from server
            getScoreData(cc, false);
            
            // setup display
            int displayNumber = 0;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            int numScreens = screens.length;
            Log.d(0, "- active displays:");
            for(int i = 0; i < screens.length; i++) {
                Log.d(0, i + ": " + screens[i].getIDstring() + " (" +
                         screens[i].getDisplayMode().getWidth() + "x" +
                         screens[i].getDisplayMode().getHeight() + ")");
            }
            Log.di(0, "- select display to output to (type in the index and press enter): ");
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                displayNumber = Integer.parseInt(r.readLine());
            } catch(Exception e) {
                Log.err("failed to parse input");
                ControlCenter.exit(1);
            }
            if(displayNumber < 0 || displayNumber >= numScreens) {
                Log.fatal(103, "display " + displayNumber + " not found");
            }
            GraphicsDevice gd = ge.getScreenDevices()[displayNumber];
            SwingUtilities.invokeLater(() -> {
                display.init();            
                display.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                display.setVisible(false);       
                display.setLocation(gd.getDefaultConfiguration().getBounds().x,
                        gd.getDefaultConfiguration().getBounds().y);
                display.setExtendedState(JFrame.MAXIMIZED_BOTH);
                if(!display.isDisplayable()) {
                    display.setUndecorated(true);
                }
                display.setVisible(true);
                cc.getRefreshThread().start();
                display.setClassificationData(c.getSortedFinishedTeams());
                if(lockMode >= 0 && lockMode <= 2) {
                    display.setMode(lockMode);
                }
            });
            
            Log.d(0, "- going into monitor mode");
            send("monitor");
            // flush response
            r.readLine();
            Score score = new Score();
            while((d = r.readLine()) != null) {
                Log.d(0, d);
                String[] tokens = d.split(" ", 2);
                switch(tokens[0]) {
                    case "DISPLAY_MODE_CHANGE":
                        if(lockMode < 0 || lockMode > 2) {
                            display.setMode(Integer.parseInt(tokens[1]));
                        }
                        break;
                    case "DISPLAY_RANK_START":
                        display.setRankStart(Integer.parseInt(tokens[1]));
                        break;
                    case "TEAM_PRE_SELECT":
                        display.setNextTeamID(Integer.parseInt(tokens[1]));
                        break;
                    case "STATE_CHANGE_SETUP":
                        tokens = tokens[1].split(" ");
                        int teamID = Integer.parseInt(tokens[0]);
                        int attempts = Integer.parseInt(tokens[1]);
                        int setup = Integer.parseInt(tokens[2])*1000;
                        int run = Integer.parseInt(tokens[3])*1000;
                        Team t = c.getTeamByID(teamID);
                        c.setState(CompetitionState.SETUP);
                        c.newSession(t, attempts, setup, run);
                        c.getSession().start();
                        break;
                    case "STATE_CHANGE_RUN":
                        c.setState(CompetitionState.RUN);
                        c.getSession().endSetup();
                        score = new Score();
                        display.newScore();
                        break;
                    case "SCORE_CHANGE":
                        tokens = tokens[1].split(" ");
                        score.setValue(tokens[0], Double.parseDouble(tokens[2]));
                        display.setScore(tokens[0],
                                Integer.parseInt(tokens[1]), 
                                Double.parseDouble(tokens[2]));
                        break;
                    case "SESSION_PAUSED":
                        c.getSession().pauseTimer();
                        break;
                    case "SESSION_RESUMED":
                        c.getSession().resumeTimer();
                        break;
                    case "SESSION_REDFLAGGED":
                        c.setRedFlag(true);
                        break;
                    case "SESSION_GREENFLAGGED":
                        c.setRedFlag(false);
                        break;
                    case "SESSION_TIME_ADDED":
                        c.getSession().addTimeSeconds(Long.parseLong(tokens[1]));
                        break;
                    case "SESSION_ATTEMPT_COMMITTED":
                        score.setCompleted(true);
                        c.getSession().getActiveTeam().addScore(score);
                        c.sort();
                        display.setClassificationData(c.getSortedFinishedTeams());
                        c.getSession().advance();
                        score = new Score();
                        display.newScore();
                        break;
                    case "SESSION_ATTEMPT_DISCARDED":
                        c.getSession().advance();
                        score = new Score();
                        display.newScore();
                        break;
                    case "STATE_CHANGE_POSTRUN":
                        c.setState(CompetitionState.POST_RUN);
                        break;
                    case "STATE_CHANGE_IDLE":
                        c.setRedFlag(false);
                        c.setState(CompetitionState.IDLE);
                        break;
                    case "DATA_CHANGED":
                    case "DATA_ADDED":
                    case "DATA_CLEARED":
                    case "DATA_IMPORTED":
                    case "DATA_RECORD_EXPUNGED":
                        getScoreData(cc, true);
                        break;
                }
            }
            cc.getRefreshThread().stopThread();
            cc.getDisplayFrame().dispose();
            s.close();
            Log.d(0, "- exiting");
        } catch(IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        } catch(NumberFormatException nfe) {
            System.err.println("Unable to parse: " + nfe.getMessage());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void getScoreData(ControlCenter cc, boolean monitoring)
            throws IOException {
        if(monitoring) {
            send("break");
            // flush response
            r.readLine(); 
        }
        String d;
        CompetitionState c = cc.getCompetitionState();
        int num = 0;
        for(Team t : c.getTeams()) {
            t.getScores().clear();
        }        
        Log.d(0, "- getting current data");            
        send("data");
        while(!(d = r.readLine()).equals("DONE")) {
            Log.d(1, d);
            Data.parseCSVLine(c, d.split(" ", 2)[1]);
            num++;
        }
        Log.d(0, "- " + num + " records parsed");
        c.sort();
        cc.getDisplayFrame().setClassificationData(c.getSortedFinishedTeams());
        
        if(monitoring) {
            send("monitor");
            // flush response
            r.readLine(); 
        }
    }    
    
    private static void send(String str) {
        w.write(str + "\n");
        w.flush();
    }
}
