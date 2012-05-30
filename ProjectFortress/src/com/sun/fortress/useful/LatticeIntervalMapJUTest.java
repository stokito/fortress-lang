/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class LatticeIntervalMapJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(ABoundingMapJUTest.class);
    }

    LongBitsLatticeOps ops = LongBitsLatticeOps.V;
    LatticeIntervalMap<String, Long, LongBitsLatticeOps> m = new LatticeIntervalMap<String, Long, LongBitsLatticeOps>(
            new BATree2<String, Long, Long>(DefaultComparator.V),
            ops);

    BoundingMap<String, Long, LongBitsLatticeOps> md = m.dual();

    Long[] l = new Long[128];

    public LatticeIntervalMapJUTest() {
        super();
        for (int i = 0; i < l.length; i++) {
            l[i] = Long.valueOf(i);
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
