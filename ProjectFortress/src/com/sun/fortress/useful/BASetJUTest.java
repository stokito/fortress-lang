/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Random;

public class BASetJUTest extends TcWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BASetJUTest.class);
    }

    BASet<String> t = new BASet<String> (String.CASE_INSENSITIVE_ORDER);
    
    String[] animals = {
         "alpaca",
         "ant",
         "auk",
         "bat",
         "beetle",
         "bison",
         "buffalo",
         "camel",
         "cat",
         "cavy",
         "crab",
         "deer",
         "dingo",
         "dodo",
         "dog",
         "dove",
         "eagle",
         "eel",
         "eland",
         "elephant",
         "elk",
         "emu",
         "finch",
         "gar",
         "giraffe",
         "gnu",
         "guanaco",
         "gull",
         "hawk",
         "hedgehog",
         "hyena",
         "ibis",
         "iguana",
         "jackal",
         "jaguar",
         "kangaroo",
         "koala",
         "lemur",
         "leopard",
         "llama",
         "manatee",
         "mule",
         "narwhal",
         "nutria",
         "octopus",
         "osprey",
         "ostrich",
         "owl",
         "penguin",
         "pigeon",
         "piranha",
         "puffin",
         "quagga",
         "quail",
         "quokka",
         "rat",
         "ray",
         "seal",
         "shark",
         "snake",
         "spider",
         "tern",
         "tiger",
         "turtle",
         "unicorn",
         "vicuna",
         "vole",
         "vulture",
         "walrus",
         "warthog",
         "worm",
         "xiphias",
         "yak",
         "zebra"
    };
    
    public void testEmpty() {
        assertEquals(t.size(), 0);
        assertEquals(t.min(), null);
        assertEquals(t.max(), null);
        t.ok();       
    }
    public void testOne() {
        t.add("cat");
        assertEquals(t.size(), 1);
        assertEquals(t.min(), "cat");
        assertEquals(t.max(), "cat");
        t.ok();       
    }

    public void testAddsAscending() {
        int l = animals.length;
        
        for (int i = 0; i < l; i++) {
            t.add(animals[i]);
            assertEquals(t.size(), i+1);
            assertEquals(t.min(), animals[0]);
            assertEquals(t.max(), animals[i]);
            t.ok();     
        }
        t.ok();       
    }

    public void testAddsDescending() {
        int l = animals.length;
        
        for (int i = 0; i < l; i++) {
            int j = l - 1 - i;
            t.add(animals[j]);
            assertEquals(t.size(), i+1);
            assertEquals(t.min(), animals[j]);
            assertEquals(t.max(), animals[l-1]);
            t.ok();     
        }
        t.ok();       
    }
    
    public void testAddsDeletesAscending() {
        testAddsAscending();
        int l = animals.length;
        for (int i = 0; i < l; i++) {
            assertEquals(t.min(), animals[i]);
            assertEquals(t.max(), animals[l-1]);
            t.remove(animals[i]);
            assertEquals(t.size(), l-i-1);
            t.ok();     
        }
    }

    public void testAddsDeletesDescending() {
        testAddsAscending();
        int l = animals.length;
        for (int i = 0; i < l; i++) {
            int j = l - 1 - i;
            assertEquals(t.min(), animals[0]);
            assertEquals(t.max(), animals[j]);
            t.remove(animals[j]);
            assertEquals(t.size(), j);
            t.ok();     
        }
    }

    public void testAddsDeletesRandom() {
        
        Random r = new Random(0x12345555);
        
        for (int k = 0; k < 1000; k++) {

            testAddsAscending();
            int l = animals.length;
            for (int i = 0; i < l; i++) {
                int j = r.nextInt(l - i);
                String s = t.getKey(j);
                t.remove(s);
                assertEquals(t.size(), l - i - 1);
                t.ok();
            }
        }
    }

    
}
