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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wira
 */
public class CompetitionState {
    private final ArrayList<Team> teams;
    private final HashMap<Integer, Team> teamByID;
    private ArrayList<Team> teamsFinished;
    private ArrayList<Team> teamsDNF;
    private SessionState session;
    private boolean redFlagged;
    private ArrayList<Callback> stateChangeHooks;
       
    private boolean sorted;    
        
    public static final int IDLE = -1;
    public static final int SETUP = 0;
    public static final int RUN = 1;
    public static final int POST_RUN = 2;
    
    private int state;
    
    public CompetitionState(HashMap<String, String> teamsMap) {
        stateChangeHooks = new ArrayList();
        teams = new ArrayList();
        teamByID = new HashMap();
        sorted = false;              
        state = IDLE;
        redFlagged = false;
        
        if(teamsMap == null) {
            Log.fatal(3, "CompetitionState: no teams section in config. file");
        }
        if(teamsMap.isEmpty()) {
            Log.fatal(3, "CompetitionState: no teams found in config. file");
        }
        
        Team t;
        String[] tokens;
        for(Map.Entry<String, String> entry : teamsMap.entrySet()) {
            try {
                Log.d(1, "CompetitionState: adding team entry "
                        + entry.getKey() + " -> " + entry.getValue());
                tokens = entry.getValue().trim().split(",");
                t = new Team(Integer.parseInt(entry.getKey()),
                            tokens[0].trim(),
                            tokens[1].trim(),
                            tokens[2].trim()
                        );
                teams.add(t);
                teamByID.put(Integer.parseInt(entry.getKey()), t);
            } catch(Exception e) {
                if(Log.debugLevel > 1) {
                    e.printStackTrace();
                }
                String key = entry.getKey();
                String value = entry.getValue() == null ?
                                    "null" : entry.getValue();
                Log.fatal(40, "Failed to parse team entry: " + 
                              key + "=" + value);
            }
        }
    }
    
    public void newSession(Team t, int attempts, long setupDuration, long windowDuration) {
        session = new SessionState(t, attempts, setupDuration, windowDuration);
    }
    
    public SessionState getSession() {
        return session;
    }
    
    public void addStateChangeHook(Callback hook) {
        stateChangeHooks.add(hook);
    }
    
    public synchronized void sort() {     
        teamsFinished = new ArrayList();
        teamsDNF = new ArrayList();
        for(Team t : teams) {
            if(t.getBestScore() != null) {
                teamsFinished.add(t);
            } else {
                teamsDNF.add(t);
            }
        }
        
        // teams that have a score are sorted by their final best scores
        teamsFinished.sort((Team a, Team b) -> (int) (a.compareTo(b)));
        
        // we sort DNF teams by a tiebreaker value, whatever that is
        teamsDNF.sort((Team a, Team b) -> (int) (b.getTiebreaker() - a.getTiebreaker()));
        
        sorted = true;
    }
    
    public void setState(int state) {
        this.state = state;
        for(Callback hook : stateChangeHooks) {
            hook.callback(this);
        }
    }
    
    public int getState() {
        return state;
    }
    
    public void setRedFlag(boolean b) {
        redFlagged = b;
    }
    
    public boolean redFlagged() {
        return redFlagged;
    }
    
    public ArrayList<Team> getTeams() {
        return teams;
    }
    
    public Team getTeamByID(int id) {
        return teamByID.get(id);
    }
    
    public synchronized ArrayList<Team> getSortedFinishedTeams() {
        return teamsFinished;
    }
    
    public synchronized ArrayList<Team> getDNFTeams() {
        return teamsDNF;
    }
    
    public boolean isSorted() {
        return sorted;
    }
}
