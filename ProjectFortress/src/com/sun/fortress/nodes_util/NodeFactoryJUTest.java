/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.TestCaseWrapper;

/**
 * More tests are needed over NodeFactory. This is a start.
 */
public class NodeFactoryJUTest extends TestCaseWrapper {
    public void testMakeAPIName() {
        APIName result = NodeFactory.makeAPINameFromPath(null, NodeFactory.testSpan, "foobar.fss", "\\.");
        assertEquals("foobar", result.toString());
    }
}
