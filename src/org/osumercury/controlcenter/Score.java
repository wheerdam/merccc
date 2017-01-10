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
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Map;

/**
 *
 * @author wira
 */
public class Score {
    private static boolean initialized = false;
    private static ArrayList<String> postfixFormula;
    private static HashMap<String, String> fields;
    private static HashMap<String, String> description;
    private static HashMap<String, Integer> type;
    private static HashMap<String, Double[]> possibleValues;
    private static HashMap<String, Double> defaultValue;
    
    private HashMap<String, Double> values;
    private boolean teamCompletedTrack = false;
    
    public static HashMap<String, String> getFields() {
        return fields;
    }
    
    public static String getDescription(String key) {
        return description.get(key);
    }
    
    public static int getType(String key) {
        return type.get(key);
    }
    
    public static Double[] getPossibleValues(String key) {
        return possibleValues.get(key);
    }
    
    public static double getDefaultValue(String key) {
        return defaultValue.get(key);
    }
    
    public static void init(String postfix, HashMap<String, String> vars) {
        int typeTemp;
        postfixFormula = new ArrayList();
        fields = vars;
        type = new HashMap();
        description = new HashMap();
        defaultValue = new HashMap();
        possibleValues = new HashMap();
        String[] tokens;
        
        if(fields == null) {
            Log.fatal(50, "Score.init: fields section not found in the " +
                          "configuration file");
        }

        for(Map.Entry<String, String> entry : fields.entrySet()) {
            try {
                Log.d(1, "Score.init: > " + entry.getKey() + "=" + entry.getValue());
                tokens = entry.getValue().split(",");
                typeTemp = Integer.parseInt(tokens[2].trim());
                description.put(entry.getKey(), tokens[0].trim());
                defaultValue.put(entry.getKey(), Double.parseDouble(tokens[1].trim()));
                type.put(entry.getKey(), typeTemp);
                if(typeTemp > 1) {
                    tokens = tokens[3].split(":");
                    Double[] pVals = new Double[tokens.length];
                    for (int i = 0; i < tokens.length; i++) {
                        pVals[i] = Double.parseDouble(tokens[i].trim());
                    }
                    possibleValues.put(entry.getKey(), pVals);
                }
            } catch(Exception e) {
                String key = entry.getKey();
                String value = entry.getValue() == null ?
                                    "null" : entry.getValue();
                Log.fatal(51, "Failed to parse score field entry: " + 
                              key + "=" + value);
            }
        }
        
        if(postfix == null) {
            Log.fatal(52, "Score.init: postfix formula was not provided");
        }

        Log.d(1, "Score.init: POSTFIX FORMULA: " + postfix);
        tokens = postfix.split("\\s+");
        for(String op : tokens) {
            op = op.trim();
            if(!op.equals("*") && !op.equals("/")  && !op.equals("+") 
                     && !op.equals("-")  && !op.equals("SQRT")
                     && !op.equals("^") && !isNumeric(op) &&
                    !fields.containsKey(op)) {
                Log.fatal(53, "Score.init: variable \"" + op + "\"" +
                              " used in the formula is not defined");
            } else {
                postfixFormula.add(op);
            }
        }

        initialized = true;            
        Log.d(0, "Score.init: scoring system is initialized");            
    }
        
    public static boolean test(HashMap<String, String> map, String strResult) {
        try {
            if(map != null && strResult != null) {
                double result = Double.parseDouble(strResult);
                Score test = new Score();
                Log.di(0, "Score.test: test values -> ");
                for(Map.Entry<String, String> e : map.entrySet()) {
                    if(!e.getKey().equals("__RESULT")) {
                        Log.di(0, e.getKey() + "=" + e.getValue() + " ");
                        test.setValue(e.getKey(), Double.parseDouble(e.getValue()));
                    }
                }
                if(result != test.getScore()) {
                    Log.err("WARNING: calculated score did not " +
                            "match the expected score from the config. file!\n" +
                            "Expected: " + result + ", calculated: " + test.getScore());
                    return false;
                } else {
                    Log.di(0, " OK\n");
                    return true;
                }
            }        
        } catch(Exception e) {
            System.err.print("Score.test: Exception");
            System.err.println(" -> " + e.toString());
            if(Log.debugLevel > 0) {
                e.printStackTrace();
            }
        }
            
        return false;
    }
    
