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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/** 
 *
 * @author wira
 */
public class Text {
    
    public static final int MAJOR_VERSION = 0;
    public static final int MINOR_VERSION = 9;
    public static final int MINOR_MINOR_VERSION = 8;
    public static final String REV = "c";
    
    public static String getVersion() {
        return MAJOR_VERSION + "." + MINOR_VERSION + "." + MINOR_MINOR_VERSION +
                REV;
    }
    
    public static final String AUTHORS = "Copyright ©2016 Wira D. Mulia, Fernando Cavazos, Carl D. Latino";
    
    public static final String LICENSE = 
"Licensed under the Apache License, Version 2.0 (the \"License\")\n" +
"You may obtain a copy of the License at\n" +
"\n" +
"    http://www.apache.org/licenses/LICENSE-2.0\n" +
"\n" +
"Unless required by applicable law or agreed to in writing, software\n" +
"distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
"See the License for the specific language governing permissions and\n" +
"limitations under the License.\n\n" + 
"Main developer: Wira (wheerdam@gmail.com)\n" +
"http://mercury.okstate.edu";       
    
    public static final String THIRD_PARTY =
"This program uses the following 3rd party software and assets:\n\n" +
"imgscalr 4.2 by Riyad Kalla\n" +
"http://github.com/rkalla/imgscalr\n" +
"Used under the Apache 2.0 license\n\n"+
"JCommander 1.58 by Cédric Beust\n" +
"http://jcommander.org\n" +
"Used under the Apache 2.0 license\n\n" + 
"DSEG fonts (DSEG7 Classic Bold) by Keshikan けしかん\n" +
"http://www.keshikan.net/fonts-e.html\n\n" +
"Google Noto Fonts (Noto Mono)\n" +
"https://www.google.com/get/noto/"            
            ;
       
    public static String getConfigFileSpecs() {
        String line;
        StringBuilder str = new StringBuilder();
        BufferedReader r =  new BufferedReader(
                new InputStreamReader(Text.class.getResourceAsStream("/org/osumercury/controlcenter/CONFIG.txt"))
        );
        try {
            while((line = r.readLine()) != null) {
                str.append(line);
                str.append("\n");
            }
        } catch(IOException ioe) {
            System.err.println("Built-in manual unavailable");
            return "Built-in manual unavailable";
        }
        
        return str.toString();
    }
    
    public static String getApache2License() {
        String line;
        StringBuilder str = new StringBuilder();
        BufferedReader r =  new BufferedReader(
                new InputStreamReader(Text.class.getResourceAsStream("/org/osumercury/controlcenter/LICENSE-2.0.txt"))
        );
        try {
            while((line = r.readLine()) != null) {
                str.append(line);
                str.append("\n");
            }
        } catch(IOException ioe) {
            System.err.println("Apache 2.0 License text not included");
            return "Apache 2.0 License text not included";
        }
        
        return str.toString();
    }
}    
