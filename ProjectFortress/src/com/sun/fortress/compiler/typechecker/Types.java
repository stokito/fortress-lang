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
import com.sun.fortress.nodes_util.NodeFactory;
import java.util.Arrays;
import java.util.Collections;

import static com.sun.fortress.nodes_util.NodeFactory.*;

public class Types {
    public static final Type BOTTOM = new BottomType();
    public static final Type ANY = NodeFactory.makeInstantiatedType("FortressBuiltin", "Any");
    public static final Type OBJECT = NodeFactory.makeInstantiatedType("FortressLibrary", "Object");
    public static final Type TUPLE = NodeFactory.makeInstantiatedType("FortressBuiltin", "Tuple");
    
    public static final Type VOID = NodeFactory.makeTupleType(Collections.<Type>emptyList());
    public static final Type FLOAT_LITERAL = NodeFactory.makeInstantiatedType("FortressBuiltin", "FloatLiteral");
    public static final Type INT_LITERAL = NodeFactory.makeInstantiatedType("FortressBuiltin", "IntLiteral");
    public static final Type BOOLEAN = NodeFactory.makeInstantiatedType("FortressBuiltin", "Boolean");
    public static final Type CHAR = NodeFactory.makeInstantiatedType("FortressBuiltin", "Char");
    public static final Type STRING = NodeFactory.makeInstantiatedType("FortressBuiltin", "String");
    public static final Type REGION = NodeFactory.makeInstantiatedType("FortressBuiltin", "Region");

    public static final Type fromVarargsType(VarargsType varargsType) {
        return NodeFactory.makeInstantiatedType(varargsType.getSpan(),
                                                false,
                                                makeQualifiedIdName(Arrays.asList(makeId("FortressBuiltin")),
                                                                    makeId("ImmutableHeapSequence")));
    }
}
