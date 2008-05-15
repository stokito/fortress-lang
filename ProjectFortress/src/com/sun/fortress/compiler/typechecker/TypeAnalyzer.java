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
import java.lang.Boolean;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.tuple.Triple;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static com.sun.fortress.compiler.Types.*;
import static com.sun.fortress.compiler.typechecker.TypeAnalyzerUtil.*;
import static com.sun.fortress.nodes_util.NodeFactory.makeInferenceVarType;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

/**
 * Provides core type analysis algorithms in a specific type context.
 */
public class TypeAnalyzer {
    private static final boolean SIMPLIFIED_SUBTYPING = false;

    private static final int MAX_SUBTYPE_DEPTH = 6;
    private static final int MAX_SUBTYPE_EXPANSIONS = 2;

    private static final Option<List<Type>> THROWS_BOTTOM =
        Option.some(Collections.singletonList(BOTTOM));

    private final TraitTable _table;
    private final StaticParamEnv _staticParamEnv;
    private final SubtypeCache _cache;
    private final SubtypeHistory _emptyHistory;

    public TypeAnalyzer(TraitTable table) {
        this(table, StaticParamEnv.make(), RootSubtypeCache.INSTANCE);
    }

    public TypeAnalyzer(TraitTable table, TypeAnalyzer enclosing, List<StaticParam> params,
                        WhereClause whereClause) {
        this(table, enclosing._staticParamEnv.extend(params, whereClause), enclosing._cache);
    }

    private TypeAnalyzer(TraitTable table, StaticParamEnv staticParamEnv, SubtypeCache parentCache) {
        _table = table;
        _staticParamEnv = staticParamEnv;
        _cache = new ChildSubtypeCache(parentCache);
        _emptyHistory = new SubtypeHistory();
    }


    /**
     * Convert the type to a normal form.
     * A normalized type has the following properties:
     * <ul>
     * <li>The throws clause of all arrow types is a singleton list.
     * </ul>
     */
    public Type normalize(Type t) {
        return t;
    }


    /**
     * Produce a formula that, if satisfied, will support s as a subtype of t.
     * Assumes s and t are normalized.
     */
    public ConstraintFormula subtype(Type s, Type t) {
        //return ConstraintFormula.TRUE;
        return SIMPLIFIED_SUBTYPING ? sub(s, t, _emptyHistory) : subtype(s, t, _emptyHistory);
    }

    public Type join(Type s, Type t) {
      return SIMPLIFIED_SUBTYPING ? jn(s, t, _emptyHistory) : join(s, t, _emptyHistory);
    }

    public Type meet(Type s, Type t) {
        return SIMPLIFIED_SUBTYPING ? mt(s, t, _emptyHistory) : meet(s, t, _emptyHistory);
    }

