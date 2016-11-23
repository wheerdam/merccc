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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author wira
 */
public class Config {
    private static HashMap<String, HashMap<String, String>> sections;
    private static HashMap<String, ArrayList<String>> orderedKeys;
    public static File CONFIG_FILE;
    public static String CONFIG_STRING;
    public static String TMP_DIR;
    public static boolean SOUND_DISABLED = false;

    public static boolean load(String f, boolean zip) {
        if(zip) {
            f = loadZip(f);  
            if(f == null) {
                return false;
            }
        }
        
        try {            
            CONFIG_FILE = new File(f);            
            Log.d(0, "Config.load: " + CONFIG_FILE.getAbsolutePath());
            StringBuilder str = new StringBuilder();
            FileReader r = new FileReader(CONFIG_FILE);
            char[] buf = new char[4096];
            int nread;
            while((nread = r.read(buf)) != -1) {
                str.append(buf, 0, nread);
            }
            CONFIG_STRING = str.toString();
            r.close();
            return parse(CONFIG_STRING);
        } catch(IOException ioe) {
            System.err.println("Config.load: failed to parse " + f);
            if(Log.debugLevel > 0) {
                ioe.printStackTrace();
            }
            return false;
        }
    }
    
    public static boolean parse(String str) {
        String lines[] = str.split("\\r?\\n");
        if(sections == null || sections.isEmpty()) {
            sections = new HashMap();
            orderedKeys = new HashMap();
            sections.put("GLOBAL", new HashMap<>());
            orderedKeys.put("GLOBAL", new ArrayList<>());
        }
        HashMap curSection = sections.get("GLOBAL");
        ArrayList curList = orderedKeys.get("GLOBAL");
        String[] tokens;
        
        for(String l : lines) {
            try {
                l = l.trim();
                if(l.startsWith("#") || l.equals("")) {
                    continue;
                }
                Pattern p = Pattern.compile("\\[(.*?)\\]", Pattern.DOTALL);
                Matcher m = p.matcher(l);
                if(m.matches()) {
                    Log.d(0, "Config.parse: found section -> " + l);
                    curSection = new HashMap();
                    curList = new ArrayList();
                    sections.put(l.substring(1, l.length()-1), curSection);
                    orderedKeys.put(l.substring(1, l.length()-1), curList);
                    continue;
                }
                tokens = l.split("#");
                tokens = tokens[0].split("=");
                curSection.put(tokens[0], tokens.length > 1 ? tokens[1].trim() : null);
                curList.add(tokens[0]);
            } catch(Exception e) {
                System.err.println("Config.parse: failed to parse \"" +
                        l + "\"");
                return false;
            }
        }
        return true;
    }
    
    public static String loadZip(String f) {
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory(null);
        } catch(IOException ioe) {
            System.err.println("Config.loadZip: unable to create temporary" +
                    " path");
            return null;
        }
        TMP_DIR = tmpDir.toAbsolutePath().toString();
        Log.d(0, "Config.loadZip: " + f);
        Log.d(0, "Config.loadZip: tmpDir=" + tmpDir.toAbsolutePath());

        try {
            String configPath = null;
            ZipFile zip = new ZipFile(new File(f));
            ZipEntry z;
            InputStream r;
            File out;
            FileOutputStream w;
            byte[] buf = new byte[4096];
            int nread;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while(entries.hasMoreElements()) {
                z = entries.nextElement();
                if(z.getName().endsWith(".merccc")) {
                    configPath = z.getName();
                }
                r = zip.getInputStream(z);
                out = new File(TMP_DIR + File.separatorChar + z.getName());
                if(z.isDirectory()) {
                    out.mkdir();
                } else {
                    w = new FileOutputStream(out);
                    while((nread = r.read(buf)) != -1) {
                        w.write(buf, 0, nread);
                    }
                    w.close();
                }
                r.close();
            }
            zip.close();
            
            if(configPath == null) {
                System.err.println("Config.loadZip: .merccc file not found in " +
                        "the archive");
                return null;
            }
            return TMP_DIR + File.separatorChar + configPath;
        } catch(IOException ioe) {
            System.err.println("Config.loadZip: read I/O exception");
            System.err.println("Config.loadZip: " + ioe.toString());
            return null;
        }        
    }
    
    public static boolean hasSection(String key) {
        return sections.containsKey(key);
    }
    
    public static String[] getSection(String key) {
        if(sections.containsKey(key)) {
            return (String[]) sections.get(key).values().toArray();
        }
        
        System.err.println("Config.getSection: \"" + key + "\" section" +
                " not found.");
        return null;
    }
    
    public static HashMap<String, String> getSectionAsMap(String key) {
        if(sections.containsKey(key)) {
            return sections.get(key);
        }
        
        System.err.println("Config.getSectionAsMap: \"" + key +
                "\" section not found.");
        return null;
    }
    
    public static String getValue(String section, String key) {
        if(!sections.containsKey(section)) {
            System.err.println("Config.getValue: \"" + section +
                "\" section not found.");
            return null;
        }
        
        if(!sections.get(section).containsKey(key)) {
            System.err.println("Config.getValue: No \"" + key +
                "\" entry in section \"" + section + "\"");
            return null;
        }
        
        return sections.get(section).get(key);
    }
    
    public static ArrayList<String> getKeysInOriginalOrder(String section) {
        return orderedKeys.get(section);
    }
    
    public static boolean deleteDirectory(String path) {
        File dir = new File(path);
        if(!dir.exists() || !dir.isDirectory()) {
            Log.d(0, "Config.deleteDirectory: " + path + " does not exist or" +
                    " is not a directory");
            return false;
        }
        File[] contents = dir.listFiles();
        for(File f : contents) {
            if(f.isDirectory()) {
                if(!deleteDirectory(f.getAbsolutePath())) {
                    Log.d(0, "Config.deleteDirectory: failed to delete " +
                            f.getAbsolutePath());
                    return false;
                }
            } else {
                if(!f.delete()) {
                    Log.d(0, "Config.deleteDirectory: failed to delete " +
                            f.getAbsolutePath());
                    return false;
                }
            }
        }
        
        return dir.delete();
    }
}
