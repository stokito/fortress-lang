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

import java.util.*;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.tuple.Triple;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static com.sun.fortress.compiler.Types.*;
import static com.sun.fortress.compiler.typechecker.TypeAnalyzerUtil.*;
import static com.sun.fortress.nodes_util.NodeFactory.make_InferenceVarType;
import static edu.rice.cs.plt.iter.IterUtil.cross;
import static edu.rice.cs.plt.iter.IterUtil.collapse;
import static edu.rice.cs.plt.iter.IterUtil.map;
import static edu.rice.cs.plt.iter.IterUtil.zip;
import static edu.rice.cs.plt.iter.IterUtil.singleton;
import static edu.rice.cs.plt.iter.IterUtil.compose;
import static edu.rice.cs.plt.iter.IterUtil.skipFirst;
import static edu.rice.cs.plt.iter.IterUtil.first;
import static edu.rice.cs.plt.iter.IterUtil.skipLast;
import static edu.rice.cs.plt.iter.IterUtil.last;
import static edu.rice.cs.plt.iter.IterUtil.asList;
import static com.sun.fortress.compiler.typechecker.ConstraintFormula.*;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

/**
 * Provides core type analysis algorithms in a specific type context.
 */
public class TypeAnalyzer {

    private static final int MAX_SUBTYPE_DEPTH = 20;
    private static final int MAX_SUBTYPE_EXPANSIONS = 4;

    private final TraitTable _table;
    private final StaticParamEnv _staticParamEnv;
    private final SubtypeCache _cache;
    private final SubtypeHistory _emptyHistory;

    public TypeAnalyzer(TraitTable table) {
        this(table, StaticParamEnv.make(), RootSubtypeCache.INSTANCE);
        validateEnvironment();
    }

    public TypeAnalyzer(TypeAnalyzer enclosing, List<StaticParam> params, WhereClause whereClause) {
        this(enclosing._table, enclosing._staticParamEnv.extend(params, whereClause), enclosing._cache);
    }

    private TypeAnalyzer(TraitTable table, StaticParamEnv staticParamEnv, SubtypeCache parentCache) {
        _table = table;
        _staticParamEnv = staticParamEnv;
        _cache = new ChildSubtypeCache(parentCache);
        _emptyHistory = new SubtypeHistory();
    }

    /** Verify that fundamental types are present in the current environment. */
    private void validateEnvironment() {
        // TODO NEB: Reinsert the following call after Jan puts Object into the library
    	// assertTraitIndex(OBJECT.getName());
    }

    /** Verify that the given name is defined and is a TraitIndex. */
    private void assertTraitIndex(Id name) {
        // this will fail if the name is undefined:
    	Option<TypeConsIndex> ind = _table.typeCons(name);
    	if(ind.isNone()){
    		 throw new IllegalArgumentException("Trait " + name +
             "in the given TraitTable.");
    	}
        TypeConsIndex index = ind.unwrap();
        if (!(index instanceof TraitIndex)) {
            throw new IllegalArgumentException("Trait " + name + " is not a trait " +
                                               "in the given TraitTable.");
        }
    }

    /**
     * Factory method for creating empty type analyzers.
     */
    public static TypeAnalyzer make(TraitTable _table) {
    	return new TypeAnalyzer(_table);
    }

    /**
     * Extend this type analyzer with the given static parameters and WhereClause constraints.
     */
    public TypeAnalyzer extend(List<StaticParam> params, WhereClause whereClause) {
    	return new TypeAnalyzer(this, params, whereClause);
    }

    /**
     * <p>Convert the type to a normal form.  The argument is assumed to be
     * well-formed: trait and variable names are defined in this context,
     * and static parameter lists have the correct arity.</p>
     *
     * <p>A normalized type has the following properties:
     * <ul>
     * <li>All component types (subtrees of the AST) are normalized.</li>
     * <li>A union does not contain other unions.</li>
     * <li>An intersection does not contain other intersections or unions.</li>
     * <li>Redundant elements of intersections/unions are eliminated.</li>
     * <li>Intersections/unions have arity of at least 2.</li>
     * <li>A tuple has no intersection or union element.</li>
     * <li>An arrow has neither a union domain nor an intersection range.</li>
     * <li>Redundant elements of throws clauses are eliminated; empty throws
     * clauses are discarded.</li>
     * <li>TODO: It is not an AbbreviatedType.</li>
     * <li>A TraitType does not reference an alias.</li>
     * </ul></p>
     *
     * <p>Note that BaseTypes will be mapped to BaseTypes (and thus throws clauses can be
     * normalized without introducing non-BaseTypes into the list).</p>
     */
    public Type normalize(Type t) {
        return norm(t, _emptyHistory);
    }

    /** Lambda for invoking {@link #normalize}. */
    public final Lambda<Type, Type> NORMALIZE = new Lambda<Type, Type>() {
        public Type value(Type t) { return normalize(t); }
    };

    /**
     * Produce a formula that, if satisfied, will support {@code s} as a subtype of {@code t}.
     * {@code s} and {@code t} need not be normalized.
     */
    public ConstraintFormula subtype(Type s, Type t) {
        return sub(normalize(s), normalize(t), _emptyHistory);
    }

    /**
     * Given normalized {@code s} and {@code t}, produce a formula that, if satisfied, will
     * support {@code s} as a subtype of {@code t}.
     */
    public ConstraintFormula subtypeNormal(Type s, Type t) {
        return sub(s, t, _emptyHistory);
    }