    private ConstraintFormula sub(final Type s, final Type t, SubtypeHistory history) {
        debug.logStart(new String[]{"s", "t"}, s, t);
        Option<ConstraintFormula> cached = _cache.get(s, t, history);
        if (cached.isSome()) {
            ConstraintFormula result = cached.unwrap();
            debug.logEnd("cached result", result);
            return result;
        }
        else if (history.contains(s, t)) {
            debug.logEnd("cyclic invocation result", ConstraintFormula.FALSE);
            return ConstraintFormula.FALSE;
        }
        else {
            final SubtypeHistory h = history.extend(s, t);
            ConstraintFormula result = ConstraintFormula.FALSE;

            // Handle trivial cases
            if (s.equals(BOTTOM)) { debug.logEnd(); return ConstraintFormula.TRUE; }
            if (t.equals(ANY)) { debug.logEnd(); return ConstraintFormula.TRUE; }
            if (s.equals(ANY) && !t.equals(ANY)) { debug.logEnd(); return ConstraintFormula.FALSE; }

            if (!result.isTrue()) {
                ConstraintFormula tResult = t.accept(new NodeAbstractVisitor<ConstraintFormula>() {

                    @Override public ConstraintFormula forType(Type t) {
                        return ConstraintFormula.FALSE;
                    }

                    @Override public ConstraintFormula forInferenceVarType(InferenceVarType t) {
                        if (s instanceof InferenceVarType && s.equals(t)) {
                            return ConstraintFormula.TRUE;
                        }
                        else {
                            return ConstraintFormula.lowerBound(t, s, h);
                        }
                    }

                    @Override public ConstraintFormula forVarType(VarType t) {
                        if (s.equals(t)) { return ConstraintFormula.TRUE; }
                        else if (s instanceof UnionType) {
                            return ConstraintFormula.FALSE;
                        }
                        else {
                            // TODO: recur on lower bounds of variables
                            return ConstraintFormula.FALSE;
                        }
                    }

                    @Override public ConstraintFormula forTraitType(TraitType t) {
                        ConstraintFormula result;
                        if (s instanceof TraitType && ((TraitType) s).getName().equals(t.getName())) {
                            return equiv(((TraitType) s).getArgs(), t.getArgs(), h);
                        }
                        else {
                            TypeConsIndex index = _table.typeCons(t.getName());
                            if (index instanceof TypeAliasIndex) {
                                TypeAliasIndex aliasIndex = (TypeAliasIndex) index;
                                Lambda<Type, Type> subst = makeSubstitution(aliasIndex.staticParameters(),
                                                                            t.getArgs());
                                return sub(s, subst.value(aliasIndex.type()), h);
                            }
                            else { return ConstraintFormula.FALSE; }
                        }
                    }

                    @Override public ConstraintFormula forUnionType(UnionType t) {
                        if (s instanceof UnionType || s instanceof IntersectionType) {
                            return ConstraintFormula.FALSE;
                        }
                        else {
                            ConstraintFormula result = ConstraintFormula.FALSE;
                            for (Type elt : t.getElements()) {
                                result = result.or(sub(s, elt, h), h);
                                if (result.isTrue()) { break; }
                            }
                            return result;
                        }
                    }

                    @Override public ConstraintFormula forIntersectionType(IntersectionType t) {
                        ConstraintFormula result = ConstraintFormula.TRUE;
                        for (Type elt : t.getElements()) {
                            result = result.and(sub(s, elt, h), h);
                            if (result.isFalse()) { break; }
                        }
                        return result;
                    }

                });
                result = result.or(tResult, h);
            }

            if (!result.isTrue()) {

                ConstraintFormula sResult = s.accept(new NodeAbstractVisitor<ConstraintFormula>() {

                    @Override public ConstraintFormula forType(Type s) {
                        return ConstraintFormula.FALSE;
                    }

                    @Override public ConstraintFormula forInferenceVarType(InferenceVarType s) {
                        return ConstraintFormula.upperBound(s, t, h);
                    }

                    @Override public ConstraintFormula forVarType(VarType s) {
                        // TODO: recur on upper bounds
                        if (t instanceof IntersectionType) { return ConstraintFormula.FALSE; }
                        else {
                            // TODO: recur on upper bounds
                            return ConstraintFormula.FALSE;
                        }
                    }

                    @Override public ConstraintFormula forTraitType(TraitType s) {
                        TypeConsIndex index = _table.typeCons(s.getName());
                        if (index instanceof TraitIndex) {
                            if (!(t instanceof TraitType)) { return ConstraintFormula.FALSE; }
                            else {
                                TraitIndex traitIndex = (TraitIndex) index;
                                Lambda<Type, Type> subst = makeSubstitution(traitIndex.staticParameters(),
                                                                            s.getArgs(),
                                                                            traitIndex.hiddenParameters());
                                ConstraintFormula result = ConstraintFormula.FALSE;
                                for (TraitTypeWhere _sup : traitIndex.extendsTypes()) {
                                    BaseType sup = _sup.getType();
                                    ConstraintFormula f = ConstraintFormula.TRUE;
                                    for (Pair<Type, Type> c : traitIndex.typeConstraints()) {
                                        f = f.and(sub(subst.value(c.first()), subst.value(c.second()), h), h);
                                        if (f.isFalse()) { break; }
                                    }
                                    if (!f.isFalse()) { f = f.and(sub(subst.value(sup), t, h), h); }
                                    result = result.or(f, h);
                                    if (result.isTrue()) { break; }
                                }
                                return result;
                            }
                        }
                        else if (index instanceof TypeAliasIndex) {
                            TypeAliasIndex aliasIndex = (TypeAliasIndex) index;
                            Lambda<Type, Type> subst = makeSubstitution(aliasIndex.staticParameters(),
                                                                        s.getArgs());
                            return sub(subst.value(aliasIndex.type()), t, h);
                        }
                        else { throw new IllegalStateException("Unexpected index type"); }
                    }

                    /*
                    @Override public ConstraintFormula forTupleType(TupleType s) {
                        if (t instanceof TupleType) {
                            if (compatibleTuples(s, (TupleType) t)) {
                                ConstraintFormula result = ConstraintFormula.TRUE;
                                TupleType tCast = (TupleType) t;
                                result = sub(s.getElements(), tCast.getElements(), h);
                                if (!result.isFalse()) {
                                    result = result.and(sub(s.getVarargs(), tCast.getVarargs(), h), h);
                                }
                            }
                            else { return ConstraintFormula.FALSE; }
                        }

                        else if (t instanceof TraitType) {
                            return sub(TUPLE, t, h);
                        }

                        else if (t instanceof AndType) {
                            // split to an And
                            List<Type> infElements1 = makeInferenceVarTypes(s.getElements().size());
                            List<Type> infElements2 = makeInferenceVarTypes(s.getElements().size());
                            Type infVarargs1 = newInferenceVar(s.getVarargs());
                            Type infVarargs2 = newInferenceVar(s.getVarargs());
                            ConstraintFormula f = ConstraintFormula.TRUE;
                            for (Triple<Type, Type, Type> ts :
                                     IterUtil.zip(s.getElements(), infElements1, infElements2)) {
                                f = f.and(equiv(ts.first(), new AndType(ts.second(), ts.third()), h), h);
                                if (f.isFalse()) { break; }
                            }
                            if (!f.isFalse()) {
                                f = f.and(equiv(s.getVarargs(), new AndType(inf1, inf2), h), h);
                            }
                            if (!f.isFalse()) {
                                Type sup = new AndType(new TupleType(infElements1, infVarargs1),
                                                       new TupleType(infElements2, infVarargs2);
                                f = f.and(sub(sup, t, h), h);
                            }
                            return f;
                        }

                        else if (t instanceof BottomType) {
                            ConstraintFormula result = ConstraintFormula.FALSE;
                            for (Type eltT : s.getElements()) {
                                ConstraintFormula f = sub(eltT, BOTTOM, h);
                                if (!f.isFalse()) { f = f.and(sub(BOTTOM, t, h), h); }
                                result = result.or(f, h);
                            }
                            return result;
                        }

                        else { return ConstraintFormula.FALSE; }
                    }
                    */

                    @Override public ConstraintFormula forVoidType(VoidType s) {
                        if (t instanceof VoidType) { return ConstraintFormula.TRUE; }
                        else if (t instanceof TraitType) {
                            // extends Any
                            return sub(ANY, t, h);
                        }
                        else { return ConstraintFormula.FALSE; }
                    }

                    @Override public ConstraintFormula forArrowType(ArrowType s) {
                        // domain is BottomType
                        ConstraintFormula result = sub(s.getDomain(), BOTTOM_DOMAIN, h);
                        if (!result.isFalse()) {
                            result = result.and(sub(new ArrowType(BOTTOM_DOMAIN, BOTTOM), t, h), h);
                        }

                        if (!result.isTrue()) {

                            if (t instanceof ArrowType) {
                                ArrowType tCast = (ArrowType) t;
                                ConstraintFormula f = sub(tCast.getDomain(), s.getDomain(), h);
                                if (!f.isFalse()) {
                                    f = f.and(sub(s.getRange(), tCast.getRange(), h), h);
                                }
                                if (!f.isFalse()) {
                                    f = f.and(sub(s.getEffect(), tCast.getEffect(), h), h);
                                }
                                result = result.or(f, h);
                            }

                            else if (t instanceof TraitType) {
                                // extends Object
                                result = result.or(sub(OBJECT, t, h), h);
                            }

//                            else if (t instanceof AndType) {
//                                // split Or domain to an And
//                                InferenceVarType d1 = makeInferenceVarType();
//                                InferenceVarType d2 = makeInferenceVarType();
//                                ConstraintFormula f = equiv(s.getDomain(), new OrType(d1, d2), h);
//                                if (!f.isFalse()) {
//                                    Type sup = new AndType(makeArrow(d1, s.getRange(), throwsType, s.isIo()),
//                                                           makeArrow(d2, s.getRange(), throwsType, s.isIo()));
//                                    f = f.and(sub(sup, t, h), h);
//                                }
//                                result = result.or(f, h);
//
//                                if (!result.isTrue()) {
//                                    // split And range/throws to an And
//                                    InferenceVarType r1 = makeInferenceVarType();
//                                    InferenceVarType r2 = makeInferenceVarType();
//                                    InferenceVarType th1 = makeInferenceVarType();
//                                    InferenceVarType th2 = makeInferenceVarType();
//                                    ConstraintFormula f2 = equiv(s.getRange(), new AndType(r1, r2), h);
//                                    if (!f2.isFalse()) {
//                                        f2 = f2.and(equiv(throwsType, new AndType(th1, th2), h), h);
//                                    }
//                                    if (!f2.isFalse()) {
//                                        Type sup = new AndType(makeArrow(s.getDomain(), r1, th1, s.isIo()),
//                                                               makeArrow(s.getDomain(), r2, th2, s.isIo()));
//                                        f2 = f2.and(sub(sup, t, h), h);
//                                    }
//                                    result = result.or(f2, h);
//                                }
//                            }
                        }
                        return result;
                    }

                    @Override public ConstraintFormula forUnionType(UnionType s) {
                        ConstraintFormula result = ConstraintFormula.TRUE;
                        for (Type elt : s.getElements()) {
                            result = result.and(sub(elt, t, h), h);
                            if (result.isFalse()) { break; }
                        }
                        return result;
                    }

                    @Override public ConstraintFormula forIntersectionType(IntersectionType s) {
                        if (t instanceof IntersectionType) { return ConstraintFormula.FALSE; }
                        //excl(s.getFirst(), s.getSecond(), h); }
                        else {
                            //ConstraintFormula result = excl(s.getFirst(), s.getSecond(), h);
                            ConstraintFormula result = ConstraintFormula.FALSE;
                            for (Type elt : s.getElements()) {
                                result = result.or(sub(elt, t, h), h);
                                if (result.isTrue()) { break; }
                            }
                            /*
                            if (!result.isTrue()) {
                                // merge tuple
                                // Simplification: assumes this rule is only relevant if one of the
                                // two types is a tuple, and then only for the tuple form it matches.
                                TupleType form = null;
                                if (s.getFirst() instanceof TupleType) { form = (TupleType) s.getFirst(); }
                                if (s.getSecond() instanceof TupleType) {
                                    if (form == null) { form = (TupleType) s.getSecond(); }
                                    else if (!compatibleTuples(form, (TupleType) s.getSecond())) { form = null; }
                                }
                                if (form != null) {
                                    List<Type> infElements1 = makeInferenceVarTypes(form.getElements().size());
                                    List<Type> infElements2 = makeInferenceVarTypes(form.getElements().size());
                                    Type infVarargs1 = newInferenceVar(form.getVarargs());
                                    Type infVarargs2 = newInferenceVar(form.getVarargs());
                                    TupleType match1 = new TupleType(infElements1, infVarargs1);
                                    TupleType match2 = new TupleType(infElements2, infVarargs2);
                                    ConstraintFormula f = equiv(s.getFirst(), match1, h);
                                    if (!f.isFalse()) { f = f.and(equiv(s.getSecond(), match2, h), h); }
                                    if (!f.isFalse()) {
                                        List<Type> andElements = new LinkedList<Type>();
                                        for (Pair<Type, Type> ts : IterUtil.zip(infElements1, infElements2)) {
                                            andElements.add(new AndType(ts.first(), ts.second()));
                                        }
                                        Type andVarargs = new AndType(infVarargs1, infVarargs2);
                                        TupleType sup = new TupleType(andElements, andVarargs);
                                        f = f.and(sub(sup, t, h), h);
                                    }
                                    result = result.or(f, h);
                                }
                            }
                            */
                            return result;
                        }
                    }

                });
                result = result.or(sResult, h);
            }
            _cache.put(s, t, history, result);
            debug.logEnd("result", result);
            return result;
        }
    }

