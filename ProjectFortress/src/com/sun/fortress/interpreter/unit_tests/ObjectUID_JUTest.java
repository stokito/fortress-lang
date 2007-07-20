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

package com.sun.fortress.interpreter.unit_tests;

import junit.framework.Assert;

import com.sun.fortress.nodes_util.UIDComparator;
import com.sun.fortress.nodes_util.UIDMapFactory;
import com.sun.fortress.nodes_util.UIDObject;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.TcWrapper;

public class ObjectUID_JUTest extends TcWrapper {

    public ObjectUID_JUTest() {
        // TODO Auto-generated constructor stub
    }

    public ObjectUID_JUTest(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    static class HasUID extends UIDObject {

    }

    public void testUID() {
        staticTestUID();
    }

    static public void staticTestUID() {
        BATree<UIDObject, UIDObject> m = UIDMapFactory.<UIDObject>make();

        for (int i = 0; i < 10; i++) {
            HasUID h = new HasUID();
            System.out.println("Sample uid = 0x" + Long.toHexString(h.getUID()));
        }

        for (int i = 0; i < 100000; i++) {
            HasUID h = new HasUID();
            Assert.assertTrue("UIDs must be positive", h.getUID() > 0);
            m.put(h, h);
        }

        System.err.println("Finished adding to map");

        for (UIDObject k : m.keySet()) {
            UIDObject v = m.get(k);
            Assert.assertTrue("UIDS must be unique (two uids maoped to same object", k == v);
        }
    }

    public static void main() {
        staticTestUID();
    }

}
