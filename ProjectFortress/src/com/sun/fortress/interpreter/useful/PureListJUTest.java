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

package com.sun.fortress.interpreter.useful;

import junit.framework.TestCase;

public class PureListJUTest extends TestCase {
    public void testIterator() {
        PureList<Integer> list = PureList.make(0,1,2,3);
        Object[] array = list.toArray();
        int counter = 0;
        for (int elt : list) {
            counter++;
            assertTrue("PureList iterator not returning correct elements",
                    elt == ((Integer)array[elt]).intValue());
        }
        assertEquals("Incorrect number of iterations over list.", 4, counter);
    }
}