    /**
     * Produce a formula that, if satisfied, will support {@code s} being equivalent to
     * {@code t}.  {@code s} and {@code t} need not be normalized.
     */
    public ConstraintFormula equivalent(Type s, Type t) {
        return equiv(normalize(s), normalize(t), _emptyHistory);
    }

    /**
     * Given normalized {@code s} and {@code t}, produce a formula that, if satisfied, will
     * support {@code s} being equivalent to {@code t}.
     */
    public ConstraintFormula equivalentNormal(Type s, Type t) {
        return equiv(s, t, _emptyHistory);
    }

    /** Create a minimal union representing the join of the given types. */
    public Type join(Type... ts) {
        return jn(map(IterUtil.make(ts), NORMALIZE), _emptyHistory);
    }

    /** Create a minimal union representing the join of the given types. */
    public Type join(Iterable<? extends Type> ts) {
        return jn(map(ts, NORMALIZE), _emptyHistory);
    }

    /**
     * Create a minimal union representing the join of the given
     * <em>normalized</em> types.
     */
    public Type joinNormal(Iterable<? extends Type> ts) {
        return jn(ts, _emptyHistory);
    }

    /** Create a minimal intersection representing the meet of the given types. */
    public Type meet(Type... ts) {
        return mt(map(IterUtil.make(ts), NORMALIZE), _emptyHistory);
    }

    /** Create a minimal intersection representing the meet of the given types. */
    public Type meet(Iterable<? extends Type> ts) {
        return mt(map(ts, NORMALIZE), _emptyHistory);
    }

    /**
     * Create a minimal intersection representing the meet of the given
     * <em>normalized</em> types.
     */
    public Type meetNormal(Iterable<? extends Type> ts) {
        return mt(ts, _emptyHistory);
    }

    /** Implementation of normalization parameterized by a history. */
    private Type norm(Type t, final SubtypeHistory history) {
        debug.logStart("t", t);
        Type result = (Type) t.accept(new NodeUpdateVisitor() {

            @Override public BaseType forTraitTypeOnly(TraitType t, Id name, List<StaticArg> normalArgs) {
                Option<TypeConsIndex> ind = _table.typeCons(name);
            	if(ind.isNone()){
            		throw new IllegalArgumentException("Unrecognized name: " + name);
            	}
            	TypeConsIndex index = ind.unwrap();
                if (index instanceof TypeAliasIndex) {
                    TypeAliasIndex aliasIndex = (TypeAliasIndex) index;
                    // TODO: can we optimize substitution so that the result is already normalized?
                    //       (if we did so, we would need aliasIndex.type() to have been normalized)
                    // TODO: can aliases map to non-trait types?  (that's a problem if this appears
                    //       in a throws clause)
                    Lambda<Type, Type> subst = makeSubstitution(aliasIndex.staticParameters(),
                                                                normalArgs);
                    return (BaseType) subst.value(aliasIndex.type()).accept(this);
                }
                else if (index instanceof TraitIndex) {
                    return (BaseType) super.forTraitTypeOnly(t, name, normalArgs);
                }
                else if (index == null) {
                    throw new IllegalArgumentException("Unrecognized name: " + name);
                }
                else {
                    throw new IllegalStateException("Unrecognized index type: " + index);
                }
            }

            @Override public Type forTupleTypeOnly(TupleType t, List<Type> normalElements) {
                Type result = handleAbstractTuple(normalElements, MAKE_TUPLE);
                return t.equals(result) ? t : result;
            }

            @Override public Type forVarargTupleTypeOnly(VarargTupleType t, List<Type> normalElements,
                                                         Type normalVarargs) {
                // the varargs type can be treated like just another tuple element, as far as
                // normalization is concerned, unless the varargs type is Bottom
                if (normalVarargs.equals(BOTTOM)) {
                    return handleAbstractTuple(normalElements, MAKE_TUPLE);
                }
                else {
                    Lambda<Iterable<Type>, Type> factory = new Lambda<Iterable<Type>, Type>() {
                        public Type value(Iterable<Type> ts) {
                            if (IterUtil.isEmpty(ts)) { return VOID; }
                            else {
                                List<Type> elts = asList(skipLast(ts));
                                Type varargs = last(ts);
                                return new VarargTupleType(elts, varargs);
                            }
                        }
                    };
                    Type result = handleAbstractTuple(compose(normalElements, normalVarargs),
                                                      factory);
                    return t.equals(result) ? t : result;
                }
            }

            private Type handleAbstractTuple(Iterable<Type> normalElements,
                                             final Lambda<Iterable<Type>, Type> factory) {
                // push unions out:
                Iterable<Iterable<Type>> elementDisjuncts = map(normalElements, DISJUNCTS);
                // given a union-less tuple, push intersections out:
                Lambda<Iterable<Type>, Type> handleDisjunct = new Lambda<Iterable<Type>, Type>() {
                    public Type value(Iterable<Type> disjunctElts) {
                        Iterable<Iterable<Type>> elementConjuncts = map(disjunctElts, CONJUNCTS);
                        // don't meet, because the tuples here aren't subtypes of each other
                        return makeIntersection(map(cross(elementConjuncts), factory));
                    }
                };
                // don't join, because the tuple intersections here aren't subtypes of each other
                return makeUnion(map(cross(elementDisjuncts), handleDisjunct));
            }

            @Override public Type forArrowTypeOnly(ArrowType t, Domain normalDomain, Type normalRange,
                                                   final Effect normalEffect) {
                Type domainArg = stripKeywords(normalDomain);
                final Map<Id, Type> domainKeys = extractKeywords(normalDomain);
                Iterable<Type> domainTs = compose(domainArg, domainKeys.values());
                // map a list of the length of domainTs back to a Domain:
                Lambda<Iterable<Type>, Domain> domainFactory = new Lambda<Iterable<Type>, Domain>() {
                    public Domain value(Iterable<Type> ts) {
                        List<KeywordType> ks = new ArrayList<KeywordType>(domainKeys.size());
                        for (Pair<Id, Type> p : zip(domainKeys.keySet(), skipFirst(ts))) {
                            ks.add(new KeywordType(p.first(), p.second()));
                        }
                        return makeDomain(first(ts), ks);
                    }
                };
                Iterable<Domain> domains = map(cross(map(domainTs, DISJUNCTS)), domainFactory);
                Iterable<Type> ranges = liftConjuncts(normalRange, history);
                Iterable<Type> overloads = cross(domains, ranges, new Lambda2<Domain, Type, Type>() {
                    public Type value(Domain d, Type r) {
                        return new ArrowType(d, r, normalEffect);
                    }
                });
                // don't meet, because the arrows here aren't subtypes of each other
                Type result = makeIntersection(overloads);
                return t.equals(result) ? t : result;
            }

            @Override public Domain forDomain(Domain d) {
                // recur on a single args type rather than each element individually
                Type args = stripKeywords(d);
                Type argsNorm = (Type) args.accept(this);
                List<KeywordType> ks = d.getKeywords();
                List<KeywordType> ksNorm = recurOnListOfKeywordType(ks);
                if (args == argsNorm && ks == ksNorm) { return d; }
                else { return makeDomain(argsNorm, ksNorm); }
            }

            @Override public Effect forEffectOnly(Effect e,
                                                  Option<List<BaseType>> normalThrows) {
                if (normalThrows.isNone()) { return e; }
                else {
                    List<BaseType> reduced = reduceDisjuncts(normalThrows.unwrap(),
                                                             _emptyHistory);
                    if (reduced.isEmpty()) { return new Effect(e.isIo()); }
                    else if (reduced.equals(e.getThrowsClause().unwrap())) {
                        return e;
                    }
                    else {
                        return new Effect(Option.some(reduced), e.isIo());
                    }
                }
            }

            @Override public Type forUnionTypeOnly(UnionType t, List<Type> normalElements) {
                Type result = jn(normalElements, history);
                return t.equals(result) ? t : result;
            }

            @Override public Type forIntersectionTypeOnly(IntersectionType t,
                                                          List<Type> normalElements) {
                Type result = mt(normalElements, history);
                return t.equals(result) ? t : result;
            }

        });
        debug.logEnd("result", result);
        return result;
    }

