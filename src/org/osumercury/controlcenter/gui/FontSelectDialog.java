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

import java.awt.GraphicsEnvironment;
import javax.swing.*;
import java.awt.Container;
import java.awt.event.*;
import java.awt.Font;

/**
 *
 * @author wira
 */
public class FontSelectDialog extends JDialog {
    private JComboBox<String> cmbFont;
    private JButton btnOK;
    private JButton btnCancel;
    private boolean OK;
    private String fontName;
    
    public FontSelectDialog(String title) {
        setTitle(title);
        
        btnOK = new JButton("OK");
        btnCancel = new JButton("Cancel");
        cmbFont = new JComboBox();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        cmbFont.addItem("-- use merccc bitmap fonts --");
        for(Font f : ge.getAllFonts()) {
            cmbFont.addItem(f.getFontName());
        }
        setSize(500,50);
        Container pane = this.getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        JPanel paneButtons = new JPanel();
        paneButtons.add(btnOK);
        paneButtons.add(btnCancel);
        OK = false;
        
        btnOK.addActionListener((ActionEvent e) -> {
            fontName = cmbFont.getSelectedIndex() == 0 ? null : 
                    (String) cmbFont.getSelectedItem();
            OK = true;
            dispose();
        });
               
        btnCancel.addActionListener((ActionEvent e) -> {
            dispose();
        });
        
        pane.add(cmbFont);
        pane.add(paneButtons);
        pack();
    }
    
    public String getFontName() {
        return fontName;
    }

    public boolean isApproved() {
        return OK;
    }
    
    public void showDialog() {
        this.setVisible(true);
    }
}
