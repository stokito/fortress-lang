/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static com.sun.fortress.compiler.WellKnownNames.*;
import com.sun.fortress.Shell;

/**
 * General-purpose type constants and constructors/destructors for types.
 * Unlike the methods in {@link NodeFactory}, these pay no attention to the
 * {@code span} and {@code parenthesized} AST fields, and are more "aware"
 * of the semantics of Fortress types.
 */
public final class Types {

    private Types() {}

    private static Span span = NodeFactory.internalSpan;
    public static final Id ANY_NAME = makeId(span, anyTypeLibrary(), "Any");
    public static final Id ARRAY_NAME = makeId(span, fortressLibrary(),"Array");
    // TODO: Replace ImmutableArray with ImmutableHeapSequence when
    //       ImmutableHeapSequence is put into the libraries.
    public static final Id IMMUTABLE_HEAP_SEQ_NAME = makeId(span, fortressLibrary(), "ImmutableArray");

    public static final AnyType ANY = NodeFactory.makeAnyType(span);
    public static final BottomType BOTTOM = NodeFactory.makeBottomType(span);
    public static final TraitType OBJECT = makeTraitType(span, fortressBuiltin(), "Object");

    public static final UnknownType UNKNOWN = NodeFactory.makeUnknownType();

    public static final Type BOTTOM_DOMAIN = BOTTOM;
    public static final ArrowType BOTTOM_ARROW = NodeFactory.makeArrowType(NodeFactory.typeSpan, ANY, BOTTOM);

    public static final TupleType VOID = NodeFactory.makeVoidType(span);
    public static final TraitType FLOAT_LITERAL = makeTraitType(span, fortressBuiltin(), "FloatLiteral");
    public static final TraitType INT_LITERAL = makeTraitType(span, fortressBuiltin(), "IntLiteral");
    public static final TraitType ZZ32 = makeTraitType(span, fortressLibrary(), "ZZ32");
    public static final TraitType BOOLEAN = makeTraitType(span, fortressBuiltin(), "Boolean");
    public static final TraitType CHARACTER = makeTraitType(span, fortressBuiltin(), "Character");
    public static TraitType STRING = makeTraitType(span, fortressLibrary(), "String");
    public static TraitType JAVASTRING = makeTraitType(span, fortressLibrary(), "JavaString");
    public static TraitType EXCEPTION = makeTraitType(span, fortressLibrary(), "Exception");
    public static TraitType CHECKED_EXCEPTION = makeTraitType(span, fortressLibrary(), "CheckedException");
    public static final TraitType REGION = makeTraitType(span, fortressLibrary(), "Region");

    /**
     * It's necessary to set STRING when using the compiler libraries because the location of String
     * in the compiler libraries differs from its location in the full libraries.
     * Similarly for JavaString and Exception and CheckedException.
     */
    public static void useCompilerLibraries() {
        STRING = makeTraitType(span, fortressBuiltin(), "String");
        JAVASTRING = makeTraitType(span, fortressBuiltin(), "JavaString");
        EXCEPTION = makeTraitType(span, fortressBuiltin(), "Exception");
        CHECKED_EXCEPTION = makeTraitType(span, fortressBuiltin(), "CheckedException");
    }
    public static void useFortressLibraries() {
        STRING = makeTraitType(span, fortressLibrary(), "String");
        JAVASTRING = makeTraitType(span, fortressLibrary(), "JavaString");
        EXCEPTION = makeTraitType(span, fortressLibrary(), "Exception");
        CHECKED_EXCEPTION = makeTraitType(span, fortressLibrary(), "CheckedException");
    }

    public static void useTypeCheckerLibraries() {
    	useCompilerLibraries();
    }

    public static final LabelType LABEL = NodeFactory.makeLabelType(span);

    public static final TraitType makeVarargsParamType(Type varargsType) {
        return makeTraitType(IMMUTABLE_HEAP_SEQ_NAME, makeTypeArg(varargsType), makeTypeArg(ZZ32));
    }

    public static TraitType makeThreadType(Type typeArg) {
        return makeTraitType(makeId(span, fortressBuiltin(), "Thread"),
                             makeTypeArg(typeArg));
    }

    public static Id getArrayKName(int k){
     String name = "Array"+k;
     return makeId(span, fortressLibrary(),name);
    }

    public static TraitType makeArrayType(Type elem, Type indexed){
     return makeTraitType(ARRAY_NAME, makeTypeArg(elem),makeTypeArg(indexed));
    }

    public static TraitType makeArrayKType(int k, List<StaticArg> args){
     return  makeTraitType(getArrayKName(k),args);
    }

    /**
     * Create a type {@code FortressLibrary.Generator[\typeArg\]}.
     */
    public static TraitType makeGeneratorType(Type typeArg) {
        return makeTraitType(makeId(span, fortressLibrary(), "Generator"),
                             makeTypeArg(typeArg));
    }

    /**
     * Create a type {@code CompilerLibrary.GeneratorZZ32}.
     */
    public static TraitType makeGeneratorZZ32Type(Span span) {
        return makeTraitType(makeId(span, fortressLibrary(), "GeneratorZZ32"));
    }

