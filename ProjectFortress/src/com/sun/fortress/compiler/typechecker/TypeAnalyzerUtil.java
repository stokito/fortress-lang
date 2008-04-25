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
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;

import static com.sun.fortress.compiler.StaticError.errorMsg;
import static edu.rice.cs.plt.debug.DebugUtil.debug;
import static edu.rice.cs.plt.tuple.Option.*;

public class TypeAnalyzerUtil {

    public static Lambda<Type, Type> makeSubstitution(Iterable<? extends StaticParam> params,
                                                Iterable<? extends StaticArg> args) {
        return makeSubstitution(params, args, IterUtil.<Id>empty());
    }

    /** Assumes param/arg lists have the same length and corresponding elements are compatible. */
    public static Lambda<Type, Type> makeSubstitution(Iterable<? extends StaticParam> params,
                                                      Iterable<? extends StaticArg> args,
                                                      Iterable<? extends Id> hiddenParams) {
        final Map<QualifiedIdName, Type> typeSubs = new HashMap<QualifiedIdName, Type>();
        final Map<Op, Op> opSubs = new HashMap<Op, Op>();
        final Map<QualifiedIdName, IntExpr> intSubs = new HashMap<QualifiedIdName, IntExpr>();
        final Map<QualifiedIdName, BoolExpr> boolSubs = new HashMap<QualifiedIdName, BoolExpr>();
        final Map<QualifiedIdName, DimExpr> dimSubs = new HashMap<QualifiedIdName, DimExpr>();
        final Map<QualifiedIdName, UnitExpr> unitSubs = new HashMap<QualifiedIdName, UnitExpr>();
        for (Pair<StaticParam, StaticArg> pair : IterUtil.zip(params, args)) {
            final StaticArg a = pair.second();
            pair.first().accept(new NodeAbstractVisitor_void() {
                @Override public void forTypeParam(TypeParam p) {
                    typeSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                 ((TypeArg) a).getType());
                }
                @Override public void forOprParam(OprParam p) {
                    opSubs.put(p.getName(), ((OprArg) a).getName());
                }
                @Override public void forIntParam(IntParam p) {
                    intSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                ((IntArg) a).getVal());
                }
                @Override public void forNatParam(NatParam p) {
                    intSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                ((IntArg) a).getVal());
                }
                @Override public void forBoolParam(BoolParam p) {
                    boolSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                 ((BoolArg) a).getBool());
                }
                @Override public void forDimParam(DimParam p) {
                    dimSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                ((DimArg) a).getDim());
                }
                @Override public void forUnitParam(UnitParam p) {
                    unitSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                 ((UnitArg) a).getUnit());
                }

            });
        }
        for (Id id : hiddenParams) {
            typeSubs.put(NodeFactory.makeQualifiedIdName(id),
                         NodeFactory.makeInferenceVarType());
        }

        return new Lambda<Type, Type>() {
            public Type value(Type t) {
                return (Type) t.accept(new NodeUpdateVisitor() {

                    /** Handle type variables */
                    @Override public Type forIdType(IdType n) {
                        if (typeSubs.containsKey(n.getName())) {
                            return typeSubs.get(n.getName());
                        }
                        else { return n; }
                    }

                    /** Handle arguments to opr parameters */
                    @Override public OprArg forOprArg(OprArg n) {
                        if (opSubs.containsKey(n.getName())) {
                            return new OprArg(n.getSpan(), n.isParenthesized(),
                                              opSubs.get(n.getName()));
                        }
                        else { return n; }
                    }

                    /** Handle names in IntExprs */
                    @Override public IntExpr forIntRef(IntRef n) {
                        if (intSubs.containsKey(n.getName())) {
                            return intSubs.get(n.getName());
                        }
                        else { return n; }
                    }

                    /** Handle names in BoolExprs */
                    @Override public BoolExpr forBoolRef(BoolRef n) {
                        if (boolSubs.containsKey(n.getName())) {
                            return boolSubs.get(n.getName());
                        }
                        else { return n; }
                    }

                    /** Handle names in DimExprs */
                    @Override public DimExpr forDimRef(DimRef n) {
                        if (dimSubs.containsKey(n.getName())) {
                            return dimSubs.get(n.getName());
                        }
                        else { return n; }
                    }

                    /** Handle names in UnitExprs */
                    @Override public UnitExpr forUnitRef(UnitRef n) {
                        if (unitSubs.containsKey(n.getName())) {
                            return unitSubs.get(n.getName());
                        }
                        else { return n; }
                    }
                });
            }
        };
    }

    public static boolean containsVariable(Type t, final List<Id> names) {
        return t.accept(new NodeAbstractVisitor<Boolean>() {
            @Override public Boolean forArrowType(ArrowType t) {
                return t.getDomain().accept(this) || t.getRange().accept(this) ||
                    throwsType(t).accept(this);
            }
            @Override public Boolean forBottomType(BottomType t) { return false; }
            @Override public Boolean forIdType(IdType t) {
                return t.getName().getApi().isNone() && names.contains(t.getName().getName());
            }
            @Override public Boolean forInstantiatedType(InstantiatedType t) {
                for (StaticArg arg : t.getArgs()) {
                    if (arg.accept(this)) { return true; }
                }
                return false;
            }
            @Override public Boolean forVoidType(VoidType t) { return false; }
            @Override public Boolean forInferenceVarType(InferenceVarType t) { return false; }
            @Override public Boolean forAndType(AndType t) {
                return t.getFirst().accept(this) || t.getSecond().accept(this);
            }
            @Override public Boolean forOrType(OrType t) {
                return t.getFirst().accept(this) || t.getSecond().accept(this);
            }
            @Override public Boolean forTypeArg(TypeArg t) { return t.getType().accept(this); }
            @Override public Boolean forIntArg(IntArg t) { return false; }
            @Override public Boolean forBoolArg(BoolArg t) { return false; }
            @Override public Boolean forOprArg(OprArg t) { return false; }
            @Override public Boolean forDimArg(DimArg t) { return false; }
            @Override public Boolean forUnitArg(UnitArg t) { return false; }
        });
    }

    public static ArrowType makeArrow(Type domain, Type range, Type throwsT, boolean io) {
        return new ArrowType(domain, range,
                             Option.some(Collections.singletonList(throwsT)), io);
    }

    public static Type throwsType(ArrowType t) {
        return IterUtil.first(Option.unwrap(t.getThrowsClause()));
    }

    public static Iterable<Type> keywordTypes(Iterable<? extends KeywordType> keys) {
        return IterUtil.map(keys, KEYWORD_TO_TYPE);
    }

    private static final Lambda<KeywordType, Type> KEYWORD_TO_TYPE =
        new Lambda<KeywordType, Type>() {
        public Type value(KeywordType k) { return k.getType(); }
    };

    /** Test whether the given tuples have the same arity and matching varargs/keyword entries */
    /*
    public static boolean compatibleTuples(TupleType s, TupleType t) {
        if (s.getElements().size() == t.getElements().size() &&
            s.getVarargs().isSome() == t.getVarargs().isSome() &&
            s.getKeywords().size() == t.getKeywords().size()) {
            for (Pair<KeywordType, KeywordType> keys :
                 IterUtil.zip(s.getKeywords(), t.getKeywords())) {
                if (!keys.first().getName().equals(keys.second().getName())) {
                    return false;
                }
            }
            return true;
        }
        else { return false; }
    }
    */

    /**
     * Figure out the static type of a non-generic function application.
     * @param checker the SubtypeChecker to use for any type comparisons
     * @param fn the type of the function, which can be some AbstractArrowType,
     *           or an intersection of such (in the case of an overloaded
     *           function)
     * @param args the arguments to apply to this function
     * @return the return type of the most applicable arrow type in {@code fn},
     *         or {@code Option.none()} if no arrow type matched the args
     */
    public static Option<Type> applicationType(final SubtypeChecker checker,
                                               final Type fn,
                                               final Iterable<Type> args) {
        return applicationType(checker, fn, NodeFactory.makeArgType(IterUtil.asList(args)));
    }

    public static Option<Type> applicationType(final SubtypeChecker checker,
                                               final Type fn,
                                               final Type arg) {

        // Make sure domain is an ArgType
        final ArgType domain;
        if (arg instanceof ArgType) {
            domain = (ArgType) arg;
        } else if (arg instanceof TupleType) {
            domain = NodeFactory.makeArgType(((TupleType)arg).getElements());
        } else {
            domain = NodeFactory.makeArgType(Collections.singletonList(arg));
        }

        // Turn fn into a list of types (i.e. flatten if an intersection)
        final Iterable<Type> arrows =
            (fn instanceof AndType) ? conjuncts((AndType)fn)
                                    : IterUtil.make(fn);

        // Get a list of the arrow types that match these arguments
        List<ArrowType> matchingArrows = new ArrayList<ArrowType>();
        for (Type arrow : arrows) {

            // Try to form a non-generic ArrowType from this arrow, if it matches the args
            Option<ArrowType> newArrow = arrow.accept(new NodeAbstractVisitor<Option<ArrowType>>() {
                @Override public Option<ArrowType> forArrowType(ArrowType that) {
                    return checker.subtype(domain, that.getDomain())
                        ? some(that)
                        : Option.<ArrowType>none();
                }
                @Override public Option<ArrowType> for_RewriteGenericArrowType(_RewriteGenericArrowType that) {
                    return none(); // TODO - implement
                }
                @Override public Option<ArrowType> defaultCase(Node that) {
                    return none();
                }
            });
            if (newArrow.isSome()) {
                matchingArrows.add(unwrap(newArrow));
            }
        }
        if (matchingArrows.isEmpty()) {
            return none();
        }

        // Find the most applicable arrow type
        ArrowType minType = matchingArrows.get(0);
        for (int i=1; i<matchingArrows.size(); ++i) {
            ArrowType t = matchingArrows.get(i);
            if (checker.subtype(t, minType)) {
                minType = t;
            }
        }
        return some(minType.getRange());
    }


    public static Option<Type> applicationType(SubtypeChecker checker,
                                               Type arrow,
                                               Iterable<Type> args,
                                               Iterable<StaticArg> staticArgs) {
        return Option.<Type>none(); // TODO implement
    }


    /** Get all the conjunct types from a nested AndType. */
    public static Iterable<Type> conjuncts(AndType types) {
        Type left = types.getFirst();
        Type right = types.getSecond();
        return IterUtil.compose(
                (left instanceof AndType) ? conjuncts((AndType)left) : IterUtil.make(left),
                (right instanceof AndType) ? conjuncts((AndType)right) : IterUtil.make(right));
    }

    /** Get all the disjunct types from a nested OrType. */
    public static Iterable<Type> disjuncts(OrType types) {
        Type left = types.getFirst();
        Type right = types.getSecond();
        return IterUtil.compose(
                (left instanceof OrType) ? disjuncts((OrType)left) : IterUtil.make(left),
                (right instanceof OrType) ? disjuncts((OrType)right) : IterUtil.make(right));
    }
}
