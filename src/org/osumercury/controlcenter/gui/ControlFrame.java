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
package org.osumercury.controlcenter.gui;

import org.osumercury.controlcenter.UserEvent;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.osumercury.controlcenter.*;

/**
 *
 * @author wira
 */
public class ControlFrame extends JFrame {
    private CompetitionState competition;
    private ControlCenter cc;
    private SessionTimer timer;
    private DisplayFrame display;
        
    private static int setupDuration;
    private static int windowDuration;
    private static int maxAttempts;
    
    public static int INITIAL_WIDTH = 1000;
    public static int INITIAL_HEIGHT = 600;
    
    //<editor-fold defaultstate="collapsed" desc="UI Elements Declarations">
    
    /** UI ELEMENTS **/
    private ControlIndicatorsCanvas indicators;
    private JPanel paneGlobal;
    private JTabbedPane paneTabbedContainer;
    private JPanel paneClassification;
    private JPanel paneRunControl;
    private JPanel paneAbout;
    private JPanel paneDataManipulation;
    
    /** OPTIONS MENU ELEMENTS **/
    private JPopupMenu menuOpts;
    private JMenuItem menuOptsFont;
    private JCheckBoxMenuItem menuOptsThumbnail;
    private JCheckBoxMenuItem menuOptsSounds;
    private JCheckBoxMenuItem menuOptsBanner;
    private JMenuItem menuOptsAppearances;
    private JMenuItem menuExtraOptions;
    
    /** GLOBAL UI ELEMENTS **/
    private JLabel lblDisplayScreen;
    private JComboBox cmbDisplayScreen;
    private JLabel lblDisplayMode;
    private JComboBox cmbDisplayMode;
    private JButton btnRefreshScreens;
    private JButton btnExit;    
    private JButton btnOptions;
    
    /** DISPLAY OPTIONS UI ELEMENTS **/
    
    private JLabel lblStartingRank;
    private JComboBox cmbStartingRank;
    
    /** RUN CONTROL UI ELEMENTS **/
    private JPanel paneRunInit;
    private JPanel paneRunTimerControl;
    private JPanel paneRunScoringControl;
    private JScrollPane paneRunScoringControlScroll;
    private JLabel lblTeamSelect;
    private JLabel lblSetupDuration;
    private JLabel lblWindowDuration;
    private JLabel lblAttempts;
    private JComboBox cmbTeamSelect;
    private JButton btnStartTeamSession;
    private JButton btnSkipSetup;
    private JButton btnPause;
    private JButton btnDiscardScore;
    private JButton btnCommitScore;
    private JButton btnAddTime;
    private JTextField txtSetupDuration;
    private JTextField txtWindowDuration;
    private JTextField txtAttempts;
    private JTextField[] txtScoreFields;
    private JToggleButton tglRedFlag;
    
    /** DATA MANIPULATION UI ELEMENTS **/
    private JPanel paneDataTop;
    private JButton btnDataSave;
    private JButton btnDataLoad;
    private JButton btnDataDelete;
    private JButton btnDataClear;
    private JButton btnDataEdit;
    private JButton btnDataAdd;
    private JTable tblData;

    /** CLASSIFICATION UI ELEMENTS */
    private JButton btnGenerateReport;
    private JButton btnSetTiebreaker;
    private JButton btnClearTiebreaker;
    private JButton btnSetFlags;
    private JTable tblClassification;    
    
//</editor-fold>
    
    /**
     * Creates new form ControlFrame
     * 
     * @param cc
     */
    public ControlFrame(ControlCenter cc) {
        this.cc = cc;
        this.competition = cc.getCompetitionState();     
        this.display = cc.getDisplayFrame();
        competition.addStateChangeHook((c) -> { setControlPhase(c.getState()); });        
    }
    
    public void init() {
        Log.d(0, "ControlFrame: init");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        setTitle("Mercury Control Center (" + Config.getConfigFile().getName() + ")");
        Container pane = this.getContentPane();
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Log.d(0, "Window closing event");
                exit();
            }
        });                
        
        //<editor-fold defaultstate="collapsed" desc="Main Pane Init">
        paneTabbedContainer = new JTabbedPane();
        paneRunControl = new JPanel();
        paneRunControl.setName("SCORING");
        paneAbout = new JPanel();
        paneAbout.setName("ABOUT");
        paneDataManipulation = new JPanel();
        paneDataManipulation.setName("DATA");
        paneClassification = new JPanel();
        paneClassification.setName("CLASSIFICATION");        
        paneTabbedContainer.setTabPlacement(JTabbedPane.TOP);
        paneTabbedContainer.setPreferredSize(new Dimension(700, 550));
        paneTabbedContainer.add(paneAbout);
        paneTabbedContainer.add(paneRunControl);
        paneTabbedContainer.add(paneDataManipulation);
        paneTabbedContainer.add(paneClassification);        
        paneTabbedContainer.setSelectedIndex(0);
        indicators = new ControlIndicatorsCanvas(competition, this);
        indicators.setPreferredSize(new Dimension(200,550));
        paneGlobal = new JPanel();
        paneGlobal.setPreferredSize(new Dimension(900,50));
        pane.add(paneGlobal, BorderLayout.PAGE_END);
        pane.add(paneTabbedContainer, BorderLayout.CENTER);
        pane.add(indicators, BorderLayout.LINE_END);
        populateAboutPane(paneAbout, false);
           
        //paneAbout.add(paneSoftwareTitle);