    public static double calculate(Score s) {
        if(!initialized) {
            System.err.println("Score.calculate: not initalized");
            return -1;
        }
        
        double val, op1, op2;
        Stack<Double> stack = new Stack();
        
        for(String op : postfixFormula) {
            Log.d(5, "Score.calculate: postfix process " + op);
            try {
                switch (op) {
                    case "+":
                        op2 = stack.pop();
                        op1 = stack.pop();
                        Log.d(5, "Score.calculate: " + op1 + " + " + op2);
                        val = op1 + op2;
                        break;
                    case "-":
                        op2 = stack.pop();
                        op1 = stack.pop();
                        Log.d(5, "Score.calculate: " + op1 + " - " + op2);
                        val = op1 - op2;
                        break;
                    case "/":
                        op2 = stack.pop();
                        op1 = stack.pop();
                        Log.d(5, "Score.calculate: " + op1 + " / " + op2);
                        val = op1 / op2;
                        break;
                    case "*":
                        op2 = stack.pop();
                        op1 = stack.pop();
                        Log.d(5, "Score.calculate: " + op1 + " * " + op2);
                        val = op1 * op2;                   
                        break;
                    case "SQRT":
                        op1 = stack.pop();
                        val = Math.sqrt(op1);
                        Log.d(5, "Score.calculate: sqrt(" + op1 + ")");
                        break;
                    case "^":
                        op2 = stack.pop();
                        op1 = stack.pop();
                        Log.d(5, "Score.calculate: " + op1 + " ^ " + op2);
                        val = Math.pow(op1, op2);             
                        break;
                    default:
                        if(isNumeric(op)) {
                            val = Double.parseDouble(op);
                            Log.d(5, "Score.calculate: " + val + " [literal]");
                        } else if(!s.containsKey(op)) {
                            val = defaultValue.get(op);
                            Log.d(5, "Score.calculate: " + val + " [default]");
                        } else {
                            val = s.getValue(op);
                        }
                        break;
                }
                stack.push(val);
                Log.d(5, "Score.calculate: push " + val);
            } catch(EmptyStackException ese) {
                Log.fatal(54, "Score.calculate: Oops! looks like scoring failed, " + 
                          "there are too many variables and not enough operators\n" +
                          "Is your formula correct?\nPostfix formula = " + 
                          Config.getValue("formula", "postfix"));
            }            
        }
        if(stack.size() > 1) {
            Log.fatal(55, "Score.calculate: Oops! looks like scoring failed, " + 
                          "there are too many operators and not enough " +
                          "variables\n" +
                          "Is your formula correct?\nPostfix formula = " + 
                          Config.getValue("formula", "postfix"));
        }
        
        return stack.pop();
    }
    
    public Score() {
        values = new HashMap();
    }
    
    public void setValue(String key, double value) {
        values.put(key, value);
    }
    
    public double getValue(String key) {
        return values.get(key);
    }
     
    public double getScore() {
        return calculate(this);
    }
    
    public boolean containsKey(String key) {
        return values.containsKey(key);
    }
    
    public void setCompleted(boolean b) {
        this.teamCompletedTrack = b;
    }
    
    public boolean isCompleted() {
        return teamCompletedTrack;
    }
    
    public static boolean isNumeric(String str) {  
        try {  
            double d = Double.parseDouble(str);  
        } catch(NumberFormatException nfe) {  
            return false;  
        }     
        return true;  
    }
    
    public static boolean initialized() {
        return initialized;
    }
}
 