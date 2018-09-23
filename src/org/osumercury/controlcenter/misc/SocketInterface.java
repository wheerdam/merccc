/*
    Copyright 2016-2018 Wira Mulia

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

import java.awt.image.BufferedImage;
import org.osumercury.controlcenter.*;
import org.osumercury.controlcenter.gui.ControlFrame;
import org.osumercury.controlcenter.gui.DisplayOverlay;
import java.util.LinkedList;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 *
 * @author wira
 */
public class SocketInterface extends Thread {
    private final int port;
    private CompetitionState c;
    private final ControlCenter cc;
    private final ControlFrame f;
    private final LinkedList<ClientHandler> clientHandlers;
    private ServerSocket ss;
    private final boolean local;
    private final boolean allowResourceCopy;
    private final boolean gui;
    
    public SocketInterface(int port, ControlCenter cc, ControlFrame f,
            boolean local, boolean allowResourceCopy) {
        this.port = port;
        this.cc = cc;
        this.c = cc.getCompetitionState();
        this.f = f;
        this.gui = f != null;
        this.local = local;
        this.allowResourceCopy = allowResourceCopy;
        clientHandlers = new LinkedList();
        ControlCenter.addScoreChangedHook((String key, int ID, String value) -> {
            broadcast("SCORE_CHANGE " + key + " " + ID + " " + value);
        });
        ControlCenter.addUserEventHook((int ID, Object param) -> {
            Object[] p;
            SessionState s;
            int run;
            Team t;
            switch(ID) {
                case UserEvent.STATE_CHANGE_IDLE:
                    broadcast("STATE_CHANGE_IDLE");
                    break;
                case UserEvent.STATE_CHANGE_SETUP:
                    p = (Object[]) param;
                    broadcast("STATE_CHANGE_SETUP " +
                            c.getSession().getActiveTeam().getNumber() + " " +
                            (Integer)p[0] + " " + (Long)p[1] + " " +
                            (Long)p[2]);
                    break;
                case UserEvent.STATE_CHANGE_RUN:
                    broadcast("STATE_CHANGE_RUN");
                    break;
                case UserEvent.STATE_CHANGE_POSTRUN:
                    broadcast("STATE_CHANGE_POSTRUN");
                    break;
                case UserEvent.SESSION_PAUSED:
                    broadcast("SESSION_PAUSED");
                    break;
                case UserEvent.SESSION_RESUMED:
                    broadcast("SESSION_RESUMED");
                    break;
                case UserEvent.SESSION_REDFLAGGED:
                    broadcast("SESSION_REDFLAGGED");
                    break;
                case UserEvent.SESSION_GREENFLAGGED:
                    broadcast("SESSION_GREENFLAGGED");
                    break;
                case UserEvent.SESSION_ATTEMPT_COMMITTED:
                    s = c.getSession();
                    t = s.getActiveTeam();
                    run = (Integer) param;
                    broadcast("SESSION_ATTEMPT_COMMITTED " + t.getNumber() + 
                            " " + run + " " +
                            s.getActiveScoreList().get(run-1).getScore());
                    break;
                case UserEvent.SESSION_ATTEMPT_DISCARDED:
                    s = c.getSession();
                    t = s.getActiveTeam();
                    run = (Integer) param;
                    broadcast("SESSION_ATTEMPT_DISCARDED " + t.getNumber() + 
                            " " + run);
                    break;
                case UserEvent.SESSION_TIME_ADDED:
                    broadcast("SESSION_TIME_ADDED " + (Long)param);
                    break;
                case UserEvent.DATA_CLEARED:
                    broadcast("DATA_CLEARED");
                    break;
                case UserEvent.DATA_IMPORTED:
                    broadcast("DATA_IMPORTED");
                    break;
                case UserEvent.DATA_RECORD_EXPUNGED:
                    p = (Object[]) param;
                    broadcast("DATA_RECORD_EXPUNGED " + (Integer)p[0] + " " 
                            + (Integer)p[1]);
                    break;
                case UserEvent.DATA_CHANGED:
                    p = (Object[]) param;
                    broadcast("DATA_CHANGED " + (Integer)p[0] + " " 
                            + (Integer)p[1] + " " + p[2] + " " + (Double)p[3]);
                    break;
                case UserEvent.DATA_ADDED:
                    p = (Object[]) param;
                    broadcast("DATA_ADDED " + (Integer)p[0] + " " +
                            (Integer)p[1]);
                    break;
                case UserEvent.TEAM_PRE_SELECT:
                    broadcast("TEAM_PRE_SELECT " + (Integer)param);
                    break;
                case UserEvent.DISPLAY_MODE_CHANGE:
                    broadcast("DISPLAY_MODE_CHANGE " + (Integer)param);
                    break;
                case UserEvent.DISPLAY_HIDE:
                    broadcast("DISPLAY_HIDE");
                    break;
                case UserEvent.DISPLAY_SHOW:
                    broadcast("DISPLAY_SHOW");
                    break;
                case UserEvent.DISPLAY_RANK_START:
                    broadcast("DISPLAY_RANK_START " + (Integer)param);
                    break;
                case UserEvent.TEAM_ADDED_ANNOTATION:
                    p = (Object[]) param;
                    broadcast("TEAM_ADDED_ANNOTATION " + (Integer)p[0] + " " +
                              (String)p[1]);
                    break;
                case UserEvent.TEAM_REMOVED_ANNOTATION:
                    p = (Object[]) param;
                    broadcast("TEAM_REMOVED_ANNOTATION " + (Integer)p[0] + " " +
                              (String)p[1]);
                    break;
                case UserEvent.TEAM_CLEARED_ANNOTATION:
                    broadcast("TEAM_CLEARED_ANNOTATION " + (Integer)param);
                    break;
            }
        });
    }
    
