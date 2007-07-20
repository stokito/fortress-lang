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

import junit.framework.TestCase;

public class LatticeIntervalMapJUTest extends com.sun.fortress.useful.TcWrapper  {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(ABoundingMapJUTest.class);
    }

    LongBitsLatticeOps ops = LongBitsLatticeOps.V;
    LatticeIntervalMap<String, Long, LongBitsLatticeOps> m = new
        LatticeIntervalMap<String, Long, LongBitsLatticeOps>
        (new BATree2<String, Long, Long>(StringComparer.V), ops);
    
    BoundingMap<String, Long, LongBitsLatticeOps> md = m.dual();

    Long[] l = new Long[128];
    
    public LatticeIntervalMapJUTest() {
        super();
        for (int i = 0; i < l.length; i++) {
            l[i] = new Long(i);
        }
    }
    
    public void testPutGet() {
        m.put("a", l[1]);
        assertEquals(l[1], m.get("a"));
    }
    
    public void testPutJoin() {
        m.joinPut("a", l[3]);
        m.joinPut("a", l[5]);
        assertEquals(l[7], m.get("a"));
    }
    
    public void testPutMeet() {
        m.meetPut("a", l[3]);
        m.meetPut("a", l[5]);
        assertEquals(l[1], m.getUpper("a"));
    }
     
    public void testDualPutJoin() {
       md.meetPut("a", l[3]);
       md.meetPut("a", l[5]);
       assertEquals(l[7], m.get("a"));
    }
    
    public void testDualPutMeet() {
        md.joinPut("a", l[3]);
        md.joinPut("a", l[5]);
        assertEquals(l[1], m.getUpper("a"));
    }
     
}