    /**
     * Create a type {@code FortressLibrary.Condition[\typeArg\]}.
     */
    public static TraitType makeConditionType(Type typeArg) {
        return makeTraitType(makeId(span, fortressLibrary(), "Condition"),
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
                default: {
                    List<Type> l = CollectUtil.makeList(ts);
                    return NodeFactory.makeUnionType(NodeFactory.makeSpan("impossible", l),
                                                     false, l);
                }
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
                default: {
                    List<Type> l = CollectUtil.makeList(ts);
                    return NodeFactory.makeIntersectionType(NodeFactory.makeSpan("impossible", l), l);
                }
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
                default: {
                    List<Type> l = CollectUtil.makeList(ts);
                    return NodeFactory.makeTupleType(NodeFactory.makeSpan("impossible", l), l);
                }
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
    public static Type varargDisjunct(TupleType t, int arity) {
        List<Type> base = t.getElements();
        int baseSize = base.size();
        if (baseSize > arity) { return BOTTOM; }
        else {
            Iterable<Type> rest = IterUtil.copy(t.getVarargs().unwrap(), arity-baseSize);
            return makeTuple(IterUtil.compose(base, rest));
        }
    }

    /**
     * Produce a type representing a Domain with any keyword types removed.
     * May return a TupleType, a VoidType, a TupleType with varargs, or a type
     * representing a singleton argument.
     */
    public static Type stripKeywords(Type d) {
        if ( d instanceof TupleType ) {
            TupleType _d = (TupleType)d;
            if ( NodeUtil.hasVarargs(_d)) {
                return NodeFactory.makeTupleType(NodeFactory.makeSpan(_d.getElements(), _d.getVarargs().unwrap()),
                                                 false, _d.getElements(), _d.getVarargs(),
                                                 Collections.<KeywordType>emptyList());
            }
            else {
                List<Type> args = _d.getElements();
                switch (args.size()) {
                    case 0: return VOID;
                    case 1: return args.get(0);
                    default: return NodeFactory.makeTupleType(NodeFactory.makeSpan("impossible", args), args);
                }
            }
        } else
            return d;
    }

    /**
     * Produce a map from keyword names to types.  The iteration order of the
     * map is identical to that of the KeywordType list.
     */
    public static Map<Id, Type> extractKeywords(Type d) {
        if ( d instanceof TupleType ) {
            TupleType _d = (TupleType)d;
            // Don't waste time allocating a map if it will be empty (the usual case)
            if (_d.getKeywords().isEmpty()) { return Collections.<Id, Type>emptyMap(); }
            else {
                Map<Id, Type> result = new LinkedHashMap<Id, Type>(8);
                for (KeywordType k : _d.getKeywords()) {
                    result.put(k.getName(), k.getKeywordType());
                }
                return result;
            }
        } else
            return Collections.<Id, Type>emptyMap();
    }

    /**
     * Construct a Domain from a single type representing the required arguments
     * and a keywords map representing the keyword arguments.
     */
    public static Type makeDomain(Type argsType, Map<Id, Type> keywords) {
        List<KeywordType> keywordList = new ArrayList<KeywordType>(keywords.size());
        for (Map.Entry<Id, Type> entry : keywords.entrySet()) {
            keywordList.add(NodeFactory.makeKeywordType(NodeFactory.makeSpan(entry.getKey(), entry.getValue()), entry.getKey(), entry.getValue()));
        }
        return makeDomain(argsType, keywordList);
    }

    /**
     * Construct a Domain from a single type representing the required arguments
     * and a list of KeywordTypes.  Unlike {@link NodeFactory#makeDomain}, does
     * not assume that {@code argsType} was produced by the parser.
     */
    public static Type makeDomain(Type argsType, final List<KeywordType> keywords) {
        return argsType.accept(new NodeAbstractVisitor<Type>() {
            @Override public Type forTupleType(TupleType t) {
                if ( ! NodeUtil.hasVarargs(t) )
                    return NodeFactory.makeTupleType(NodeFactory.makeSpan(t, keywords),
                                                     false, t.getElements(),
                                                     Option.<Type>none(), keywords);
                else
                    return NodeFactory.makeTupleType(NodeFactory.makeSpan(t, keywords),
                                                     false, t.getElements(), t.getVarargs(),
                                                     keywords);
            }
            @Override public Type forType(Type t) {
                if ( keywords.isEmpty() )
                    return t;
                else
                    return NodeFactory.makeTupleType(NodeFactory.makeSpan(t, keywords), false,
                                                     Collections.singletonList(t),
                                                     Option.<Type>none(), keywords);
            }
        });
    }

    /**
     * Given A and Op, returns the type
     * TotalOperatorOrder[\A,<,<=,>=,>,Op]
     */
 public static Type makeTotalOperatorOrder(Type A, Op op) {
//  NodeFactory.makeTraitType(makeId(span, "TotalOperater"), sargs)
//  NodeFactory.makeOpArg(span, "whoa");

  return NI.nyi();
 }

}
