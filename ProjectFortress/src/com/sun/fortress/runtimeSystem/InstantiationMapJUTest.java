/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
        StringBuffer b = new StringBuffer();
        int i = map.maybeVarInOxfords("A"+Naming.RIGHT_OXFORD, 0, b);
  
        assertEquals(2, i);
        assertEquals("ant"+Naming.RIGHT_OXFORD, b.toString());
 
        b = new StringBuffer();
        i = map.maybeVarInOxfords("BB"+Naming.RIGHT_OXFORD, 0, b);
        assertEquals(3, i);
        assertEquals("BB"+Naming.RIGHT_OXFORD, b.toString());

        b = new StringBuffer();
        i = map.maybeVarInOxfords("C;D"+Naming.RIGHT_OXFORD, 0, b);
        assertEquals(4, i);
        assertEquals("cat;dog"+Naming.RIGHT_OXFORD, b.toString());
        
        b = new StringBuffer();
        i = map.maybeVarInOxfords("C=C;D=D"+Naming.RIGHT_OXFORD, 0, b);
        assertEquals(8, i);
        assertEquals("C=cat;D=dog"+Naming.RIGHT_OXFORD, b.toString());
    }

    public void testMaybeVarInLSemi() {
        // fail("Not yet implemented"); // TODO
    }

    public void testMaybeBareVar() {
        StringBuffer b = new StringBuffer();
        int i = map.maybeBareVar("A", 0, b, false);
        assertEquals(1, i);
        assertEquals("ant", b.toString());
 
        b = new StringBuffer();
        i = map.maybeBareVar("B;", 0, b, false);
        assertEquals(1, i);
        assertEquals("bat", b.toString());

        b = new StringBuffer();
        i = map.maybeBareVar("CC;", 0, b, false);
        assertEquals(2, i);
        assertEquals("CC", b.toString());

    }

}
