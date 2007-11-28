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
import junit.framework.TestCase;

import static com.sun.fortress.compiler.typechecker.EmptyTypeEnv.NIL;
import static com.sun.fortress.nodes_util.NodeFactory.*;

public class TypeEnvJUTest extends TestCase {
    public void testEmptyTypeEnv() {
        try {
            NIL.type(makeIdName("x"));
            NIL.mods(makeIdName("x"));
            NIL.mutable(makeIdName("x"));
            fail("Attempt to access an empty type environment should signal an error.");
        }
        catch (RuntimeException e) {}
    }
    
    public void testLookupType() {
        final Type FOO = makeIdType("Foo");
        TypeEnv extended = NIL.extend(makeLValue("x", FOO));
        
        assertEquals(FOO, extended.type("x"));
        assert(! (makeIdType("Bar").equals(extended.type("x"))));
    }
    
    public void testExtendedLookup() {
        final Type FOO = makeIdType("Foo");
        final Type BAZ = makeIdType("Baz");
        final Type BAR = makeIdType("Bar");
        
        TypeEnv extended = NIL.extend(makeLValue("x", FOO), 
                                      makeLValue("y", BAZ), 
                                      makeLValue("z", BAR));
        assertEquals(FOO, extended.type("x"));
        assertEquals(BAZ, extended.type("y"));
        assertEquals(BAR, extended.type("z"));
    }
}
    