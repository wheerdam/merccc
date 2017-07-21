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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import org.osumercury.controlcenter.CompetitionState;
import org.osumercury.controlcenter.Data;
import org.osumercury.controlcenter.Team;

/**
 *
 * @author wira
 */
public class TeamFlagsDialog extends JDialog {
    private List<JCheckBox> flags;
    private final JButton btnOK;
    private final JButton btnCancel;
    private boolean approved = false;
    
    public TeamFlagsDialog(CompetitionState c, Team t) {
        setTitle("Flags for " + t.getName());
        List<String> criteria = c.getAllCriteria();
        flags = new ArrayList<>();
        JPanel paneCriteria = new JPanel();
        for(String criterion : criteria) {
            JCheckBox chkBox = new JCheckBox(criterion);
            chkBox.setSelected(t.hasAnnotation(criterion));
            paneCriteria.add(chkBox);
            flags.add(chkBox);
        }        
        JPanel paneButtons = new JPanel();
        btnOK = new JButton("OK");
        btnCancel = new JButton("Cancel");
        
        btnOK.addActionListener((e) -> {
            Data.lock().writeLock().lock();
            try {
                t.clearAnnotations();
                for(JCheckBox chkBox : flags) {
                    if(chkBox.isSelected()) {
                        t.addAnnotation(chkBox.getText());
                    }
                }
            } finally {
                Data.lock().writeLock().unlock();
            }
            approved = true;
            dispose();
        });
        
        btnCancel.addActionListener((e) -> {
            dispose();
        });
        
        paneButtons.add(btnOK);
        paneButtons.add(btnCancel);
        this.getContentPane().add(paneCriteria, BorderLayout.CENTER);
        this.getContentPane().add(paneButtons, BorderLayout.PAGE_END);
        pack();
    }
    
    public boolean isApproved() {
        return approved;
    }
}
