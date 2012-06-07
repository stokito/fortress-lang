/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.nodes_util.UIDMapFactory;
import com.sun.fortress.nodes_util.UIDObject;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.TestCaseWrapper;
import junit.framework.Assert;

public class ObjectUID_JUTest extends TestCaseWrapper {

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

        for (BATree.Entry<UIDObject, UIDObject> k : m.entrySet()) {
            UIDObject v = k.getValue();
            Assert.assertTrue("UIDS must be unique (two uids maoped to same object", k.getKey() == v);
        }
    }

    public static void main() {
        staticTestUID();
    }

}
