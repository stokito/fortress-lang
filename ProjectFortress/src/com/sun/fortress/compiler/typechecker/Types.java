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

import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
import java.util.ArrayList;

public class Types {
    public static final Type VOID = NodeFactory.makeTupleType(new ArrayList<Type>());
    public static final Type FLOAT_LITERAL = NodeFactory.makeInstantiatedType("FortressBuiltin", "FloatLiteral");
    public static final Type INT_LITERAL = NodeFactory.makeInstantiatedType("FortressBuiltin", "IntLiteral");
    public static final Type BOOLEAN = NodeFactory.makeInstantiatedType("FortressBuiltin", "Boolean");
    public static final Type CHAR = NodeFactory.makeInstantiatedType("FortressBuiltin", "Char");
    public static final Type STRING = NodeFactory.makeInstantiatedType("FortressBuiltin", "String");
    public static final Type REGION = NodeFactory.makeInstantiatedType("FortressBuiltin", "Region");
}
