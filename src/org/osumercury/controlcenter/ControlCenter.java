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

import org.osumercury.controlcenter.gui.RefreshThread;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.osumercury.controlcenter.gui.ThumbnailFrame;
import org.osumercury.controlcenter.misc.DisplayClient;

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
    private boolean confFormat = false;
    
    @Parameter(names = { "--localization" })
    private boolean localization = false;
    
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
    
    @Parameter(names = { "-x" })
    private String client = null;
    
    @Parameter(names = { "--client" })
    private boolean clientModeHelp = false;
    
    @Parameter(names = { "--lockmode" })
    private int lockMode = -1;
    
    @Parameter(names = { "--classicdigits" })
    private boolean classicDigits = false;
    
    @Parameter(names = { "--copyresources" })
    private boolean copyResources = false;
    
    private Boolean GUI = true;
    private File resourcePath;
    
    private CompetitionState competition;
    private DisplayFrame display;
    private ControlFrame control;    
    private ThumbnailFrame thumb;
    private RefreshThread refresh;
    private SocketInterface socket;
    private SocketInterface loopback;
    
    private int displayNumber;
    private String controlHost;
    private int controlPort;
    
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
        System.out.println("Mercury Control Center v" + Text.getVersion());
        String val;
        
        Log.debugLevel = debug;
        
        if(help) {
            printHelp();
            return;
        }
        
        if(confFormat) {
            System.out.println(Text.getConfigFileSpecs());
            return;
        }
        
        if(localization) {
            System.out.println(Text.getLocalizationInformation());
            return;
        }
        
        if(clientModeHelp) {
            printClientModeHelp();
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
        
        boolean fetchConfig = false;
        if(client != null) {
            Log.d(0, "- running in client mode");
            String[] tokens = client.trim().split(":");
            try {
                displayNumber = Integer.parseInt(tokens[2].trim());
                controlHost = tokens[0].trim();
                controlPort = Integer.parseInt(tokens[1].trim());
            } catch(NumberFormatException nfe) {
                Log.fatal(100, "Unable to parse: " + client);
            }
        }
        
        // client mode, and config was not provided, let's fetch it
        if (client != null && configFile == null && zipFile == null) {
            fetchConfig = true;
            Log.d(0, "- no local config provided");
            String configStr = DisplayClient.getConfigString(
                    controlHost, controlPort, copyResources);
            if(configStr != null) {
                Config.parse(configStr);                
            } else {
                Log.fatal(101, "Unable to fetch config from server");
            }
            
        } else if(configFile == null && zipFile == null && GUI) {
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
            competition = new CompetitionState(Config.getSectionAsMap("teams"));
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
            if(!fetchConfig && (resourceDir == null || !(new File(resourceDir)).exists() ||
                    !(new File(resourceDir).isDirectory()))) {
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
            if(resourceDir != null) {
                resourcePath = new File(resourceDir);
            }
            Assets.loadInternalAssets();
            Assets.load(resourceDir);
            if(!noTheme) {
                Assets.theme(Config.getSectionAsMap("theme"));
            }
            Assets.setClassicDigits(classicDigits);
            
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
            
            DisplayFrame.DRAW_RENDER_TIME = drawRenderTime;
            
            if(client == null) {
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
                    display.init();
                    control.updateDataView();     
                    refresh.start();
                });

                if(port > 0 && port <= 65535) {
                    socket = new SocketInterface(port, this, control, false, copyResources);
                    socket.start();
                }

                if(localPort > 0 && localPort <= 65535) {
                    loopback = new SocketInterface(localPort, this, control, true, copyResources);
                    loopback.start();
                }
            } else {
                // display client mode (no control window)
                display = new DisplayFrame(this, sysFont);
                refresh = new RefreshThread(this, refreshRateMs);
                DisplayClient.connect(this, controlHost, controlPort,
                        displayNumber, fetchConfig, lockMode);
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
    
    public File getResourcePath() {
        return resourcePath;
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
                 "      --localization       display information about text localization\n"+
                 "      --font FONT          use the specified FONT instead of the built-in font\n"+
                 "      --ask-font           list system fonts to use with font selection dialog\n"+
                 "  -p, --port PORT          open tcp server socket interface\n"+
                 "      --localport PORT     open loopback only tcp server socket interface\n"+
                 "  -m, --nosound            disable all audio playback\n"+
                 "  -t, --notheme            ignore user's theme defined in the configuration\n"+
                 "  -d, --debug LEVEL        set program verbosity for debugging\n"+
                 "  -r, --refreshrate TIME   set display refresh rate in milliseconds\n"+
                 "      --rendertime         display the time it took to render a frame\n"+
                 "  -x HOST:PORT:DISPLAY     run merccc in client mode (--client for details)\n"+
                 "\n"+
                 "keyboard shortcuts:\n"+
                 "  CTRL+[1-4]               select active control tab\n"+
                 "  F1                       set output mode to logo and time\n"+
                 "  F3                       set output mode to run status\n"+
                 "  F4                       set output mode to classification\n"+
                 "  F5                       show/hide preview window\n"+
                 "  CTRL+S                   save recorded data set\n"+
                 "  CTRL+L                   load previously saved data set\n"+
                 "  CTRL+A                   add a score without running a scoring session\n"+
                 "  CTRL+M                   toggle sound playback\n"+
                 "  CTRL+F                   change display window font\n"+
                 "");
    }
    
    public static void printClientModeHelp() {
        Log.d(0, "\n"+
                 "client mode: java -jar <jarfile> -x HOST:PORT:DISPLAY [options]\n\n"+
                 "  Connect to a remote instance of merccc running on the specified host. The\n"+
                 "  local display window will reflect the state of the remote server and be\n"+
                 "  displayed on a screen as specified by DISPLAY. DISPLAY is the index of the\n"+
                 "  screen as enumerated by the Java Virtual Machine (indexed 0 to n).\n"+
                 "\n"+
                 "  If neither '-c' nor '-z' were provided, merccc will attempt to fetch the\n"+
                 "  configuration from the server. If a configuration was provided, merccc will\n"+
                 "  check if the hash codes for the remote and local configurations match. If\n"+
                 "  they do not match merccc will not start.\n"+
                 "\n"+
                 "  Resources will not be transferred. Local resources will be loaded if the\n"+
                 "  local directory is found or if a ZIP file containing the resources is loaded\n"+
                 "  using the '-z' option.\n"+
                 "\n"+
                 "client mode specific options:\n"+
                 "      --lockmode MODE      lock the display window in a specific MODE:\n"+
                 "                           0: logo and time, 1: run status, 2: classification\n"
        );
    }
}
