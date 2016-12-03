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
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.osumercury.controlcenter.gui.ThumbnailFrame;

/**
 *
 * @author wira
 */
public class ControlCenter {
    @Parameter(names = { "-c", "--config" })
    private String configFile = null;
    
    @Parameter(names = { "-z", "--zip" })
    private String zipFile = null;
    
    @Parameter(names = { "-l", "--load" })
    private String dataFile = null;
    
    @Parameter(names = { "-d", "--debug" })
    private Integer debug = 0;
    
    @Parameter(names = { "-p", "--port" })
    private Integer port = -1;
    
    @Parameter(names = { "--localport" })
    private Integer localPort = -1;
    
    @Parameter(names = { "--help" }, help = true)
    private boolean help = false;
    
    @Parameter(names = { "--about" })
    private boolean about = false;
    
    @Parameter(names = { "-f", "--format" })
    private boolean confformat = false;
    
    @Parameter(names = { "-m", "--nosound" })
    private boolean nosound = false;
    
    @Parameter(names = { "--rendertime" })
    private boolean drawRenderTime = false;
    
    @Parameter(names = { "-t", "--notheme" })
    private boolean noTheme = false;
    
    @Parameter(names = { "--font" })
    private String sysFont = null;
    
    @Parameter(names = { "--ask-font" })
    private boolean askFont = false;
    
    @Parameter(names = { "--width"})
    private Integer frameWidth = -1;
    
    @Parameter(names = { "--height"})
    private Integer frameHeight = -1;
    
    @Parameter(names = { "-r", "--refreshrate" })
    private Long refreshRateMs = 100L;
    
    private Boolean GUI = true;
    
    private CompetitionState competition;
    private DisplayFrame display;
    private ControlFrame control;    
    private ThumbnailFrame thumb;
    private RefreshThread refresh;
    private SocketInterface socket;
    private SocketInterface loopback;
    
    private static JCommander jc;
    public static ControlCenter cc;
    
