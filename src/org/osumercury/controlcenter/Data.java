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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author wira
 */
public class Data {
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock(
            true
    );
    private static File dataWorkDir = new File(".");
    
    public static void setDataWorkDir(String f) {
        dataWorkDir = new File(f);
    }
    
    public static File getDataWorkDir() {
        return dataWorkDir;
    }
    
    public static ReentrantReadWriteLock lock() {
        return lock;
    }
    
    public static AbstractTableModel getTableModel(CompetitionState c) {
        DefaultTableModel m = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
       
        m.setColumnIdentifiers(getColHeaders());
        for(String[] entry : getData(c)) {
            m.addRow(entry);
        }
        
        return m;
    }
    
    public static AbstractTableModel getResultsTableModel(CompetitionState c) {
        DefaultTableModel m = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        
        String[] colHeader = {"Rank", "Team#", "Team Name", "Institution", "Score"};
        m.setColumnIdentifiers(colHeader);
        for(String[] entry : getResultsData(c)) {
            m.addRow(entry);
        }
        
        return m;
    }
    
    public static String getDataAsCSV(CompetitionState c) {
        int hash = Config.getConfigString().hashCode();
        StringBuilder str = new StringBuilder();
        str.append("# config:");
        str.append(Config.getConfigFile().getName());
        str.append("\n# hash:");
        str.append(String.valueOf(hash));
        str.append("\n# tiebreakers: ");
        for(Team t : c.getTeams()) {
            str.append(t.getNumber());
            str.append("-");
            str.append(t.getTiebreaker());
            str.append(" ");
        }
        str.append("\n# ");
        for(String s : getColHeaders()) {
            str.append(s);
            str.append(",");
        }
        str.append("\n");
        for(String[] ss : getData(c)) {
            for(String s : ss) {
                str.append(s);
                str.append(",");
            }
            str.append("\n");
        }
        return str.toString();
    }
    
    public static void saveAsCSV(CompetitionState c, String f) {
        Log.d(0, "Data.saveAsCSV: " + f);
        try {
            FileWriter w = new FileWriter(f);
            w.write(getDataAsCSV(c));
            w.close();
            String parentDir = new File(f).getParent();
            dataWorkDir = parentDir == null ? new File(".") : new File(parentDir);
        } catch(IOException ioe) {
            System.err.println("Data.saveAsCSV: failed to export to " + f);
        }
    }
    
