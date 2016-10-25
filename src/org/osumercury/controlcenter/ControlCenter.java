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
package org.osumercury.controlcenter;

import org.osumercury.controlcenter.gui.Assets;
import org.osumercury.controlcenter.gui.DisplayFrame;
import org.osumercury.controlcenter.gui.ControlFrame;
import org.osumercury.controlcenter.gui.FontSelectDialog;
import org.osumercury.controlcenter.misc.SocketInterface;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import com.beust.jcommander.*;
import javax.swing.JOptionPane;
import java.io.File;

/**
 *
 * @author wira
 */
public class ControlCenter {
    @Parameter(names = { "-c", "--config" }, description = "merccc configuration file to load")
    private String configFile = null;
    
    @Parameter(names = { "-z", "--zip" }, description = "Zipped merccc configuration to load")
    private String zipFile = null;
    
    @Parameter(names = { "-l", "--load" }, description = "Load previously saved CSV data file")
    private String dataFile = null;
    
    @Parameter(names = { "-d", "--debug" }, description = "Debug level")
    private Integer debug = 0;
    
    @Parameter(names = { "-p", "--port" }, description = "Socket interface port number")
    private Integer port = -1;
    
    @Parameter(names = { "--localport" }, description = "Loopback socket interface port number")
    private Integer localPort = -1;
    
    @Parameter(names = { "--help" }, help = true, description = "Display this helpful message")
    private boolean help = false;
    
    @Parameter(names = { "--about" }, help = true, description = "Display information about the software")
    private boolean about = false;
    
    @Parameter(names = { "-f", "--format" }, help = true, description = "Display configuration file format")
    private boolean confformat = false;
    
    @Parameter(names = { "--nosound" }, help = true, description = "Disable sound")
    private boolean nosound = false;
    
    @Parameter(names = { "--rendertime" }, help = true, description = "Draw render time")
    private boolean drawRenderTime = false;
    
    @Parameter(names = { "--notheme" }, help = true, description = "Ignore user-defined fonts")
    private boolean noTheme = false;
    
    @Parameter(names = { "--font" }, help = true, description = "Use this system font instead of bitmap fonts")
    private String sysFont = null;
    
    @Parameter(names = { "--ask-font" }, help = true, description = "Ask user to choose a font on startup")
    private boolean askFont = false;
    
    private Boolean GUI = true;
    