    /**
     * Implementation of equivalence parameterized by a history. Arguments must be
     * normalized.
     */
    private ConstraintFormula equiv(Type s, Type t, SubtypeHistory history) {
        // TODO: optimize by performing both checks simultaneously?
        debug.logStart(new String[]{"s", "t"}, s, t);
        ConstraintFormula result = sub(s, t, history);
        if (!result.isFalse()) { result = result.and(sub(t, s, history), history); }
        debug.logEnd("result", result);
        return result;
    }

    /**
     * Implementation of join parameterized by a history.  Arguments must be
     * normalized; the result will be normalized.
     */
    private Type jn(Iterable<? extends Type> ts, SubtypeHistory h) {
        // collpase nested unions and eliminate redundant elements
        Iterable<Type> disjuncts = collapse(map(ts, DISJUNCTS));
        return makeUnion(this.<Type>reduceDisjuncts(disjuncts, h));
    }

    /**
     * Implementation of meet parameterized by a history.  Arguments must be
     * normalized; the result will be normalized.
     */
    private Type mt(Iterable<? extends Type> ts, final SubtypeHistory h) {
        // push unions out:
        Iterable<Iterable<Type>> sumOfProducts = cross(map(ts, DISJUNCTS));
        // given a union-less intersection, collapse and eliminate redundant elements:
        Lambda<Iterable<Type>, Type> handleDisjunct = new Lambda<Iterable<Type>, Type>() {
            public Type value(Iterable<Type> ts) {
                Iterable<Type> conjuncts = collapse(map(ts, CONJUNCTS));
                return makeIntersection(reduceConjuncts(conjuncts, h));
            }
        };
        Iterable<Type> disjuncts = map(sumOfProducts, handleDisjunct);
        // disjuncts may be redundant (but, unlike jn, they don't need to be collapsed)
        return makeUnion(reduceDisjuncts(disjuncts, h));
    }