    public static void loadCSV(CompetitionState c, String f) {
        Log.d(0, "Data.loadCSV: " + f);
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String l;
            String[] tokens;
            while((l = r.readLine()) != null) {
                if(l.startsWith("#")) {
                    if(l.contains("hash")) {
                        tokens = l.split("hash:");
                        int hash = Integer.parseInt(tokens[1]);
                        if(hash != Config.getConfigString().hashCode()) {
                            System.err.println();
                            System.err.println("WARNING! Config file hashes between " + 
                                    "the current active config and the saved CSV " +
                                    "DID NOT MATCH.");
                            System.err.println("Active config=" + 
                                    Config.getConfigString().hashCode() +
                                    " Saved config=" + hash);
                            System.err.println();
                        }
                    } else if(l.contains("tiebreakers")) {
                        tokens = l.split("tiebreakers:");
                        String[] entries = tokens[1].trim().split("\\s+");
                        for(String t : entries) {
                            String[] pair = t.trim().split("-");
                            int teamID = Integer.parseInt(pair[0]);
                            int tiebreaker = Integer.parseInt(pair[1]);
                            Team team = c.getTeamByID(teamID);
                            lock.writeLock().lock();
                            try {       
                                if(team != null) {
                                    c.getTeamByID(teamID).setTiebreaker(tiebreaker);
                                } else {
                                    Log.d(0, "Data.loadCSV: team with ID " + teamID + " not found" +
                                        ", ignoring line");
                                }
                            } finally {
                                lock.writeLock().unlock();
                            }
                        }
                    }
                } else {
                    parseCSVLine(c, l);
                }
            }
            c.sort();
            r.close();
            String parentDir = new File(f).getParent();
            dataWorkDir = parentDir == null ? new File(".") : new File(parentDir);
            ControlCenter.triggerEvent(UserEvent.DATA_IMPORTED, Data.getData(c));
        } catch(Exception e) {
            System.err.println("Data.loadCSV: failed to import from " + f);
            if(Log.debugLevel > 0) {
                e.printStackTrace();
            }
        }
    }
    
    public static void parseCSVLine(CompetitionState c, String l) {
        String[] tokens;
        tokens = l.trim().split(",");
        String ID = tokens[0].trim();
        Score s = new Score();        
        lock.writeLock().lock();
        try {
            for(int i = 3; i < 3+Score.getFields().size(); i++) {
            s.setValue(Config.getKeysInOriginalOrder("fields").get(i-3),
                    Double.parseDouble(tokens[i].trim()));
            }
            s.setCompleted(true);
            Team t = c.getTeamByID(Integer.parseInt(ID));
            if(t != null) {
                t.addScore(s);
            } else {
                Log.d(0, "Data.parseCSVLine: team with ID " + ID + " not found" +
                        ", ignoring line");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Log.d(0, "Data.parseCSVLine: failed to parse " + l);
            if(Log.debugLevel > 0) {
                e.printStackTrace();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static String[] getColHeaders() {
        int n = Score.getFields().size();
        int cols = 3 + n + 1;
        
        String[] colHeader = new String[cols];
        colHeader[0] = "Team#";
        colHeader[1] = "TeamName";
        colHeader[2] = "Score#";
        for(int i = 3; i < n+3; i++) {
            colHeader[i] = Config.getKeysInOriginalOrder("fields").get(i-3);
        }
        colHeader[3+n] = "Total";
        
        return colHeader;
    }
    
    public static ArrayList<String[]> getData(CompetitionState c) {
        int n = Score.getFields().size();
        int cols = 3 + n + 1;
        ArrayList<String[]> rows = new ArrayList();
        
        String[] row;
        lock.readLock().lock();
        try {
            for(Team t : c.getTeams()) {
                int scoreID = 0;
                for(Score s : t.getScores()) {
                    row = new String[cols];
                    row[0] = t.getNumber() + "";
                    row[1] = t.getName();
                    row[2] = scoreID + "";
                    int scoreCol = 3;
                    for(String key : Config.getKeysInOriginalOrder("fields")) {
                        row[scoreCol] = s.getValue(key) + "";
                        scoreCol++;
                    }
                    row[3+n] = s.getScore() + "";
                    rows.add(row);
                    scoreID++;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return rows;
    }
    
    public static ArrayList<String[]> getResultsData(CompetitionState c) {
        ArrayList<String[]> rows = new ArrayList();
        String[] row;
        
        int rank = 1;
        lock.readLock().lock();
        try {
            c.sort();
            for(Team t : c.getSortedFinishedTeams()) {
                row = new String[5];
                row[0] = rank + "";
                row[1] = t.getNumber() + "";
                row[2] = t.getName();
                row[3] = t.getInstitution();
                row[4] = String.format("%.2f", t.getBestScore().getScore()) + "";
                rows.add(row);
                rank++;
            }
        } finally {
            lock.readLock().unlock();
        }
        boolean first = true;
        int prevRank = -1;
        int prevTiebreaker = -1; 
        lock.readLock().lock();
        try {
            for(Team t : c.getDNFTeams()) {
                if(prevTiebreaker != t.getTiebreaker()) {
                    if(!first) {
                        rank++;
                    } else {
                        first = false;
                    }
                }

                row = new String[5];
                row[0] = (rank != prevRank ? rank : "-") + "";
                row[1] = t.getNumber() + "";
                row[2] = t.getName();
                row[3] = t.getInstitution();
                row[4] = "DNF (" + t.getTiebreaker() + ")";
                prevRank = rank;
                prevTiebreaker = t.getTiebreaker();
                rows.add(row);
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return rows;
    }
    
    public static void removeScore(CompetitionState c, int teamID, int scoreID) {
        lock().writeLock().lock();
        try {
            c.getTeamByID(teamID).getScores().remove(scoreID);
        } finally {
            lock().writeLock().unlock();
        }
        Object[] params = {teamID, scoreID};
        ControlCenter.triggerEvent(UserEvent.DATA_RECORD_EXPUNGED, params);
    }
    
    public static void clearData(CompetitionState c) {
        lock().writeLock().lock();
        try {
            for(Team t : c.getTeams()) {
                t.getScores().clear();
            }
        } finally {
                lock().writeLock().unlock();
        }
        ControlCenter.triggerEvent(UserEvent.DATA_CLEARED, null);
    }
    
    public static void generateReport(CompetitionState c, String f) {
        Log.d(0, "Data.generateReport: " + f);
        try {
            StringBuilder str = new StringBuilder();
            FileWriter w = new FileWriter(f);
            ArrayList<String[]> rows = getResultsData(c);
            str.append("\n");
            str.append(alignCenter("MERCURY REPORT OUTPUT", 80, ' '));
            str.append("\n");
            String exported = "Exported " + (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")).format(new Date());
            str.append(alignCenter(exported, 80, ' '));
            str.append("\n\n");
            str.append("Rank ID#  Name                           Institution                    Score\n");
            str.append("--------------------------------------------------------------------------------\n");
            for(String[] row : rows) {
                str.append(padBefore(row[0], 4));
                str.append(" ");
                str.append(padBefore(row[1], 3));
                str.append("  ");
                str.append(padAfter(row[2], 31));
                str.append(padAfter(row[3], 31));
                str.append(row[4]);
                str.append("\n");
            }
            
            str.append("\nScore Data----------------------------------------------------------------------\n");
            str.append(getDataAsCSV(c));
            
            str.append("\nConfiguration-------------------------------------------------------------------\n");
            str.append(Config.getConfigString());           
            
            str.append("\nEND OF FILE---------------------------------------------------------------------\n");
            w.write(str.toString());
            w.close();
        } catch(IOException ioe) {
            System.err.println("Data.generateReport: failed to export to " + f);
        }
    }
    
    public static String padAfter(String str, int len) {
        String s = str;
        if(str.length() > len) {
            s = str.substring(0, len-4) + "... ";
        } else {
            for(int i = 0; i < len-str.length(); i++) {
                s += " ";
            }
        }
        
        return s;
    }
    
    public static String padBefore(String str, int len) {
        String s = str;
        if(str.length() > len) {
            s = str.substring(0, len-4) + "... ";
        } else {
            for(int i = 0; i < len-str.length(); i++) {
                s = " " + s;
            }
        }
        
        return s;
    }      
    
    public static String alignCenter(String str, int lineLen, char spaces) {
        int pad = (lineLen - str.length()) / 2;
        String s = str;
        for(int i = 0; i < pad; i++) {
            s = spaces + s;
        }
        for(int i = pad+str.length(); i < lineLen; i++) {
            s = s + spaces;
        }
        return s;
    }
}