    public static CompetitionState competition;
    public static DisplayFrame display;
    public static ControlFrame control;
    public static JCommander jc;
    public static SocketInterface socket;
    public static SocketInterface loopback;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
        ControlCenter cc = new ControlCenter();
        try {
            jc = new JCommander(cc, args);
        } catch(ParameterException pe) {
            Log.fatal(0, "Unknown/malformed argument: " + pe.toString() + 
                    "\nrun with '--help' for options");
            return;
        }
        cc.run();
    }   
    
    public void run() {
        System.out.println("Mercury Control Center v" +
            Text.getVersion());
        String val;
        
        if(help) {
            jc.usage();
            return;
        }
        
        if(confformat) {
            System.out.println(Text.getConfigFileSpecs());
            return;
        }
        
        if(about) {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(650, 600);
            f.setTitle("About Mercury Control Center");
            ControlFrame.populateAboutPane(f.getContentPane(), true);
            f.setVisible(true);
            return;
        }       
        
        Config.SOUND_DISABLED = nosound;        
        Log.errorDialogBox = GUI;
        
        if(zipFile != null && configFile != null) {
            Log.fatal(5, "'-c' and '-z' options are exclusive");
        }
        
        if(configFile == null && zipFile == null && GUI) {
            String selectedFile;
            System.err.println("Configuration file was not specified");
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new java.io.File("."));
            fc.setDialogTitle("Open configuration file");
            if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fc.getSelectedFile().toString();
                if(!Config.load(fc.getSelectedFile().toString(),
                        selectedFile.toLowerCase().endsWith(".zip") ||
                        selectedFile.toLowerCase().endsWith(".merccz"))) {
                    Log.fatal(1, "Failed to load configuration file " + Config.CONFIG_FILE);
                }
            } else {
                Log.fatal(1, "Configuration file is required");
            }
        } else if(zipFile != null) {
            if(!Config.load(zipFile, true)) {
                Log.fatal(1, "Failed to load zip file " + zipFile);
            }
        } else {
            if(!Config.load(configFile, false)) {
                Log.fatal(1, "Failed to load configuration file " + Config.CONFIG_FILE);
            }
        }
        Log.debugLevel = debug;
        
        val = Config.getValue("tournament", "sortorder");
        if(val != null) {
            try {
                int sortOrder = Integer.parseInt(val);
                switch (sortOrder) {
                    case Team.SORT_ASCENDING:
                        Team.setSortOrder(Team.SORT_ASCENDING);
                        break;
                    case Team.SORT_DESCENDING:
                        Team.setSortOrder(Team.SORT_DESCENDING);
                        break;
                    default:
                        System.err.println("Failed to set sorting order value, " +
                                "reverting to default SORT_DESCENDING");
                        Team.setSortOrder(Team.SORT_DESCENDING);
                        break;
                }
            } catch(NumberFormatException nfe) {
                System.err.println("Failed to set sorting order value, " +
                        "reverting to default SORT_DESCENDING");            
                Team.setSortOrder(Team.SORT_DESCENDING);
            }
        }
 
        Score.init();
        if(!Score.initialized()) {
            Log.fatal(2, "Failed to initialize scoring system\n" +
                    "This is most likely caused by an invalid configuration file");
        }
        try {
            competition = new CompetitionState();
        } catch(Exception e) {
            Log.fatal(3, "Failed to initialize competition state.\n" + 
                    "This is most likely caused by an invalid configuration file.\n" +
                    e.toString() + ": " + e.getMessage());
        }
        
        if(dataFile != null) {
            Data.loadCSV(competition, dataFile);
        }
        
        // graphical UI
        if(GUI) {
            String dirParent = Config.CONFIG_FILE.getParent();
            String resourceDir = (dirParent != null ? dirParent + File.separatorChar : "") + 
                    Config.getValue("system", "resourcedir");
            Log.d(0, "Checking " + resourceDir);
            if(resourceDir == null || !(new File(resourceDir)).exists() ||
                    !(new File(resourceDir).isDirectory())) {
                JOptionPane.showMessageDialog(null, "Resource directory is not specified or " + 
                        "was not found", "Resource directory", JOptionPane.ERROR_MESSAGE);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(new File(Config.CONFIG_FILE.getParent()));
                fileChooser.showOpenDialog(null);
                if(fileChooser.getSelectedFile() != null) {
                    resourceDir = fileChooser.getSelectedFile().getAbsolutePath();
                }
            }
            Assets.loadInternalAssets();
            Assets.load(resourceDir);
            if(!noTheme) {
                Assets.theme();
            }
            if(askFont) {
                FontSelectDialog fSelect = new FontSelectDialog("Choose Display Font");
                fSelect.setModal(true);
                fSelect.setLocationRelativeTo(null);
                fSelect.showDialog();
                if(fSelect.isApproved()) {
                    sysFont = fSelect.getFontName();
                }
            }
            display = new DisplayFrame(sysFont);
            control = new ControlFrame();
            control.init();
            control.pack();
            control.setVisible(true);
            DisplayFrame.DRAW_RENDER_TIME = drawRenderTime;
            display.init();
            control.updateDataView();
            
            if(port > 0 && port < 65535) {
                socket = new SocketInterface(port, competition, control, false);
                socket.start();
            }
            
            if(localPort > 0 && localPort < 65535) {
                loopback = new SocketInterface(localPort, competition, control, true);
                loopback.start();
            }
        }
    }
    
    public static void exit(int ret) {
        Log.d(0, "Cleanup");
        if(Config.TMP_DIR != null) {
            Log.d(0, "Deleting " + Config.TMP_DIR);
            if(!Config.deleteDirectory(Config.TMP_DIR)) {
                System.err.println("Failed to delete directory");
            }
        }
        
        if(socket != null) {
            socket.close();
        }
        
        System.exit(ret);
    }
}