    /**
     * Implementation of subtyping parameterized by a history.  Arguments must
     * be normalized.
     */
    private ConstraintFormula sub(final Type s, final Type t, SubtypeHistory history) {
        debug.logStart(new String[]{"s", "t"}, s, t);
        ConstraintFormula result;
        Option<ConstraintFormula> cached = _cache.get(s, t, history);
        if (cached.isSome()) {
            result = cached.unwrap();
            debug.log("found in cache");
        }
        else if (history.expansions() > MAX_SUBTYPE_EXPANSIONS) {
            result = FALSE;
            debug.log("reached max subtype expansions");
        }
        else if (history.size() > MAX_SUBTYPE_DEPTH) {
            result = FALSE;
            debug.logEnd("reached max subtype depth");
        }
        else if (history.contains(s, t)) {
            result = FALSE;
            debug.log("cyclic invocation");
        }
        else if (s instanceof BottomType) { result = TRUE; }
        else if (t instanceof AnyType) { result = TRUE; }
        else if (s.equals(t)) { result = TRUE; }
        else if (s instanceof _InferenceVarType) {
            if (t instanceof _InferenceVarType) {
                ConstraintFormula f1 = upperBound((_InferenceVarType) s, t, history);
                ConstraintFormula f2 = lowerBound((_InferenceVarType) t, s, history);
                result = f1.and(f2, history);
            }
            else { result = upperBound((_InferenceVarType) s, t, history); }
        }
        else if (t instanceof _InferenceVarType) {
            result = lowerBound((_InferenceVarType) t, s, history);
        }
        else {
            final SubtypeHistory h = history.extend(s, t);
            // a null result indicates that s should be used for dispatching instead
            result = t.accept(new NodeAbstractVisitor<ConstraintFormula>() {

                @Override public ConstraintFormula forType(Type t) { return null; }

                @Override public
                ConstraintFormula forVarargTupleType(final VarargTupleType t) {
                    return s.accept(new NodeAbstractVisitor<ConstraintFormula>() {
                        @Override public ConstraintFormula forType(Type s) {
                            // defer to handling of s
                            return null;
                        }
                        @Override public ConstraintFormula forAnyType(AnyType s) {
                            return anySubVararg(s, t, h);
                        }
                        @Override public ConstraintFormula forTraitType(TraitType s) {
                            return traitSubVararg(s, t, h);
                        }
                        @Override public ConstraintFormula forVoidType(VoidType s) {
                            return voidSubVararg(s, t, h);
                        }
                        @Override public ConstraintFormula forTupleType(TupleType s) {
                            return tupleSubVararg(s, t, h);
                        }
                        @Override public
                        ConstraintFormula forVarargTupleType(VarargTupleType s) {
                            return varargSubVararg(s, t, h);
                        }
                    });
                }

                @Override public ConstraintFormula forVarType(final VarType t) {
                    return s.accept(new NodeAbstractVisitor<ConstraintFormula>() {
                        @Override public ConstraintFormula forType(Type s) {
                            return subVar(s, t, h);
                        }
                        @Override public ConstraintFormula forVarType(VarType s) {
                            return varSubVar(s, t, h);
                        }
                        @Override public
                        ConstraintFormula forIntersectionType(IntersectionType s) {
                            return intersectionSubVar(s, t, h);
                        }
                        @Override public ConstraintFormula forUnionType(UnionType s) {
                            return unionSubVar(s, t, h);
                        }
                    });
                }

                @Override public
                ConstraintFormula forIntersectionType(final IntersectionType t) {
                    return s.accept(new NodeAbstractVisitor<ConstraintFormula>() {
                        @Override public ConstraintFormula forType(Type s) {
                            return subIntersection(s, t, h);
                        }
                        @Override public ConstraintFormula forVarType(VarType s) {
                            return varSubIntersection(s, t, h);
                        }
                        @Override public
                        ConstraintFormula forIntersectionType(IntersectionType s) {
                            return intersectionSubIntersection(s, t, h);
                        }
                        @Override public ConstraintFormula forUnionType(UnionType s) {
                            return unionSubIntersection(s, t, h);
                        }
                    });
                }

                @Override public ConstraintFormula forUnionType(final UnionType t) {
                    return s.accept(new NodeAbstractVisitor<ConstraintFormula>() {
                        @Override public ConstraintFormula forType(Type s) {
                            return subUnion(s, t, h);
                        }
                        @Override public ConstraintFormula forVarType(VarType s) {
                            return varSubUnion(s, t, h);
                        }
                        @Override public
                        ConstraintFormula forIntersectionType(IntersectionType s) {
                            return intersectionSubUnion(s, t, h);
                        }
                        @Override public ConstraintFormula forUnionType(UnionType s) {
                            return unionSubUnion(s, t, h);
                        }
                    });
                }

            });
            if (result == null) {
                result = s.accept(new NodeAbstractVisitor<ConstraintFormula>() {
                    @Override public ConstraintFormula forType(Type s) { return FALSE; }
                    @Override public ConstraintFormula forTraitType(TraitType s) {
                        if (t instanceof TraitType) {
                            return traitSubTrait(s, (TraitType) t, h);
                        }
                        else { return FALSE; }
                    }
                    @Override public ConstraintFormula forTupleType(TupleType s) {
                        if (t instanceof TupleType) {
                            return tupleSubTuple(s, (TupleType) t, h);
                        }
                        else { return FALSE; }
                    }
                    @Override public ConstraintFormula forArrowType(ArrowType s) {
                        if (t instanceof ArrowType) {
                            return arrowSubArrow(s, (ArrowType) t, h);
                        }
                        else { return FALSE; }
                    }
                    @Override public ConstraintFormula forVarType(VarType s) {
                        return varSub(s, t, h);
                    }
                    @Override public
                    ConstraintFormula forIntersectionType(IntersectionType s) {
                        return intersectionSub(s, t, h);
                    }
                    @Override public ConstraintFormula forUnionType(UnionType s) {
                        return unionSub(s, t, h);
                    }
                });
            }
            _cache.put(s, t, history, result);
        }
        debug.logEnd("result", result);
        return result;
    }

