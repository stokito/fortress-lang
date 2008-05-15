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

public class TypeAnalyzerUtil {

    public static Lambda<Type, Type> makeSubstitution(Iterable<? extends StaticParam> params,
                                                      Iterable<? extends StaticArg> args) {
        return makeSubstitution(params, args, IterUtil.<Id>empty());
    }

    /** Assumes param/arg lists have the same length and corresponding elements are compatible. */
    public static Lambda<Type, Type> makeSubstitution(Iterable<? extends StaticParam> params,
                                                      Iterable<? extends StaticArg> args,
                                                      Iterable<? extends Id> hiddenParams) {
        final Map<Id, Type> typeSubs = new HashMap<Id, Type>();
        final Map<Op, Op> opSubs = new HashMap<Op, Op>();
        final Map<Id, IntExpr> intSubs = new HashMap<Id, IntExpr>();
        final Map<Id, BoolExpr> boolSubs = new HashMap<Id, BoolExpr>();
        final Map<Id, DimExpr> dimSubs = new HashMap<Id, DimExpr>();
        final Map<Id, UnitExpr> unitSubs = new HashMap<Id, UnitExpr>();
        for (Pair<StaticParam, StaticArg> pair : IterUtil.zip(params, args)) {
            final StaticArg a = pair.second();
            pair.first().accept(new NodeAbstractVisitor_void() {
                @Override public void forTypeParam(TypeParam p) {
                    typeSubs.put(p.getName(),
                                 ((TypeArg) a).getType());
                }
                @Override public void forOpParam(OpParam p) {
                    opSubs.put(p.getName(), ((OpArg) a).getName());
                }
                @Override public void forIntParam(IntParam p) {
                    intSubs.put(p.getName(),
                                ((IntArg) a).getVal());
                }
                @Override public void forNatParam(NatParam p) {
                    intSubs.put(p.getName(),
                                ((IntArg) a).getVal());
                }
                @Override public void forBoolParam(BoolParam p) {
                    boolSubs.put(p.getName(),
                                 ((BoolArg) a).getBool());
                }
                @Override public void forDimParam(DimParam p) {
                    dimSubs.put(p.getName(),
                                ((DimArg) a).getDim());
                }
                @Override public void forUnitParam(UnitParam p) {
                    unitSubs.put(p.getName(),
                                 ((UnitArg) a).getUnit());
                }

            });
        }
        for (Id id : hiddenParams) {
            typeSubs.put(id, NodeFactory.makeInferenceVarType());
        }

        return new Lambda<Type, Type>() {
            public Type value(Type t) {
                return (Type) t.accept(new NodeUpdateVisitor() {

                    /** Handle type variables */
                    @Override public Type forVarType(VarType n) {
                        if (typeSubs.containsKey(n.getName())) {
                            return typeSubs.get(n.getName());
                        }
                        else { return n; }
                    }

                    /** Handle arguments to opr parameters */
                    @Override public OpArg forOpArg(OpArg n) {
                        if (opSubs.containsKey(n.getName())) {
                            return new OpArg(n.getSpan(), opSubs.get(n.getName()));
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
            private Boolean recurOnList(List<? extends Type> ts) {
                for (Type t : ts) {
                    if (t.accept(this)) { return true; }
                }
                return false;
            }
            private Boolean recurOnKeywords(List<KeywordType> ks) {
                for (KeywordType k : ks) {
                    if (k.getType().accept(this)) { return true; }
                }
                return false;
            }
            @Override public Boolean forArrowType(ArrowType t) {
                Domain d = t.getDomain();
                Effect e = t.getEffect();
                return recurOnList(d.getArgs()) ||
                    (d.getVarargs().isSome() && d.getVarargs().unwrap().accept(this)) ||
                    recurOnKeywords(d.getKeywords()) ||
                    t.getRange().accept(this) ||
                    recurOnList(t.getEffect().getThrowsClause().unwrap(Collections.<BaseType>emptyList()));
            }
            @Override public Boolean forTupleType(TupleType t) {
                return recurOnList(t.getElements());
            }
            @Override public Boolean forVarargTupleType(VarargTupleType t) {
                return recurOnList(t.getElements()) || t.getVarargs().accept(this);
            }
            @Override public Boolean forAnyType(AnyType t) { return false; }
            @Override public Boolean forBottomType(BottomType t) { return false; }
            @Override public Boolean forVarType(VarType t) {
                return t.getName().getApi().isNone() && names.contains(t.getName());
            }
            @Override public Boolean forTraitType(TraitType t) {
                for (StaticArg arg : t.getArgs()) {
                    if (arg.accept(this)) { return true; }
                }
                return false;
            }
            @Override public Boolean forVoidType(VoidType t) { return false; }
            @Override public Boolean forInferenceVarType(InferenceVarType t) { return false; }
            @Override public Boolean forBoundType(BoundType t) {
                return recurOnList(t.getElements());
            }
            @Override public Boolean forTypeArg(TypeArg t) { return t.getType().accept(this); }
            @Override public Boolean forIntArg(IntArg t) { return false; }
            @Override public Boolean forBoolArg(BoolArg t) { return false; }
            @Override public Boolean forOpArg(OpArg t) { return false; }
            @Override public Boolean forDimArg(DimArg t) { return false; }
            @Override public Boolean forUnitArg(UnitArg t) { return false; }
        });
    }

}
