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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.sun.fortress.nodes_util.NodeFactory.*;

public final class Types {

    private Types() {}

    public static final Id ANY_NAME = makeId("AnyType", "Any");
    public static final Type ANY = new AnyType();
    public static final Type BOTTOM = new BottomType();
    public static final Type OBJECT = makeTraitType("FortressLibrary", "Object");
    // public static final Type TUPLE = NodeFactory.makeTraitType("FortressBuiltin", "Tuple");
    
    public static final Domain BOTTOM_DOMAIN = makeDomain(BOTTOM);

    public static final Type VOID = new VoidType();
    public static final Type FLOAT_LITERAL = makeTraitType("FortressBuiltin", "FloatLiteral");
    public static final Type INT_LITERAL = makeTraitType("FortressBuiltin", "IntLiteral");
    public static final Type BOOLEAN = makeTraitType("FortressBuiltin", "Boolean");
    public static final Type CHAR = makeTraitType("FortressBuiltin", "Char");
    public static final Type STRING = makeTraitType("FortressBuiltin", "String");
    public static final Type REGION = makeTraitType("FortressLibrary", "Region");
    public static final Type LABEL = new LabelType();
    
    public static final Type makeVarargsParamType(Type varargsType) {
        // TODO: parameterize?
        return makeTraitType("FortressBuiltin", "ImmutableHeapSequence");
    }
    
    public static Type makeThreadType(Type typeArg) {
        return makeTraitType(makeId("FortressBuiltin", "Thread"),
                             makeTypeArg(typeArg));
    }
    
    public static Type makeGeneratorType(Type typeArg) {
        return makeTraitType(makeId("FortressLibrary", "Generator"),
                             makeTypeArg(typeArg));
    }

    /**
     * Produce a type representing a Domain with any keyword types removed.
     * May return a TupleType, a VarargTupleType, or a type representing a singleton
     * argument.
     */
    public static Type stripKeywords(Domain d) {
        if (d.getVarargs().isSome()) {
            return new VarargTupleType(d.getArgs(), d.getVarargs().unwrap());
        }
        else {
            List<Type> args = d.getArgs();
            switch (args.size()) {
                case 0: return VOID;
                case 1: return args.get(0);
                default: return new TupleType(args);
            }
        }
    }
    
    /**
     * Produce a map from keyword names to types.
     */
    public static Map<Id, Type> extractKeywords(Domain d) {
        // Don't waste time allocating a map if it will be empty (the usual case)
        if (d.getKeywords().isEmpty()) { return Collections.<Id, Type>emptyMap(); }
        else {
            Map<Id, Type> result = new HashMap<Id, Type>(8);
            for (KeywordType k : d.getKeywords()) {
                result.put(k.getName(), k.getType());
            }
            return result;
        }
    }
    
}