    /* SUBTYPING RULES: to eliminate ambiguity, for any pair of Types there should
     * either be one most-specific applicable signature, or none at all.  That is,
     * if the declarations all had the same name, they would comprise a valid Fortress
     * overloaded function definition.  All functions may assume that the arguments
     * are normalized, that they are not equal, that neither is an inference variable,
     * that {@code s} is not Bottom, and that {@code t} is not Any.
     */

    private ConstraintFormula traitSubTrait(TraitType s, TraitType t, SubtypeHistory h) {
        ConstraintFormula result = FALSE;
        if (s.getName().equals(t.getName())) {
            ConstraintFormula f = TRUE;
            for (Pair<StaticArg, StaticArg> p : zip(s.getArgs(), t.getArgs())) {
                f = f.and(equiv(p.first(), p.second(), h), h);
                if (f.isFalse()) { break; }
            }
            result = result.or(f, h);
        }
        if (!result.isTrue()) {
        	Option<TypeConsIndex> ind = _table.typeCons(s.getName());
        	if(ind.isNone()){
        		throw new IllegalArgumentException(s.getName() +"is undefined");
        	}
            TraitIndex index = (TraitIndex) ind.unwrap();
            List<Id> hidden = index.hiddenParameters();
            Lambda<Type, Type> subst = makeSubstitution(index.staticParameters(),
                                                        s.getArgs(),
                                                        hidden);
            for (TraitTypeWhere sup : index.extendsTypes()) {
                ConstraintFormula f = TRUE;
                for (Pair<Type, Type> c : index.typeConstraints()) {
                    // TODO: optimize substitution/normalization?
                    SubtypeHistory newH = h;
                    if (containsVariable(c.first(), hidden) ||
                        containsVariable(c.second(), hidden)) {
                        newH = h.expand();
                    }
                    Type lower = norm(subst.value(c.first()), newH);
                    Type upper = norm(subst.value(c.second()), newH);
                    f = f.and(sub(lower, upper, newH), h);
                    if (f.isFalse()) { break; }
                }
                if (!f.isFalse()) {
                    // TODO: optimize substitution/normalization?
                    Type supT = sup.getType();
                    SubtypeHistory newH = containsVariable(supT, hidden) ? h.expand() : h;
                    Type supInstance = norm(subst.value(supT), newH);
                    f = f.and(sub(supInstance, t, newH), h);
                }
                result = result.or(f, h);
                if (result.isTrue()) { break; }
            }
            // TODO NEB: Reinsert the following conditional after Jan puts object into the library
//            if (!s.equals(OBJECT)) {
//                result = result.or(sub(OBJECT, t, h), h);
//            }
            if( !s.equals(ANY) ) {
            	result = result.or(sub(ANY,t,h), h);
            }
        }
        return result;
    }

    private ConstraintFormula arrowSubArrow(ArrowType s, ArrowType t, SubtypeHistory h) {
        ConstraintFormula f = sub(t.getDomain(), s.getDomain(), h);
        if (!f.isFalse()) {
            f = f.and(sub(s.getRange(), t.getRange(), h), h);
        }
        if (!f.isFalse()) {
            f = f.and(sub(s.getEffect(), t.getEffect(), h), h);
        }
        return f;
    }

    private ConstraintFormula tupleSubTuple(TupleType s, TupleType t, SubtypeHistory h) {
        if (s.getElements().size() == t.getElements().size()) {
            ConstraintFormula f = TRUE;
            for (Pair<Type, Type> p : zip(s.getElements(), t.getElements())) {
                f = f.and(sub(p.first(), p.second(), h), h);
                if (f.isFalse()) { break; }
            }
            return f;
        }
        else { return FALSE; }
    }

    private ConstraintFormula voidSubVararg(VoidType s, VarargTupleType t, SubtypeHistory h) {
        return sub(s, varargDisjunct(t, 0), h);
    }

    private ConstraintFormula traitSubVararg(TraitType s, VarargTupleType t, SubtypeHistory h) {
        return sub(s, varargDisjunct(t, 1), h);
    }

    private ConstraintFormula anySubVararg(AnyType s, VarargTupleType t, SubtypeHistory h) {
        return sub(s, varargDisjunct(t, 1), h);
    }

    private ConstraintFormula tupleSubVararg(TupleType s, VarargTupleType t, SubtypeHistory h) {
        return sub(s, varargDisjunct(t, s.getElements().size()), h);
    }

    private ConstraintFormula varargSubVararg(VarargTupleType s, VarargTupleType t,
                                              SubtypeHistory h) {
        int n = s.getElements().size();
        // if t is too wide, this results in false:
        ConstraintFormula f = sub(varargDisjunct(s, n), varargDisjunct(t, n), h);
        if (!f.isFalse()) {
            f = f.and(sub(s.getVarargs(), t.getVarargs(), h), h);
        }
        return f;
    }

    private ConstraintFormula varSub(VarType s, Type t, SubtypeHistory h) {
        // TODO
        return FALSE;
    }

    private ConstraintFormula subVar(Type s, VarType t, SubtypeHistory h) {
        // TODO
        return FALSE;
    }

    private ConstraintFormula varSubVar(VarType s, VarType t, SubtypeHistory h) {
        // TODO
        return FALSE;
    }

