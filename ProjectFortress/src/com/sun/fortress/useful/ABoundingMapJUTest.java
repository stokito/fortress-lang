/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class ABoundingMapJUTest extends TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(ABoundingMapJUTest.class);
    }

    LongBitsLatticeOps ops = LongBitsLatticeOps.V;
    ABoundingMap<String, Long, LongBitsLatticeOps> abm =
            new ABoundingMap<String, Long, LongBitsLatticeOps>(new BATree<String, Long>(DefaultComparator.V), ops);

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.ABoundingMap.ABoundingMap(Map<T, U>, LatticeOps<U>)'
     */
    public void testABoundingMapMapOfTULatticeOpsOfU() {
        // TODO Auto-generated method stub
        System.err.println("before a success");

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.ABoundingMap.dual()'
     */
    public void testDual() {
        // TODO Auto-generated method stub
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.ABoundingMap.meetPut(T, U)'
     */
    public void testMeetPut() {
        Long o = abm.meetPut("3", Long.valueOf(11));
        assertEquals(o.longValue(), 11);
        o = abm.meetPut("3", Long.valueOf(7));
        assertEquals(o.longValue(), 3);
        o = abm.get("3");
        assertEquals(o.longValue(), 3);

        o = abm.meetPut("3", ops.one());
        assertEquals(o.longValue(), 3);
        o = abm.get("3");
        assertEquals(o.longValue(), 3);
        o = abm.meetPut("3", ops.zero());
        assertEquals(o.longValue(), 0);
        o = abm.get("3");
        assertEquals(o, ops.zero());


    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.ABoundingMap.joinPut(T, U)'
     */
    public void testJoinPut() {
        Long o = abm.joinPut("15", Long.valueOf(11));
        assertEquals(o.longValue(), 11);
        o = abm.joinPut("15", Long.valueOf(7));
        assertEquals(o.longValue(), 15);
        o = abm.get("15");
        assertEquals(o.longValue(), 15);


        o = abm.joinPut("15", ops.zero());
        assertEquals(o.longValue(), 15);
        o = abm.get("15");
        assertEquals(o.longValue(), 15);
        o = abm.joinPut("15", ops.one());
        assertEquals(o.longValue(), ops.one().longValue());
        o = abm.get("15");
        assertEquals(o, ops.one());


    }

}