    @Override
    public void run() {
        if(ss != null) {
            System.err.println("SocketInterface.run: " +
                    "already listening");
            return;
        }
        
        try {
            if(!local) {
                ss = new ServerSocket(port);
            } else {
                ss = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
            }
            Log.d(0, "SocketInterface.run: listening for a connection" +
                    " on port " + port);            
            while(true) {
                Socket s = ss.accept();
                Log.d(0, "SocketInterface.run: new connection from " + 
                        s.getInetAddress().getHostAddress());
                ClientHandler client = new ClientHandler(s);
                clientHandlers.add(client);
                client.start();
            }
        } catch(Exception e) {
            System.err.println("SocketInterface.run: " +
                    "exception on server socket listen");
            System.err.println("SocketInterface.run: " +
                    e.toString());
        }
        
        ss = null;
        for(ClientHandler client : clientHandlers) {
            client.disconnect();
        }
        clientHandlers.clear();
        Log.d(0, "SocketInterface.run: exit");
    }
    
    public void close() {
        if(ss != null) {
            try {
                ss.close();
            } catch(Exception e) {      
                System.err.println("SocketInterface.close: " +
                        "exception on server socket close");
                System.err.println("SocketInterface.close: " +
                        e.toString());
            }
            ss = null;
        }
        for(ClientHandler client : clientHandlers) {
            client.disconnect();
        }
    }
    
    public void broadcast(String str) {
        for(ClientHandler client : clientHandlers) {
            client.monitor(str);
        }
    }
    
    class ClientHandler extends Thread {
        private Socket s;
        private PrintWriter w;
        private BufferedReader r;
        private boolean stop = false;
        private boolean monitor = false;
        private boolean prompt = true;
        private SessionTimer timer;
        
        public ClientHandler(Socket s) {
            this.s = s;            
        }
        