    private ConstraintFormula intersectionSubVar(IntersectionType s, VarType t,
                                                 SubtypeHistory h) {
        ConstraintFormula result = intersectionSub(s, t, h);
        if (!result.isTrue()) {
            result = result.or(subVar(s, t, h), h);
        }
        return result;
    }

    private ConstraintFormula varSubIntersection(VarType s, IntersectionType t,
                                                 SubtypeHistory h) {
        return subIntersection(s, t, h);
    }

    private ConstraintFormula varSubUnion(VarType s, UnionType t, SubtypeHistory h) {
        ConstraintFormula result = varSub(s, t, h);
        if (!result.isTrue()) {
            result = result.or(subUnion(s, t, h), h);
        }
        return result;
    }

    private ConstraintFormula unionSubVar(UnionType s, VarType t, SubtypeHistory h) {
        return unionSub(s, t, h);
    }

    private ConstraintFormula subIntersection(Type s, IntersectionType t, SubtypeHistory h) {
        ConstraintFormula f = TRUE;
        for (Type elt : t.getElements()) {
            f = f.and(sub(s, elt, h), h);
            if (f.isFalse()) { break; }
        }
        return f;
    }

    private ConstraintFormula intersectionSub(IntersectionType s, Type t, SubtypeHistory h) {
        ConstraintFormula result = FALSE;
        for (Pair<Type, ConstraintFormula> sElt : expandIntersection(s, h)) {
            ConstraintFormula f = sElt.second();
            f = f.and(sub(sElt.first(), t, h), h);
            result = result.or(f, h);
            if (result.isTrue()) { break; }
        }
        return result;
    }

    private ConstraintFormula subUnion(Type s, UnionType t, SubtypeHistory h) {
        ConstraintFormula result = FALSE;
        for (Type elt : t.getElements()) {
            result = result.or(sub(s, elt, h), h);
            if (result.isTrue()) { break; }
        }
        return result;
    }

    private ConstraintFormula unionSub(UnionType s, Type t, SubtypeHistory h) {
        ConstraintFormula f = TRUE;
        for (Type elt : s.getElements()) {
            f = f.and(sub(elt, t, h), h);
            if (f.isFalse()) { break; }
        }
        return f;
    }

    private ConstraintFormula intersectionSubIntersection(IntersectionType s, IntersectionType t,
                                                          SubtypeHistory h) {
        ConstraintFormula result = TRUE;
        for (Type tElt : t.getElements()) {
            ConstraintFormula r = FALSE;
            for (Pair<Type, ConstraintFormula> sElt : expandIntersection(s, h)) {
                ConstraintFormula f = sElt.second();
                f = f.and(sub(sElt.first(), tElt, h), h);
                r = r.or(f, h);
                if (r.isTrue()) { break; }
            }
            result = result.and(r, h);
            if (result.isFalse()) { break; }
        }
        return result;
    }

    private ConstraintFormula intersectionSubUnion(IntersectionType s, UnionType t,
                                                   SubtypeHistory h) {
        ConstraintFormula result = FALSE;
        for (Pair<Type, ConstraintFormula> sElt : expandIntersection(s, h)) {
            for (Pair<Type, ConstraintFormula> tElt : expandUnion(t, h)) {
                ConstraintFormula f = sElt.second().and(tElt.second(), h);
                if (!f.isFalse()) {
                    f = f.and(sub(sElt.first(), tElt.first(), h), h);
                }
                result = result.or(f, h);
                if (result.isTrue()) { break; }
            }
            if (result.isTrue()) { break; }
        }
        return result;
    }

    private ConstraintFormula unionSubIntersection(UnionType s, IntersectionType t,
                                                   SubtypeHistory h) {
        ConstraintFormula result = TRUE;
        for (Type sElt : s.getElements()) {
            for (Type tElt : t.getElements()) {
                result = result.and(sub(sElt, tElt, h), h);
                if (result.isTrue()) { break; }
            }
            if (result.isTrue()) { break; }
        }
        return result;
    }

    private ConstraintFormula unionSubUnion(UnionType s, UnionType t, SubtypeHistory h) {
        ConstraintFormula result = TRUE;
        for (Type sElt : s.getElements()) {
            ConstraintFormula r = FALSE;
            for (Pair<Type, ConstraintFormula> tElt : expandUnion(t, h)) {
                ConstraintFormula f = tElt.second();
                f = f.and(sub(sElt, tElt.first(), h), h);
                r = r.or(f, h);
                if (r.isTrue()) { break; }
            }
            result = result.and(r, h);
            if (result.isFalse()) { break; }
        }
        return result;
    }

    /**
     * Produce a list of all element types that can be inferred from the given union,
     * combined with a (non-false) assumption under which each type's inclusion may
     * be inferred.
     */
    private Iterable<Pair<Type, ConstraintFormula>> expandUnion(UnionType t,
                                                                SubtypeHistory h) {
        // TODO: implement non-trivial cases
        return cross(t.getElements(), singleton(TRUE));
    }

    /**
     * Produce a list of all element types that can be inferred from the given intersection,
     * combined with a (non-false) assumption under which each type's inclusion may
     * be inferred.
     */
    private Iterable<Pair<Type, ConstraintFormula>> expandIntersection(IntersectionType t,
                                                                       SubtypeHistory h) {
        // TODO: implement non-trivial cases
        return cross(t.getElements(), singleton(TRUE));
    }

