/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.runtimeSystem;

import junit.framework.TestCase;

public class BAlongTreeJUTest extends TestCase {
    
    BAlongTree bat = new BAlongTree();

    void zero() {
        bat.put(0, "zero");
    }
    
    void one() {
        bat.put(1, "one");
    }
    
    void two() {
        bat.put(2, "two");
    }
    
    void three() {
        bat.put(3, "three");
    }
    
    private void load() {
        zero();
        one();
        two();
        three();
    }


    public void testSize() {
        assertEquals(bat.size(), 0);
        zero();
        assertEquals(bat.size(), 1);
        zero();
        assertEquals(bat.size(), 1);

    }

    public void testGet() {
        load();
        assertEquals(bat.get(0), "zero");
        assertEquals(bat.get(1), "one");
        assertEquals(bat.get(2), "two");
        assertEquals(bat.get(3), "three");
    }

     public void testGetData() {
         load();
         assertEquals(bat.getData(0), "zero");
         assertEquals(bat.getData(1), "one");
         assertEquals(bat.getData(2), "two");
         assertEquals(bat.getData(3), "three");
    }

    public void testGetKey() {
        load();
        assertEquals(bat.getKey(0), 0);
        assertEquals(bat.getKey(1), 1);
        assertEquals(bat.getKey(2), 2);
        assertEquals(bat.getKey(3), 3);
    }

    public void testIndexOf() {
        load();
        assertEquals(bat.indexOf(0), 0);
        assertEquals(bat.indexOf(1), 1);
        assertEquals(bat.indexOf(2), 2);
        assertEquals(bat.indexOf(3), 3);
    }

    public void testMin() {
        load();
        assertEquals(bat.min(), "zero");
    }

    public void testMax() {
        load();
        assertEquals(bat.max(), "three");

    }

}
