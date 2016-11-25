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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.ArrayList;
import org.osumercury.controlcenter.*;

/**
 *
 * @author wira
 */
public class ControlFrame extends JFrame {
    private CompetitionState competition;
    private ControlCenter cc;
    private SessionTimer timer;
    private Score activeScore;
    private DisplayFrame display;
    private ArrayList<ScoreChangedCallback> scoreChangedHooks;
    private ArrayList<UserEvent> userEventHooks;
        
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
    
    /** GLOBAL UI ELEMENTS **/
    private JLabel lblDisplayScreen;
    private JComboBox cmbDisplayScreen;
    private JLabel lblDisplayMode;
    private JComboBox cmbDisplayMode;
    private JButton btnRefreshScreens;
    private JButton btnExit;
    private JCheckBox chkPlaySounds;
    
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
        competition.addStateChangeHook((CompetitionState c) -> {
            setControlPhase(c.getState());
        });        
        scoreChangedHooks = new ArrayList();
        userEventHooks = new ArrayList();
    }
    
    public void init() {
        Log.d(0, "ControlFrame: init");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        setTitle("Mercury Control Center (" + Config.CONFIG_FILE.getName() + ")");
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
        chkPlaySounds = new JCheckBox("Sounds");
        chkPlaySounds.setSelected(true);
        SoundPlayer.setEnabled(true);
        paneGlobal.add(lblDisplayScreen);
        paneGlobal.add(cmbDisplayScreen);
        paneGlobal.add(btnRefreshScreens);
        paneGlobal.add(new JSeparator());
        paneGlobal.add(lblDisplayMode);
        paneGlobal.add(cmbDisplayMode);
        paneGlobal.add(chkPlaySounds);
        paneGlobal.add(new JSeparator());
        paneGlobal.add(btnExit);
        if(Config.SOUND_DISABLED) {
            chkPlaySounds.setEnabled(false);
            chkPlaySounds.setSelected(false);
        }
        
        btnExit.addActionListener((ActionEvent e) -> {
            exit();
        });
        
        btnRefreshScreens.addActionListener((ActionEvent e) -> { refreshDisplayList(); } );
        cmbDisplayMode.addActionListener((ActionEvent e) -> {
            display.setMode(cmbDisplayMode.getSelectedIndex());
        });
        
        cmbDisplayScreen.addActionListener((ActionEvent e) -> {            
            outputDisplayToScreen();
        });
        
        chkPlaySounds.addActionListener((ActionEvent e) -> {
            SoundPlayer.setEnabled(chkPlaySounds.isSelected());
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
        Team team;
        for(String key : Config.getKeysInOriginalOrder("teams")) {
            team = competition.getTeamByID(Integer.parseInt(key));
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

        btnStartTeamSession.addActionListener((ActionEvent e) -> {
            if(competition.getState() == CompetitionState.IDLE) {
                btnStartTeamSession.setText("END SCORING SESSION");
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
                btnStartTeamSession.setForeground(Color.RED);
            } else {
                // warn first!
                if(!confirmYesNo("This will discard the current run and end"
                        + " the session for the team. Are you sure?",
                        "End Session Confirmation")) {
                    return;
                }

                competition.setState(CompetitionState.IDLE);
                btnStartTeamSession.setText("START SCORING SESSION");
                btnStartTeamSession.setForeground(Color.BLACK);
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
                triggerEvent(UserEvent.SESSION_RESUMED);
                btnPause.setText("PAUSE");
            } else {
                competition.getSession().pauseTimer();
                triggerEvent(UserEvent.SESSION_PAUSED);
                btnPause.setText("RESUME");
            }
        });

        btnSkipSetup.addActionListener((ActionEvent e) -> {
            if(competition.getState() != CompetitionState.SETUP) {
                return;
            }
            
            if(!confirmYesNo("Skip setup and start scoring window?",
                    "Skip Setup")) {
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
                triggerEvent(UserEvent.SESSION_TIME_ADDED, dialog.getValueInt());
            }
        });
        
        tglRedFlag.addActionListener((ActionEvent e) -> {
            triggerEvent(tglRedFlag.isSelected() ?
                    UserEvent.SESSION_REDFLAGGED : UserEvent.SESSION_GREENFLAGGED);
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
                    Data.lock.writeLock().lock();
                    try {
                        competition.getTeamByID(teamID).getScores().remove(score);
                    } finally {
                        Data.lock.writeLock().unlock();
                    }
                    Object[] params = {teamID, score};
                    triggerEvent(UserEvent.DATA_RECORD_EXPUNGED, params);
                    updateDataView();                  
                }
            }
        });

        btnDataClear.addActionListener((ActionEvent e) -> {
            if(confirmYesNo("This will delete ALL recorded scores. ARE YOU SURE?!",
                    "Delete All Data")) {
                Data.lock.writeLock().lock();
                try {
                    for(Team t : competition.getTeams()) {
                        t.getScores().clear();
                    }
                } finally {
                        Data.lock.writeLock().unlock();
                }
                triggerEvent(UserEvent.DATA_CLEARED);
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
        btnSetTiebreaker = new JButton("Set Tiebreaker");
        paneClassificationTop.add(btnGenerateReport);
        paneClassificationTop.add(btnSetTiebreaker);

        btnGenerateReport.addActionListener((ActionEvent e) -> {
            competition.sort();
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new java.io.File("."));
            fc.setDialogTitle("Generate Competition Report");
            fc.setFileFilter(
                    new FileNameExtensionFilter("Mercury Report (.txt)", "txt")
            );
            if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
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
                            t.getName(), t.getTiebreaker(), NumberInputDialog.INTEGER);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            if(dialog.isApproved()) {
                competition.getTeamByID(teamID).setTiebreaker(dialog.getValueInt());
            }
            competition.sort();
            tblClassification.setModel(Data.getResultsTableModel(competition));
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
            display.setRankStart(cmbStartingRank.getSelectedIndex()*10+1);
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
                if(chkPlaySounds.isEnabled()) {
                    chkPlaySounds.setSelected(!chkPlaySounds.isSelected());
                    SoundPlayer.setEnabled(chkPlaySounds.isSelected());
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
                cc.getDisplayFrame().showThumbnailWindow(!cc.getThumbnailFrame().isVisible());
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

        validate();
        triggerEvent(UserEvent.GUI_INIT, this);
        pack();
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        setVisible(true);
    }
    
    private void populateScoreControl(Container pane, JTextField[] scoreFields, boolean editable) {            
        JLabel[] lblScoreFields = new JLabel[Score.fields.size()];
        JPanel[] paneScoreFieldContainer = new JPanel[Score.fields.size()];        
                
        pane.removeAll();

        int i = 0;
        for(String key : Config.getKeysInOriginalOrder("fields")) {
            lblScoreFields[i] = new JLabel(key + " " +
                    Score.description.get(key));
            scoreFields[i] = new JTextField("" + Score.defaultValue.get(key));
            scoreFields[i].setEditable(editable);
            paneScoreFieldContainer[i] = new JPanel();
            paneScoreFieldContainer[i].setLayout(new GridLayout(0, 2));
            JPanel paneLabelAndField = new JPanel();
            paneLabelAndField.setLayout(new GridLayout(0, 2));
            paneLabelAndField.add(lblScoreFields[i]);
            paneLabelAndField.add(scoreFields[i]);
            paneScoreFieldContainer[i].add(paneLabelAndField);
            
            if(Score.type.get(key) == 1) {
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
            } else if(Score.type.get(key) > 1) {
                JPanel panePossibleValues = new JPanel();
                panePossibleValues.setLayout(new GridLayout(1, 8, 5, 0));
                Double[] possibleValues = (Double[]) Score.possibleValues.get(key);
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
    
    public static void populateAboutPane(Container paneContainer, boolean exitOption) {
        paneContainer.setLayout(new BoxLayout(paneContainer, BoxLayout.PAGE_AXIS));
        JLabel logo = new JLabel();
        logo.setIcon(new ImageIcon(Assets.getMercuryLogo(100)));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Mercury Control Center v" +
                Text.MAJOR_VERSION + "." + Text.MINOR_VERSION + "." + Text.MINOR_MINOR_VERSION);
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
        cmbTextSelect.addItem("Configuration File Format");
        cmbTextSelect.addItem("Current Configuration");
        cmbTextSelect.addItem("Apache License 2.0");        
        cmbTextSelect.addItem("Loaded Resources");
        
        if(exitOption) {
            cmbTextSelect.addItem("Exit");
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
                    text.setText(Text.getConfigFileSpecs());
                    break;
                case 3:
                    text.setText(Config.CONFIG_STRING);
                    break;                    
                case 4:
                    text.setText(Text.getApache2License());
                    break;
                case 5:
                    text.setText(Assets.getAssetInfo());
                    break;
                case 6:
                    System.exit(0);
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
                setTitle("Mercury Control Center (" + Config.CONFIG_FILE.getName() + ")");
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

                break;

            case CompetitionState.SETUP:   
                setTitle("Mercury Control Center (" + Config.CONFIG_FILE.getName() + ") - SETUP");
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
                
                if(timer != null) {
                    timer.stopTimer();
                }
                Team t = competition.getTeamByID(cmbTeamSelect.getSelectedIndex());
                competition.newSession(t,
                        maxAttempts, setupDuration*1000, windowDuration*1000);
                timer = new SessionTimer(competition);
                indicators.set(competition.getSession());
                competition.getSession().start();
                timer.start();
                SoundPlayer.play("setup-start.wav");
                break;
                
            case CompetitionState.RUN:
                setTitle("Mercury Control Center (" + Config.CONFIG_FILE.getName() + ") - RUNNING");
                btnSkipSetup.setEnabled(false);
                btnCommitScore.setEnabled(true);
                btnDiscardScore.setEnabled(true);
                btnPause.setText("PAUSE");
                
                // re-init and enable scoring controls                
                paneRunScoringControlScroll.setVisible(true);
                newScore();
                validate();
                competition.getSession().advance();
                SoundPlayer.play("window-start.wav");
                break;
                
            case CompetitionState.POST_RUN:
                setTitle("Mercury Control Center (" + Config.CONFIG_FILE.getName() + ") - POST RUN");
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
        triggerEvent(phase);
        repaintDisplay();
    }
    
    public void repaintDisplay() {
        if(indicators != null) {
            indicators.repaint();
            //indicators.validate();
        }
    }
    
    public void exit() {
        // warn first!
        if(!confirmYesNo("UNSAVED DATA WILL BE LOST! "
                + "Are you sure?", 
                "Exit Confirmation")) {
            return;
        }
        
        triggerEvent(UserEvent.EXIT);
        
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
    
    public int getTeamSelectIndex() {
        return cmbTeamSelect.getSelectedIndex();
    }
    
    public boolean populateScore(Score s, JTextField[] fields) {
        int i = 0;
        String[] keys = Config.getKeysInOriginalOrder("fields").toArray(new String[0]);
        if(fields.length != keys.length) {
            return false;
        }
        for(String key : keys) {
            try {
                s.setValue(key, Double.parseDouble(fields[i].getText()));
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
        Team team;
        for(String key : Config.getKeysInOriginalOrder("teams")) {
            team = competition.getTeamByID(Integer.parseInt(key));
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
            Team t = competition.getTeamByID(cmbTeams.getSelectedIndex());
            Data.lock.writeLock().lock();
            try {
                t.addScore(s);
            } finally {
                Data.lock.writeLock().unlock();
            }
            Object[] params = { t.getNumber(), t.getScores().size()-1 };
            triggerEvent(UserEvent.DATA_ADDED, params);
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

    private void commitScore() {
        // populate activeScore fields here
        if(!populateScore(activeScore, txtScoreFields)) {
            return;
        }
        
        /*
        int i = 0;
        for(String key : Config.getKeysInOriginalOrder("fields")) {
            try {
                activeScore.setValue(key, Double.parseDouble(txtScoreFields[i].getText()));
            } catch(NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this,
                        "Failed to parse score for " + key + "\n" +
                                "Offending value: " + txtScoreFields[i].getText(),
                        "Score Commit Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            i++;
        }
        */
        
        SessionState session = competition.getSession();
        
        if(competition.getState() == CompetitionState.RUN) {
            if(session.getRunNumber() <= maxAttempts) {
                activeScore.setCompleted(true);
                Data.lock.writeLock().lock();
                try {
                    session.getActiveTeam().addScore(activeScore);
                    session.getActiveScoreList().add(activeScore);
                } finally {
                    Data.lock.writeLock().unlock();
                }
                triggerEvent(UserEvent.SESSION_ATTEMPT_COMMITTED, session.getRunNumber());
                if(session.getRunNumber() < maxAttempts) {
                    session.advance();
                    newScore();
                } else { // we've reached maximum attempts, end session
                    competition.setState(CompetitionState.POST_RUN);
                    btnCommitScore.setEnabled(false);
                    btnDiscardScore.setEnabled(false);
                }                
            }
        } else if(competition.getState() == CompetitionState.POST_RUN) {
            if(session.getRunNumber() <= maxAttempts) {
                activeScore.setCompleted(true);
                Data.lock.writeLock().lock();
                try {
                    session.getActiveTeam().addScore(activeScore);
                    session.getActiveScoreList().add(activeScore);
                } finally {
                    Data.lock.writeLock().unlock();
                }

                triggerEvent(UserEvent.SESSION_ATTEMPT_COMMITTED, session.getRunNumber());
            }
            btnCommitScore.setEnabled(false);
            btnDiscardScore.setEnabled(false);
        }
        
        updateDataView();     
    }
    
    private void discardScore() {
        if(competition.getState() == CompetitionState.RUN) {
            triggerEvent(UserEvent.SESSION_ATTEMPT_DISCARDED, 
                        competition.getSession().getRunNumber());
            if(competition.getSession().getRunNumber() < maxAttempts) {
                competition.getSession().getActiveScoreList().add(null);
                competition.getSession().advance();
                newScore();
            } else {
                competition.setState(CompetitionState.POST_RUN);
                btnCommitScore.setEnabled(false);
                btnDiscardScore.setEnabled(false);
            }
        }
    }
    
    private void newScore() {
        activeScore = new Score();
        txtScoreFields = new JTextField[Score.fields.size()];
        populateScoreControl(paneRunScoringControl, txtScoreFields, false);        
        cc.getDisplayFrame().newScore();
    }
    
    public void updateDataView() {
        tblData.setModel(Data.getTableModel(competition));
        tblData.validate();
        tblClassification.setModel(Data.getResultsTableModel(competition));
        tblClassification.validate();
        competition.sort();
        display.setClassificationData(competition.getSortedFinishedTeams());          
    }
    
    private void outputDisplayToScreen() {                
        if(cmbDisplayScreen.getSelectedIndex() <= 0) {
            display.setVisible(false);
            return;
        }
        
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
        fc.setCurrentDirectory(Data.dataWorkDir);
        if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
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
        fc.setCurrentDirectory(Data.dataWorkDir);
        if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            Data.loadCSV(competition, fc.getSelectedFile().getAbsolutePath());
            triggerEvent(UserEvent.DATA_IMPORTED, Data.getData(competition));
        }
        updateDataView();
    }
    
    private void editData() {
        int col = tblData.getSelectedColumn();
        if(col-3 < 0 || col-3 >= Score.fields.size()) {
            return;
        }
        Data.lock.writeLock().lock();
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
                m.setValueAt(Score.calculate(s) + "", row, 3+Score.fields.size());
                Object[] params = {teamID, scoreID, field, dialog.getValueDouble()};
                triggerEvent(UserEvent.DATA_CHANGED, params);
                updateDataView();
            }
        } finally {
            Data.lock.writeLock().unlock();
        }
    }
    
    private void triggerEvent(int id, Object param) {
        Log.d(2, "ControlFrame.triggerEvent: " + id);
        for(UserEvent ue : userEventHooks) {
            ue.callback(id, param);
        }
    }
    
    private void triggerEvent(int id) {
        Log.d(2, "ControlFrame.triggerEvent: " + id);
        for(UserEvent ue : userEventHooks) {
            ue.callback(id, null);
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
    
    public void addScoreChangedHook(ScoreChangedCallback c) {
        scoreChangedHooks.add(c);
    }
    
    public void removeScoreChangedCallback(ScoreChangedCallback c) {
        scoreChangedHooks.remove(c);
    }
    
    public void addUserEventHook(UserEvent c) {
        userEventHooks.add(c);
    }
    
    public void removeUserEventHook(UserEvent c) {
        userEventHooks.remove(c);
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
            double curVal = Double.parseDouble(target.getText());
            if(increment) {
                curVal++;
            } else {
                if(curVal == 0)
                    return;
                curVal--;
            }
            target.setText("" + curVal);
            
            for(ScoreChangedCallback c : scoreChangedHooks) {
                c.callback(key, id, "" + curVal);
            }
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
            for(ScoreChangedCallback c : scoreChangedHooks) {
                c.callback(key, id, "" + value);
            }
        }
    }
}
