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
package org.osumercury.controlcenter.misc;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.osumercury.controlcenter.*;
import org.osumercury.controlcenter.gui.DisplayFrame;
import org.osumercury.controlcenter.gui.Assets;
import org.osumercury.controlcenter.gui.DisplayOptionsFrame;

/**
 *
 * @author wira
 */
public class DisplayClient {
    private static PrintWriter w;
    private static BufferedReader r;
    private static Socket s;
    private static File fetchedResources;
    private static DisplayOptionsFrame displayOptions;
    
    public static Map clientConnectWindow(String hostAddress) {
        Map<String, String> fields = new HashMap<>();
        JDialog dialog = new JDialog();
        dialog.setTitle("merccc " + Text.getVersion() + " Client Mode Connection");
        dialog.setModal(true);
        Container dialogPane = dialog.getContentPane();
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        JLabel lblAddress = new JLabel("Server (host:port):");
        JTextField txtAddress = new JTextField(hostAddress != null ? hostAddress : "");
        JLabel lblDisplay = new JLabel("Output Display:");
        JComboBox<String> cmbDisplay = new JComboBox<>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        for(int i = 0; i < screens.length; i++) {
            cmbDisplay.addItem(screens[i].getIDstring() + " (" +
                     screens[i].getDisplayMode().getWidth() + "x" +
                     screens[i].getDisplayMode().getHeight() + ")");
        }
        if(cmbDisplay.getItemCount() > 0) {
            cmbDisplay.setSelectedIndex(0);
        } else {
            Log.fatal(105, "No displays found");
        }
        JLabel lblLockMode = new JLabel("Lock Output Mode:");
        JComboBox<String> cmbLockMode = new JComboBox<>();
        cmbLockMode.addItem("Mirror the server output mode");
        cmbLockMode.addItem("Logo and Time");
        cmbLockMode.addItem("Run Status");
        cmbLockMode.addItem("Classification");
        cmbLockMode.setSelectedIndex(0);
        JLabel lblConfig = new JLabel("Configuration:");
        JComboBox<String> cmbConfig = new JComboBox<>();
        cmbConfig.addItem("Fetch from server");
        cmbConfig.addItem("Browse...");
        cmbConfig.setSelectedIndex(0);
        cmbConfig.addActionListener((e) -> {
            if(cmbConfig.getSelectedIndex() == 1) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new java.io.File("."));
                fc.setDialogTitle("Open configuration file");
                if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String selectedFile = fc.getSelectedFile().toString();
                    if(cmbConfig.getItemCount() == 2) {
                        cmbConfig.addItem(selectedFile);
                    } else {
                        cmbConfig.removeItemAt(2);
                        cmbConfig.addItem(selectedFile);
                    }
                    cmbConfig.setSelectedIndex(2);
                }
            }
        });
        JCheckBox chkCopyResources = new JCheckBox("Copy resources from server (if enabled)");
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        
        c.gridy = 0;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.35;
        pane.add(lblAddress, c);
        c.gridx = 1;
        c.weightx = 0.65;
        pane.add(txtAddress, c);
        
        c.gridy = 1;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(lblDisplay, c);
        c.gridx = 1;
        pane.add(cmbDisplay, c);
        
        c.gridy = 2;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(lblLockMode, c);
        c.gridx = 1;
        pane.add(cmbLockMode, c);
        
        c.gridy = 3;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        pane.add(lblConfig, c);
        c.gridx = 1;
        pane.add(cmbConfig, c);
        
        c.gridy = 4;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 2;
        pane.add(chkCopyResources, c);
        
        c.gridy = 5;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        pane.add(new JLabel(""), c);
        
        JPanel buttonsPane = new JPanel();
        JButton btnConnect = new JButton("Connect");
        JButton btnCancel = new JButton("Cancel");
        btnConnect.addActionListener((e) -> {
            dialog.dispose();
        });
        btnCancel.addActionListener((e) -> {
           ControlCenter.exit(0); 
        });
        buttonsPane.add(btnConnect);
        buttonsPane.add(btnCancel);
        
        dialogPane.add(pane, BorderLayout.CENTER);
        dialogPane.add(buttonsPane, BorderLayout.PAGE_END);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setSize(500, 400);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ControlCenter.exit(0);
            }
        });
        dialog.setVisible(true);
        if(cmbConfig.getSelectedIndex() == 2) {
            String file = cmbConfig.getItemAt(2);
            if(file.toLowerCase().endsWith(".zip") ||
                    file.toLowerCase().endsWith(".merccz")) {
                fields.put("localzip", file);
            } else {
                fields.put("localconfig", file);
            }
        }
        fields.put("address", txtAddress.getText().trim());
        fields.put("displaynumber", String.valueOf(cmbDisplay.getSelectedIndex()));
        fields.put("lockmode", String.valueOf(cmbLockMode.getSelectedIndex()-1));
        if(chkCopyResources.isSelected()) {
            fields.put("copyresources", "yes");
        }
        return fields;
    }
    
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
            int displayNumber, int lockMode) {
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
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();
            int numScreens = screens.length;
            Log.d(0, "- active displays:");
            for(int i = 0; i < screens.length; i++) {
                Log.d(0, i + ": " + screens[i].getIDstring() + " (" +
                         screens[i].getDisplayMode().getWidth() + "x" +
                         screens[i].getDisplayMode().getHeight() + ")");
            }
            if(displayNumber < 0) {
                Log.di(0, "- select display to output to (type in the index and press enter): ");
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                    displayNumber = Integer.parseInt(r.readLine());
                } catch(Exception e) {
                    Log.err("failed to parse input");
                    ControlCenter.exit(1);
                }
            }
            if(displayNumber < 0 || displayNumber >= numScreens) {
                Log.fatal(103, "display " + displayNumber + " not found");
            }
            GraphicsDevice gd = ge.getScreenDevices()[displayNumber];
            SwingUtilities.invokeLater(() -> {
                displayOptions = new DisplayOptionsFrame(cc);
                display.init();
                displayOptions.init();
                display.getCanvas().addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        if(evt.getButton() == MouseEvent.BUTTON1 &&
                           evt.getClickCount() == 2) {
                            if(!displayOptions.isVisible()) {
                                displayOptions.reset();
                                displayOptions.setVisible(true);
                            }
                        }
                    }
                });
                display.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        try {
                            s.close();
                        } catch(IOException ioe) {
                            System.err.println("IOException: " + ioe.getMessage());
                        }
                    }
                });
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
            Team t;
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
                        t = c.getTeamByID(teamID);
                        c.setState(CompetitionState.SETUP);
                        c.newSession(t, attempts, setup, run);
                        c.getSession().start();
                        break;
                    case "STATE_CHANGE_RUN":
                        c.setState(CompetitionState.RUN);
                        c.getSession().endSetup();
                        display.newScore();
                        t = c.getSession().getActiveTeam();
                        display.setBestScore(t.getBestScore());
                        break;
                    case "SCORE_CHANGE":
                        tokens = tokens[1].split(" ");
                        c.getSession().modifyCurrentScore(tokens[0], Double.parseDouble(tokens[2]));
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
                        t = c.getSession().getActiveTeam();
                        c.getSession().completeRun(true);
                        c.sort();
                        display.setClassificationData(c.getSortedFinishedTeams());
                        display.newScore();
                        display.setBestScore(t.getBestScore());
                        break;
                    case "SESSION_ATTEMPT_DISCARDED":
                        c.getSession().completeRun(false);
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
            Log.d(0, "- server closed connection");
            s.close();
        } catch(IOException ioe) {
            System.err.println("- server connection error: " + ioe.getMessage());
        } catch(NumberFormatException nfe) {
            System.err.println("- unable to parse: " + nfe.getMessage());
        } catch(Exception e) {
            System.err.println("- unhandled exception");
            e.printStackTrace();
        }
        cc.getDisplayFrame().dispose();
        Log.d(0, "- exiting");
        ControlCenter.exit(0);
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
