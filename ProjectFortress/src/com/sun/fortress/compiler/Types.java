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
import com.sun.fortress.nodes_util.NodeFactory;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.nodes_util.NodeFactory.*;

/**
 * General-purpose type constants and constructors/destructors for types.
 * Unlike the methods in {@link NodeFactory}, these pay no attention to the
 * {@code span} and {@code parenthesized} AST fields, and are more "aware"
 * of the semantics of Fortress types.
 */
public final class Types {

    private Types() {}

    public static final Id ANY_NAME = makeId("AnyType", "Any");
    public static final AnyType ANY = new AnyType();
    public static final BottomType BOTTOM = new BottomType();
    public static final TraitType OBJECT = makeTraitType("FortressLibrary", "Object");
    // public static final Type TUPLE = NodeFactory.makeTraitType("FortressBuiltin", "Tuple");
    
    public static final Domain BOTTOM_DOMAIN = NodeFactory.makeDomain(BOTTOM);

    public static final VoidType VOID = new VoidType();
    public static final TraitType FLOAT_LITERAL = makeTraitType("FortressBuiltin", "FloatLiteral");
    public static final TraitType INT_LITERAL = makeTraitType("FortressBuiltin", "IntLiteral");
    public static final TraitType BOOLEAN = makeTraitType("FortressBuiltin", "Boolean");
    public static final TraitType CHAR = makeTraitType("FortressBuiltin", "Char");
    public static final TraitType STRING = makeTraitType("FortressBuiltin", "String");
    public static final TraitType REGION = makeTraitType("FortressLibrary", "Region");
    
    public static final LabelType LABEL = new LabelType();
    
    public static final TraitType makeVarargsParamType(Type varargsType) {
        // TODO: parameterize?
        return makeTraitType("FortressBuiltin", "ImmutableHeapSequence");
    }
    
    public static TraitType makeThreadType(Type typeArg) {
        return makeTraitType(makeId("FortressBuiltin", "Thread"),
                             makeTypeArg(typeArg));
    }
    
    public static TraitType makeGeneratorType(Type typeArg) {
        return makeTraitType(makeId("FortressLibrary", "Generator"),
                             makeTypeArg(typeArg));
    }
    
    
    /** Construct the appropriate type from a list of union elements. */
    public static Type makeUnion(Iterable<Type> disjuncts) {
        return MAKE_UNION.value(disjuncts);
    }
    
    /** Construct the appropriate type from a list of union elements. */
    public static final Lambda<Iterable<Type>, Type> MAKE_UNION =
        new Lambda<Iterable<Type>, Type>() {
        public Type value(Iterable<Type> ts) {
            switch (IterUtil.sizeOf(ts, 2)) {
                case 0: return BOTTOM;
                case 1: return IterUtil.first(ts);
                default: return new UnionType(IterUtil.asList(ts));
            }
        }
    };

    /** Construct the appropriate type from a list of intersection elements. */
    public static Type makeIntersection(Iterable<Type> conjuncts) {
        return MAKE_INTERSECTION.value(conjuncts);
    }
    
    /** Construct the appropriate type from a list of intersection elements. */
    public static final Lambda<Iterable<Type>, Type> MAKE_INTERSECTION =
        new Lambda<Iterable<Type>, Type>() {
        public Type value(Iterable<Type> ts) {
            switch (IterUtil.sizeOf(ts, 2)) {
                case 0: return ANY;
                case 1: return IterUtil.first(ts);
                default: return new IntersectionType(IterUtil.asList(ts));
            }
        }
    };
    
    /** Treat an arbitrary type as a union and enumerate its elements. */
    public static Iterable<Type> disjuncts(Type t) {
        return t.accept(DISJUNCTS);
    }
    
    /** Treat an arbitrary type as a union and enumerate its elements. */
    public static final NodeVisitorLambda<Iterable<Type>> DISJUNCTS =
        new NodeAbstractVisitor<Iterable<Type>>() {
        @Override public Iterable<Type> forType(Type t) {
            return IterUtil.singleton(t);
        }
        @Override public Iterable<Type> forUnionType(UnionType t) {
            return t.getElements();
        }
        @Override public Iterable<Type> forBottomType(BottomType t) {
            return IterUtil.empty();
        }
    };
    
    /** Treat an arbitrary type as an intersection and enumerate its elements. */
    public static Iterable<Type> conjuncts(Type t) {
        return t.accept(CONJUNCTS);
    }

