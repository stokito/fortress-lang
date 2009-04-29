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
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Useful;
import junit.framework.TestCase;

import com.sun.fortress.compiler.typechecker.EmptyTypeEnv;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

public class TypeEnvJUTest extends TestCase {
    private static Span span = NodeFactory.makeSpan("TypeEnvJUTest bogus");

    private final Type FOO = makeVarType(span, "Foo");
    private final Type BAZ = makeVarType(span, "Baz");
    private final Type BAR = makeVarType(span, "Bar");

    private final TypeEnv extended = TypeEnv.make(makeLValue("x", FOO),
                                                  makeLValue("y", BAZ),
                                                  makeLValue("z", BAR));

    private final TypeEnv moreExtended =
        extended.extend(makeLValue("a", FOO, Modifiers.Var),
                        makeLValue("b",
                                   BAZ,
                                   Modifiers.Abstract),
                        makeLValue("c",
                                   BAR,
                                   Modifiers.combine(Modifiers.Wrapped,
                                                     Modifiers.Hidden,
                                                     Modifiers.Settable)));

    public void testEmptyTypeEnv() {
        assertEquals(none(), TypeEnv.make().getType(makeId(span,"x")));
        assertEquals(none(), TypeEnv.make().mods(makeId(span,"x")));
        assertEquals(none(), TypeEnv.make().mutable(makeId(span,"x")));
    }

    public void testLookupType() {
        assertEquals(FOO, extended.type("x").unwrap());
        assertEquals(BAZ, extended.type("y").unwrap());
        assertEquals(BAR, extended.type("z").unwrap());

        assert(! (BAR.equals(extended.type("x").unwrap())));
    }

    public void testLookupMods() {
        assertEquals(Modifiers.None, moreExtended.mods("x").unwrap());
        assertEquals(Modifiers.Abstract, moreExtended.mods("b").unwrap());
    }

    public void testLookupMutable() {
        assertTrue("Variable a should not be mutable", moreExtended.mutable("a").unwrap());
        assertTrue("Variable c should not be mutable", moreExtended.mutable("c").unwrap());
        assertFalse("Variable b should not be mutable", moreExtended.mutable("b").unwrap());

        assertFalse("Variable x should not be mutable", moreExtended.mutable("x").unwrap());

        assertEquals("Variable d does not exist", none(), moreExtended.mutable("d"));
    }
}
