/*
    Copyright 2017 Wira Mulia

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import org.osumercury.controlcenter.ControlCenter;
import org.osumercury.controlcenter.Log;

/**
 *
 * @author wira
 */
public class DisplayOptionsFrame extends JFrame {
    private ControlCenter cc;
    private JTabbedPane tabs;
    
    private JPanel mainPaneTheme;
    private JPanel paneThemeButtons;
    private JButton btnThemeApply;
    private JTextArea txtTheme;
    
    private JPanel mainPaneDimensions;
    private JPanel paneButtons;
    private JPanel paneControls;
    private JScrollPane scrollPaneControls;
    private JButton btnApply;
    private JButton btnClose;
    private JButton btnReset;
    private JButton btnDefaults;
    private JLabel lblLogoH;
    private JLabel lblLogoY;
    private JLabel lblDigitsH;
    private JLabel lblDigitsSmallH;
    private JLabel lblTextH;
    private JLabel lblSpacingUnit;
    private JLabel lblPauseBarH;
    private JLabel lblTimeBarH;
    private JLabel lblHorizBarH;
    private JLabel lblClockMargin;
    private JSlider slideLogoH;
    private JSlider slideLogoY;
    private JSlider slideDigitsH;
    private JSlider slideDigitsSmallH;
    private JSlider slideTextH;
    private JSlider slideSpacingUnit;
    private JSlider slidePauseBarH;
    private JSlider slideTimeBarH;
    private JSlider slideHorizBarH;
    private JSlider slideClockMargin;
    private JTextField txtLogoH;
    private JTextField txtLogoY;
    private JTextField txtDigitsH;
    private JTextField txtDigitsSmallH;
    private JTextField txtTextH;
    private JTextField txtSpacingUnit;
    private JTextField txtPauseBarH;
    private JTextField txtTimeBarH;
    private JTextField txtHorizBarH;
    private JTextField txtClockMargin;
    private JPanel paneLogoH;
    private JPanel paneLogoY;
    private JPanel paneDigitsH;
    private JPanel paneDigitsSmallH;
    private JPanel paneTextH;
    private JPanel paneSpacingUnit;
    private JPanel panePauseBarH;
    private JPanel paneTimeBarH;
    private JPanel paneHorizBarH;
    private JPanel paneClockMargin;
    
    public DisplayOptionsFrame(ControlCenter cc) {
        this.cc = cc;
    }
    
