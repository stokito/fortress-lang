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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.Useful;
import junit.framework.TestCase;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

public class StaticParamEnvJUTest extends TestCase {
    private final StaticParam FOO = makeTypeParam("Foo");
    private final StaticParam BAZ = makeTypeParam("Baz");
    private final StaticParam BAR = makeTypeParam("Bar");

    private final BoolParam P = makeBoolParam("p");
    private final NatParam N = makeNatParam("n");
    private final IntParam Z = makeIntParam("z");
    private final UnitParam M_ = makeUnitParam("m_");
    private final DimParam LENGTH = makeDimParam("Length");
    private final OprParam AND = makeOprParam("AND");

    private final StaticParamEnv NIL = StaticParamEnv.make();

    private final StaticParamEnv extended = StaticParamEnv.make(FOO, BAZ, BAR);

    private final StaticParamEnv moreExtended =
        extended.extend(P, N, Z, M_, LENGTH, AND);

    public void testEmptyStaticParamEnv() {

        assertEquals(none(), NIL.binding("blah"));
    }

    public void testBinding() {
        assertEquals(FOO, unwrap(extended.binding("Foo")));
        assertEquals(BAZ, unwrap(extended.binding("Baz")));
        assertEquals(BAR, unwrap(extended.binding("Bar")));

        assertEquals(none(), extended.binding("goo"));
    }

    public void testBindingExtended() {
        assertEquals(P, unwrap(moreExtended.binding("p")));
        assertEquals(N, unwrap(moreExtended.binding("n")));
        assertEquals(Z, unwrap(moreExtended.binding("z")));
        assertEquals(M_, unwrap(moreExtended.binding("m_")));
        assertEquals(LENGTH, unwrap(moreExtended.binding("Length")));
        assertEquals(AND, unwrap(moreExtended.opBinding("AND")));
    }
}
