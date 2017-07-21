/*
    Copyright 2016-2017 Wira Mulia

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
import java.util.List;
import java.util.Map;

/**
 *
 * @author wira
 */
public class CompetitionState {
    private final List<Team> teams;
    private final Map<Integer, Team> teamByID;
    private List<Team> teamsClassified;
    private List<Team> teamsDNF;
    private SessionState session;
    private boolean redFlagged;
    private List<Callback> stateChangeHooks;
    private final List<String> championCriteria;
    private final List<String> classificationCriteria;
       
    private boolean sorted;    
        
    public static final int IDLE = -1;
    public static final int SETUP = 0;
    public static final int RUN = 1;
    public static final int POST_RUN = 2;
    
    private int state;
    
    public CompetitionState(Map<String, String> teamsMap,
                            List<String> teamsOrder,
                            String csvChampion, String csvClassification) {
        stateChangeHooks = new ArrayList();
        teams = new ArrayList();
        teamByID = new HashMap();
        sorted = false;              
        state = IDLE;
        redFlagged = false;
        this.championCriteria = new ArrayList<>();
        this.classificationCriteria = new ArrayList<>();
        
        if(teamsMap == null) {
            Log.fatal(3, "CompetitionState: no teams section in config. file");
        }
        if(teamsMap.isEmpty()) {
            Log.fatal(3, "CompetitionState: no teams found in config. file");
        }
        
        String[] criteria;
        if(csvChampion != null) {
            criteria = csvChampion.trim().split(",");
            for(String s : criteria) {
                championCriteria.add(s.trim());
                Log.d(1, "CompetitionState: added champion criterion " +
                         "'" + s + "'");
            }
        }
        if(csvClassification != null) {
            criteria = csvClassification.trim().split(",");
            for(String s : criteria) {
                classificationCriteria.add(s.trim());
                Log.d(1, "CompetitionState: added classification criterion " +
                         "'" + s + "'");
            }
        }
        
        Team t;
        String[] tokens;
        for(String teamIDString : teamsOrder) {
            String teamData = teamsMap.get(teamIDString);
            try {
                Log.d(1, "CompetitionState: adding team entry "
                        + teamIDString + " -> " + teamData);
                tokens = teamData.trim().split(",");
                if(tokens.length > 2) {
                    t = new Team(Integer.parseInt(teamIDString),
                                tokens[0].trim(),
                                tokens[1].trim(),
                                tokens[2].trim()
                            );
                } else {
                    Log.d(0, "CompetitionentryState: badge not specified for #" +
                             teamIDString);
                    t = new Team(Integer.parseInt(teamIDString),
                                tokens[0].trim(),
                                tokens[1].trim(),
                                "no-logo-for-this-team"
                            );
                }
                teams.add(t);
                teamByID.put(Integer.parseInt(teamIDString), t);
            } catch(Exception e) {
                if(Log.debugLevel > 1) {
                    e.printStackTrace();
                }
                String key = teamIDString;
                String value = teamData == null ?
                                    "null" : teamData;
                Log.fatal(40, "Failed to parse team entry: " + 
                              key + "=" + value);
            }
        }
    }
    
    public List<String> getChampionCriteria() {
        return championCriteria;
    }
    
    public List<String> getClassificationCriteria() {
        return classificationCriteria;
    }
    
    public List<String> getAllCriteria() {
        List<String> criteria = new ArrayList<>();
        for(String s : championCriteria) {
            criteria.add(s);
        }
        for(String s : classificationCriteria) {
            boolean duplicate = false;
            for(String cstr : championCriteria) {
                if(s.equals(cstr)) {
                    duplicate = true;
                }
            }
            if(!duplicate) {
                criteria.add(s);
            }
        }
        return criteria;
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
        Log.d(3, "CompetitionState.sort: sorting teams");
        teamsClassified = new ArrayList();
        teamsDNF = new ArrayList();
        for(Team t : teams) {
            if(t.getBestScore() != null) {
                boolean classified = true;
                for(String criterion : classificationCriteria) {
                    if(!t.hasAnnotation(criterion)) {
                        classified = false;
                    }
                }
                if(classified) {
                    teamsClassified.add(t);
                } else {
                    teamsDNF.add(t);
                }
            } else {
                teamsDNF.add(t);
            }
        }
        
        // teams that are classified are sorted by their final best scores
        teamsClassified.sort((Team a, Team b) -> (int) (a.compareTo(b)));
        
        // we sort DNF teams by a tiebreaker value, whatever that is
        teamsDNF.sort((Team a, Team b) -> a.compareTiebreaker(b));
        
        sorted = true;
        Log.d(3, "CompetitionState.sort: sorting done");
    }
    
    public void setState(int state) {
        this.state = state;
        for(Callback hook : stateChangeHooks) {
            hook.callback(this);
        }
        switch(state) {
            case IDLE:
                ControlCenter.triggerEvent(UserEvent.STATE_CHANGE_IDLE, null);
                break;
            case SETUP:
                Object[] params = {session.getMaxAttempts(), 
                                   session.getSetupDuration()/1000,
                                   session.getWindowDuration()/1000};
                ControlCenter.triggerEvent(UserEvent.STATE_CHANGE_SETUP, params);
                break;
            case RUN:
                ControlCenter.triggerEvent(UserEvent.STATE_CHANGE_RUN, null);
                break;
            case POST_RUN:
                ControlCenter.triggerEvent(UserEvent.STATE_CHANGE_POSTRUN, null);                
                break;
        }
    }
    
    public int getState() {
        return state;
    }
    
    public void setRedFlag(boolean b) {
        redFlagged = b;
        ControlCenter.triggerEvent(b ?
                    UserEvent.SESSION_REDFLAGGED : UserEvent.SESSION_GREENFLAGGED,
                    null);
    }
    
    public boolean redFlagged() {
        return redFlagged;
    }
    
    public List<Team> getTeams() {
        return teams;
    }
    
    public Team getTeamByID(int id) {
        return teamByID.get(id);
    }
    
    public synchronized List<Team> getSortedClassifiedTeams() {
        return teamsClassified;
    }
    
    public synchronized List<Team> getDNFTeams() {
        return teamsDNF;
    }
    
    public boolean isTeamEligibleForChampionship(Team t) {
        for(String criterion : championCriteria) {
            if(!t.hasAnnotation(criterion)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isSorted() {
        return sorted;
    }
}
