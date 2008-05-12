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

package com.sun.fortress.compiler;

import com.sun.fortress.nodes.*;
import java.util.Collections;

import static com.sun.fortress.nodes_util.NodeFactory.makeId;
import static com.sun.fortress.nodes_util.NodeFactory.makeTraitType;

public final class Types {

    private Types() {}

    public static final Id ANY_NAME = makeId("AnyType", "Any");
    public static final Type ANY = new AnyType();
    public static final Type BOTTOM = new BottomType();
    public static final Type OBJECT = makeTraitType("FortressLibrary", "Object");
    // public static final Type TUPLE = NodeFactory.makeTraitType("FortressBuiltin", "Tuple");

    public static final Type VOID = new VoidType();
    public static final Type FLOAT_LITERAL = makeTraitType("FortressBuiltin", "FloatLiteral");
    public static final Type INT_LITERAL = makeTraitType("FortressBuiltin", "IntLiteral");
    public static final Type BOOLEAN = makeTraitType("FortressBuiltin", "Boolean");
    public static final Type CHAR = makeTraitType("FortressBuiltin", "Char");
    public static final Type STRING = makeTraitType("FortressBuiltin", "String");
    public static final Type REGION = makeTraitType("FortressLibrary", "Region");
    public static final Type LABEL = new LabelType();
}