        @Override
        public void run() {
            Log.d(0, "SocketInterface$ClientHandler.run: start, IP: " + 
                    s.getInetAddress().getHostAddress());
            try {
                w = new PrintWriter(s.getOutputStream());
                r = new BufferedReader(new InputStreamReader(s.getInputStream()));
            } catch(IOException ioe) {
                System.err.println("SocketInterface$ClientHandler.run: unable to " +
                        "open I/O streams");
                System.err.println("SocketInterface$ClientHandler.run: " +
                        ioe.toString());
                disconnect();
            }
            if(!gui && local) {
                c.addStateChangeHook((state) -> {
                    if(c.getState() != CompetitionState.RUN) {
                        return;
                    }
                    c.getSession().endSetup();
                });
            }
            String line;
            try {
                String header = "merccc-" + Text.getVersion();
                header += local ? " local" : "";
                send(header);
                sendPrompt();
                while(!stop && (line = r.readLine()) != null) {
                    line = line.trim();
                    if(line.equals("monitor") && !monitor) {
                        send("MONITOR");
                        monitor = true;
                    } else if(line.equals("break") && monitor) {
                        send("COMMAND");
                        monitor = false;
                        sendPrompt();
                    } else if(!monitor) {
                        switch(line) {
                            case "q":
                                disconnect();
                                break;
                            case "data":
                                for(String[] dataLines : Data.getData(c)) {
                                    StringBuilder str = new StringBuilder();
                                    str.append("DATA ");
                                    for(String s : dataLines) {
                                        str.append(s);
                                        str.append(", ");
                                    }
                                    str.delete(str.length()-2, str.length());
                                    send(str.toString());
                                }
                                send("DONE");
                                break;
                            case "header":
                                for(String s : Data.getColHeaders()) {
                                    send("HEADER " + s);
                                }
                                send("DONE");
                                break;
                            case "teams":
                                for(Team t : c.getTeams()) {
                                    send("TEAM " + t.getNumber() + ", " +
                                            t.getName() + ", " +
                                            t.getInstitution() + ", " +
                                            t.getLogoFileName() + ", " +
                                            (t.hasScore() ? t.getBestScore().getScore() : "DNF"));
                                }
                                send("DONE");
                                break;
                            case "annotations": 
                                for(Team t : c.getTeams()) {
                                    List<String> annotations = t.getAnnotations();
                                    if(!annotations.isEmpty()) {
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("ANNOTATION ");
                                        sb.append(String.valueOf(t.getNumber()));
                                        sb.append(" ");
                                        for(String a : annotations) {
                                            sb.append(a);
                                            sb.append(",");
                                        }
                                        String response = sb.toString();
                                        send(response.substring(0, response.length()-1));
                                    }
                                }
                                send("DONE");
                                break;
                            case "config":
                                send(Config.getConfigString());
                                send("DONE");
                                break;
                            case "state":
                                send("STATE " + c.getState() + 
                                        ((c.getState() >= 0) ?
                                        " " + c.getSession().getActiveTeam().getNumber() + " " +
                                        c.getSession().getRunNumber() + " " +
                                        c.getSession().getRemainingTimeMilliseconds() + " " +
                                        c.getSession().isPaused() + " " + 
                                        c.redFlagged() : ""));
                                break;
                            case "classification":
                                for(String[] entry : Data.getResultsData(c)) {
                                    StringBuilder str = new StringBuilder();
                                    str.append("CLASSIFICATION ");
                                    for(String s : entry) {
                                        str.append(s);
                                        str.append(", ");
                                    }
                                    str.delete(str.length()-2, str.length());
                                    send(str.toString());
                                }
                                send("DONE");
                                break;
                            case "hash":
                                send("HASH " + String.valueOf(
                                        Config.getConfigString().hashCode()));
                                break;
                            case "resolution":
                                if(gui && cc.getDisplayFrame().isVisible()) {
                                    send("RESOLUTION " + 
                                            String.valueOf(cc.getDisplayFrame().getCanvas().getWidth()) + "x" +
                                            String.valueOf(cc.getDisplayFrame().getCanvas().getHeight())
                                            );
                                } else {
                                    send("RESOLUTION -1x-1");
                                }
                                break;
                            case "promptoff":
                                prompt = false;
                                send("PROMPT OFF");
                                break;
                            case "resources":
                                if(allowResourceCopy) {
                                    Log.d(0, "resource request");
                                    Sock.put(s, cc.getResourcePath().getAbsolutePath(), null);
                                } else {
                                    Log.d(0, "resource request is not allowed");
                                    Sock.send(s, "-1");
                                }
                                break;
                            default:
                                if(local) {
                                    handleCommand(line);
                                } else {
                                    send("ERROR");
                                }
                        }
                        sendPrompt();
                    }
                }
            } catch(Exception e) {      
                System.err.println("SocketInterface$ClientHandler.run: " +
                        "exception on receive");
                System.err.println("SocketInterface$ClientHandler.run: " +
                        e.toString());
                if(Log.debugLevel >= 3) {
                    e.printStackTrace();
                }
                disconnect();
            }
            clientHandlers.remove(this);
            Log.d(0, "SocketInterface$ClientHandler.run: exit");
        }
        
