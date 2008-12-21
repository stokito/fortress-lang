/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Useful;
import junit.framework.TestCase;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.nodes_util.NodeFactory.*;

public class StaticParamEnvJUTest extends TestCase {
    private final StaticParam FOO = makeTypeParam(testSpan, "Foo");
    private final StaticParam BAZ = makeTypeParam(testSpan, "Baz");
    private final StaticParam BAR = makeTypeParam(testSpan, "Bar");

    private final StaticParam P = makeBoolParam(testSpan, "p");
    private final StaticParam N = makeNatParam(testSpan, "n");
    private final StaticParam Z = makeIntParam(testSpan, "z");
    private final StaticParam M_ = makeUnitParam(testSpan, "m_");
    private final StaticParam LENGTH = makeDimParam(testSpan, "Length");
    private final StaticParam AND = makeOpParam(testSpan, "AND");

    private final StaticParamEnv NIL = StaticParamEnv.make();

    private final StaticParamEnv extended = StaticParamEnv.make(FOO, BAZ, BAR);

    private final StaticParamEnv moreExtended =
        extended.extend(P, N, Z, M_, LENGTH, AND);

    public void testEmptyStaticParamEnv() {

        assertEquals(Option.none(), NIL.binding(testSpan, "blah"));
    }

    public void testBinding() {
        assertEquals(FOO, extended.binding(testSpan, "Foo").unwrap());
        assertEquals(BAZ, extended.binding(testSpan, "Baz").unwrap());
        assertEquals(BAR, extended.binding(testSpan, "Bar").unwrap());

        assertEquals(Option.none(), extended.binding(testSpan, "goo"));
    }

    public void testBindingExtended() {
        assertEquals(P, moreExtended.binding(testSpan, "p").unwrap());
        assertEquals(N, moreExtended.binding(testSpan, "n").unwrap());
        assertEquals(Z, moreExtended.binding(testSpan, "z").unwrap());
        assertEquals(M_, moreExtended.binding(testSpan, "m_").unwrap());
        assertEquals(LENGTH, moreExtended.binding(testSpan, "Length").unwrap());
        assertEquals(AND, moreExtended.opBinding(testSpan, "AND").unwrap());
    }
}