//</editor-fold>
    
        //<editor-fold defaultstate="collapsed" desc="Global Controls Init">
        
        paneGlobal.setLayout(new FlowLayout(FlowLayout.CENTER,
                5, 5));
        lblDisplayScreen = new JLabel("Output :");
        cmbDisplayScreen = new JComboBox();
        refreshDisplayList();
        lblDisplayMode = new JLabel("Mode :");
        cmbDisplayMode = new JComboBox();
        cmbDisplayMode.addItem("Logo and Time");
        cmbDisplayMode.addItem("Run Status");
        cmbDisplayMode.addItem("Classification");
        btnRefreshScreens = new JButton("Detect");
        btnExit = new JButton("Exit");
        btnOptions = new JButton("Options");

        paneGlobal.add(lblDisplayScreen);
        paneGlobal.add(cmbDisplayScreen);
        paneGlobal.add(btnRefreshScreens);
        paneGlobal.add(new JSeparator());
        paneGlobal.add(lblDisplayMode);
        paneGlobal.add(cmbDisplayMode);
        paneGlobal.add(new JSeparator());
        paneGlobal.add(btnOptions);
        paneGlobal.add(new JSeparator());
        paneGlobal.add(btnExit);
        
        btnOptions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                menuOpts.show(btnOptions, e.getX(), e.getY());
            }
        });
        
        btnExit.addActionListener((ActionEvent e) -> {
            exit();
        });
        
        btnRefreshScreens.addActionListener((ActionEvent e) -> { refreshDisplayList(); } );
        cmbDisplayMode.addActionListener((ActionEvent e) -> {
            display.setMode(cmbDisplayMode.getSelectedIndex());
            ControlCenter.triggerEvent(UserEvent.DISPLAY_MODE_CHANGE, cmbDisplayMode.getSelectedIndex());
        });
        
        cmbDisplayScreen.addActionListener((ActionEvent e) -> {
            outputDisplayToScreen();
        });
        
//</editor-fold>                              

        //<editor-fold defaultstate="collapsed" desc="Run Controls Init">
        try {
            setupDuration = Integer.parseInt(Config.getValue("tournament", "setup"));
            windowDuration = Integer.parseInt(Config.getValue("tournament", "window"));
            maxAttempts = Integer.parseInt(Config.getValue("tournament", "maxattempts"));
        } catch(NullPointerException | NumberFormatException e) {
            if(Log.debugLevel > 0) {
                e.printStackTrace();
            }
            Log.fatal(10, "ControlFrame: failed to load -> " + e.toString());
        }

        paneRunInit = new JPanel();
        paneRunTimerControl = new JPanel();
        paneRunControl.setLayout(new BorderLayout());
        paneRunInit.setPreferredSize(new Dimension(700, 80));
        paneRunTimerControl.setPreferredSize(new Dimension(700, 50));
        paneRunTimerControl.setBackground(Color.BLACK);

        cmbTeamSelect = new JComboBox();
        for(Team team : competition.getTeams()) {
            cmbTeamSelect.addItem(team.getNumber() + ": " + team.getName());
        }
        cmbTeamSelect.setSelectedIndex(0);

        lblTeamSelect = new JLabel("Team : ");
        lblAttempts = new JLabel("Attempts : ");
        lblSetupDuration = new JLabel("Setup : ");
        lblWindowDuration = new JLabel("Run Window : ");

        txtAttempts = new JTextField("" + maxAttempts);
        txtAttempts.setMinimumSize(new Dimension(100, 30));
        txtSetupDuration = new JTextField("" + setupDuration);
        txtSetupDuration.setMinimumSize(new Dimension(100, 30));
        txtWindowDuration = new JTextField("" + windowDuration);
        txtWindowDuration.setMinimumSize(new Dimension(100, 30));

        JPanel paneRunInitTop = new JPanel();
        JPanel paneRunInitBottom = new JPanel();
        btnStartTeamSession = new JButton("START SCORING SESSION");
        btnStartTeamSession.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        cmbTeamSelect.addActionListener((ActionEvent e) -> {
            ControlCenter.triggerEvent(UserEvent.TEAM_PRE_SELECT, getSelectedTeamID());
        });
        
        btnStartTeamSession.addActionListener((ActionEvent e) -> {
            if(competition.getState() == CompetitionState.IDLE) {                
                int savedAttempts = maxAttempts;
                int savedSetup = setupDuration;
                int savedWindow = windowDuration;
                try {
                    maxAttempts = Integer.parseInt(txtAttempts.getText());
                    setupDuration = Integer.parseInt(txtSetupDuration.getText());
                    windowDuration = Integer.parseInt(txtWindowDuration.getText());
                } catch(NumberFormatException nfe) {
                    maxAttempts = savedAttempts;
                    setupDuration = savedSetup;
                    windowDuration = savedWindow;
                    txtAttempts.setText("" + maxAttempts);
                    txtSetupDuration.setText("" + setupDuration);
                    txtWindowDuration.setText("" + windowDuration);
                }
                competition.setState(CompetitionState.SETUP);                
            } else {
                // warn first!
                if(!confirmYesNo("This will discard the current run and end"
                        + " the session for the team. Are you sure?",
                        "End Session Confirmation")) {
                    return;
                }

                competition.setState(CompetitionState.IDLE);                
            }
        });

        paneRunInitTop.setLayout(new FlowLayout(FlowLayout.LEFT));
        // paneRunInitBottom.setLayout(new GridLayout(1,6,5,0));
        paneRunInitBottom.setLayout(new BoxLayout(
                paneRunInitBottom, BoxLayout.LINE_AXIS
        ));
        paneRunInitTop.add(lblTeamSelect);
        paneRunInitTop.add(cmbTeamSelect);
        paneRunInitTop.add(btnStartTeamSession);
        paneRunInitBottom.add(lblAttempts);
        paneRunInitBottom.add(txtAttempts);
        paneRunInitBottom.add(lblSetupDuration);
        paneRunInitBottom.add(txtSetupDuration);
        paneRunInitBottom.add(lblWindowDuration);
        paneRunInitBottom.add(txtWindowDuration);

        paneRunInit.setLayout(new BoxLayout(paneRunInit, BoxLayout.PAGE_AXIS));
        paneRunInit.add(paneRunInitTop);
        paneRunInit.add(paneRunInitBottom);

        btnSkipSetup = new JButton("SKIP");
        btnPause= new JButton("PAUSE");
        btnCommitScore = new JButton("COMMIT");
        btnDiscardScore = new JButton("DISCARD");
        tglRedFlag = new JToggleButton("RED FLAG");
        btnAddTime = new JButton("ADD TIME");
        
        btnSkipSetup.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnCommitScore.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnDiscardScore.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnDiscardScore.setForeground(Color.RED);

        btnPause.addActionListener((ActionEvent e) -> {
            if(competition.getState() != CompetitionState.SETUP &&
                    competition.getState() != CompetitionState.RUN) {
                return;
            }

            if(competition.getSession().isPaused()) {
                competition.getSession().resumeTimer();
                btnPause.setText("PAUSE");
            } else {
                competition.getSession().pauseTimer();
                btnPause.setText("RESUME");
            }
        });

        btnSkipSetup.addActionListener((ActionEvent e) -> {
            if(!confirmYesNo("Skip setup and start scoring window?",
                    "Skip Setup")) {
                return;
            }
            
            if(competition.getState() != CompetitionState.SETUP) {
                return;
            }

            competition.setState(CompetitionState.RUN);
        });

        btnCommitScore.addActionListener((ActionEvent e) -> {
            // warn first!
            if(!confirmYesNo("End the current attempt and commit the score?",
                    "Commit Run Confirmation")) {
                return;
            }

            commitScore();
        });

        btnDiscardScore.addActionListener((ActionEvent e) -> {
            // warn first!
            if(!confirmYesNo("This will discard the current run. "
                    + "Are you sure?",
                    "Discard Run Confirmation")) {
                return;
            }

            discardScore();
        });
        
        btnAddTime.addActionListener((ActionEvent e) -> {
            NumberInputDialog dialog = new NumberInputDialog("Add/deduct Time in seconds",
                    0, NumberInputDialog.INTEGER);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.showDialog();
            if(dialog.isApproved()) {
                competition.getSession().addTimeSeconds(dialog.getValueInt());
            }
        });
        
        tglRedFlag.addActionListener((ActionEvent e) -> {            
            competition.setRedFlag(tglRedFlag.isSelected());
        });

        paneRunTimerControl.add(btnSkipSetup);
        paneRunTimerControl.add(btnPause);
        paneRunTimerControl.add(Box.createHorizontalStrut(10));
        paneRunTimerControl.add(btnCommitScore);
        paneRunTimerControl.add(btnDiscardScore);
        paneRunTimerControl.add(Box.createHorizontalStrut(10));
        paneRunTimerControl.add(tglRedFlag);
        paneRunTimerControl.add(btnAddTime);

        paneRunControl.add(paneRunInit, BorderLayout.PAGE_START);
        paneRunControl.add(paneRunTimerControl, BorderLayout.PAGE_END);

        paneRunScoringControl = new JPanel();
        paneRunScoringControl.setLayout(new BoxLayout(
                paneRunScoringControl, BoxLayout.PAGE_AXIS));
        paneRunScoringControl.setAutoscrolls(true);

        paneRunScoringControlScroll = new JScrollPane(paneRunScoringControl,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        
        paneRunControl.add(paneRunScoringControlScroll, BorderLayout.CENTER);         
        paneRunTimerControl.setVisible(false);
        paneRunScoringControlScroll.setVisible(false);

//</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Data Manipulation Init">

        paneDataTop = new JPanel();
        paneDataTop.setPreferredSize(new Dimension(700, 50));

        btnDataSave = new JButton("SAVE");
        btnDataLoad = new JButton("LOAD");
        btnDataDelete = new JButton("DELETE");
        btnDataClear = new JButton("CLEAR");
        btnDataEdit = new JButton("EDIT");
        btnDataAdd = new JButton("ADD");
        btnDataClear.setForeground(Color.RED);
        btnDataDelete.setForeground(Color.RED);
        
        btnDataSave.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnDataLoad.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnDataClear.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnDataLoad.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnDataLoad.setForeground(Color.RED);

        btnDataSave.addActionListener((ActionEvent e) -> {
            saveData();
        });

        btnDataLoad.addActionListener((ActionEvent e) -> {
            loadData();
        });

        btnDataDelete.addActionListener((ActionEvent e) -> {
            if(confirmYesNo("This will delete the specified record. ARE YOU SURE?!",
                    "Delete Score")) {
                int rows[] = tblData.getSelectedRows();
                if(rows.length > 0) {
                    int teamID = Integer.parseInt((String)tblData.getValueAt(rows[0], 0));
                    int score = Integer.parseInt((String)tblData.getValueAt(rows[0], 2));
                    Data.removeScore(competition, teamID, score);
                    updateDataView();                  
                }
            }
        });

        btnDataClear.addActionListener((ActionEvent e) -> {
            if(confirmYesNo("This will delete ALL recorded scores. ARE YOU SURE?!",
                    "Delete All Data")) {
                Data.clearData(competition);
                updateDataView();
            }
        });

        btnDataEdit.addActionListener((ActionEvent e) -> {
            editData();
        });
        
        btnDataAdd.addActionListener((ActionEvent e) -> {
            addScore();
        });

        paneDataTop.add(btnDataSave);
        paneDataTop.add(btnDataLoad);
        paneDataTop.add(new JSeparator());
        paneDataTop.add(btnDataAdd);
        paneDataTop.add(btnDataEdit);
        paneDataTop.add(btnDataDelete);        
        paneDataTop.add(new JSeparator());
        paneDataTop.add(btnDataClear);

        tblData = new JTable(Data.getTableModel(competition));
        tblData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);       

        paneDataManipulation.setLayout(new BorderLayout());
        paneDataManipulation.add(paneDataTop, BorderLayout.PAGE_START);
        paneDataManipulation.add(new JScrollPane(tblData), BorderLayout.CENTER);
