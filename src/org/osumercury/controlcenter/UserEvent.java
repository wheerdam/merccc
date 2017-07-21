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

/**
 *
 * @author wira
 */
public interface UserEvent {
    public static final int GUI_INIT                    = -2;
    public static final int STATE_CHANGE_IDLE           = -1;
    public static final int STATE_CHANGE_SETUP          = 0;
    public static final int STATE_CHANGE_RUN            = 1;
    public static final int STATE_CHANGE_POSTRUN        = 2;
    public static final int GUI_SCORE_FIELD_UPDATED     = 3;
    public static final int SESSION_ATTEMPT_COMMITTED   = 4;
    public static final int SESSION_ATTEMPT_DISCARDED   = 5;
    public static final int SESSION_PAUSED              = 6;
    public static final int SESSION_RESUMED             = 7;
    public static final int SESSION_TIME_ADDED          = 8;
    public static final int SESSION_REDFLAGGED          = 9;
    public static final int SESSION_GREENFLAGGED        = 10;
    public static final int DATA_CHANGED                = 11;
    public static final int DATA_CLEARED                = 12;
    public static final int DATA_RECORD_EXPUNGED        = 13;
    public static final int DATA_IMPORTED               = 14;
    public static final int DATA_ADDED                  = 15;
    public static final int TEAM_PRE_SELECT             = 16;
    public static final int DISPLAY_MODE_CHANGE         = 17;
    public static final int DISPLAY_HIDE                = 18;
    public static final int DISPLAY_SHOW                = 19;
    public static final int DISPLAY_RANK_START          = 20;
    public static final int TEAM_ADDED_ANNOTATION       = 21;
    public static final int TEAM_REMOVED_ANNOTATION     = 22;
    public static final int TEAM_CLEARED_ANNOTATION     = 23;
    public static final int EXIT                        = 200;
            
    public abstract void callback(int eventID, Object param);
}