    /**
     * Produce a formula that, if satisfied, will support s as a subtype of t.
     * Assumes s and t are normalized.
     */
    private ConstraintFormula subtype(final Type s, final Type t, SubtypeHistory history) {
        debug.logStart(new String[]{"s", "t"}, s, t);
        debug.logValues(new String[]{"cache size", "history size", "history expansions"},
                        _cache.size(), history.size(), history.expansions());
        //debug.logStack();
        Option<ConstraintFormula> cached = _cache.get(s, t, history);
        if (cached.isSome()) {
            ConstraintFormula result = cached.unwrap();
            debug.logEnd("cached result", result);
            return result;
        }
        else if (history.expansions() > MAX_SUBTYPE_EXPANSIONS) {
            debug.logEnd("max subtype expansions result", ConstraintFormula.FALSE);
            return ConstraintFormula.FALSE;
        }
        else if (history.size() > MAX_SUBTYPE_DEPTH) {
            debug.logEnd("max subtype depth result", ConstraintFormula.FALSE);
            return ConstraintFormula.FALSE;
        }
        else if (history.contains(s, t)) {
            debug.logEnd("cyclic invocation result", ConstraintFormula.FALSE);
            return ConstraintFormula.FALSE;
        }
        else {
            final SubtypeHistory h = history.extend(s, t);

            ConstraintFormula result = t.accept(new NodeAbstractVisitor<ConstraintFormula>() {

                @Override public ConstraintFormula forType(Type t) {
                    return ConstraintFormula.FALSE;
                }

                @Override public ConstraintFormula forAnyType(AnyType t) {
                    return ConstraintFormula.TRUE;
                }

                @Override public ConstraintFormula forInferenceVarType(InferenceVarType t) {
                    if (s instanceof InferenceVarType && s.equals(t)) {
                        return ConstraintFormula.TRUE;
                    }
                    else {
                        return ConstraintFormula.lowerBound(t, s, h);
                    }
                }

            });

            if (!result.isTrue()) {

                ConstraintFormula sResult = s.accept(new NodeAbstractVisitor<ConstraintFormula>() {

                    @Override public ConstraintFormula forAnyType(AnyType s) {
                        return ConstraintFormula.FALSE; // t is not Any, because result is not true
                    }

                    @Override public ConstraintFormula forBottomType(BottomType s) {
                        return ConstraintFormula.TRUE;
                    }

                    @Override public ConstraintFormula forInferenceVarType(InferenceVarType s) {
                        return ConstraintFormula.upperBound(s, t, h);
                    }

                    @Override public ConstraintFormula forVarType(VarType s) {
                        if (s.equals(t)) { return ConstraintFormula.TRUE; }
                        else { return ConstraintFormula.FALSE; }
                    }

                    @Override public ConstraintFormula forTraitType(TraitType s) {
                        ConstraintFormula result;
                        if (t instanceof TraitType && s.getName().equals(((TraitType) t).getName())) {
                            result = equivalent(s.getArgs(), ((TraitType) t).getArgs(), h);
                        }
                        else { result = ConstraintFormula.FALSE; }

                        if (!result.isTrue()) {
                            TypeConsIndex index = _table.typeCons(s.getName());
                            if (index instanceof TraitIndex) {
                                TraitIndex traitIndex = (TraitIndex) index;
                                List<Id> traitHidden = traitIndex.hiddenParameters();
                                Lambda<Type, Type> subst = makeSubstitution(traitIndex.staticParameters(),
                                                                            s.getArgs(), traitHidden);
                                for (TraitTypeWhere _sup : traitIndex.extendsTypes()) {
                                    BaseType sup = _sup.getType();
                                    ConstraintFormula f = ConstraintFormula.TRUE;
                                    for (Pair<Type, Type> c : traitIndex.typeConstraints()) {
                                        SubtypeHistory h2;
                                        if (containsVariable(c.first(), traitHidden) ||
                                            containsVariable(c.second(), traitHidden)) {
                                          h2 = h.expand();
                                        }
                                        else { h2 = h; }
                                        f = f.and(subtype(subst.value(c.first()), subst.value(c.second()), h2), h);
                                        if (f.isFalse()) { break; }
                                    }
                                    if (!f.isFalse()) {
                                      SubtypeHistory h2 = containsVariable(sup, traitHidden) ? h.expand() : h;
                                      f = f.and(subtype(subst.value(sup), t, h2), h);
                                    }
                                    result = result.or(f, h);
                                    if (result.isTrue()) { break; }
                                }
                            }
                            else if (index instanceof TypeAliasIndex) {
                                TypeAliasIndex aliasIndex = (TypeAliasIndex) index;
                                Lambda<Type, Type> subst = makeSubstitution(aliasIndex.staticParameters(),
                                                                            s.getArgs());
                                result = result.or(subtype(subst.value(aliasIndex.type()), t, h), h);
                            }
                        }
                        return result;
                    }

                    /*
                    @Override public ConstraintFormula forTupleType(TupleType s) {
                        ConstraintFormula result;
                        if (t instanceof TupleType && compatibleTuples(s, (TupleType) t)) {
                            // Simplification: we allow covariance here
                            TupleType tCast = (TupleType) t;
                            result = subtype(s.getElements(), tCast.getElements(), h);
                            if (!result.isFalse()) {
                                result = result.and(subtype(s.getVarargs(), tCast.getVarargs(), h), h);
                            }
                        }
                        else { result = ConstraintFormula.FALSE; }

                        if (!result.isTrue()) {
                            // extends Tuple
                            result = result.or(subtype(TUPLE, t, h), h);
                        }
                        if (!result.isTrue()) {
                            // covariance
                            List<Type> infElements = makeInferenceVarTypes(s.getElements().size());
                            Type infVarargs = newInferenceVar(s.getVarargs());
                            ConstraintFormula f = subtype(s.getElements(), infElements, h);
                            if (!f.isFalse()) { f = f.and(subtype(s.getVarargs(), infVarargs, h), h); }
                            if (!f.isFalse()) {
                                TupleType sup = new TupleType(infElements, infVarargs);
                                f = f.and(subtype(sup, t, h), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // split to an And
                            List<Type> infElements1 = makeInferenceVarTypes(s.getElements().size());
                            List<Type> infElements2 = makeInferenceVarTypes(s.getElements().size());
                            Type infVarargs1 = newInferenceVar(s.getVarargs());
                            Type infVarargs2 = newInferenceVar(s.getVarargs());
                            ConstraintFormula f = ConstraintFormula.TRUE;
                            for (Triple<Type, Type, Type> ts :
                                     IterUtil.zip(s.getElements(), infElements1, infElements2)) {
                                f = f.and(equivalent(ts.first(), new AndType(ts.second(), ts.third()), h), h);
                                if (f.isFalse()) { break; }
                            }
                            if (!f.isFalse()) {
                                f = f.and(equivalent(s.getVarargs(), new AndType(infVarargs1, infVarargs2), h), h);
                            }
                            if (!f.isFalse()) {
                                Type sup = new AndType(new TupleType(infElements1, infVarargs1),
                                                       new TupleType(infElements2, infVarargs2));
                                f = f.and(subtype(sup, t, h), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // BottomType
                            for (Type eltT : s.getElements()) {
                                ConstraintFormula f = subtype(eltT, BOTTOM, h);
                                if (!f.isFalse()) { f = f.and(subtype(BOTTOM, t, h), h); }
                                result = result.or(f, h);
                            }
                        }
                        return result;
                    }
                    */

                    @Override public ConstraintFormula forVoidType(VoidType s) {
                        if (t instanceof VoidType) { return ConstraintFormula.TRUE; }
                        else {
                            // extends Any
                            return subtype(ANY, t, h);
                        }
                    }

                    @Override public ConstraintFormula forArrowType(ArrowType s) {
                        ConstraintFormula result;
                        if (t instanceof ArrowType) {
                            // Simplification: allow covariance/contravariance here
                            ArrowType tCast = (ArrowType) t;
                            result = subdomain(tCast.getDomain(), s.getDomain(), h);
                            if (!result.isFalse()) {
                                result = result.and(subtype(s.getRange(), tCast.getRange(), h), h);
                            }
                            if (!result.isFalse()) {
                                result = result.and(subeffect(s.getEffect(), tCast.getEffect(), h), h);
                            }
                        }
                        else { result = ConstraintFormula.FALSE; }

                        if (!result.isTrue()) {
                            // extends Object
                            result = result.or(subtype(OBJECT, t, h), h);
                        }
//                        if (!result.isTrue()) {
//                            // covariance/contravariance
//                            InferenceVarType infDomain = makeInferenceVarType();
//                            InferenceVarType infRange = makeInferenceVarType();
//                            InferenceVarType infThrowsType = makeInferenceVarType();
//                            ConstraintFormula f = ConstraintFormula.upperBound(infDomain, s.getDomain(), h);
//                            f = f.and(ConstraintFormula.lowerBound(infRange, s.getRange(), h), h);
//                            f = f.and(ConstraintFormula.lowerBound(infThrowsType, throwsType, h), h);
//                            Type sup = makeArrow(infDomain, infRange, infThrowsType, s.isIo());
//                            ConstraintFormula f1 = f.and(subtype(sup, t, h.expand()), h);
//                            result = result.or(f1, h);
//                            if (!result.isTrue() && !s.isIo()) {
//                                sup = makeArrow(infDomain, infRange, infThrowsType, true);
//                                ConstraintFormula f2 = f.and(subtype(sup, t, h.expand()), h);
//                                result = result.or(f2, h);
//                            }
//                        }
                        if (!result.isTrue()) {
                            // domain is BottomType
                            ConstraintFormula f = subdomain(s.getDomain(), BOTTOM_DOMAIN, h);
                            if (!f.isFalse()) {
                                f = f.and(subtype(new ArrowType(BOTTOM_DOMAIN, BOTTOM), t, h), h);
                            }
                            result = result.or(f, h);
                        }
//                        if (!result.isTrue()) {
//                            // split Or domain to an And
//                            InferenceVarType d1 = makeInferenceVarType();
//                            InferenceVarType d2 = makeInferenceVarType();
//                            ConstraintFormula f = equivalent(s.getDomain(), new OrType(d1, d2), h.expand());
//                            if (!f.isFalse()) {
//                                Type sup = new AndType(makeArrow(d1, s.getRange(), throwsType, s.isIo()),
//                                                       makeArrow(d2, s.getRange(), throwsType, s.isIo()));
//                                f = f.and(subtype(sup, t, h.expand()), h);
//                            }
//                            result = result.or(f, h);
//                        }
//                        if (!result.isTrue()) {
//                            // split And range/throws to an And
//                            InferenceVarType r1 = makeInferenceVarType();
//                            InferenceVarType r2 = makeInferenceVarType();
//                            InferenceVarType th1 = makeInferenceVarType();
//                            InferenceVarType th2 = makeInferenceVarType();
//                            ConstraintFormula f = equivalent(s.getRange(), new AndType(r1, r2), h.expand());
//                            if (!f.isFalse()) {
//                                f = f.and(equivalent(throwsType, new AndType(th1, th2), h.expand()), h);
//                            }
//                            if (!f.isFalse()) {
//                                Type sup = new AndType(makeArrow(s.getDomain(), r1, th1, s.isIo()),
//                                                       makeArrow(s.getDomain(), r2, th2, s.isIo()));
//                                f = f.and(subtype(sup, t, h.expand()), h);
//                            }
//                            result = result.or(f, h);
//                        }
                        return result;
                    }

                    @Override public ConstraintFormula forUnionType(UnionType s) {
                        ConstraintFormula result = ConstraintFormula.TRUE;
                        for (Type elt : s.getElements()) {
                            result = result.and(sub(elt, t, h), h);
                            if (result.isFalse()) { break; }
                        }
                        return result;
                    }

                    @Override public ConstraintFormula forIntersectionType(IntersectionType s) {
                        ConstraintFormula result = ConstraintFormula.FALSE;
                        for (Type elt : s.getElements()) {
                            result = result.or(sub(elt, t, h), h);
                            if (result.isTrue()) { break; }
                        }
                        return result;
                    }
                    /*
                    @Override public ConstraintFormula forOrType(OrType s) {
                        ConstraintFormula result;
                        if (t instanceof OrType) {
                            result = equivalent(s.getFirst(), ((OrType) t).getFirst(), h);
                            if (!result.isFalse()) {
                                result = result.and(equivalent(s.getSecond(), ((OrType) t).getSecond(), h), h);
                            }
                        }
                        else { result = ConstraintFormula.FALSE; }

                        if (!result.isTrue()) {
                            // common supertype
                            ConstraintFormula f = subtype(s.getFirst(), t, h);
                            if (!f.isFalse()) { f = f.and(subtype(s.getSecond(), t, h), h); }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // distribution of And
                            InferenceVarType inf1 = makeInferenceVarType();
                            InferenceVarType inf2 = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), new AndType(inf1, inf2), h.expand());
                            if (!f.isFalse()) {
                                Type sup = new AndType(new OrType(inf1, s.getSecond()),
                                                       new OrType(inf2, s.getSecond()));
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // commutativity
                            result = result.or(subtype(new OrType(s.getSecond(), s.getFirst()), t, h), h);
                        }
                        if (!result.isTrue()) {
                            // associativity
                            InferenceVarType inf1 = makeInferenceVarType();
                            InferenceVarType inf2 = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), new OrType(inf1, inf2), h.expand());
                            if (!f.isFalse()) {
                                Type sup = new OrType(inf1, new OrType(inf2, s.getSecond()));
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        return result;
                    }
                    */

                    /*
                    @Override public ConstraintFormula forAndType(AndType s) {
                        ConstraintFormula result;
                        if (t instanceof AndType) {
                            result = equivalent(s.getFirst(), ((AndType) t).getFirst(), h);
                            if (!result.isFalse()) {
                                result = result.and(equivalent(s.getSecond(), ((AndType) t).getSecond(), h), h);
                            }
                        }
                        else { result = ConstraintFormula.FALSE; }

                        if (!result.isTrue()) {
                            // extends its first element
                            result = result.or(subtype(s.getFirst(), t, h), h);
                        }
                        if (!result.isTrue()) {
                            // extends its second element
                            result = result.or(subtype(s.getSecond(), t, h), h);
                        }
                        if (!result.isTrue()) {
                            // elements exclude each other
                            ConstraintFormula f = excludes(s.getFirst(), s.getSecond(), h);
                            if (!f.isFalse()) {
                                f = f.and(subtype(BOTTOM, t, h), h);
                            }
                            result = result.or(f, h);
                        }
                        */
                        /*
                        if (!result.isTrue()) {
                            // merge tuple
                            // Simplification: assumes this rule is only relevant if one of the
                            // two types is a tuple, and then only for the tuple form it matches.
                            TupleType form = null;
                            if (s.getFirst() instanceof TupleType) { form = (TupleType) s.getFirst(); }
                            if (s.getSecond() instanceof TupleType) {
                                if (form == null) { form = (TupleType) s.getSecond(); }
                                else if (!compatibleTuples(form, (TupleType) s.getSecond())) { form = null; }
                            }
                            if (form != null) {
                                List<Type> infElements1 = makeInferenceVarTypes(form.getElements().size());
                                List<Type> infElements2 = makeInferenceVarTypes(form.getElements().size());
                                Type infVarargs1 = newInferenceVar(form.getVarargs());
                                Type infVarargs2 = newInferenceVar(form.getVarargs());
                                TupleType match1 = new TupleType(infElements1, infVarargs1);
                                TupleType match2 = new TupleType(infElements2, infVarargs2);
                                ConstraintFormula f = equivalent(s.getFirst(), match1, h);
                                if (!f.isFalse()) { f = f.and(equivalent(s.getSecond(), match2, h), h); }
                                if (!f.isFalse()) {
                                    List<Type> andElements = new LinkedList<Type>();
                                    for (Pair<Type, Type> ts : IterUtil.zip(infElements1, infElements2)) {
                                        andElements.add(new AndType(ts.first(), ts.second()));
                                    }
                                    Type andVarargs = new AndType(infVarargs1, infVarargs2);
                                    TupleType sup = new TupleType(andElements, andVarargs);
                                    f = f.and(subtype(sup, t, h), h);
                                }
                                result = result.or(f, h);
                            }
                        }
                        */
                        /*
                        if (!result.isTrue()) {
                            // merge arrow domain (non-io)
                            InferenceVarType d1 = makeInferenceVarType();
                            InferenceVarType d2 = makeInferenceVarType();
                            InferenceVarType r = makeInferenceVarType();
                            InferenceVarType th = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), makeArrow(d1, r, th, false), h.expand());
                            if (!f.isFalse()) {
                                f = f.and(equivalent(s.getSecond(), makeArrow(d2, r, th, false), h.expand()), h);
                            }
                            if (!f.isFalse()) {
                                ArrowType sup = makeArrow(new AndType(d1, d2), r, th, false);
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // merge arrow domain (io)
                            InferenceVarType d1 = makeInferenceVarType();
                            InferenceVarType d2 = makeInferenceVarType();
                            InferenceVarType r = makeInferenceVarType();
                            InferenceVarType th = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), makeArrow(d1, r, th, true), h.expand());
                            if (!f.isFalse()) {
                                f = f.and(equivalent(s.getSecond(), makeArrow(d2, r, th, true), h.expand()), h);
                            }
                            if (!f.isFalse()) {
                                ArrowType sup = makeArrow(new AndType(d1, d2), r, th, true);
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // merge arrow range (non-io)
                            InferenceVarType d = makeInferenceVarType();
                            InferenceVarType r1 = makeInferenceVarType();
                            InferenceVarType r2 = makeInferenceVarType();
                            InferenceVarType th1 = makeInferenceVarType();
                            InferenceVarType th2 = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), makeArrow(d, r1, th1, false), h.expand());
                            if (!f.isFalse()) {
                                f = f.and(equivalent(s.getSecond(), makeArrow(d, r2, th2, false), h.expand()), h);
                            }
                            if (!f.isFalse()) {
                                ArrowType sup = makeArrow(d, new AndType(r1, r2), new AndType(th1, th2), false);
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // merge arrow range (io)
                            InferenceVarType d = makeInferenceVarType();
                            InferenceVarType r1 = makeInferenceVarType();
                            InferenceVarType r2 = makeInferenceVarType();
                            InferenceVarType th1 = makeInferenceVarType();
                            InferenceVarType th2 = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), makeArrow(d, r1, th1, true), h.expand());
                            if (!f.isFalse()) {
                                f = f.and(equivalent(s.getSecond(), makeArrow(d, r2, th2, true), h.expand()), h);
                            }
                            if (!f.isFalse()) {
                                ArrowType sup = makeArrow(d, new AndType(r1, r2), new AndType(th1, th2), true);
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        */
                    /*
                        if (!result.isTrue()) {
                            // distribution of Or
                            InferenceVarType inf1 = makeInferenceVarType();
                            InferenceVarType inf2 = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), new OrType(inf1, inf2), h.expand());
                            if (!f.isFalse()) {
                                Type sup = new OrType(new AndType(inf1, s.getSecond()),
                                                      new AndType(inf2, s.getSecond()));
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        if (!result.isTrue()) {
                            // commutativity
                            result = result.or(subtype(new AndType(s.getSecond(), s.getFirst()), t, h), h);
                        }
                        if (!result.isTrue()) {
                            // associativity
                            InferenceVarType inf1 = makeInferenceVarType();
                            InferenceVarType inf2 = makeInferenceVarType();
                            ConstraintFormula f = equivalent(s.getFirst(), new AndType(inf1, inf2), h.expand());
                            if (!f.isFalse()) {
                                Type sup = new AndType(inf1, new OrType(inf2, s.getSecond()));
                                f = f.and(subtype(sup, t, h.expand()), h);
                            }
                            result = result.or(f, h);
                        }
                        return result;
                    }
                    */

                });
                result = result.or(sResult, h);
            }

            /*
            if (!result.isTrue()) {
                // expand to intersection
                InferenceVarType inf = makeInferenceVarType();
                ConstraintFormula f = subtype(new AndType(s, inf), t, h.expand());
                f = f.and(ConstraintFormula.lowerBound(inf, s, h), h);
                result = result.or(f, h);
            }
            if (!result.isTrue()) {
                // expand to union
                InferenceVarType inf = makeInferenceVarType();
                result = result.or(subtype(new OrType(s, inf), t, h.expand()), h);
            }
            */

            // match where declarations
            // reverse aliases

            _cache.put(s, t, history, result);
            debug.logEnd("result", result);
            return result;
        }
    }