//</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Classification Init">
        JPanel paneClassificationTop = new JPanel();
        paneClassificationTop.setPreferredSize(new Dimension(700, 50));
        btnGenerateReport = new JButton("Generate Report");
        btnSetTiebreaker = new JButton("Set DNF Tiebreaker");
        btnClearTiebreaker = new JButton("Clear DNF Tiebreaker");
        btnSetFlags = new JButton("Set Flags");
        paneClassificationTop.add(btnGenerateReport);
        paneClassificationTop.add(btnSetTiebreaker);
        paneClassificationTop.add(btnClearTiebreaker);
        paneClassificationTop.add(btnSetFlags);
        if(competition.getAllCriteria().isEmpty()) {
            btnSetFlags.setEnabled(false);
        }

        btnGenerateReport.addActionListener((ActionEvent e) -> {
            competition.sort();
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new java.io.File("."));
            fc.setDialogTitle("Generate Competition Report");
            fc.setFileFilter(
                    new FileNameExtensionFilter("Mercury Report (.txt)", "txt")
            );
            if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                if(fc.getSelectedFile().exists()) {
                    if(!confirmYesNo(fc.getSelectedFile().getName() + " exists." +
                            " Overwrite?", "File Exists")) {
                        return;
                    }
                }
                Data.generateReport(competition, fc.getSelectedFile().getAbsolutePath());
            }
        });

        btnSetTiebreaker.addActionListener((ActionEvent e) -> {
            if(tblClassification.getSelectedRow() < 0) {
                return;
            }
            int teamID = Integer.parseInt((String)tblClassification.getValueAt(
                    tblClassification.getSelectedRow(), 1));
            Team t = competition.getTeamByID(teamID);
            NumberInputDialog dialog = new NumberInputDialog(
                    "Set Tiebreaker Value for " +
                            t.getName(), t.getTiebreaker(), NumberInputDialog.FLOAT);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            if(dialog.isApproved()) {
                Data.lock().writeLock().lock();
                try {
                    competition.getTeamByID(teamID).setTiebreaker(dialog.getValueDouble());
                    competition.sort();
                    tblClassification.setModel(Data.getResultsTableModel(competition));
                } finally {
                    Data.lock().writeLock().unlock();
                }
            }
        });
        
        btnClearTiebreaker.addActionListener((ActionEvent e) -> {
            if(tblClassification.getSelectedRow() < 0) {
                return;
            }
            int teamID = Integer.parseInt((String)tblClassification.getValueAt(
                    tblClassification.getSelectedRow(), 1));
            Team t = competition.getTeamByID(teamID);
            Data.lock().writeLock().lock();
            try {
                t.clearTiebreaker();
            } finally {
                Data.lock().writeLock().unlock();
            }
            updateDataView();
        });
        
        btnSetFlags.addActionListener((ActionEvent e) -> {
            if(tblClassification.getSelectedRow() < 0) {
                return;
            }
            int teamID = Integer.parseInt((String)tblClassification.getValueAt(
                    tblClassification.getSelectedRow(), 1));
            TeamFlagsDialog dialog = new TeamFlagsDialog(competition,
                    competition.getTeamByID(teamID));
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            if(dialog.isApproved()) {
                updateDataView();
            }
        });

        tblClassification = new JTable(Data.getResultsTableModel(competition));
        tblClassification.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        lblStartingRank = new JLabel("Classification Display Ranks : ");
        cmbStartingRank = new JComboBox();
        cmbStartingRank.setPreferredSize(new Dimension(200, 40));
        cmbStartingRank.addItem("1-10");
        cmbStartingRank.addItem("11-20");
        cmbStartingRank.addItem("21-30");
        cmbStartingRank.addItem("31-40");
        cmbStartingRank.addItem("41-50");
        cmbStartingRank.addActionListener((ActionEvent e) -> {
            int rankStart = cmbStartingRank.getSelectedIndex()*10+1;
            ControlCenter.triggerEvent(UserEvent.DISPLAY_RANK_START, rankStart);
            display.setRankStart(rankStart);
        });
        
        JPanel paneDisplayStartingRank = new JPanel();
        //paneDisplayStartingRank.setLayout(new FlowLayout(FlowLayout.LEFT));
        paneDisplayStartingRank.add(lblStartingRank);
        paneDisplayStartingRank.add(cmbStartingRank);
        paneDisplayStartingRank.setPreferredSize(new Dimension(700, 50));

        paneClassification.setLayout(new BorderLayout());
        paneClassification.add(paneClassificationTop, BorderLayout.PAGE_START);
        paneClassification.add(new JScrollPane(tblClassification), BorderLayout.CENTER);
        paneClassification.add(paneDisplayStartingRank, BorderLayout.PAGE_END);