        private void handleCommand(String line) {
            int i, nr, px;
            int teamIndex, scoreIndex;
            Team t;
            File path;
            Score score;
            ControlFrame cf = cc.getControlFrame();
            String[] tokens = line.trim().split("\\s+");
            DisplayOverlay overlay;
            SessionState session;
            if(tokens.length == 0) {
                return;
            }
            Log.d(1, "SocketInterface$ClientHandler.handleCommand: " + line);
            switch(tokens[0]) {
                case "add-score":
                    if(tokens.length == (2 + Score.getFields().size())) {
                        try {
                            score = new Score();
                            List<String> fields = Config.getKeysInOriginalOrder("fields");
                            i = 2;
                            for(String field : fields) {
                                score.setValue(field, Double.parseDouble(tokens[i]));
                                i++;
                            }
                            score.setCompleted(true);
                            Data.lock().writeLock().lock();
                            t = c.getTeamByID(Integer.parseInt(tokens[1]));
                            t.addScore(score);
                            c.sort();
                            if(gui && cf != null) {
                                cf.refreshDataView();
                            }
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        } finally {
                            Data.lock().writeLock().unlock();
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "delete-score":
                    if(tokens.length == 3) {
                        try {
                            teamIndex = Integer.parseInt(tokens[1]);
                            scoreIndex = Integer.parseInt(tokens[2]);
                            Data.removeScore(c, teamIndex, scoreIndex);
                            c.sort();
                            if(gui && cf != null) {
                                cf.refreshDataView();
                            }
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "clear-data":
                    try {
                        Data.clearData(c);
                        if(gui && cf != null) {
                            cf.refreshDataView();
                        }
                        send("OK");
                    } catch(Exception e) {
                        Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                 e);
                        send("ERROR");
                    }
                    break;
                case "save-data":
                    tokens = line.trim().split("\\s+", 2);
                    if(tokens.length == 2) {
                        try {
                            path = new File(tokens[1]);
                            Data.saveAsCSV(c, path.getCanonicalPath());
                            if(gui && cf != null) {
                                cf.refreshDataView();
                            }
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR " + e);
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "load-data":
                    tokens = line.trim().split("\\s+", 2);
                    if(tokens.length == 2) {
                        try {
                            path = new File(tokens[1]);
                            Data.loadCSV(c, path.getCanonicalPath());
                            c.sort();
                            if(gui && cf != null) {
                                cf.refreshDataView();
                            }
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR " + e);
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "change-active-score-field":
                    if(tokens.length == 3) {
                        try {
                            if(c.getState() != CompetitionState.RUN && 
                                    c.getState() != CompetitionState.POST_RUN) {
                                send("ERROR not in RUN nor POST-RUN state");
                            } else {
                                if(Score.getFields().containsKey(tokens[1])) {
                                    double value = Double.parseDouble(tokens[2]);
                                    if(gui && cf != null) {
                                        cf.setCurrentScore(tokens[1], value);
                                    } else {
                                        ControlCenter.triggerScoreChangeEvent(tokens[1],
                                            Score.getFieldID(tokens[1]), 
                                            tokens[2]);
                                    }
                                    session = cc.getCompetitionState().getSession();
                                    session.modifyCurrentScore(tokens[1], value);
                                    send("OK");
                                } else {
                                    send("ERROR invalid score field key");
                                }
                            }
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR " + e);
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "get-current-score":
                    try {
                        if(c.getState() != CompetitionState.RUN && 
                                c.getState() != CompetitionState.POST_RUN) {
                            send("ERROR not in RUN nor POST-RUN state");
                        } else {
                            String ret = "CURRENT RUN=" + c.getSession().getRunNumber() + " ";
                            for(String key : Config.getKeysInOriginalOrder("fields")) {
                                ret += key + "=";
                                ret += c.getSession().getCurrentScoreValue(key) + " ";
                            }
                            send(ret);
                        }
                    } catch(Exception e) {
                        Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                    e);
                        send("ERROR " + e);
                    }
                    break;
                case "start-scoring-session":
                    if(tokens.length == 5) {
                        try {
                            if(c.getState() != CompetitionState.IDLE) {
                                send("ERROR competition is not in IDLE state");
                            } else {
                                int teamID = Integer.parseInt(tokens[1]);
                                t = c.getTeamByID(teamID);
                                if(t == null) {
                                    send("ERROR invalid team ID");
                                    break;
                                }
                                int attempts = Integer.parseInt(tokens[2]);
                                int setupWindow = Integer.parseInt(tokens[3]);
                                int runWindow = Integer.parseInt(tokens[4]);
                                if(gui && cf != null) {
                                    cf.setRunParameters(attempts, 
                                                        setupWindow/1000, 
                                                        runWindow/1000);
                                    cf.setSelectedTeamID(teamID);
                                } else {
                                    c.newSession(t, attempts, setupWindow, runWindow);
                                    if(timer != null) {
                                        timer.stopTimer();
                                    }
                                    timer = new SessionTimer(c);
                                    c.getSession().start();
                                    timer.start();
                                }
                                c.setState(CompetitionState.SETUP);
                                send("OK");
                            }
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR " + e);
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "end-scoring-session":
                    if(c.getState() == CompetitionState.IDLE) {
                        send("ERROR competition state is IDLE");
                    } else {
                        if(!gui && timer != null) {
                            timer.stopTimer();
                        }
                        if(c.redFlagged()) {
                            c.setRedFlag(false);
                        }
                        c.setState(CompetitionState.IDLE);
                        send("OK");
                    }
                    break;
                case "skip-setup":
                    if(c.getState() == CompetitionState.SETUP) {
                        c.setState(CompetitionState.RUN);
                        send("OK");
                    } else {
                        send("ERROR not in SETUP state");
                    }
                    break;
                case "pause":
                    if(c.getState() == CompetitionState.SETUP ||
                            c.getState() == CompetitionState.RUN) {
                        c.getSession().pauseTimer();
                        send("OK");
                    } else {
                        send("ERROR not in SETUP nor RUN state");
                    }
                    break;
                case "resume":
                    if(c.getState() == CompetitionState.SETUP ||
                            c.getState() == CompetitionState.RUN) {
                        c.getSession().resumeTimer();
                        send("OK");
                    } else {
                        send("ERROR not in SETUP nor RUN state");
                    }
                    break;
                case "redflag":
                    if(c.getState() == CompetitionState.SETUP ||
                            c.getState() == CompetitionState.RUN) {
                        c.setRedFlag(true);
                        send("OK");
                    } else {
                        send("ERROR not in SETUP nor RUN state");
                    }
                    break;
                case "greenflag":
                    if(c.getState() == CompetitionState.SETUP ||
                            c.getState() == CompetitionState.RUN) {
                        c.setRedFlag(false);
                        send("OK");
                    } else {
                        send("ERROR not in SETUP nor RUN state");
                    }
                    break;
                case "commit-score":
                    session = c.getSession();
                    if(c.getState() < CompetitionState.RUN) {
                        send("ERROR must be in RUN or POST-RUN state");
                    } else if(session.getRunNumber() <= session.getMaxAttempts()) {
                        if(gui && cf != null) {
                            SwingUtilities.invokeLater(() -> {
                                cf.commitScore();
                            });
                        } else {
                            session.completeRun(true);
                        }
                        if(session.isFinished()) {
                            c.setState(CompetitionState.POST_RUN);
                        }
                        send("OK");
                    } else {
                        send("ERROR already have max number of attempts");
                    }
                    break;
                case "discard-score":
                    session = c.getSession();
                    if(c.getState() < CompetitionState.RUN) {
                        send("ERROR must be in RUN or POST-RUN state");
                    } else if(session.getRunNumber() <= session.getMaxAttempts()) {
                        if(gui && cf != null) {
                            SwingUtilities.invokeLater(() -> {
                                cf.discardScore();
                            });
                        } else {
                            session.completeRun(false);
                        }
                        if(session.isFinished()) {
                            c.setState(CompetitionState.POST_RUN);
                        }
                        send("OK");
                    } else {
                        send("ERROR already have max number of attempts");
                    }
                    break;
                case "add-time":
                    if(tokens.length == 2) {
                        try {
                            if(c.getState() == CompetitionState.SETUP ||
                                    c.getState() == CompetitionState.RUN) {
                                c.getSession().addTimeSeconds(Long.parseLong(tokens[1]));
                                send("OK");
                            } else {
                                send("ERROR not in SETUP nor RUN mode");
                            }
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "trigger-display-change":
                    if(tokens.length == 2) {
                        try {
                            int displayMode = Integer.parseInt(tokens[1]);
                            ControlCenter.triggerEvent(UserEvent.DISPLAY_MODE_CHANGE, displayMode);
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "trigger-display-rank-start":
                    if(tokens.length == 2) {
                        try {
                            int displayRank = Integer.parseInt(tokens[1]);
                            ControlCenter.triggerEvent(UserEvent.DISPLAY_RANK_START, displayRank);
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "current-directory":
                    send("OK " + System.getProperty("user.dir"));
                    break;
                case "change-directory":
                    tokens = line.trim().split("\\s+", 2);
                    if(tokens.length == 2) {
                        try {
                            path = (new File(tokens[1])).getCanonicalFile();
                            if(!path.exists() || !path.isDirectory()) {
                                send("ERROR invalid directory");
                            } else {
                                String pathString = path.getCanonicalPath();
                                System.setProperty("user.dir", pathString);
                                send("OK");
                            }
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "add-team-annotation":
                    tokens = line.trim().split("\\s+", 3);
                    if(tokens.length == 3) {
                        Data.lock().writeLock().lock();
                        try {
                            int teamID = Integer.parseInt(tokens[1]);
                            c.getTeamByID(teamID).addAnnotation(tokens[2]);
                            if(gui && cf != null) {
                                cf.refreshDataView();
                            }
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        } finally {
                            Data.lock().writeLock().unlock();
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "remove-team-annotation":
                    tokens = line.trim().split("\\s+", 3);
                    if(tokens.length == 3) {
                        Data.lock().writeLock().lock();
                        try {
                            int teamID = Integer.parseInt(tokens[1]);
                            c.getTeamByID(teamID).removeAnnotation(tokens[2]);
                            if(gui && cf != null) {
                                cf.refreshDataView();
                            }
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        } finally {
                            Data.lock().writeLock().unlock();
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "add-overlay-file":
                    tokens = line.trim().split("\\s+", 8);
                    if(gui && tokens.length == 8) {
                        try {
                            float xfloat = Float.parseFloat(tokens[2]);
                            float yfloat = Float.parseFloat(tokens[3]);
                            path = new File(tokens[7]);
                            Log.d(0, "loading '" + path.getCanonicalPath() + "'");
                            BufferedImage bImg = ImageIO.read(path.getCanonicalFile());
                            overlay = new DisplayOverlay(
                                        bImg, xfloat, yfloat,
                                        tokens[4].equals("yes"),
                                        tokens[5].equals("yes"),
                                        tokens[6].equals("yes")
                                );
                            cc.getDisplayFrame().addOverlay(tokens[1], overlay);
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "add-overlay":
                    // overlay command format:
                    // add-overlay name xfloat yfloat logo runstate classification length-bytes
                    if(gui && tokens.length == 8) {
                        try {
                            float xfloat = Float.parseFloat(tokens[2]);
                            float yfloat = Float.parseFloat(tokens[3]);
                            int len = Integer.parseInt(tokens[7]);
                            if(len > 64 * 1024 * 1024) {
                                // image is too big
                                return;
                            }
                            byte[] recvBuf = new byte[4096];
                            byte[] img = new byte[len];
                            i = 0;
                            send("READY");
                            while((nr = s.getInputStream().read(recvBuf)) != -1) {
                                System.arraycopy(recvBuf, 0, img, i, nr);
                                i += nr;
                                Log.d(3, i + " total bytes read");
                                if(i == len) {
                                    break;
                                }
                            }
                            InputStream imgStream = new ByteArrayInputStream(img);
                            BufferedImage bImg = ImageIO.read(imgStream);
                            overlay = new DisplayOverlay(
                                    bImg, xfloat, yfloat,
                                    tokens[4].equals("yes"),
                                    tokens[5].equals("yes"),
                                    tokens[6].equals("yes")
                            );
                            cc.getDisplayFrame().addOverlay(tokens[1], overlay);
                            send("OK");
                        } catch(Exception e) {
                            Log.d(0, "SocketInterface$ClientHandler.handleCommand: " +
                                     e);
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "remove-overlay":
                    if(gui && tokens.length == 2) {
                        cc.getDisplayFrame().removeOverlay(tokens[1]);
                        send("OK");
                    } else {
                        send("ERROR");
                    }
                    break;
                case "set-overlay-visibility":
                    if(gui && tokens.length == 3) {
                        cc.getDisplayFrame().setOverlayVisibility(tokens[1],
                                tokens[2].equals("yes"));
                        send("OK");
                    } else {
                        send("ERROR");
                    }
                    break;
                case "rescale-overlay-width":
                    if(gui && tokens.length == 3) {
                        px = (int) (Float.parseFloat(tokens[2]) * 
                                cc.getDisplayFrame().getCanvas().getWidth());
                        overlay = cc.getDisplayFrame().getOverlayHandle(tokens[1]);
                        if(overlay != null) {
                            overlay.rescaleWidth(px);
                            send("OK");
                        } else {
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "rescale-overlay-height":
                    if(gui && tokens.length == 3) {
                        px = (int) (Float.parseFloat(tokens[2]) * 
                                cc.getDisplayFrame().getCanvas().getHeight());
                        overlay = cc.getDisplayFrame().getOverlayHandle(tokens[1]);
                        if(overlay != null) {
                            overlay.rescaleHeight(px);
                            send("OK");
                        } else {
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                case "reposition-overlay":
                    if(gui && tokens.length == 4) {
                        overlay = cc.getDisplayFrame().getOverlayHandle(tokens[1]);
                        if(overlay != null) {
                            overlay.reposition(
                                    Float.parseFloat(tokens[2]),
                                    Float.parseFloat(tokens[3])
                            );
                            send("OK");
                        } else {
                            send("ERROR");
                        }
                    } else {
                        send("ERROR");
                    }
                    break;
                default:
                    send("ERROR");
            }
        }
        
        public void send(String str) {
            if(stop) {
                return;
            }
            
            try {
                w.print(str + "\n");
                w.flush();
            } catch(Exception e) {      
                System.err.println("SocketInterface$ClientHandler.send: " +
                        "Exception on send");
                System.err.println("SocketInterface$ClientHandler.send: " +
                        e.toString());
                disconnect();
            }
        }
        
        public void sendPrompt() {
            if(stop || !prompt) {
                return;
            }
            
            try {
                w.print("> ");
                w.flush();
            } catch(Exception e) {      
                System.err.println("SocketInterface$ClientHandler.send: " +
                        "Exception on send");
                System.err.println("SocketInterface$ClientHandler.send: " +
                        e.toString());
                disconnect();
            }
        }
        
        public void monitor(String str) {
            if(monitor) {
                send(str);
            }
        }
        
        public void disconnect() {
            try {
                s.close();
            } catch(IOException ioe) {
                
            }
            stop = true;
        }
    }
}
