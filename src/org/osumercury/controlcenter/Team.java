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
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author wira
 */
public class Team implements Comparable {
    private final String logo;
    private final int teamNumber;
    private final String teamName;
    private final String teamInstitution;
    private final List<Score> scores;
    private final List<String> annotations;
    private double tiebreaker;    
    
    public static final int SORT_DESCENDING = 0;
    public static final int SORT_ASCENDING = 1;
    public static final int SORT_COMPARE_PRECISION = 1000;
    public static final double SORT_MAX_MAGNITUDE = 1000000;
    
    private static int SORT_ORDER = SORT_DESCENDING; // higher is better by default
    
    public Team(int teamNumber, String teamName, String teamInstitution, String logo) {
        this.teamNumber = teamNumber;
        this.teamName = teamName;
        this.teamInstitution = teamInstitution;
        this.logo = logo;
        scores = new ArrayList();
        clearTiebreaker();
        annotations = new ArrayList<>();
    }
    
    public String getName() {
        return teamName;
    }
    
    public int getNumber() {
        return teamNumber;
    }
    
    public String getInstitution() {
        return teamInstitution;
    }
    
    public String getLogoFileName() {
        return logo;
    }
    
    public void addScore(Score s) {
        Log.d(0, "Team.addScore(" + teamNumber + "=" + teamName +
                "): result=" + s.getScore());
        scores.add(s);
    }
    
    public void removeScore(int index) {
        scores.remove(index);
    }
    
    public boolean hasScore() {
        return !scores.isEmpty();
    }
    
    public boolean hasCompletedScore() {
        for(Score s : scores) {
            if(s.isCompleted()) {
                return true;
            }
        }
        return false;
    }
    
    public Score getBestScore() {
        if(!hasCompletedScore()) {
            return null;
        }
        
        Score bestScore = null;
        double best = SORT_ORDER == SORT_DESCENDING ? -1*SORT_MAX_MAGNITUDE : SORT_MAX_MAGNITUDE;
        for(Score s : scores) {
            switch(SORT_ORDER) {
                default:
                    if(s.getScore() > best) {
                        bestScore = s;
                        best = s.getScore();
                    }
                    break;
                case SORT_ASCENDING:
                    if(s.getScore() < best) {
                        bestScore = s;
                        best = s.getScore();
                    }
                    break;
            }
        }
        return bestScore;
    }
        
    public List<Score> getScores() {
        return scores;
    }
    
    public void setTiebreaker(double t) {
        this.tiebreaker = t;
    }
    
    public final double getTiebreaker() {
        return hasScore() ? getBestScore().getScore() : tiebreaker;
    }
    
    public void clearTiebreaker() {
        tiebreaker = SORT_ORDER == SORT_DESCENDING ? 
                     -1*SORT_MAX_MAGNITUDE : SORT_MAX_MAGNITUDE;
    }
    
    public void addAnnotation(String value) {
        
        if(!hasAnnotation(value)) {
            Log.d(1, "Team.addAnotation(" + teamNumber + "=" + teamName +
                     "): '" + value + "'");
            annotations.add(value);
            Object[] params = { teamNumber, value };
            ControlCenter.triggerEvent(UserEvent.TEAM_ADDED_ANNOTATION, params);
        }
    }
    
    public boolean hasAnnotation(String value) {
        for(String string : annotations) {
            if(string.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    public void removeAnnotation(String value) {
        Iterator<String> it = annotations.listIterator();
        while(it.hasNext()) {
            if(it.next().equals(value)) {
                Log.d(1, "Team.removeAnotation(" + teamNumber + "=" + teamName +
                         "): '" + value + "'");
                it.remove();
                Object[] params = { teamNumber, value };
                ControlCenter.triggerEvent(UserEvent.TEAM_REMOVED_ANNOTATION, 
                                           params);
            }
        }
    }
    
    public List<String> getAnnotations() {
        return annotations;
    }
    
    public void clearAnnotations() {
        annotations.clear();
        ControlCenter.triggerEvent(UserEvent.TEAM_CLEARED_ANNOTATION, teamNumber);
    }
    
    public static void setSortOrder(int n) {
        SORT_ORDER = n;
    }
    
    @Override
    public int compareTo(Object o) {
        Team t = (Team) o;
        switch(SORT_ORDER) {            
            case SORT_ASCENDING:
                return (int) (this.getBestScore().getScore()*SORT_COMPARE_PRECISION -
                        t.getBestScore().getScore()*SORT_COMPARE_PRECISION);
            default:
                return (int) (t.getBestScore().getScore()*SORT_COMPARE_PRECISION - 
                        this.getBestScore().getScore()*SORT_COMPARE_PRECISION);
        }
    }
    
    public int compareTiebreaker(Team t) {
        switch(SORT_ORDER) {
            case SORT_ASCENDING:
                return (int) (this.getTiebreaker()*SORT_COMPARE_PRECISION - 
                        t.getTiebreaker()*SORT_COMPARE_PRECISION);
            default:
                return (int) (t.getTiebreaker()*SORT_COMPARE_PRECISION - 
                        this.getTiebreaker()*SORT_COMPARE_PRECISION);
        }
    }
}