    /** Treat an arbitrary type as an intersection and enumerate its elements. */
    public static final NodeVisitorLambda<Iterable<Type>> CONJUNCTS =
        new NodeAbstractVisitor<Iterable<Type>>() {
        @Override public Iterable<Type> forType(Type t) {
            return IterUtil.singleton(t);
        }
        @Override public Iterable<Type> forIntersectionType(IntersectionType t) {
            return t.getElements();
        }
        // TODO: the rules say Any = AND{}, but that allows tuples and arrows
        // containing Any to be equivalent to Any, which isn't what we want.
        // Need to work this out in the rules.
        //@Override public Iterable<Type> forAnyType(AnyType t) {
        //    return IterUtil.empty();
        //}
    };
    
    /**
     * Construct the appropriate type (void, a tuple, or the type itself) from a list of
     * tuple elements.
     */
    public static Type makeTuple(Iterable<Type> elements) {
        return MAKE_TUPLE.value(elements);
    }

    /** Construct the appropriate type from a list of tuple elements. */
    public static final Lambda<Iterable<Type>, Type> MAKE_TUPLE =
        new Lambda<Iterable<Type>, Type>() {
        public Type value(Iterable<Type> ts) {
            switch (IterUtil.sizeOf(ts, 2)) {
                case 0: return VOID;
                case 1: return IterUtil.first(ts);
                default: return new TupleType(IterUtil.asList(ts));
            }
        }
    };
    
    /**
     * Produce the union disjunct of the given vararg tuple at a certain arity.
     * This is the arity+1st element of a union equivalent to the vararg tuple
     * like the following:<ul>
     * <li>(T...) = () | T | (T, T) | (T, T, T) | ...</li>
     * <li>(A, B, C...) = Bottom | Bottom | (A, B) | (A, B, C) | ...</li>
     * </ul>
     * Note that the result is defined for all arities, but may sometimes be
     * Bottom.
     */
    public static Type varargDisjunct(VarargTupleType t, int arity) {
        List<Type> base = t.getElements();
        int baseSize = base.size();
        if (baseSize > arity) { return BOTTOM; }
        else {
            Iterable<Type> rest = IterUtil.copy(t.getVarargs(), arity-baseSize);
            return makeTuple(IterUtil.compose(base, rest));
        }
    }

    /**
     * Produce a type representing a Domain with any keyword types removed.
     * May return a TupleType, a VoidType, a VarargTupleType, or a type
     * representing a singleton argument.
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
     * Produce a map from keyword names to types.  The iteration order of the
     * map is identical to that of the KeywordType list.
     */
    public static Map<Id, Type> extractKeywords(Domain d) {
        // Don't waste time allocating a map if it will be empty (the usual case)
        if (d.getKeywords().isEmpty()) { return Collections.<Id, Type>emptyMap(); }
        else {
            Map<Id, Type> result = new LinkedHashMap<Id, Type>(8);
            for (KeywordType k : d.getKeywords()) {
                result.put(k.getName(), k.getType());
            }
            return result;
        }
    }
    
    /**
     * Construct a Domain from a single type representing the required arguments
     * and a keywords map representing the keyword arguments.
     */
    public static Domain makeDomain(Type argsType, Map<Id, Type> keywords) {
        List<KeywordType> keywordList = new ArrayList<KeywordType>(keywords.size());
        for (Map.Entry<Id, Type> entry : keywords.entrySet()) {
            keywordList.add(new KeywordType(entry.getKey(), entry.getValue()));
        }
        return makeDomain(argsType, keywordList);
    }
    
    /**
     * Construct a Domain from a single type representing the required arguments
     * and a list of KeywordTypes.  Unlike {@link NodeFactory#makeDomain}, does
     * not assume that {@code argsType} was produced by the parser.
     */
    public static Domain makeDomain(Type argsType, final List<KeywordType> keywords) {
        return argsType.accept(new NodeAbstractVisitor<Domain>() {
            @Override public Domain forVoidType(VoidType t) {
                return new Domain(Collections.<Type>emptyList(), keywords);
            }
            @Override public Domain forTupleType(TupleType t) {
                return new Domain(t.getElements(), keywords);
            }
            @Override public Domain forVarargTupleType(VarargTupleType t) {
                return new Domain(t.getElements(), Option.some(t.getVarargs()),
                                  keywords);
            }
            @Override public Domain forType(Type t) {
                return new Domain(Collections.singletonList(t), keywords);
            }
        });
    }
    
}