    public ConstraintFormula equivalent(Type s, Type t, SubtypeHistory history) {
        debug.logStart(new String[]{"s", "t"}, s, t);
        ConstraintFormula result = subtype(s, t, history);
        if (!result.isFalse()) { result = result.and(subtype(t, s, history), history); }
        debug.logEnd("result", result);
        return result;
    }

    public ConstraintFormula equiv(Type s, Type t, SubtypeHistory history) {
        debug.logStart(new String[]{"s", "t"}, s, t);
        ConstraintFormula result = sub(s, t, history);
        if (!result.isFalse()) { result = result.and(sub(t, s, history), history); }
        debug.logEnd("result", result);
        return result;
    }


    public ConstraintFormula equivalent(StaticArg a1, final StaticArg a2,
                                        final SubtypeHistory history) {
        return a1.accept(new NodeAbstractVisitor<ConstraintFormula>() {
            @Override public ConstraintFormula forTypeArg(TypeArg a1) {
                if (a2 instanceof TypeArg) {
                    return equivalent(a1.getType(), ((TypeArg) a2).getType(), history);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forIntArg(IntArg a1) {
                if (a2 instanceof IntArg) {
                    boolean result = a1.getVal().equals(((IntArg) a2).getVal());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forBoolArg(BoolArg a1) {
                if (a2 instanceof BoolArg) {
                    boolean result = a1.getBool().equals(((BoolArg) a2).getBool());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forOpArg(OpArg a1) {
                if (a2 instanceof OpArg) {
                    boolean result = a1.getName().equals(((OpArg) a2).getName());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forDimArg(DimArg a1) {
                if (a2 instanceof DimArg) {
                    boolean result = a1.getDim().equals(((DimArg) a2).getDim());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forUnitArg(UnitArg a1) {
                if (a2 instanceof UnitArg) {
                    boolean result = a1.getUnit().equals(((UnitArg) a2).getUnit());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
        });
    }

    public ConstraintFormula equiv(StaticArg a1, final StaticArg a2, final SubtypeHistory history) {
        return a1.accept(new NodeAbstractVisitor<ConstraintFormula>() {
            @Override public ConstraintFormula forTypeArg(TypeArg a1) {
                if (a2 instanceof TypeArg) {
                    return equiv(a1.getType(), ((TypeArg) a2).getType(), history);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forIntArg(IntArg a1) {
                if (a2 instanceof IntArg) {
                    boolean result = a1.getVal().equals(((IntArg) a2).getVal());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forBoolArg(BoolArg a1) {
                if (a2 instanceof BoolArg) {
                    boolean result = a1.getBool().equals(((BoolArg) a2).getBool());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forOpArg(OpArg a1) {
                if (a2 instanceof OpArg) {
                    boolean result = a1.getName().equals(((OpArg) a2).getName());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forDimArg(DimArg a1) {
                if (a2 instanceof DimArg) {
                    boolean result = a1.getDim().equals(((DimArg) a2).getDim());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
            @Override public ConstraintFormula forUnitArg(UnitArg a1) {
                if (a2 instanceof UnitArg) {
                    boolean result = a1.getUnit().equals(((UnitArg) a2).getUnit());
                    return ConstraintFormula.fromBoolean(result);
                }
                else { return ConstraintFormula.FALSE; }
            }
        });
    }

    public ConstraintFormula excludes(Type s, Type t, SubtypeHistory history) {
        return ConstraintFormula.FALSE;
    }

    public ConstraintFormula excl(Type s, Type t, SubtypeHistory history) {
        return ConstraintFormula.FALSE;
    }

    public Type meet(Type s, Type t, SubtypeHistory history) {
        if (subtype(s, t, history).isTrue()) { return s; }
        else if (subtype(t, s, history).isTrue()) { return t; }
        else { return NodeFactory.makeIntersectionType(s, t); }
    }

    public Type mt(Type s, Type t, SubtypeHistory history) {
        if (sub(s, t, history).isTrue()) { return s; }
        else if (sub(t, s, history).isTrue()) { return t; }
        else { return NodeFactory.makeIntersectionType(s, t); }
    }

    public Type join(Type s, Type t, SubtypeHistory history) {
        if (subtype(s, t, history).isTrue()) { return t; }
        else if (subtype(t, s, history).isTrue()) { return s; }
        else { return NodeFactory.makeUnionType(s, t); }
    }

    public Type jn(Type s, Type t, SubtypeHistory history) {
        if (sub(s, t, history).isTrue()) { return t; }
        else if (sub(t, s, history).isTrue()) { return s; }
        else { return NodeFactory.makeUnionType(s, t); }
    }


    /** Assumes type lists have the same length. */
    private ConstraintFormula subtype(Iterable<? extends Type> ss, Iterable<? extends Type> ts,
                                      SubtypeHistory history) {
        ConstraintFormula result = ConstraintFormula.TRUE;
        for (Pair<Type, Type> pair : IterUtil.zip(ss, ts)) {
            result = result.and(subtype(pair.first(), pair.second(), history), history);
            if (result.isFalse()) { break; }
        }
        return result;
    }

    /** Assumes type lists have the same length. */
    private ConstraintFormula sub(Iterable<? extends Type> ss, Iterable<? extends Type> ts,
                                  SubtypeHistory history) {
        ConstraintFormula result = ConstraintFormula.TRUE;
        for (Pair<Type, Type> pair : IterUtil.zip(ss, ts)) {
            result = result.and(sub(pair.first(), pair.second(), history), history);
            if (result.isFalse()) { break; }
        }
        return result;
    }
    
    private ConstraintFormula subdomain(Domain s, Domain t, SubtypeHistory history) {
        ConstraintFormula result = subtype(stripKeywords(s), stripKeywords(t), history);
        if (!result.isFalse()) {
            Map<Id, Type> sMap = extractKeywords(s);
            Map<Id, Type> tMap = extractKeywords(t);
            if (tMap.keySet().containsAll(sMap.keySet())) {
                for (Map.Entry<Id, Type> entry : sMap.entrySet()) {
                    Type sup = tMap.get(entry.getKey());
                    result = result.and(subtype(entry.getValue(), sup, history), history);
                    if (result.isFalse()) { break; }
                }
            }
            else { result = ConstraintFormula.FALSE; }
        }
        return result;
    }
    
    private ConstraintFormula sub(Domain s, Domain t, SubtypeHistory history) {
        ConstraintFormula result = sub(stripKeywords(s), stripKeywords(t), history);
        if (!result.isFalse()) {
            Map<Id, Type> sMap = extractKeywords(s);
            Map<Id, Type> tMap = extractKeywords(t);
            if (tMap.keySet().containsAll(sMap.keySet())) {
                for (Map.Entry<Id, Type> entry : sMap.entrySet()) {
                    Type sup = tMap.get(entry.getKey());
                    result = result.and(sub(entry.getValue(), sup, history), history);
                    if (result.isFalse()) { break; }
                }
            }
            else { result = ConstraintFormula.FALSE; }
        }
        return result;
    }
    
    private ConstraintFormula subeffect(Effect s, Effect t, SubtypeHistory history) {
        if (t.isIo() || !s.isIo()) {
            return ConstraintFormula.TRUE; // TODO: check throws clauses
        }
        else { return ConstraintFormula.FALSE; }
    }

    private ConstraintFormula sub(Effect s, Effect t, SubtypeHistory history) {
        if (t.isIo() || !s.isIo()) {
            return ConstraintFormula.TRUE; // TODO: check throws clauses
        }
        else { return ConstraintFormula.FALSE; }
    }

    private Option<Type> newInferenceVar(Option<Type> varargs) {
        if (varargs.isSome()) { return Option.<Type>some(makeInferenceVarType()); }
        else { return Option.none(); }
    }

    private List<KeywordType> newInferenceVars(List<KeywordType> keys) {
        List<KeywordType> result = new ArrayList<KeywordType>(keys.size());
        for (KeywordType k : keys) {
            result.add(new KeywordType(k.getName(), makeInferenceVarType()));
        }
        return result;
    }



    /** Assumes arg lists have the same length. */
    private ConstraintFormula equivalent(Iterable<? extends StaticArg> a1s,
                                         Iterable<? extends StaticArg> a2s,
                                         SubtypeHistory history) {
        ConstraintFormula result = ConstraintFormula.TRUE;
        for (Pair<StaticArg, StaticArg> pair : IterUtil.zip(a1s, a2s)) {
            result = result.and(equivalent(pair.first(), pair.second(), history), history);
            if (result.isFalse()) { break; }
        }
        return result;
    }

    /** Assumes arg lists have the same length. */
    private ConstraintFormula equiv(Iterable<? extends StaticArg> a1s,
                                    Iterable<? extends StaticArg> a2s,
                                    SubtypeHistory history) {
        ConstraintFormula result = ConstraintFormula.TRUE;
        for (Pair<StaticArg, StaticArg> pair : IterUtil.zip(a1s, a2s)) {
            result = result.and(equiv(pair.first(), pair.second(), history), history);
            if (result.isFalse()) { break; }
        }
        return result;
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
        public ConstraintFormula subtype(Type s, Type t) {
            return SIMPLIFIED_SUBTYPING ?
                TypeAnalyzer.this.sub(s, t, this) :
                TypeAnalyzer.this.subtype(s, t, this);
        }
        public Type meet(Type s, Type t) {
            return SIMPLIFIED_SUBTYPING ?
                TypeAnalyzer.this.mt(s, t, this) :
                TypeAnalyzer.this.meet(s, t, this);
        }
        public Type join(Type s, Type t) {
            return SIMPLIFIED_SUBTYPING ?
                TypeAnalyzer.this.jn(s, t, this) :
                TypeAnalyzer.this.join(s, t, this);
        }
        public String toString() {
          return IterUtil.multilineToString(_entries) + "\n" + _expansions + " expansions";
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