    public void init() {
        setTitle("Adjust Appearances");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        tabs = new JTabbedPane();
        mainPaneDimensions = new JPanel();
        mainPaneDimensions.setName("Dimensions");
        mainPaneDimensions.setLayout(new BorderLayout());
        
        paneButtons = new JPanel();
        btnApply = new JButton("Apply");
        btnClose = new JButton("Close");
        btnReset = new JButton("Reset");
        btnDefaults = new JButton("Defaults");
        btnApply.addActionListener((ActionEvent e) -> {
            apply();
        });
        btnReset.addActionListener((ActionEvent e) -> {
            reset();
        });
        btnDefaults.addActionListener((ActionEvent e) -> {
            defaults();
        });
        btnClose.addActionListener((ActionEvent e) -> {
            dispose();
        });
        paneButtons.add(btnReset);
        paneButtons.add(btnDefaults);
        paneButtons.add(btnApply);
        mainPaneDimensions.add(paneButtons, BorderLayout.PAGE_END);
        
        paneControls = new JPanel();
        paneControls.setLayout(new BoxLayout(paneControls, BoxLayout.PAGE_AXIS));
        paneControls.setAutoscrolls(true);
        scrollPaneControls = new JScrollPane(
                paneControls, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );        
        
        lblLogoH = new JLabel("Logo Height");
        lblLogoY = new JLabel("Logo Y Position");
        lblDigitsH = new JLabel("Digits Height");
        lblDigitsSmallH = new JLabel("Small Digits Height");
        lblTextH = new JLabel("Text Height");
        lblSpacingUnit = new JLabel("Spacing Unit");
        lblPauseBarH = new JLabel("Pause Bar Height");
        lblTimeBarH = new JLabel("Time Left Bar Height");
        lblHorizBarH = new JLabel("Horizontal Bar Height");
        lblClockMargin = new JLabel("Clock Side Margin");
        
        txtLogoH = new JTextField();
        txtLogoY = new JTextField();
        txtDigitsH = new JTextField();
        txtDigitsSmallH = new JTextField();
        txtTextH = new JTextField();
        txtSpacingUnit = new JTextField();
        txtPauseBarH = new JTextField();
        txtTimeBarH = new JTextField();
        txtHorizBarH = new JTextField();
        txtClockMargin = new JTextField();
        
        slideLogoH = new JSlider(0, 10000);
        slideLogoY = new JSlider(0, 10000);
        slideDigitsH = new JSlider(0, 2000);
        slideDigitsSmallH = new JSlider(0, 2000);
        slideTextH = new JSlider(0, 2000);
        slideSpacingUnit = new JSlider(0, 500);
        slidePauseBarH = new JSlider(0, 3000);
        slideTimeBarH = new JSlider(0, 200);
        slideHorizBarH = new JSlider(0, 200);
        slideClockMargin = new JSlider(0, 2000);
        
        slideLogoH.addChangeListener((ChangeEvent e) -> {
            txtLogoH.setText(getText(slideLogoH.getValue()));
        });
        slideLogoY.addChangeListener((ChangeEvent e) -> {
            txtLogoY.setText(getText(slideLogoY.getValue()));
        });
        slideDigitsH.addChangeListener((ChangeEvent e) -> {
            txtDigitsH.setText(getText(slideDigitsH.getValue()));
        });
        slideDigitsSmallH.addChangeListener((ChangeEvent e) -> {
            txtDigitsSmallH.setText(getText(slideDigitsSmallH.getValue()));
        });
        slideTextH.addChangeListener((ChangeEvent e) -> {
            txtTextH.setText(getText(slideTextH.getValue()));
        });
        slideSpacingUnit.addChangeListener((ChangeEvent e) -> {
            txtSpacingUnit.setText(getText(slideSpacingUnit.getValue()));
        });
        slidePauseBarH.addChangeListener((ChangeEvent e) -> {
            txtPauseBarH.setText(getText(slidePauseBarH.getValue()));
        });
        slideTimeBarH.addChangeListener((ChangeEvent e) -> {
            txtTimeBarH.setText(getText(slideTimeBarH.getValue()));
        });
        slideHorizBarH.addChangeListener((ChangeEvent e) -> {
            txtHorizBarH.setText(getText(slideHorizBarH.getValue()));
        });
        slideClockMargin.addChangeListener((ChangeEvent e) -> {
            txtClockMargin.setText(getText(slideClockMargin.getValue()));
        });
        
        paneLogoH = new JPanel();
        paneLogoY = new JPanel();
        paneDigitsH = new JPanel();
        paneDigitsSmallH = new JPanel();
        paneTextH = new JPanel();
        paneSpacingUnit = new JPanel();
        panePauseBarH = new JPanel();
        paneTimeBarH = new JPanel();
        paneHorizBarH = new JPanel();
        paneClockMargin = new JPanel();
        
        paneLogoH.setLayout(new GridLayout(1, 3));
        paneLogoY.setLayout(new GridLayout(1, 3));
        paneDigitsH.setLayout(new GridLayout(1, 3));
        paneDigitsSmallH.setLayout(new GridLayout(1, 3));
        paneTextH.setLayout(new GridLayout(1, 3));
        paneSpacingUnit.setLayout(new GridLayout(1, 3));
        panePauseBarH.setLayout(new GridLayout(1, 3));
        paneTimeBarH.setLayout(new GridLayout(1, 3));
        paneHorizBarH.setLayout(new GridLayout(1, 3));
        paneClockMargin.setLayout(new GridLayout(1, 3));
        
        paneLogoH.add(lblLogoH);
        paneLogoH.add(slideLogoH);
        paneLogoH.add(txtLogoH);
        paneLogoY.add(lblLogoY);
        paneLogoY.add(slideLogoY);
        paneLogoY.add(txtLogoY);
        paneDigitsH.add(lblDigitsH);
        paneDigitsH.add(slideDigitsH);
        paneDigitsH.add(txtDigitsH);
        paneDigitsSmallH.add(lblDigitsSmallH);
        paneDigitsSmallH.add(slideDigitsSmallH);
        paneDigitsSmallH.add(txtDigitsSmallH);
        paneTextH.add(lblTextH);
        paneTextH.add(slideTextH);
        paneTextH.add(txtTextH);
        paneSpacingUnit.add(lblSpacingUnit);
        paneSpacingUnit.add(slideSpacingUnit);
        paneSpacingUnit.add(txtSpacingUnit);
        panePauseBarH.add(lblPauseBarH);
        panePauseBarH.add(slidePauseBarH);
        panePauseBarH.add(txtPauseBarH);
        paneTimeBarH.add(lblTimeBarH);
        paneTimeBarH.add(slideTimeBarH);
        paneTimeBarH.add(txtTimeBarH);
        paneHorizBarH.add(lblHorizBarH);
        paneHorizBarH.add(slideHorizBarH);
        paneHorizBarH.add(txtHorizBarH);
        paneClockMargin.add(lblClockMargin);
        paneClockMargin.add(slideClockMargin);
        paneClockMargin.add(txtClockMargin);
        
        paneControls.add(paneLogoH);
        paneControls.add(paneLogoY);
        paneControls.add(paneDigitsH);
        paneControls.add(paneDigitsSmallH);
        paneControls.add(paneTextH);
        paneControls.add(paneSpacingUnit);
        paneControls.add(panePauseBarH);
        paneControls.add(paneTimeBarH);
        paneControls.add(paneHorizBarH);
        paneControls.add(paneClockMargin);
        
        mainPaneDimensions.add(scrollPaneControls, BorderLayout.CENTER);
        tabs.add(mainPaneDimensions);
        
        mainPaneTheme = new JPanel();
        mainPaneTheme.setName("Theme Entry");
        mainPaneTheme.setLayout(new BorderLayout());
        
        paneThemeButtons = new JPanel();
        btnThemeApply = new JButton("Apply");
        txtTheme = new JTextArea();
        txtTheme.setBackground(Color.BLACK);
        txtTheme.setForeground(new Color(0x20, 0xbb, 0xff));
        txtTheme.setMargin(new Insets(15, 15, 15, 15));
        txtTheme.setOpaque(true);
        txtTheme.setAutoscrolls(true);
        JScrollPane scrollText = new JScrollPane(txtTheme, 
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        paneThemeButtons.add(btnThemeApply);
        btnThemeApply.addActionListener((ActionEvent e) -> {
            String[] lines = txtTheme.getText().split("\\r?\\n");
            HashMap<String, String> entries = new HashMap<>();
            for(String line : lines) {
                String[] tokens = line.split("=", 2);
                if(tokens.length == 2) {
                    entries.put(tokens[0], tokens[1]);
                }
            }
            Assets.theme(entries);
            reset();
            cc.getDisplayFrame().rescale();
        });
        mainPaneTheme.add(scrollText, BorderLayout.CENTER);
        mainPaneTheme.add(paneThemeButtons, BorderLayout.PAGE_END);
        tabs.add(mainPaneTheme);
        
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(btnClose, BorderLayout.PAGE_END);
        pack();
        setSize(1000, 600);
    }
    
    public void reset() {
        slideLogoH.setValue(normalize(DisplayFrame.LOGO_HEIGHT_PROPORTION));
        slideLogoY.setValue(normalize(DisplayFrame.LOGO_Y_POSITION_PROPORTION));
        slideDigitsH.setValue(normalize(DisplayFrame.DIGITS_H));
        slideDigitsSmallH.setValue(normalize(DisplayFrame.DIGITS_SMALL_H));
        slideTextH.setValue(normalize(DisplayFrame.TEXT_H));
        slideSpacingUnit.setValue(normalize(DisplayFrame.SPACING_XS));
        slidePauseBarH.setValue(normalize(DisplayFrame.PAUSE_BAR_H));
        slideTimeBarH.setValue(normalize(DisplayFrame.TIME_BAR_H));
        slideHorizBarH.setValue(normalize(DisplayFrame.HORIZ_BAR_H));
        slideClockMargin.setValue(normalize(DisplayFrame.CLOCK_MARGIN));
        txtLogoH.setText("" + DisplayFrame.LOGO_HEIGHT_PROPORTION);
        txtLogoY.setText("" + DisplayFrame.LOGO_Y_POSITION_PROPORTION);
        txtDigitsH.setText("" + DisplayFrame.DIGITS_H);
        txtDigitsSmallH.setText("" + DisplayFrame.DIGITS_SMALL_H);
        txtTextH.setText("" + DisplayFrame.TEXT_H);
        txtSpacingUnit.setText("" + DisplayFrame.SPACING_XS);
        txtPauseBarH.setText("" + DisplayFrame.PAUSE_BAR_H);
        txtTimeBarH.setText("" + DisplayFrame.TIME_BAR_H);
        txtHorizBarH.setText("" + DisplayFrame.HORIZ_BAR_H);
        txtClockMargin.setText("" + DisplayFrame.CLOCK_MARGIN);
    }
    
    public void apply() {
        try {
            DisplayFrame.LOGO_HEIGHT_PROPORTION = realVal(txtLogoH.getText());
            DisplayFrame.LOGO_Y_POSITION_PROPORTION = realVal(txtLogoY.getText());
            DisplayFrame.DIGITS_H = realVal(txtDigitsH.getText());
            DisplayFrame.DIGITS_SMALL_H = realVal(txtDigitsSmallH.getText());
            DisplayFrame.TEXT_H = realVal(txtTextH.getText());
            float spacingUnit = realVal(txtSpacingUnit.getText());
            DisplayFrame.SPACING_XS = 1*spacingUnit;
            DisplayFrame.SPACING_S = 2*spacingUnit;
            DisplayFrame.SPACING_M = 3*spacingUnit;
            DisplayFrame.SPACING_L = 4*spacingUnit;
            DisplayFrame.SPACING_XL = 8*spacingUnit;
            DisplayFrame.PAUSE_BAR_H = realVal(txtPauseBarH.getText());
            DisplayFrame.TIME_BAR_H = realVal(txtTimeBarH.getText());
            DisplayFrame.HORIZ_BAR_H = realVal(txtHorizBarH.getText());
            DisplayFrame.CLOCK_MARGIN = realVal(txtClockMargin.getText());
            cc.getDisplayFrame().rescale();
            reset();
        } catch(Exception e) {
            Log.err("Apply failed: " + e);
        }
    }
    
    public void defaults() {
        DisplayFrame.LOGO_HEIGHT_PROPORTION = 0.4f;
        DisplayFrame.LOGO_Y_POSITION_PROPORTION = 0.4f;
        DisplayFrame.DIGITS_H = 0.1f;
        DisplayFrame.DIGITS_SMALL_H = 0.07f;
        DisplayFrame.TEXT_H = 0.06f;
        float spacingUnit = 0.0066f;
        DisplayFrame.SPACING_XS = 1*spacingUnit;
        DisplayFrame.SPACING_S = 2*spacingUnit;
        DisplayFrame.SPACING_M = 3*spacingUnit;
        DisplayFrame.SPACING_L = 4*spacingUnit;
        DisplayFrame.SPACING_XL = 8*spacingUnit;
        DisplayFrame.PAUSE_BAR_H = 0.1f;
        DisplayFrame.TIME_BAR_H = 0.008f;
        DisplayFrame.HORIZ_BAR_H = 0.003f;
        DisplayFrame.CLOCK_MARGIN = 0.074f;
        reset();
        apply();
    }
    
    private static int normalize(float val) {
        return (int)(val * 10000);
    }
    
    private static String getText(int val) {
        return "" + (val / 10000.0f);
    }
    
    private static float realVal(String val) {
        return Float.parseFloat(val);
    }
}
