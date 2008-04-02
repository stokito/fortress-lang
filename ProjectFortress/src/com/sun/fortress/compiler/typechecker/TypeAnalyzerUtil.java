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
import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

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
                @Override public void forSimpleTypeParam(SimpleTypeParam p) {
                    typeSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                 ((TypeArg) a).getType());
                }
                @Override public void forOperatorParam(OperatorParam p) {
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
                @Override public void forDimensionParam(DimensionParam p) {
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


}
