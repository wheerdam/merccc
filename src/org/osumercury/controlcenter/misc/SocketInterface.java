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

import java.awt.image.BufferedImage;
import org.osumercury.controlcenter.*;
import org.osumercury.controlcenter.gui.ControlFrame;
import org.osumercury.controlcenter.gui.UserEvent;
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
import javax.imageio.ImageIO;
import org.osumercury.controlcenter.gui.DisplayOverlay;

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
                                if(cc.getDisplayFrame().isVisible()) {
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
                disconnect();
            }
            clientHandlers.remove(this);
            Log.d(0, "SocketInterface$ClientHandler.run: exit");
        }
        
        private void handleCommand(String line) {
            int i, nr, px;
            File path;
            String[] tokens = line.trim().split("\\s+");
            DisplayOverlay overlay;
            if(tokens.length == 0) {
                return;
            }
            Log.d(1, "SocketInterface$ClientHandler.handleCommand: " + line);
            switch(tokens[0]) {
                case "current-directory":
                    send("OK " + System.getProperty("user.dir"));
                    break;
                case "change-directory":
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
                        }
                    }
                    break;
                case "add-overlay-file":
                    if(tokens.length == 8) {
                        try {
                            float xfloat = Float.parseFloat(tokens[2]);
                            float yfloat = Float.parseFloat(tokens[3]);
                            path = new File(tokens[4]);
                            Log.d(0, "loading '" + path.getCanonicalPath() + "'");
                            BufferedImage bImg = ImageIO.read(path.getCanonicalFile());
                            overlay = new DisplayOverlay(
                                        bImg, xfloat, yfloat,
                                        tokens[5].equals("yes"),
                                        tokens[6].equals("yes"),
                                        tokens[7].equals("yes")
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
                    // add-overlay name xfloat yfloat length-bytes logo runstate classification
                    if(tokens.length == 8) {
                        try {
                            float xfloat = Float.parseFloat(tokens[2]);
                            float yfloat = Float.parseFloat(tokens[3]);
                            int len = Integer.parseInt(tokens[4]);
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
                                    tokens[5].equals("yes"),
                                    tokens[6].equals("yes"),
                                    tokens[7].equals("yes")
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
                    if(tokens.length == 2) {
                        cc.getDisplayFrame().removeOverlay(tokens[1]);
                        send("OK");
                    } else {
                        send("ERROR");
                    }
                    break;
                case "set-overlay-visibility":
                    if(tokens.length == 3) {
                        cc.getDisplayFrame().setOverlayVisibility(tokens[1],
                                tokens[2].equals("yes"));
                        send("OK");
                    } else {
                        send("ERROR");
                    }
                    break;
                case "rescale-overlay-width":
                    if(tokens.length == 3) {
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
                    if(tokens.length == 3) {
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
                    if(tokens.length == 4) {
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