    /**
     * Restructure the given normalized type so that intersection, rather than union, occurs at
     * the outermost level.  Produce the minimal list of conjuncts that make up that intersection.
     */
    private List<Type> liftConjuncts(Type t, final SubtypeHistory h) {
        // Note the analogy between this method and mt()
        // push intersections out:
        Iterable<Iterable<Type>> productOfSums = cross(map(disjuncts(t), CONJUNCTS));
        // given a union (of neither intersections nor unions), eliminate redundant elements:
        Lambda<Iterable<Type>, Type> handleConjunct = new Lambda<Iterable<Type>, Type>() {
            public Type value(Iterable<Type> disjuncts) {
                return makeUnion(reduceDisjuncts(disjuncts, h));
            }
        };
        Iterable<Type> conjuncts = map(productOfSums, handleConjunct);
        // conjuncts may be redundant
        return reduceConjuncts(conjuncts, h);
    }

    /**
     * Eliminate redundant conjuncts from the given list of normalized types.  A type is
     * redundant if some other type in the list is a subtype.  Where two elements are
     * equivalent, the second of the two will be discarded.
     */
    private <T extends Type> List<T> reduceConjuncts(Iterable<? extends T> conjuncts,
                                                     SubtypeHistory h) {
        return reduceList(conjuncts, h, true);
    }

    /**
     * Eliminate redundant disjuncts from the given list of normalized types.  A type is
     * redundant if some other type in the list is a supertype.  Where two elements are
     * equivalent, the second of the two will be discarded.
     */
    private <T extends Type> List<T> reduceDisjuncts(Iterable<? extends T> disjuncts,
                                                     SubtypeHistory h) {
        // TODO: check for exclusions (resulting in Bottom)
        return reduceList(disjuncts, h, false);
    }

    /**
     * Generalization of {@link #reduceConjuncts} and {@link #reduceDisjuncts}: eliminate
     * redundant elements from the list of normalized types; where two are equivalent,
     * the second is discarded.
     * @param preferSubs  If {@code true}, where S is a subtype of T, discard T; otherwise,
     *                    discard S.
     */
    private <T extends Type> List<T> reduceList(Iterable<? extends T> ts, SubtypeHistory h,
                                                boolean preferSubs) {
        switch (IterUtil.sizeOf(ts, 2)) {
            case 0: return Collections.emptyList();
            case 1: return Collections.singletonList(first(ts));
            default:
                LinkedList<? extends T> workList = IterUtil.asLinkedList(ts);
                LinkedList<T> result = new LinkedList<T>();
                Iterable<T> remainingTs = compose(workList, result);
                while (!workList.isEmpty()) {
                    // prefer discarding later elements when two are equivalent
                    T t = workList.removeLast();
                    boolean keep = true;
                    for (T other : remainingTs) {
                        // only discard if subtyping holds for all variable instantiations
                        if (preferSubs) { keep &= ! sub(other, t, h).isTrue(); }
                        else { keep &= ! sub(t, other, h).isTrue(); }
                        if (!keep) { break; }
                    }
                    if (keep) { result.addFirst(t); }
                }
                return result;
        }
    }

    /** Equivalence for StaticArgs. */
    private ConstraintFormula equiv(StaticArg a1, final StaticArg a2,
                                    final SubtypeHistory history) {

        return a1.accept(new NodeAbstractVisitor<ConstraintFormula>() {
            @Override public ConstraintFormula forTypeArg(TypeArg a1) {
                if (a2 instanceof TypeArg) {
                    return equiv(a1.getType(), ((TypeArg) a2).getType(), history);
                }
                else { return FALSE; }
            }
            @Override public ConstraintFormula forIntArg(IntArg a1) {
                if (a2 instanceof IntArg) {
                    boolean result = a1.getVal().equals(((IntArg) a2).getVal());
                    return fromBoolean(result);
                }
                else { return FALSE; }
            }
            @Override public ConstraintFormula forBoolArg(BoolArg a1) {
                if (a2 instanceof BoolArg) {
                    boolean result = a1.getBool().equals(((BoolArg) a2).getBool());
                    return fromBoolean(result);
                }
                else { return FALSE; }
            }
            @Override public ConstraintFormula forOpArg(OpArg a1) {
                if (a2 instanceof OpArg) {
                    boolean result = a1.getName().equals(((OpArg) a2).getName());
                    return fromBoolean(result);
                }
                else { return FALSE; }
            }
            @Override public ConstraintFormula forDimArg(DimArg a1) {
                if (a2 instanceof DimArg) {
                    boolean result = a1.getDim().equals(((DimArg) a2).getDim());
                    return fromBoolean(result);
                }
                else { return FALSE; }
            }
            @Override public ConstraintFormula forUnitArg(UnitArg a1) {
                if (a2 instanceof UnitArg) {
                    boolean result = a1.getUnit().equals(((UnitArg) a2).getUnit());
                    return fromBoolean(result);
                }
                else { return FALSE; }
            }
        });
    }

    /** Subtyping for Domains. */
    private ConstraintFormula sub(Domain s, Domain t, SubtypeHistory h) {
        Map<Id, Type> sMap = extractKeywords(s);
        Map<Id, Type> tMap = extractKeywords(t);
        if (tMap.keySet().containsAll(sMap.keySet())) {
            ConstraintFormula f = sub(stripKeywords(s), stripKeywords(t), h);
            if (!f.isFalse()) {
                for (Map.Entry<Id, Type> entry : sMap.entrySet()) {
                    Type tType = tMap.get(entry.getKey());
                    f = f.and(sub(entry.getValue(), tType, h), h);
                    if (f.isFalse()) { break; }
                }
            }
            return f;
        }
        else { return FALSE; }
    }

