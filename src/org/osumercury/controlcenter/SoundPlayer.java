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
import java.io.*;
import javax.sound.sampled.*;

/**
 *
 * @author wira
 */
public class SoundPlayer {

    private static boolean enabled = true;

    public static void setEnabled(boolean b) {
        enabled = b;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }

    public static void play(String key) {
        if(enabled && !Config.SOUND_DISABLED)
            (new PlayThread(key)).start();
    }    
}

class PlayThread extends Thread {
    private String key;

    public PlayThread(String key) {
        this.key = key;
    }

    @Override
    public void run() {
        try {
            File f = new File(Assets.getSoundAssetPath(key));
            AudioInputStream stream;
            AudioFormat format;
            DataLine.Info info;
            Clip clip;

            stream = AudioSystem.getAudioInputStream(f);
            format = stream.getFormat();
            info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.start();
            Thread.sleep(3000);
            clip.close();
        }
        catch (Exception e) {
            System.out.println("Unable to play sound asset: " + key + " - reason: " + e.toString());
        }
    }
}
