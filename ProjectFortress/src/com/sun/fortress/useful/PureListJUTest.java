/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class PureListJUTest extends com.sun.fortress.useful.TestCaseWrapper {
    public void testIterator() {
        PureList<Integer> list = PureList.make(0, 1, 2, 3);
        Object[] array = list.toArray();
        int counter = 0;
        for (int elt : list) {
            counter++;
            assertTrue("PureList iterator not returning correct elements", elt == ((Integer) array[elt]).intValue());
        }
        assertEquals("Incorrect number of iterations over list.", 4, counter);
    }
}
