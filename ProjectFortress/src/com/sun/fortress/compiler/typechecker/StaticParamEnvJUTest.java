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
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.nodes_util.NodeFactory.*;

public class StaticParamEnvJUTest extends TestCase {
    private final StaticParam FOO = makeTypeParam("Foo");
    private final StaticParam BAZ = makeTypeParam("Baz");
    private final StaticParam BAR = makeTypeParam("Bar");

    private final BoolParam P = makeBoolParam("p");
    private final NatParam N = makeNatParam("n");
    private final IntParam Z = makeIntParam("z");
    private final UnitParam M_ = makeUnitParam("m_");
    private final DimParam LENGTH = makeDimParam("Length");
    private final OpParam AND = makeOpParam("AND");

    private final StaticParamEnv NIL = StaticParamEnv.make();

    private final StaticParamEnv extended = StaticParamEnv.make(FOO, BAZ, BAR);

    private final StaticParamEnv moreExtended =
        extended.extend(P, N, Z, M_, LENGTH, AND);

    public void testEmptyStaticParamEnv() {

        assertEquals(Option.none(), NIL.binding("blah"));
    }

    public void testBinding() {
        assertEquals(FOO, extended.binding("Foo").unwrap());
        assertEquals(BAZ, extended.binding("Baz").unwrap());
        assertEquals(BAR, extended.binding("Bar").unwrap());

        assertEquals(Option.none(), extended.binding("goo"));
    }

    public void testBindingExtended() {
        assertEquals(P, moreExtended.binding("p").unwrap());
        assertEquals(N, moreExtended.binding("n").unwrap());
        assertEquals(Z, moreExtended.binding("z").unwrap());
        assertEquals(M_, moreExtended.binding("m_").unwrap());
        assertEquals(LENGTH, moreExtended.binding("Length").unwrap());
        assertEquals(AND, moreExtended.opBinding("AND").unwrap());
    }
}
