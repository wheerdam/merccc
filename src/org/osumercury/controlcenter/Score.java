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
import java.util.Stack;
import java.util.Map;

/**
 *
 * @author wira
 */
public class Score {
    private static boolean initialized = false;
    private static ArrayList<String> postfixFormula;
    public static HashMap<String, String> fields;
    public static HashMap<String, String> description;
    public static HashMap<String, Integer> type;
    public static HashMap<String, Double[]> possibleValues;
    public static HashMap<String, Double> defaultValue;
    
    private HashMap<String, Double> values;
    private boolean teamCompletedTrack = false;
    
    public static void init() {
        try {
            String postfix = Config.getValue("formula", "postfix");
            int typeTemp;
            postfixFormula = new ArrayList();
            fields = Config.getSectionAsMap("fields");
            type = new HashMap();
            description = new HashMap();
            defaultValue = new HashMap();
            possibleValues = new HashMap();
            String[] tokens;
            
            for(Map.Entry<String, String> e : fields.entrySet()) {
                Log.d(0, "Score.init: > " + e.getKey() + "=" + e.getValue());
                tokens = e.getValue().split(",");
                typeTemp = Integer.parseInt(tokens[2].trim());
                description.put(e.getKey(), tokens[0].trim());
                defaultValue.put(e.getKey(), Double.parseDouble(tokens[1].trim()));
                type.put(e.getKey(), typeTemp);
                if(typeTemp > 1) {
                    tokens = tokens[3].split(":");
                    Double[] pVals = new Double[tokens.length];
                    for (int i = 0; i < tokens.length; i++) {
                        pVals[i] = Double.parseDouble(tokens[i].trim());
                    }
                    possibleValues.put(e.getKey(), pVals);
                }
            }

            Log.d(0, "Score.init: POSTFIX FORMULA: " + postfix);
            tokens = postfix.split("\\s+");
            for(String op : tokens) {
                op = op.trim();
                if(!op.equals("*") && !op.equals("/")  && !op.equals("+") 
                         && !op.equals("-")  && !op.equals("SQRT")
                         && !op.equals("^") && !isNumeric(op) &&
                        !fields.containsKey(op)) {
                    System.err.println("Score.init: \"" + op + "\"" +
                            " is not a valid field");
                    return;
                } else {
                    postfixFormula.add(op);
                }
            }
            
            initialized = true;            
            
            HashMap<String, String> map = Config.getSectionAsMap("test");
            if(map != null) {
                Score test = new Score();
                Log.di(0, "Score.init: test values -> ");
                for(Map.Entry<String, String> e : map.entrySet()) {
                    if(!e.getKey().equals("__RESULT")) {
                        Log.di(0, e.getKey() + "=" + e.getValue() + " ");
                        test.setValue(e.getKey(), Double.parseDouble(e.getValue()));
                    }
                }
                Log.d(0, "");
                double result = Double.parseDouble(map.get("__RESULT"));
                Log.di(0, "Score.init: test result -> expected=" + result +
                        " calculated=" + test.getScore());
                if(result != test.getScore()) {
                    Log.di(0, " ERROR\n");
                    System.err.println("===");
                    System.err.println("WARNING: calculated score did not "
                    + "match the expected score from the config. file!");
                    System.err.println("===");
                } else {
                    Log.di(0, " OK\n");
                }
            }
        } catch(Exception e) {
            System.err.print("Score.initialize: Exception");
            System.err.println(" -> " + e.toString());
            if(Log.debugLevel > 0) {
                e.printStackTrace();
            }
        }
    }
    
    public static double calculate(Score s) {
        if(!initialized) {
            System.err.println("Score.calculate: not initalized");
            return -1;
        }
        
        double val, op1, op2;
        Stack<Double> stack = new Stack();
        
        for(String op : postfixFormula) {
            Log.d(2, "Score.calculate: postfix process " + op);
            switch (op) {
                case "+":
                    op2 = stack.pop();
                    op1 = stack.pop();
                    Log.d(2, "Score.calculate: " + op1 + " + " + op2);
                    val = op1 + op2;
                    break;
                case "-":
                    op2 = stack.pop();
                    op1 = stack.pop();
                    Log.d(2, "Score.calculate: " + op1 + " - " + op2);
                    val = op1 - op2;
                    break;
                case "/":
                    op2 = stack.pop();
                    op1 = stack.pop();
                    Log.d(2, "Score.calculate: " + op1 + " / " + op2);
                    val = op1 / op2;
                    break;
                case "*":
                    op2 = stack.pop();
                    op1 = stack.pop();
                    Log.d(2, "Score.calculate: " + op1 + " * " + op2);
                    val = op1 * op2;                   
                    break;
                case "SQRT":
                    op1 = stack.pop();
                    val = Math.sqrt(op1);
                    Log.d(2, "Score.calculate: sqrt(" + op1 + ")");
                    break;
                case "^":
                    op2 = stack.pop();
                    op1 = stack.pop();
                    Log.d(2, "Score.calculate: " + op1 + " ^ " + op2);
                    val = Math.pow(op1, op2);             
                    break;
                default:
                    if(isNumeric(op)) {
                        val = Double.parseDouble(op);
                        Log.d(2, "Score.calculate: " + val + " [literal]");
                    } else if(!s.containsKey(op)) {
                        val = defaultValue.get(op);
                        Log.d(2, "Score.calculate: " + val + " [default]");
                    } else {
                        val = s.getValue(op);
                    }
                    break;
            }
            stack.push(val);
            Log.d(2, "Score.calculate: push " + val);
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
 