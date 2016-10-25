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

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.*;

/**
 *
 * @author wira
 */
public class NumberInputDialog extends JDialog {
    
    private JTextField txtValue;
    private JButton btnOK;
    private JButton btnCancel;
    private boolean OK;
    private Object defaultValue;
    private double val;
    private int type;
    
    public static final int INTEGER = 0;
    public static final int FLOAT = 1;
    
    public NumberInputDialog(String title, double defaultValue, int type) {
        setTitle(title);
        txtValue = new JTextField("" + String.valueOf(
                (type == INTEGER) ? (int) defaultValue : defaultValue));
        txtValue.setSelectionStart(0);
        txtValue.setSelectionEnd(txtValue.getText().length());
        btnOK = new JButton("OK");
        btnCancel = new JButton("Cancel");
        this.defaultValue = defaultValue;
        this.type = type;
        setSize(300,50);
        this.setLayout(new GridLayout(1, 2));
        JPanel paneButtons = new JPanel();
        paneButtons.add(btnOK);
        paneButtons.add(btnCancel);
        OK = false;
        
        btnOK.addActionListener((ActionEvent e) -> {
            parseValue();
            if(OK)
                dispose();
        });
        
        txtValue.addActionListener((ActionEvent e) -> {
            parseValue();
            if(OK)
                dispose();
        });
        
        btnCancel.addActionListener((ActionEvent e) -> {
            dispose();
        });
        
        txtValue.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                
            }
            
            @Override
            public void keyTyped(KeyEvent e) {
                
            }
        });
        add(txtValue);
        add(paneButtons);
        pack();
    }
    
    private void parseValue() {
        try {
            val = (type == INTEGER) ? (int)Integer.parseInt(txtValue.getText()) :
                    Double.parseDouble(txtValue.getText());
            OK = true;
        } catch(NumberFormatException nfe) {
            txtValue.setText(String.valueOf(defaultValue));
            txtValue.setSelectionStart(0);
            txtValue.setSelectionEnd(txtValue.getText().length());
        }
    }
    
    public int getValueInt() {
        return (int) val;
    }
    
    public double getValueDouble() {
        return val;
    }
    
    public boolean isApproved() {
        return OK;
    }
    
    public void showDialog() {
        this.setVisible(true);
    }
}
