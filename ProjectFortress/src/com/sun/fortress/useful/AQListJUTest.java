/*******************************************************************************
 Copyright 2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

import java.util.List;

public class AQListJUTest extends TestCaseWrapper {

    
    List<String> make(String s) {
        List<String> l = new ArrayQueueList<String>();
        l.add(s);
        return l;
    }
    
    public void testOne() {
        List<String> l = make("cat");
        assertEquals(1, l.size());
        assertEquals("cat", l.get(0));
        assertEquals("cat", l.remove(0));
        assertEquals(0, l.size());
    }
 
    public void testMore() {
        List<String> l = make("cat");
        String[] animals = {"dog","emu","fox", "gnu", "hen", "iguana", "joey", "kite", "lemur", "monkey", "narwhal"};
        List<String> al = Useful.list(animals);
        l.addAll(al);
        assertEquals(1+animals.length, l.size());
        assertEquals("cat", l.get(0));
        assertEquals("cat", l.remove(0));
        assertEquals("dog", l.get(0));

        assertEquals(animals.length, l.size());
        assertEquals(l, al);
        
        for (int i = 0; i < animals.length; i++) {
            String a = l.remove(0);
            assertEquals(a, animals[i]);
            l.add(a);
        }

        assertEquals(animals.length, l.size());
        assertEquals(l, al);
        
    }

    public void testAddI() {
        List<String> l = make("cat");
        String[] animals = {"dog","emu","fox", "gnu", "hen", "iguana", "joey", "kite", "lemur", "monkey", "narwhal"};
        List<String> al = Useful.list(animals);
        l.addAll(al);
        l.remove(0);

        for (int i = 0; i < animals.length; i++) {
            String a =  animals[i];
            l.add(2*i, a);
        }

        assertEquals(2*animals.length, l.size());
        for (int i = 0; i < animals.length; i++) {
            String a =  animals[i];
            assertEquals(l.get(2*i), a);
            assertEquals(l.get(2*i+1), a);
        }
        
        for (int i = animals.length-1; i >= 0; i--) {
            String a =  animals[i];
            int ii = 2*i;
            String b = l.remove(ii);
            assertEquals(a, l.get(ii));
            assertEquals(a, b);
        }

        assertEquals(animals.length, l.size());
        assertEquals(l, al);
    }

    public void testAddMore() {
        List<String> l = make("cat");
        String[] animals = {"dog","emu","fox", "gnu", "hen", "iguana", "joey", "kite", "lemur", "monkey", "narwhal"};
        List<String> al = Useful.list(animals);
        l.addAll(al);
        l.remove(0);

        for (int i = 0; i < animals.length; i++) {
            String a =  animals[i];
            l.add(a);
            al.add(a);
            if (! al.equals(l)) 
                assertEquals(al, l);
        }

        for (int i = 0; i < animals.length; i++) {
            String a =  animals[i];
            l.add(a);
            al.add(a);
            if (! al.equals(l)) 
                assertEquals(al, l);
        }

        for (int i = 0; i < animals.length; i++) {
            String a =  animals[i];
            l.add(a);
            al.add(a);
            if (! al.equals(l)) 
                assertEquals(al, l);
        }
        
        
        for (int j = 0; j < 100; j++) {
        
        for (int i = 0; i < animals.length; i++) {
            assertEquals(al.remove(0), l.remove(0));
            assertEquals(al, l);
        }
        
        for (int i = 0; i < animals.length; i++) {
            String a =  animals[i];
            l.add(a);
            al.add(a);
            assertEquals(al, l);
        }
        }
        
    }

    
}