    /** Subtyping for Effects. */
    private ConstraintFormula sub(Effect s, Effect t, SubtypeHistory h) {
        if (!s.isIo() || t.isIo()) {
            List<BaseType> empty = Collections.<BaseType>emptyList();
            Type sThrows = makeUnion(IterUtil.<Type>relax(s.getThrowsClause().unwrap(empty)));
            Type tThrows = makeUnion(IterUtil.<Type>relax(s.getThrowsClause().unwrap(empty)));
            return sub(sThrows, tThrows, h);
        }
        else { return FALSE; }
    }


    /** An immutable record of all subtyping invocations in the call stack. */
    // Package private -- accessed by ConstraintFormula
    class SubtypeHistory {

        private final Relation<Type, Type> _entries;
        private final int _expansions;

        public SubtypeHistory() {
            // no need for an index in either direction
            _entries = new HashRelation<Type, Type>(false, false);
            _expansions = 0;
        }

        private SubtypeHistory(Relation<Type, Type> entries, int expansions) {
            _entries = entries;
            _expansions = expansions;
        }

        public int size() { return _entries.size(); }

        public int expansions() { return _expansions; }

        public boolean contains(Type s, Type t) {
            InferenceVarTranslator trans = new InferenceVarTranslator();
            return _entries.contains(trans.canonicalizeVars(s), trans.canonicalizeVars(t));
        }

        public SubtypeHistory extend(Type s, Type t) {
            Relation<Type, Type> newEntries = new HashRelation<Type, Type>();
            newEntries.addAll(_entries);
            InferenceVarTranslator trans = new InferenceVarTranslator();
            newEntries.add(trans.canonicalizeVars(s), trans.canonicalizeVars(t));
            return new SubtypeHistory(newEntries, _expansions);
        }

        public SubtypeHistory expand() {
          return new SubtypeHistory(_entries, _expansions + 1);
        }

        public ConstraintFormula subtypeNormal(Type s, Type t) {
            return TypeAnalyzer.this.sub(s, t, this);
        }

        public Type meetNormal(Type... ts) {
            return TypeAnalyzer.this.mt(IterUtil.make(ts), this);
        }

        public Type joinNormal(Type... ts) {
            return TypeAnalyzer.this.jn(IterUtil.make(ts), this);
        }

        public String toString() {
          return IterUtil.multilineToString(_entries) + "\n" + _expansions + " expansions";
        }

        public SubtypeHistory combine(SubtypeHistory h){
        	int newexpand = Math.max(this._expansions,h._expansions);
        	Relation<Type,Type> newrel= new HashRelation<Type,Type>();
        	newrel.addAll(this._entries);
        	newrel.addAll(h._entries);
        	// TODO: NEB we are trying this out but we are not
        	// sure why we should choose the other one.
        	return new SubtypeHistory(newrel, newexpand);
        }

    }


    /**
     * A mutable collection of subtyping results from previously-completed invocations
     * of subtyping in a specific type context.
     */
    protected static abstract class SubtypeCache {
        public abstract void put(Type s, Type t, SubtypeHistory h, ConstraintFormula c);
        public abstract Option<ConstraintFormula> get(Type s, Type t, SubtypeHistory h);
        public abstract int size();
    }

    protected static class RootSubtypeCache extends SubtypeCache {
        public static final RootSubtypeCache INSTANCE = new RootSubtypeCache();
        private RootSubtypeCache() {}
        public void put(Type s, Type t, SubtypeHistory h, ConstraintFormula c) {
            throw new IllegalArgumentException("Can't add values to the root cache");
        }
        public Option<ConstraintFormula> get(Type s, Type t, SubtypeHistory h) {
            return Option.none();
        }
        public int size() { return 0; }
        public String toString() { return "<root cache>"; }
    };

    protected static class ChildSubtypeCache extends SubtypeCache {

        private final SubtypeCache _parent;
        private final HashMap<Pair<Type,Type>, ConstraintFormula> _results;

        public ChildSubtypeCache(SubtypeCache parent) {
            _parent = parent;
            _results = new HashMap<Pair<Type,Type>, ConstraintFormula>();
        }

        public void put(Type s, Type t, SubtypeHistory h, ConstraintFormula c) {
            InferenceVarTranslator trans = new InferenceVarTranslator();
            _results.put(Pair.make(trans.canonicalizeVars(s), trans.canonicalizeVars(t)),
                         c.applySubstitution(trans.canonicalSubstitution()));
        }

        public Option<ConstraintFormula> get(Type s, Type t, SubtypeHistory h) {
            // we currently ignore the history, leading to incorrect results in some cases
            InferenceVarTranslator trans = new InferenceVarTranslator();
            ConstraintFormula result = _results.get(Pair.make(trans.canonicalizeVars(s),
                                                              trans.canonicalizeVars(t)));
            if (result == null) { return _parent.get(s, t, h); }
            else { return Option.some(result.applySubstitution(trans.revertingSubstitution())); }
        }

        public int size() { return _results.size() + _parent.size(); }

        public String toString() {
            return IterUtil.multilineToString(_results.entrySet()) + "\n=====\n" + _parent.toString();
        }
    }

}
