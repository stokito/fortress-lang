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
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import static com.sun.fortress.exceptions.StaticError.errorMsg;
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
            final IdOrOp name = pair.first().getName();
            pair.first().getKind().accept(new NodeAbstractVisitor_void() {
                    @Override public void forKindType(KindType k) {
                        typeSubs.put((Id)name,
                                     ((TypeArg) a).getTypeArg());
                    }
                    @Override public void forKindInt(KindInt k) {
                        intSubs.put((Id)name,
                                    ((IntArg) a).getIntVal());
                    }
                    @Override public void forKindNat(KindNat k) {
                        intSubs.put((Id)name,
                                    ((IntArg) a).getIntVal());
                    }
                    @Override public void forKindBool(KindBool k) {
                        boolSubs.put((Id)name,
                                     ((BoolArg) a).getBoolArg());
                    }
                    @Override public void forKindDim(KindDim k) {
                        dimSubs.put((Id)name,
                                    ((DimArg) a).getDimArg());
                    }
                    @Override public void forKindUnit(KindUnit k) {
                        unitSubs.put((Id)name,
                                     ((UnitArg) a).getUnitArg());
                    }
                    @Override public void forKindOp(KindOp p) {
                        opSubs.put((Op)name, (Op) ((OpArg) a).getName().getOriginalName());
                    }
                });
        }
        for (Id id : hiddenParams) {
            typeSubs.put(id, NodeFactory.make_InferenceVarType(NodeUtil.getSpan(id)));
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
                            return NodeFactory.makeOpArg(NodeUtil.getSpan(n),
                                                         ExprFactory.makeOpRef(opSubs.get(n.getName())));
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

    /**
     * Does thie given type t contain any of the type variable names given by names?
     * @param type Given type
     * @param names type variable names to look for
     */
    public static boolean containsVariable(Type type, final List<Id> names) {
        return type.accept(new NodeAbstractVisitor<Boolean>() {
            private Boolean recurOnList(List<? extends Type> ts) {
                for (Type t : ts) {
                    if (t.accept(this)) { return true; }
                }
                return false;
            }
            private Boolean recurOnKeywords(List<KeywordType> ks) {
                for (KeywordType k : ks) {
                    if (k.getKeywordType().accept(this)) { return true; }
                }
                return false;
            }
            @Override public Boolean forArrowType(ArrowType t) {
                Type d = t.getDomain();
                Effect e = t.getEffect();
                if ( d instanceof TupleType ) {
                    TupleType _d = (TupleType)d;
                    return recurOnList(_d.getElements()) ||
                        (NodeUtil.hasVarargs(_d) &&
                         _d.getVarargs().unwrap().accept(this)) ||
                        recurOnKeywords(_d.getKeywords()) ||
                        t.getRange().accept(this) ||
                        recurOnList(t.getEffect().getThrowsClause().unwrap(Collections.<BaseType>emptyList()));
                } else
                    return d.accept(this) ||
                        t.getRange().accept(this) ||
                        recurOnList(t.getEffect().getThrowsClause().unwrap(Collections.<BaseType>emptyList()));
            }
            @Override public Boolean forTupleType(TupleType t) {
                if ( ! NodeUtil.hasVarargs(t) )
                    return recurOnList(t.getElements());
                else
                    return recurOnList(t.getElements()) || t.getVarargs().unwrap().accept(this);
            }
            @Override public Boolean forAnyType(AnyType t) { return false; }
            @Override public Boolean forBottomType(BottomType t) { return false; }
            @Override public Boolean forVarType(VarType t) {
                return t.getName().getApiName().isNone() && names.contains(t.getName());
            }
            @Override public Boolean forTraitType(TraitType t) {
                for (StaticArg arg : t.getArgs()) {
                    if (arg.accept(this)) { return true; }
                }
                return false;
            }
            @Override public Boolean for_InferenceVarType(_InferenceVarType t) { return false; }
            @Override public Boolean forBoundType(BoundType t) {
                return recurOnList(t.getElements());
            }
            @Override public Boolean forTypeArg(TypeArg t) { return t.getTypeArg().accept(this); }
            @Override public Boolean forIntArg(IntArg t) { return false; }
            @Override public Boolean forBoolArg(BoolArg t) { return false; }
            @Override public Boolean forOpArg(OpArg t) { return false; }
            @Override public Boolean forDimArg(DimArg t) { return false; }
            @Override public Boolean forUnitArg(UnitArg t) { return false; }
        });
    }

}
