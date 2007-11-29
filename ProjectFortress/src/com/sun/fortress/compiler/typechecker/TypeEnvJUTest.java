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

import static com.sun.fortress.compiler.typechecker.EmptyTypeEnv.NIL;
import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

public class TypeEnvJUTest extends TestCase {
    private final Type FOO = makeIdType("Foo");
    private final Type BAZ = makeIdType("Baz");
    private final Type BAR = makeIdType("Bar");
        
    private final TypeEnv extended = NIL.extend(makeLValue("x", FOO), 
                                                makeLValue("y", BAZ), 
                                                makeLValue("z", BAR));
    
    private final TypeEnv moreExtended = 
        extended.extend(makeLValue("a", FOO, Useful.<Modifier>list(new ModifierVar())),
                        makeLValue("b", 
                                   BAZ, 
                                   Useful.<Modifier>list(new ModifierAbstract())),
                        makeLValue("c", 
                                   BAR, 
                                   Useful.<Modifier>list(new ModifierWrapped(), 
                                                         new ModifierHidden(),
                                                         new ModifierSettable())));
            
    public void testEmptyTypeEnv() {
        assertEquals(none(), NIL.type(makeIdName("x")));
        assertEquals(none(), NIL.mods(makeIdName("x")));
        assertEquals(none(), NIL.mutable(makeIdName("x")));
    }
    
    public void testLookupType() {
        assertEquals(FOO, unwrap(unwrap(extended.type("x"))));
        assertEquals(BAZ, unwrap(unwrap(extended.type("y"))));
        assertEquals(BAR, unwrap(unwrap(extended.type("z"))));
        
        assert(! (BAR.equals(unwrap(unwrap(extended.type("x"))))));
    }
    
    public void testLookupMods() {
        assertEquals(0, unwrap(moreExtended.mods("x")).size());
        assertEquals(Useful.<Modifier>list(new ModifierAbstract()), 
                     unwrap(moreExtended.mods("b")));
    }
    
    public void testLookupMutable() {
        assertTrue("Variable a should not be mutable", unwrap(moreExtended.mutable("a")));
        assertTrue("Variable c should not be mutable", unwrap(moreExtended.mutable("c")));
        assertFalse("Variable b should not be mutable", unwrap(moreExtended.mutable("b")));
    
        assertFalse("Variable x should not be mutable", unwrap(moreExtended.mutable("x")));
        
        assertEquals("Variable d does not exist", none(), moreExtended.mutable("d"));
    }
}
    