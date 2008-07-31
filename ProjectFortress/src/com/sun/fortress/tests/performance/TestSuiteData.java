package com.sun.fortress.tests.performance;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestSuiteData implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2469807233782317273L;
    private final Map<String, Map<String,Double>> data;
    
    TestSuiteData() {
        data = new HashMap<String,Map<String,Double>>(); 
    }

    public Set<String> getTestSuites() {
        return data.keySet();
    }
    
    public Set<String> getTestCases(String testSuite) {
        return data.get(testSuite).keySet();
    }
    
    public Double getTime(String testSuite, String testCase) {
        return data.get(testSuite).get(testCase);
    }

    public void add(String testSuite, String testCase, Double time) {
        Map<String, Double> value = data.get(testSuite);
        if (value == null) {
            value = new HashMap<String,Double>();
            value.put(testCase, time);
            data.put(testSuite, value);
        } else {
            value.put(testCase, time);
        }        
    }
    

}
