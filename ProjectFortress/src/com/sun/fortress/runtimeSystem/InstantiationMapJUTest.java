/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.util.HashMap;

import junit.framework.TestCase;

public class InstantiationMapJUTest extends TestCase {

    InstantiationMap map = newInstantiationMap(); 
    static InstantiationMap newInstantiationMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("A", "ant");
        map.put("B", "bat");
        map.put("C", "cat");
        map.put("D", "dog");
        map.put("E", "emu");
        map.put("F", "fox");
        return new InstantiationMap(map);
    }
    
    public void testMaybeVarInOxfords() {
        StringBuilder b = new StringBuilder();
        int i = map.maybeVarInOxfords("A"+Naming.RIGHT_OXFORD, 0, b, true);
  
        assertEquals(2, i);
        assertEquals("ant"+Naming.RIGHT_OXFORD, b.toString());
 
        b = new StringBuilder();
        i = map.maybeVarInOxfords("BB"+Naming.RIGHT_OXFORD, 0, b, true);
        assertEquals(3, i);
        assertEquals("BB"+Naming.RIGHT_OXFORD, b.toString());

        b = new StringBuilder();
        i = map.maybeVarInOxfords("C" + Naming.GENERIC_SEPARATOR + "D"+Naming.RIGHT_OXFORD, 0, b, true);
        assertEquals(4, i);
        assertEquals("cat" + Naming.GENERIC_SEPARATOR + "dog"+Naming.RIGHT_OXFORD, b.toString());
        
        // Don't know what this is for, we aren't using it.
//        b = new StringBuilder();
//        i = map.maybeVarInOxfords("C=C;D=D"+Naming.RIGHT_OXFORD, 0, b);
//        assertEquals(8, i);
//        assertEquals("C=cat;D=dog"+Naming.RIGHT_OXFORD, b.toString());
    }

    public void testMaybeVarInLSemi() {
        // fail("Not yet implemented"); // TODO
    }

    public void testMaybeBareVar() {
        StringBuilder b = new StringBuilder();
        int i = map.maybeBareVar("A", 0, b, false, false, true);
        assertEquals(1, i);
        assertEquals("ant", b.toString());
 
        b = new StringBuilder();
        i = map.maybeBareVar("B;", 0, b, false, false, true);
        assertEquals(1, i);
        assertEquals("bat", b.toString());

        b = new StringBuilder();
        i = map.maybeBareVar("CC;", 0, b, false, false, true);
        assertEquals(2, i);
        assertEquals("CC", b.toString());

    }

}