    public static long beginTime = -1;
    public static boolean SOUND_DISABLED = false;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
        cc = new ControlCenter();
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
            printHelp();
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
            JPanel aboutPane = new JPanel();
            f.getContentPane().add(aboutPane, BorderLayout.CENTER);
            ControlFrame.populateAboutPane(aboutPane, true);
            JButton aboutPaneBtnExit = new JButton("Exit");
            aboutPaneBtnExit.addActionListener((ActionEvent e) -> {
                System.exit(0);
            });
            f.getContentPane().add(aboutPaneBtnExit, BorderLayout.PAGE_END);
            f.setVisible(true);
            return;
        }       
        
        SOUND_DISABLED = nosound;        
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
                    Log.fatal(1, "Failed to load configuration file " + Config.getConfigFile());
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
                Log.fatal(1, "Failed to load configuration file " + Config.getConfigFile());
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
        
        val = Config.getValue("theme", "alignclockleft");
        if(val != null && val.equals("1")) {
            DisplayFrame.ALIGN_CLOCK_LEFT = true;
        }
 
        Score.init(
                Config.getValue("formula", "postfix"),
                Config.getSectionAsMap("fields")
        );
        if(!Score.initialized()) {
            Log.fatal(2, "Failed to initialize scoring system\n" +
                    "This is most likely caused by an invalid configuration file");
        }
        Score.test(
                Config.getSectionAsMap("test"),
                Config.getValue("test", "__RESULT")
        );
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
            String dirParent = Config.getConfigFileParent();
            String resourceDir = (dirParent != null ? dirParent + File.separatorChar : "") + 
                    Config.getValue("system", "resourcedir");
            Log.d(0, "Checking " + resourceDir);
            if(resourceDir == null || !(new File(resourceDir)).exists() ||
                    !(new File(resourceDir).isDirectory())) {
                JOptionPane.showMessageDialog(null, "Resource directory is not specified or " + 
                        "was not found", "Resource directory", JOptionPane.ERROR_MESSAGE);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(new File(dirParent));
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
            
            val = Config.getValue("theme", "systemfont");
            if(val != null) {
                sysFont = val;
                Log.d(0, "Using sysfont: " + sysFont);
            }
            
            if(askFont) {
                FontSelectDialog fSelect = new FontSelectDialog("Choose Display Font");
                fSelect.setModal(true);
                fSelect.setLocationRelativeTo(null);
                fSelect.showDialog();
                if(fSelect.isApproved()) {
                    sysFont = fSelect.getFontName();
                    Log.d(0, "Using sysfont: " + sysFont);
                }
            }
            
            if(frameWidth > 0) {
                ControlFrame.INITIAL_WIDTH = frameWidth;
            }
            
            if(frameHeight > 0) {
                ControlFrame.INITIAL_HEIGHT = frameHeight;
            }

            display = new DisplayFrame(this, sysFont);
            control = new ControlFrame(this);
            thumb = new ThumbnailFrame(this);
            refresh = new RefreshThread(this, refreshRateMs);
            SwingUtilities.invokeLater(() -> {
                control.init();          
                DisplayFrame.DRAW_RENDER_TIME = drawRenderTime;
                display.init();
                control.updateDataView();     
                refresh.start();
            });
            
            if(port > 0 && port <= 65535) {
                socket = new SocketInterface(port, competition, control, false);
                socket.start();
            }
            
            if(localPort > 0 && localPort <= 65535) {
                loopback = new SocketInterface(localPort, competition, control, true);
                loopback.start();
            }
        }
    }    
    
    public CompetitionState getCompetitionState() {
        return competition;
    }
    
    public DisplayFrame getDisplayFrame() {
        return display;
    }
    
    public ControlFrame getControlFrame() {
        return control;
    }
    
    public ThumbnailFrame getThumbnailFrame() {
        return thumb;
    }
    
    public RefreshThread getRefreshThread() {
        return refresh;
    }
    
    public SocketInterface getSocketHandle() {
        return socket;
    }
    
    public SocketInterface getLoopbackSocketHandle() {
        return loopback;
    }
    
    public static void exit(int ret) {
        Log.d(0, "Cleanup");
        if(Config.getTmpDir() != null) {
            Log.d(0, "Deleting " + Config.getTmpDir());
            if(!Config.deleteDirectory(Config.getTmpDir())) {
                System.err.println("Failed to delete directory");
            }
        }
        
        if(cc.getSocketHandle() != null) {
            cc.getSocketHandle().close();
        }
        
        if(cc.getLoopbackSocketHandle() != null) {
            cc.getLoopbackSocketHandle().close();
        }
        
        if(cc.getRefreshThread() != null) {
            cc.getRefreshThread().stopThread();
        }
        
        System.exit(ret);
    }
    
    public static void printHelp() {
        Log.d(0, "\n"+
                 "usage: java -jar <jarfile> [options]\n"+
                 "\n"+
                 "options (exclusive):\n"+
                 "  -c, --config FILE        load FILE to configure merccc\n"+
                 "  -z, --zip ZIPFILE        load ZIPFILE containing a merccc configuration\n"+
                 "\n"+
                 "if neither option is specified, merccc will present a file open dialog.\n"+
                 "if no configuration file or a zip file containing a configuration is specified,\n"+
                 "merccc will not start\n"+
                 "\n"+
                 "additional options:\n"+
                 "  -f, --format             print configuration file format to console and quit\n"+
                 "  -l, --load FILE          load saved .csv data from a previous scoring session\n"+
                 "      --help               display this help message\n"+
                 "      --about              display information about the software\n"+
                 "      --font FONT          use the specified FONT instead of the built-in font\n"+
                 "      --ask-font           list system fonts to use with font selection dialog\n"+
                 "  -p, --port PORT          open tcp socket interface\n"+
                 "      --localport PORT     open loopback only tcp socket interface\n"+
                 "  -m, --nosound            disable all audio playback\n"+
                 "  -t, --notheme            ignore user's theme defined in the configuration\n"+
                 "  -d, --debug LEVEL        set program verbosity for debugging\n"+
                 "  -r, --refreshrate TIME   set display refresh rate in milliseconds\n"+
                 "      --rendertime         display the time it took to render a frame\n"+
                 "\n"+
                 "keyboard shortcuts:\n"+
                 "  CTRL+[1-4]               select active control tab\n"+
                 "  F1                       set output mode to logo and time\n"+
                 "  F3                       set output mode to run status\n"+
                 "  F4                       set output mode to classification\n"+
                 "  F5                       show/hide thumbnailed view\n"+
                 "  CTRL+S                   save recorded data set\n"+
                 "  CTRL+L                   load previously saved data set\n"+
                 "  CTRL+A                   add a score without running a scoring session\n"+
                 "  CTRL+M                   toggle sound playback\n"+
                 "  CTRL+F                   change display window font\n"+
                 "");
    }
}