//</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Keybindings Init">
        String keySaveAction = "SAVE_DATA";
        this.getRootPane().getActionMap().put(keySaveAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
            }
        });
        
        String keyLoadAction = "LOAD_DATA";
        this.getRootPane().getActionMap().put(keyLoadAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadData();
            }
        });
        
        String keyAddAction = "ADD_DATA";
        this.getRootPane().getActionMap().put(keyAddAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addScore();
            }
        });
        
        String keyTab1Action = "TAB_1";
        this.getRootPane().getActionMap().put(keyTab1Action, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paneTabbedContainer.setSelectedIndex(0);
            }
        });
        
        String keyTab2Action = "TAB_2";
        this.getRootPane().getActionMap().put(keyTab2Action, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paneTabbedContainer.setSelectedIndex(1);
            }
        });
        
        String keyTab3Action = "TAB_3";
        this.getRootPane().getActionMap().put(keyTab3Action, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paneTabbedContainer.setSelectedIndex(2);
            }
        });
        
        String keyTab4Action = "TAB_4";
        this.getRootPane().getActionMap().put(keyTab4Action, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paneTabbedContainer.setSelectedIndex(3);
            }
        });
        
        String keyOutputLogo = "OUTPUT_LOGO";
        this.getRootPane().getActionMap().put(keyOutputLogo, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmbDisplayMode.setSelectedIndex(0);
            }
        });
        
        String keyOutputRunStatus = "OUTPUT_RUNSTATUS";
        this.getRootPane().getActionMap().put(keyOutputRunStatus, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmbDisplayMode.setSelectedIndex(1);
            }
        });
        
        String keyOutputClassification = "OUTPUT_CLASSIFICATION";
        this.getRootPane().getActionMap().put(keyOutputClassification, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmbDisplayMode.setSelectedIndex(2);
            }
        });
        
        String keyToggleSound = "TOGGLE_SOUND";
        this.getRootPane().getActionMap().put(keyToggleSound, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(menuOptsSounds.isEnabled()) {
                    menuOptsSounds.setSelected(!menuOptsSounds.isSelected());
                    SoundPlayer.setEnabled(menuOptsSounds.isSelected());
                }
            }
        });
        
        String keyChangeFont = "CHANGE_FONT";
        this.getRootPane().getActionMap().put(keyChangeFont, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FontSelectDialog fsd = new FontSelectDialog("Select Display Window Font");
                fsd.setLocationRelativeTo(cc.getControlFrame());
                fsd.setModal(true);
                fsd.showDialog();
                if(fsd.isApproved()) {
                    String fontName = fsd.getFontName();
                    Log.d(0, "Setting font to " + fontName);
                    cc.getDisplayFrame().setFont(fontName);
                }
            }
        });
        
        String keyToggleThumbnail = "TOGGLE_THUMBNAIL";
        this.getRootPane().getActionMap().put(keyToggleThumbnail, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showThumbnailWindow(!cc.getThumbnailFrame().isVisible());
            }
        });
        
        InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), keySaveAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), keyLoadAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), keyAddAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK), keyTab1Action);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK), keyTab2Action);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.CTRL_DOWN_MASK), keyTab3Action);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, InputEvent.CTRL_DOWN_MASK), keyTab4Action);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), keyOutputLogo);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), keyOutputRunStatus);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), keyOutputClassification);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), keyToggleThumbnail);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK), keyToggleSound);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), keyChangeFont);
        //</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Popup Menu Init">
        menuOpts = new JPopupMenu();
        menuOptsFont = new JMenuItem("Select font...");
        menuOptsFont.setMnemonic(KeyEvent.VK_F);
        menuOptsThumbnail = new JCheckBoxMenuItem("Preview window");
        menuOptsThumbnail.setMnemonic(KeyEvent.VK_T);
        menuOptsThumbnail.setSelected(DisplayFrame.GENERATE_THUMBNAIL);        
        menuOptsSounds = new JCheckBoxMenuItem("Play sounds");
        menuOptsBanner = new JCheckBoxMenuItem("Show banner");
        menuOptsBanner.setSelected(DisplayFrame.SHOW_BANNER);
        menuOptsAppearances = new JMenuItem("Adjust appearances...");
        
        if(ControlCenter.SOUND_DISABLED) {
            menuOptsSounds.setEnabled(false);
            menuOptsSounds.setSelected(false);
        } else {
            menuOptsSounds.setSelected(true);
            SoundPlayer.setEnabled(true);
        }
        
        menuOptsFont.addActionListener((ActionEvent e) -> {
            FontSelectDialog fsd = new FontSelectDialog("Select Display Window Font");
            fsd.setLocationRelativeTo(this);
            fsd.setModal(true);
            fsd.showDialog();
            if(fsd.isApproved()) {
                String fontName = fsd.getFontName();
                Log.d(0, "Setting font to " + fontName);
                cc.getDisplayFrame().setFont(fontName);
            }
        });
               
        menuOptsThumbnail.addActionListener((ActionEvent e) -> {
            DisplayFrame.GENERATE_THUMBNAIL = menuOptsThumbnail.isSelected();
            cc.getThumbnailFrame().setVisible(menuOptsThumbnail.isSelected());
        });
        
        menuOptsSounds.addActionListener((ActionEvent e) -> {
            SoundPlayer.setEnabled(menuOptsSounds.isSelected());
        });
        
        menuOptsBanner.addActionListener((ActionEvent e) -> {
            DisplayFrame.SHOW_BANNER = menuOptsBanner.isSelected();
        });
        
        menuOptsAppearances.addActionListener((ActionEvent e) -> {
            if(!cc.getDisplayOptionsFrame().isVisible()) {
                cc.getDisplayOptionsFrame().reset();
                cc.getDisplayOptionsFrame().setVisible(true);
            }
        });
        
        menuOpts.add(menuOptsSounds);
        menuOpts.add(menuOptsFont);
        menuOpts.add(menuOptsThumbnail);
        menuOpts.add(menuOptsBanner);
        menuOpts.add(new JSeparator());
        menuOpts.add(menuOptsAppearances);
        
        //</editor-fold>

        validate();
        ControlCenter.triggerEvent(UserEvent.GUI_INIT, this);
        pack();
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setVisible(true);
    }
    
    public void showThumbnailWindow(boolean b) {
        menuOptsThumbnail.setSelected(b);
        DisplayFrame.GENERATE_THUMBNAIL = b;
        cc.getThumbnailFrame().setVisible(b);
    }
    
    private void populateScoreControl(Container pane, JTextField[] scoreFields, boolean editable) {            
        JLabel[] lblScoreFields = new JLabel[Score.getFields().size()];
        JPanel[] paneScoreFieldContainer = new JPanel[Score.getFields().size()];        
                
        pane.removeAll();

        int i = 0;
        for(String key : Config.getKeysInOriginalOrder("fields")) {
            lblScoreFields[i] = new JLabel(key + " (" +
                    Score.getDescription(key) + ")");
            scoreFields[i] = new JTextField("" + Score.getDefaultValue(key));
            scoreFields[i].setEditable(editable);
            paneScoreFieldContainer[i] = new JPanel();
            paneScoreFieldContainer[i].setLayout(new GridLayout(0, 2));
            JPanel paneLabelAndField = new JPanel();
            paneLabelAndField.setLayout(new GridLayout(0, 2));
            paneLabelAndField.add(lblScoreFields[i]);
            paneLabelAndField.add(scoreFields[i]);
            paneScoreFieldContainer[i].add(paneLabelAndField);
            
            if(Score.getType(key) == 1) {
                JPanel paneButtonsContainer = new JPanel();
                paneButtonsContainer.setLayout(new GridLayout(1, 8, 5, 0));
                JButton add = new JButton("+1");
                JButton undo = new JButton("Undo");
                add.addActionListener(new ScoringUpUndoActionListener(
                        key, i, scoreFields[i], true
                ));
                undo.addActionListener(new ScoringUpUndoActionListener(
                        key, i, scoreFields[i], false
                ));
                                
                paneButtonsContainer.add(add);
                paneButtonsContainer.add(undo);
                paneScoreFieldContainer[i].add(paneButtonsContainer);
            } else if(Score.getType(key) > 1) {
                JPanel panePossibleValues = new JPanel();
                panePossibleValues.setLayout(new GridLayout(1, 8, 5, 0));
                Double[] possibleValues = (Double[]) Score.getPossibleValues(key);
                JButton[] values = new JButton[possibleValues.length];
                int j = 0;
                for(Double val : possibleValues) {
                    values[j] = new JButton("" + val);
                    values[j].addActionListener(new ScoringActionListener(
                            key, i, scoreFields[i], val
                    ));
                    panePossibleValues.add(values[j]);
                    
                    j++;
                }
                paneScoreFieldContainer[i].add(panePossibleValues);
            } else {
                JButton btnEnterValue = new JButton("Enter value");
                btnEnterValue.addActionListener(new ScoringActionListener(
                        key, i, scoreFields[i]
                ));
                paneScoreFieldContainer[i].add(btnEnterValue);
            }
            
            pane.add(paneScoreFieldContainer[i]);
            pane.add(Box.createRigidArea(new Dimension(0, 5)));
            i++;
        }                
        pane.validate();
    }
    
    public static void populateAboutPane(Container paneContainer, boolean aboutOnly) {
        paneContainer.setLayout(new BoxLayout(paneContainer, BoxLayout.PAGE_AXIS));
        JLabel logo = new JLabel();
        logo.setIcon(new ImageIcon(Assets.getMercuryLogo(100)));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Mercury Control Center v" +
                Text.getVersion());
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setBackground(Color.BLACK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setOpaque(true);
        JLabel copyright = new JLabel(Text.AUTHORS);
        copyright.setForeground(new Color(0x20, 0xbb, 0xff));
        copyright.setBackground(Color.BLACK);
        copyright.setOpaque(true);
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setText(Text.LICENSE);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        text.setAutoscrolls(true);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollText = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JComboBox cmbTextSelect = new JComboBox();
        cmbTextSelect.addItem("License");
        cmbTextSelect.addItem("3rd Party");
        cmbTextSelect.addItem("Apache License 2.0");
        cmbTextSelect.addItem("Configuration File Format");
        cmbTextSelect.addItem("Localization Information");
        if(!aboutOnly) {
            cmbTextSelect.addItem("Current Configuration");
            cmbTextSelect.addItem("Loaded Resources");
        }
        
        cmbTextSelect.setMaximumSize(new Dimension(600, 50));
        //cmbTextSelect.setMinimumSize(new Dimension(600, 50));
        //cmbTextSelect.setPreferredSize(new Dimension(600, 50));
        
        cmbTextSelect.addActionListener((ActionEvent e) -> {
            switch(cmbTextSelect.getSelectedIndex()) {
                case 0:
                    text.setText(Text.LICENSE);
                    break;
                case 1:
                    text.setText(Text.THIRD_PARTY);
                    break;
                case 2:
                    text.setText(Text.getApache2License());
                    break;
                case 3:
                    text.setText(Text.getConfigFileSpecs());
                    break;      
                case 4:
                    text.setText(Text.getLocalizationInformation());
                    break;     
                case 5:
                    text.setText(Config.getConfigString());
                    break;
                case 6:
                    text.setText(Assets.getAssetInfo());
                    break;
            }
            text.setCaretPosition(0);
        });
        
        text.setMargin(new Insets(15, 15, 15, 15));
        text.setOpaque(true);
        text.setBackground(Color.BLACK);
        text.setForeground(new Color(0x20, 0xbb, 0xff));
                
        paneContainer.setBackground(Color.BLACK);
        paneContainer.add(Box.createRigidArea(new Dimension(5,5)));
        paneContainer.add(logo);
        paneContainer.add(Box.createRigidArea(new Dimension(5,5)));
        paneContainer.add(title);
        paneContainer.add(copyright);
        paneContainer.add(Box.createRigidArea(new Dimension(5,15)));
        paneContainer.add(cmbTextSelect);
        paneContainer.add(Box.createRigidArea(new Dimension(5,15)));       
        paneContainer.add(scrollText); 
    }
    
    public long getRenderTime() {
        return indicators.renderTime;
    }

    public void setControlPhase(int phase) {
        Log.d(0, "ControlFrame: entering phase " + phase);
        switch (phase) {
            case CompetitionState.IDLE:
                setTitle("Mercury Control Center (" + Config.getConfigFile().getName() + ")");
                if(timer != null) {
                    timer.stopTimer();
                    timer = null;
                }
                // enable team selection controls
                indicators.set(null);
                cmbTeamSelect.setEnabled(true);
                btnStartTeamSession.setText("Start Session");
                paneRunScoringControlScroll.setVisible(false);
                paneRunTimerControl.setVisible(false);
                tglRedFlag.setSelected(false);
                competition.setRedFlag(false);
                btnStartTeamSession.setText("START SCORING SESSION");
                btnStartTeamSession.setForeground(Color.BLACK);
                break;

            case CompetitionState.SETUP:   
                setTitle("Mercury Control Center (" + Config.getConfigFile().getName() + ") - SETUP");
                // disable team selection controls
                cmbTeamSelect.setEnabled(false);
                // enable timing controls
                paneRunTimerControl.setVisible(true);
                btnSkipSetup.setEnabled(true);
                btnPause.setEnabled(true);
                btnPause.setText("PAUSE");
                btnCommitScore.setEnabled(false);
                btnDiscardScore.setEnabled(false);
                btnAddTime.setEnabled(true);
                btnStartTeamSession.setText("END SCORING SESSION");
                btnStartTeamSession.setForeground(Color.RED);
                
                if(timer != null) {
                    timer.stopTimer();
                }
                Team t = competition.getTeamByID(getSelectedTeamID());
                competition.newSession(t,
                        maxAttempts, setupDuration*1000, windowDuration*1000);
                timer = new SessionTimer(competition);
                indicators.set(competition.getSession());
                competition.getSession().start();
                timer.start();                
                SoundPlayer.play("setup-start.wav");
                break;
                
            case CompetitionState.RUN:
                setTitle("Mercury Control Center (" + Config.getConfigFile().getName() + ") - RUNNING");
                btnSkipSetup.setEnabled(false);
                btnCommitScore.setEnabled(true);
                btnDiscardScore.setEnabled(true);
                btnPause.setText("PAUSE");
                
                // re-init and enable scoring controls                
                paneRunScoringControlScroll.setVisible(true);
                newScore();
                validate();
                competition.getSession().endSetup();
                SoundPlayer.play("window-start.wav");
                break;
                
            case CompetitionState.POST_RUN:
                setTitle("Mercury Control Center (" + Config.getConfigFile().getName() + ") - POST RUN");
                cmbTeamSelect.setEnabled(true);
                competition.getSession().end();
                btnPause.setEnabled(false);
                btnDiscardScore.setEnabled(false);
                btnAddTime.setEnabled(false);
                if(timer != null) {
                    timer.stopTimer();
                    timer = null;
                }
                break;
            default:
                break;
        }
        repaintDisplay();
    }
    
    public void repaintDisplay() {
        if(indicators != null) {
            indicators.repaint();
            //indicators.validate();
        }
    }
    
    public JPanel getGlobalControlsPane() {
        return paneGlobal;
    }
    
    public void exit() {
        // warn first!
        if(!confirmYesNo("UNSAVED DATA WILL BE LOST! "
                + "Are you sure?", 
                "Exit Confirmation")) {
            return;
        }
        
        ControlCenter.triggerEvent(UserEvent.EXIT, null);
        
        if(timer != null) {
            // warn of current run
            timer.stopTimer();
        }
        
        ControlCenter.exit(0);
    }
    
    public final void refreshDisplayList() {
        cmbDisplayScreen.removeAllItems();
        cmbDisplayScreen.addItem("Hide");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        for(int i = 0; i < gd.length; i++) {
            cmbDisplayScreen.addItem(gd[i].getIDstring() + " (" +
                    gd[i].getDisplayMode().getWidth() + "x" +
                    gd[i].getDisplayMode().getHeight() + ")");
        }
    }
    
    public int getSelectedTeamID() {
        return Integer.parseInt(
                ((String)cmbTeamSelect.getSelectedItem()).split(":")[0]
        );
    }
    
    public void setSelectedTeamID(int teamID) {
        for(int i = 0; i < cmbTeamSelect.getItemCount(); i++) {
            String item = (String) cmbTeamSelect.getItemAt(i);
            int id = Integer.parseInt(item.split(":")[0]);
            if(id == teamID) {
                cmbTeamSelect.setSelectedIndex(i);
                break;
            }
        }
    }
    
    public void setRunParameters(int attempts, int setup, int window) {
        maxAttempts = attempts;
        setupDuration = setup;
        windowDuration = window;
    }
    
    public boolean populateScore(Score s, JTextField[] fields) {
        int i = 0;
        String[] keys = Config.getKeysInOriginalOrder("fields").toArray(new String[0]);
        if(fields.length != keys.length) {
            return false;
        }
        for(String key : keys) {
            try {
                if(s == null) {
                    competition.getSession().modifyCurrentScore(
                            key, Double.parseDouble(fields[i].getText()));
                } else {
                    s.setValue(key, Double.parseDouble(fields[i].getText()));
                }
            } catch(NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this,
                        "Failed to parse score for " + key + "\n" +
                                "Offending value: " + fields[i].getText(),
                        "Score Commit Failed", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            i++;
        }      
        return true;
    }
    
    public void addScore() {
        JDialog d = new JDialog();
        d.setModal(true);
        d.setSize(700, 600);
        d.setTitle("Add Score Entry");
        Container pane = d.getContentPane();
        JPanel paneButtons = new JPanel();
        JPanel paneScoreInput = new JPanel();
        JComboBox<String> cmbTeams = new JComboBox();
        for(Team team : competition.getTeams()) {
            cmbTeams.addItem(team.getNumber() + ": " + team.getName());
        }
        cmbTeams.setSelectedIndex(0);
        JButton btnCommit = new JButton("Commit");
        JButton btnCancel = new JButton("Cancel");
        paneButtons.add(btnCommit);
        paneButtons.add(btnCancel);
        paneScoreInput.setLayout(new BoxLayout(
                paneScoreInput, BoxLayout.PAGE_AXIS));
        paneScoreInput.setAutoscrolls(true);
        JScrollPane paneScoreInputScroll;
        paneScoreInputScroll = new JScrollPane(paneScoreInput,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        JTextField[] fields = new JTextField[Config.getSection("fields").length];
        populateScoreControl(paneScoreInput, fields, true);
        
        btnCancel.addActionListener((ActionEvent e) -> {
            d.dispose();
        });
        
        btnCommit.addActionListener((ActionEvent e) -> {
            Score s = new Score();
            if(!populateScore(s, fields)) {
                return;
            }
            s.setCompleted(true);
            Team t = competition.getTeamByID(
                    Integer.parseInt(
                    ((String)cmbTeams.getSelectedItem()).split(":")[0])
            );
            Data.lock().writeLock().lock();
            try {
                t.addScore(s);
            } finally {
                Data.lock().writeLock().unlock();
            }
            Object[] params = { t.getNumber(), t.getScores().size()-1 };
            ControlCenter.triggerEvent(UserEvent.DATA_ADDED, params);
            updateDataView();
            d.dispose();
        });
        
        pane.add(cmbTeams, BorderLayout.PAGE_START);
        pane.add(paneScoreInputScroll, BorderLayout.CENTER);
        pane.add(paneButtons, BorderLayout.PAGE_END);
        
        String keyEscape = "ESCAPE";
        d.getRootPane().getActionMap().put(keyEscape, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        });
        
        InputMap im = d.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keyEscape);
        
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    public void commitScore() {
        SessionState session = competition.getSession();
        // populate session's current score from score fields' values
        if(!populateScore(null, txtScoreFields)) {
            return;
        }        
        
        if(competition.getState() == CompetitionState.RUN) {
            if(session.getRunNumber() <= maxAttempts) {
                session.completeRun(true);
                if(session.getRunNumber() <= maxAttempts) {
                    newScore();
                } else { // we've reached maximum attempts, end session
                    competition.setState(CompetitionState.POST_RUN);
                    btnCommitScore.setEnabled(false);
                    btnDiscardScore.setEnabled(false);
                }                
            }
        } else if(competition.getState() == CompetitionState.POST_RUN) {
            if(session.getRunNumber() <= maxAttempts) {
                session.completeRun(true);
            }
            btnCommitScore.setEnabled(false);
            btnDiscardScore.setEnabled(false);
        }
        
        updateDataView();     
    }
    
    public void discardScore() {
        if(competition.getState() == CompetitionState.RUN) {
            competition.getSession().completeRun(false);
            if(competition.getSession().getRunNumber() <= maxAttempts) {
                newScore();
            } else {
                competition.setState(CompetitionState.POST_RUN);
                btnCommitScore.setEnabled(false);
                btnDiscardScore.setEnabled(false);
            }            
        }
    }
    
    public void newScore() {
        txtScoreFields = new JTextField[Score.getFields().size()];
        populateScoreControl(paneRunScoringControl, txtScoreFields, false);        
        cc.getDisplayFrame().newScore();
    }
    
    public void setCurrentScore(String key, double value) {
        int state = competition.getState();
        if(state != CompetitionState.RUN && state != CompetitionState.POST_RUN) {
            return;
        }
        int fieldID = Score.getFieldID(key);
        if(fieldID >= 0) {
            txtScoreFields[fieldID].setText(String.valueOf(value));
            ControlCenter.triggerScoreChangeEvent(key, fieldID, String.valueOf(value));
        }
    }
    
    private void updateDataView() {
        tblData.setModel(Data.getTableModel(competition));
        tblData.validate();
        tblClassification.setModel(Data.getResultsTableModel(competition));
        tblClassification.validate();
        competition.sort();
        display.setClassificationData(competition.getSortedClassifiedTeams());          
    }
    
    public void refreshDataView() {
        SwingUtilities.invokeLater(() -> {
            updateDataView();
        });
    }
    
    private void outputDisplayToScreen() {                
        if(cmbDisplayScreen.getSelectedIndex() <= 0) {
            ControlCenter.triggerEvent(UserEvent.DISPLAY_HIDE, null);
            display.setVisible(false);
            return;
        }
        ControlCenter.triggerEvent(UserEvent.DISPLAY_SHOW, null);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getScreenDevices()[cmbDisplayScreen.getSelectedIndex()-1];
        display.setVisible(false);       
        
        //gd.setFullScreenWindow(displayFrame);
        display.setLocation(gd.getDefaultConfiguration().getBounds().x, gd.getDefaultConfiguration().getBounds().y);
        display.setExtendedState(JFrame.MAXIMIZED_BOTH);
        if(!display.isDisplayable()) {
            display.setUndecorated(true);
        }
        display.setVisible(true);
    }
    
    private void saveData() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new java.io.File("."));
        fc.setDialogTitle("Save Data as CSV");
        fc.setFileFilter(
                new FileNameExtensionFilter("Saved Mercury Data (.csv)", "csv")
        );
        fc.setCurrentDirectory(Data.getDataWorkDir());
        if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if(fc.getSelectedFile().exists()) {
                if(!confirmYesNo(fc.getSelectedFile().getName() + " exists." +
                        " Overwrite?", "File Exists")) {
                    return;
                }
            }
            Data.saveAsCSV(competition, fc.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void loadData() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new java.io.File("."));
        fc.setDialogTitle("Load CSV Data");
        fc.setFileFilter(
                new FileNameExtensionFilter("Saved Mercury Data (.csv)", "csv")
        );
        fc.setCurrentDirectory(Data.getDataWorkDir());
        if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Data.loadCSV(competition, fc.getSelectedFile().getAbsolutePath());
        }
        updateDataView();
    }
    
    private void editData() {
        int col = tblData.getSelectedColumn();
        if(col-3 < 0 || col-3 >= Score.getFields().size()) {
            return;
        }
        Data.lock().writeLock().lock();
        try {
            DefaultTableModel m = (DefaultTableModel) tblData.getModel();
            int row = tblData.getSelectedRow();
            int column = tblData.getSelectedColumn();
            double initialValue = Double.parseDouble((String)m.getValueAt(row, column));
            int teamID = Integer.parseInt((String)m.getValueAt(row, 0));
            int scoreID = Integer.parseInt((String)m.getValueAt(row, 2));
            Team t = competition.getTeamByID(teamID);
            String field = Config.getKeysInOriginalOrder("fields").get(column-3);

            NumberInputDialog dialog = new NumberInputDialog("Edit " + field +
                    " for " + t.getName(), 
                    initialValue, NumberInputDialog.FLOAT);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.showDialog();

            if(dialog.isApproved()) {
                Score s = t.getScores().get(scoreID);
                s.setValue(field, dialog.getValueDouble());
                m.setValueAt(dialog.getValueDouble() + "", row, column);
                m.setValueAt(s.getScore() + "", row, 3+Score.getFields().size());
                Object[] params = {teamID, scoreID, field, dialog.getValueDouble()};
                ControlCenter.triggerEvent(UserEvent.DATA_CHANGED, params);
                updateDataView();
            }
        } finally {
            Data.lock().writeLock().unlock();
        }
    }
    
    private boolean confirmYesNo(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    
    private boolean confirmOKCancel(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }    
    
    class ScoringUpUndoActionListener implements ActionListener {
        private JTextField target;
        private boolean increment;
        private int id;
        private String key;
        
        public ScoringUpUndoActionListener(String key, int id, JTextField target, boolean increment) {
            this.target = target;
            this.increment = increment;
            this.id = id;
            this.key = key;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            double curVal = 0;
            try {
                curVal = Double.parseDouble(target.getText());
            } catch(NumberFormatException nfe) {
                JOptionPane.showMessageDialog(target,
                        "Failed to parse score for " + key + "\n" +
                                "Offending value: " + target.getText(),
                        "Score Edit Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(increment) {
                curVal++;
            } else {
                if(curVal == 0)
                    return;
                curVal--;
            }
            target.setText("" + curVal);
            if(competition.getSession() != null) {
                competition.getSession().modifyCurrentScore(key, curVal);
            }
            ControlCenter.triggerScoreChangeEvent(key, id, String.valueOf(curVal));
        }
    }
    
    class ScoringActionListener implements ActionListener {
        private JTextField target;
        private double value;
        private int id;
        private boolean needDialog;
        private String key;
        
        public ScoringActionListener(String key, int id, JTextField target, double value) {
            this.target = target;
            this.value = value;
            this.key = key;
            this.needDialog = false;
            this.id = id;
        }
        
        public ScoringActionListener(String key, int id, JTextField target) {
            this.target = target;
            this.key = key;
            this.needDialog = true;
            this.id = id;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(needDialog) {
                double originalValue;
                try {
                    originalValue = Double.parseDouble(target.getText());
                } catch(NumberFormatException nfe) {
                    System.err.println("ScoringActionListener: old value parse error");
                    return;
                }

                NumberInputDialog dialog = new NumberInputDialog(
                        "Enter value for " + key,
                        originalValue,
                        NumberInputDialog.FLOAT
                ); 
                dialog.setModal(true);
                dialog.setLocationRelativeTo(cc.getControlFrame());
                dialog.showDialog();
                if(!dialog.isApproved()) {
                    return;
                }
                value = dialog.getValueDouble();
            }
            target.setText("" + value);
            if(competition.getSession() != null) {
                competition.getSession().modifyCurrentScore(key, value);
            }
            ControlCenter.triggerScoreChangeEvent(key, id, String.valueOf(value));
        }
    }
}
