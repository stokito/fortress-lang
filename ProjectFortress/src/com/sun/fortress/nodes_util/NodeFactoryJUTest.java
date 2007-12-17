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

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.TcWrapper;

/**
 * More tests are needed over NodeFactory. This is a start.
 */
public class NodeFactoryJUTest extends TcWrapper {
    public void testMakeAPIName() {
        APIName result = NodeFactory.makeAPIName(new Span(), "foobar.fss", "\\.");
        assertEquals("foobar", result.toString());
    }
}
