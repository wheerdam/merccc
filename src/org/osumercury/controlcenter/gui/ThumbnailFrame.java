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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.osumercury.controlcenter.*;

/**
 *
 * @author wira
 */
public class ThumbnailFrame extends JFrame {
    private ControlCenter cc;
    private ThumbnailCanvas canvas;
    private long renderTime = 0;
    
    public ThumbnailFrame(ControlCenter cc) {
        this.cc = cc;
        setTitle("Preview");
        setSize(300, 200);
        //setResizable(false);
        canvas = new ThumbnailCanvas(this);
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BufferedImage thumb = cc.getDisplayFrame().getThumbnail();
                if(thumb != null && e.getClickCount() == 2) {
                    setSize(getWidth(), thumb.getHeight() + 
                            getInsets().top + getInsets().bottom);
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cc.getControlFrame().showThumbnailWindow(false);
            }
        });
        getContentPane().add(canvas);
    }
    
    public long getRenderTime() {
        return renderTime;
    }
    
    class ThumbnailCanvas extends JPanel {
        private ThumbnailFrame f;
        
        public ThumbnailCanvas(ThumbnailFrame f) {
            this.f = f;
        }
        
        @Override
        public void paint(Graphics g) {
            long startTime = System.nanoTime();
            cc.getDisplayFrame().setThumbnailWidth(getWidth());
            BufferedImage thumb = cc.getDisplayFrame().getThumbnail();
            g.setColor(new Color(33, 0, 33));
            g.fillRect(0, 0, getWidth(), getHeight());
            if(thumb != null) {
                Dimension d = new Dimension(thumb.getWidth(), thumb.getHeight());
                this.setPreferredSize(d);
                g.drawImage(thumb, 0, 0, null);
            }
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            String str = thumb == null ? "no thumb" : 
                    thumb.getWidth() + "x" + thumb.getHeight();
            FontMetrics m = g.getFontMetrics();
            g.drawString(str, 2, 2+m.getHeight()-m.getDescent());
            renderTime = System.nanoTime() - startTime;
        }
    }
}
