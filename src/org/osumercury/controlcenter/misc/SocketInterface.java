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

import org.osumercury.controlcenter.*;
import org.osumercury.controlcenter.gui.ControlFrame;
import org.osumercury.controlcenter.gui.UserEvent;
import java.util.LinkedList;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;

/**
 *
 * @author wira
 */
public class SocketInterface extends Thread {
    private int port;
    private CompetitionState c;
    private ControlCenter cc;
    private ControlFrame f;
    private LinkedList<ClientHandler> clientHandlers;
    private ServerSocket ss;
    private boolean local;
    private boolean allowResourceCopy;
    
    public SocketInterface(int port, ControlCenter cc, ControlFrame f,
            boolean local, boolean allowResourceCopy) {
        this.port = port;
        this.cc = cc;
        this.c = cc.getCompetitionState();
        this.f = f;
        this.local = local;
        this.allowResourceCopy = allowResourceCopy;
        clientHandlers = new LinkedList();
        f.addScoreChangedHook((String key, int ID, String value) -> {
            broadcast("SCORE_CHANGE " + key + " " + ID + " " + value);
        });
        f.addUserEventHook((int ID, Object param) -> {
            Object[] p;
            SessionState s;
            Team t;
            switch(ID) {
                case UserEvent.STATE_CHANGE_IDLE:
                    broadcast("STATE_CHANGE_IDLE");
                    break;
                case UserEvent.STATE_CHANGE_SETUP:
                    p = (Object[]) param;
                    broadcast("STATE_CHANGE_SETUP " +
                            c.getSession().getActiveTeam().getNumber() + " " +
                            (Integer)p[0] + " " + (Integer)p[1] + " " +
                            (Integer)p[2]);
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
                    broadcast("SESSION_ATTEMPT_COMMITTED " + t.getNumber() + 
                            " " + s.getRunNumber() + " " +
                            s.getActiveScoreList().get(s.getRunNumber()-1).getScore());
                    break;
                case UserEvent.SESSION_ATTEMPT_DISCARDED:
                    s = c.getSession();
                    t = s.getActiveTeam();
                    broadcast("SESSION_ATTEMPT_DISCARDED " + t.getNumber() + 
                            " " + s.getRunNumber());
                    break;
                case UserEvent.SESSION_TIME_ADDED:
                    broadcast("SESSION_TIME_ADDED " + (Integer)param);
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
            String line;
            try {
                send("merccc-" + Text.getVersion());
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
                        }
                        sendPrompt();
                    }
                }
            } catch(Exception e) {      
                System.err.println("SocketInterface$ClientHandler.run: " +
                        "exception on receive");
                System.err.println("SocketInterface$ClientHandler.run: " +
                        e.toString());
                disconnect();
            }
            clientHandlers.remove(this);
            Log.d(0, "SocketInterface$ClientHandler.run: exit");
        }
        
        public void send(String str) {
            if(stop) {
                return;
            }
            
            try {
                w.println(str);
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
