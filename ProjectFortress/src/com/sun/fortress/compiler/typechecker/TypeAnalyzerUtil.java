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
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import static com.sun.fortress.compiler.typechecker.ConstraintFormula.SimpleFormula;

public class TypeAnalyzerUtil {

    private static final Type BOTTOM = new BottomType();
    private static final Option<List<Type>> THROWS_BOTTOM =
        Option.some(Collections.singletonList(BOTTOM));
    private static final List<InferenceVarType> emptyList =
        new ArrayList<InferenceVarType>();

    /**
     * Convert the type to a normal form.
     * A normalized type has the following properties:
     * <ul>
     * <li>The throws clause of all arrow types is a singleton list.
     * </ul>
     */
    public static Type normalize(Type t) {
        return (Type) t.accept(new NodeUpdateVisitor() {

            public Node forArrowTypeOnly(ArrowType t, Type newDomain,
                                         Type newRange,
                                         Option<List<Type>> newThrows) {
                // fix newThrows so that it is a singleton list
                if (newThrows.isNone()) { newThrows = THROWS_BOTTOM; }
                else {
                    List<Type> throwsList = Option.unwrap(newThrows);
                    if (throwsList.isEmpty()) { newThrows = THROWS_BOTTOM; }
                    else if (throwsList.size() > 1) {
                        Type union = null;
                        for (Type elt : throwsList) {
                            if (union == null) { union = elt; }
                            else { union = new OrType(union, elt); }
                        }
                        newThrows = Option.some(Collections.singletonList(union));
                    }
                }
                if (t.getDomain() == newDomain && t.getRange() == newRange &&
                    t.getThrowsClause() == newThrows)
                     { return t; }
                else { return NodeFactory.makeArrowType(t, newDomain, newRange,
                                                        newThrows);
                }
            }

        });
    }

    private static List<InferenceVarType> collectVs(Option<VarargsType> varargs) {
        if (varargs.isNone()) return new ArrayList<InferenceVarType>();
        else return collectV(Option.unwrap(varargs).getType());
    }

    private static List<InferenceVarType> collectVs(List<KeywordType> keywords) {
        List<InferenceVarType> list = new ArrayList<InferenceVarType>();
        for (KeywordType kwd : keywords) {
            list.addAll(collectV(kwd.getType()));
        }
        return list;
    }

    private static List<InferenceVarType> collectVs(Iterable<? extends Type> ts) {
        List<InferenceVarType> list = new ArrayList<InferenceVarType>();
        for (Type type : ts) {
            list.addAll(collectV(type));
        }
        return list;
    }

    private static List<InferenceVarType> collectVs(Type... ts) {
        List<InferenceVarType> list = new ArrayList<InferenceVarType>();
        for (Type type : ts) {
            list.addAll(collectV(type));
        }
        return list;
    }

    private static List<InferenceVarType> collectV(Type type) {
        return (List<InferenceVarType>)
               type.accept(new NodeAbstractVisitor<List<InferenceVarType>>() {
            @Override
            public List<InferenceVarType> forType(Type t) { return emptyList; }
            @Override
            public List<InferenceVarType> forExponentType(ExponentType t) {
                return collectV(t.getBase());
            }
            @Override
            public List<InferenceVarType> forProductDim(ProductDim t) {
                List<InferenceVarType> vars = collectV(t.getMultiplier());
                vars.addAll(collectV(t.getMultiplicand()));
                return vars;
            }
            @Override
            public List<InferenceVarType> forQuotientDim(QuotientDim t) {
                List<InferenceVarType> vars = collectV(t.getNumerator());
                vars.addAll(collectV(t.getDenominator()));
                return vars;
            }
            @Override
            public List<InferenceVarType> forExponentDim(ExponentDim t) {
                return collectV(t.getBase());
            }
            @Override
            public List<InferenceVarType> forOpDim(OpDim t) {
                return collectV(t.getVal());
            }
            @Override
            public List<InferenceVarType> forArrowType(ArrowType t) {
                List<InferenceVarType> vars = collectV(t.getDomain());
                vars.addAll(collectV(t.getRange()));
                vars.addAll(collectV(Option.unwrap(t.getThrowsClause()).get(0)));
                return vars;
            }
            @Override
            public List<InferenceVarType> for_RewriteGenericArrowType(_RewriteGenericArrowType t) {
                // ???????????????????????????
                return emptyList;
            }
            @Override
            public List<InferenceVarType> forArrayType(ArrayType t) {
                // ???????????????????????????
                return emptyList;
            }
            @Override
            public List<InferenceVarType> forInferenceVarType(InferenceVarType t) {
                List<InferenceVarType> list = new ArrayList<InferenceVarType>();
                list.add(t);
                return list;
            }
            @Override
            public List<InferenceVarType> forMatrixType(MatrixType t) {
                // ???????????????????????????
                return emptyList;
            }
            @Override
            public List<InferenceVarType> forInstantiatedType(InstantiatedType t) {
                return collectVs(t.getArgs());
            }
            @Override
            public List<InferenceVarType> forTupleType(TupleType t) {
                return collectVs(t.getElements());
            }
            @Override
            public List<InferenceVarType> forArgType(ArgType t) {
                List<InferenceVarType> vars = collectVs(t.getElements());
                vars.addAll(collectVs(t.getVarargs()));
                vars.addAll(collectVs(t.getKeywords()));
                return vars;
            }
            @Override
            public List<InferenceVarType> forAndType(AndType t) {
                List<InferenceVarType> vars = collectV(t.getFirst());
                vars.addAll(collectV(t.getSecond()));
                return vars;
            }
            @Override
            public List<InferenceVarType> forOrType(OrType t) {
                List<InferenceVarType> vars = collectV(t.getFirst());
                vars.addAll(collectV(t.getSecond()));
                return vars;
            }
            @Override
            public List<InferenceVarType> forFixedPointType(FixedPointType t) {
                return collectV(t.getBody());
            }
            @Override
            public List<InferenceVarType> forTaggedDimType(TaggedDimType t) {
                List<InferenceVarType> vars = collectV(t.getType());
                vars.addAll(collectV(t.getDim()));
                return vars;
            }
            @Override
            public List<InferenceVarType> forTaggedUnitType(TaggedUnitType t) {
                return collectV(t.getType());
            }
            @Override
            public List<InferenceVarType> forTypeArg(TypeArg t) {
                return collectV(t.getType());
            }
            @Override
            public List<InferenceVarType> forDimArg(DimArg t) {
                return collectV(t.getDim());
            }
        });
    }

    private static Map<InferenceVarType,Integer> mapVs(List<InferenceVarType> vs) {
        Map<InferenceVarType,Integer> map =
            new HashMap<InferenceVarType,Integer>();
        int index = 0;
        for (InferenceVarType v : vs) {
            map.put(v,index++);
        }
        return map;
    }

    private static Map<InferenceVarType,Integer> mapVs(Type... types) {
        return mapVs(collectVs(types));
    }

    private static Map<InferenceVarType,Integer> mapVs(Iterable<? extends Type> ts) {
        return mapVs(collectVs(ts));
    }

    private static Type canon(Type t, Map<InferenceVarType,Integer> map) {
        if (t instanceof DimExpr) {
            return canon((DimExpr)t, map);
        } else if (t instanceof AbstractArrowType) {
            return canon((AbstractArrowType)t, map);
        } else if (t instanceof NonArrowType) {
            return canon((NonArrowType)t, map);
        } else { // if (t instanceof StaticArg)
            return canon((StaticArg)t, map);
        }
    }

    private static DimExpr canon(DimExpr t,
                                 Map<InferenceVarType,Integer> map) {
        if (t instanceof ExponentType) {
            ExponentType ty = (ExponentType)t;
            return NodeFactory.makeExponentType(ty, canon(ty.getBase(),map));
        } else if (t instanceof ProductDim) {
            ProductDim ty = (ProductDim)t;
            return NodeFactory.makeProductDim(ty,
                                              (DimExpr)canon(ty.getMultiplier(),map),
                                              (DimExpr)canon(ty.getMultiplicand(),map));
        } else if (t instanceof QuotientDim) {
            QuotientDim ty = (QuotientDim)t;
            return NodeFactory.makeQuotientDim(ty,
                                               (DimExpr)canon(ty.getNumerator(),map),
                                               (DimExpr)canon(ty.getDenominator(),map));
        } else if (t instanceof ExponentDim) {
            ExponentDim ty = (ExponentDim)t;
            return NodeFactory.makeExponentDim(ty,
                                               (DimExpr)canon(ty.getBase(),map));
        } else if (t instanceof OpDim) {
            OpDim ty = (OpDim)t;
            return NodeFactory.makeOpDim(ty, (DimExpr)canon(ty.getVal(),map));
        } else {
            return t;
        }
    }

    private static AbstractArrowType canon(AbstractArrowType t,
                                           Map<InferenceVarType,Integer> map) {
        if (t instanceof ArrowType) {
            ArrowType ty = (ArrowType)t;
            List<Type> newThrows = new ArrayList<Type>();
            newThrows.add(canon(Option.unwrap(ty.getThrowsClause()).get(0),map));
            return NodeFactory.makeArrowType(ty, canon(ty.getDomain(),map),
                                             canon(ty.getRange(),map),
                                             Option.some(newThrows));
        } else { // (t instanceof _RewriteGenericArrowType)
            // ???????????????????????????
            return t;
        }
    }

    private static NonArrowType canon(NonArrowType t,
                                      Map<InferenceVarType,Integer> map) {
        if (t instanceof ArrayType) {
            // ???????????????????????????
            return t;
        } else if (t instanceof InferenceVarType) {
            return NodeFactory.makeCanonicalizedInferenceVarType(map.get((InferenceVarType)t).intValue());
        } else if (t instanceof MatrixType) {
            // ???????????????????????????
            return t;
        } else if (t instanceof InstantiatedType) {
            InstantiatedType ty = (InstantiatedType)t;
            List<StaticArg> args = new ArrayList<StaticArg>();
            for (StaticArg a : ty.getArgs()) {
                args.add((StaticArg)canon(a, map));
            }
            return NodeFactory.makeInstantiatedType(ty, args);
        } else if (t instanceof TupleType) {
            TupleType ty = (TupleType)t;
            List<Type> tys = new ArrayList<Type>();
            for (Type type : ty.getElements()) {
                tys.add(canon(type, map));
            }
            return NodeFactory.makeTupleType(ty, tys);
        } else if (t instanceof ArgType) {
            ArgType ty = (ArgType)t;
            List<Type> tys = new ArrayList<Type>();
            for (Type type : ty.getElements()) {
                tys.add(canon(type, map));
            }
            Option<VarargsType> varargs = ty.getVarargs();
            if (varargs.isNone()) varargs = Option.<VarargsType>none();
            else varargs = Option.some(canon(Option.unwrap(varargs),map));
            List<KeywordType> keywords = new ArrayList<KeywordType>();
            for (KeywordType kwd : ty.getKeywords()) {
                keywords.add(canon(kwd, map));
            }
            return NodeFactory.makeArgType(ty, tys, varargs, keywords);
        } else if (t instanceof AndType) {
            AndType ty = (AndType)t;
            return NodeFactory.makeAndType(ty, canon(ty.getFirst(),map),
                                           canon(ty.getSecond(),map));
        } else if (t instanceof OrType) {
            OrType ty = (OrType)t;
            return NodeFactory.makeOrType(ty, canon(ty.getFirst(),map),
                                          canon(ty.getSecond(),map));
        } else if (t instanceof FixedPointType) {
            FixedPointType ty = (FixedPointType)t;
            return NodeFactory.makeFixedPointType(ty, canon(ty.getBody(),map));
        } else if (t instanceof TaggedDimType) {
            TaggedDimType ty = (TaggedDimType)t;
            return NodeFactory.makeTaggedDimType(ty, canon(ty.getType(),map),
                                                 (DimExpr)canon(ty.getDim(),map));
        } else if (t instanceof TaggedUnitType) {
            TaggedUnitType ty = (TaggedUnitType)t;
            return NodeFactory.makeTaggedUnitType(ty, canon(ty.getType(),map));
        } else {
            return t;
        }
    }

    private static StaticArg canon(StaticArg t,
                                   Map<InferenceVarType,Integer> map) {
        if (t instanceof TypeArg) {
            TypeArg ty = (TypeArg) t;
            return NodeFactory.makeTypeArg(ty, canon(ty.getType(),map));
        } else if (t instanceof DimArg) {
            DimArg ty = (DimArg) t;
            return NodeFactory.makeDimArg(ty, (DimExpr)canon(ty.getDim(),map));
        } else {
            return t;
        }
    }

    private static VarargsType canon(VarargsType t,
                                     Map<InferenceVarType,Integer> map) {
        return NodeFactory.makeVarargsType(t, canon(t.getType(),map));
    }
    private static KeywordType canon(KeywordType t,
                                     Map<InferenceVarType,Integer> map) {
        return NodeFactory.makeKeywordType(t, canon(t.getType(),map));
    }

    public static Map<InferenceVarType,Integer> mapVs(Type t, Type s) {
        List<InferenceVarType> list = collectV(t);
        list.addAll(collectV(s));
        return mapVs(list);
    }

    /**
     * Convert the type to a canonical form.
     * A canonicalized type has the following properties:
     * <ul>
     * <li>Inference variables in the type are numbered: the first inference
     * variable is converted to #0, the second to #1, etc.
     * </ul>
     * Assumes type is normalized.
     */
    public static Pair<Pair<Type,Type>, Map<InferenceVarType,Integer>>
        canonicalize(Type t, Type s) {
        Map<InferenceVarType,Integer> map = mapVs(t, s);
        return new Pair(new Pair(canon(t, map), canon(s, map)), map);
    }

    private static Map<InferenceVarType,Type> canon(Map<InferenceVarType,Type> bounds,
                                                    Map<InferenceVarType,Integer> map) {
        Map<InferenceVarType,Type> result = new HashMap<InferenceVarType,Type>();
        for (InferenceVarType t : bounds.keySet()) {
            result.put((InferenceVarType)canon(t,map), canon(bounds.get(t),map));
        }
        return result;
    }

    public static ConstraintFormula canonicalize(ConstraintFormula c,
                                                 Map<InferenceVarType,Integer> map) {
        if (c instanceof SimpleFormula) {
            Map<InferenceVarType,Type> upperBounds = ((SimpleFormula)c).getUpper();
            Map<InferenceVarType,Type> lowerBounds = ((SimpleFormula)c).getLower();
            int index = map.size();
            for (InferenceVarType t : CollectUtil.union(upperBounds.keySet(),
                                                        lowerBounds.keySet())) {
                if (!map.keySet().contains(t))
                    map.put(t, index++);
            }
            return new SimpleFormula(canon(upperBounds, map),
                                     canon(lowerBounds, map));
        } else { // (c instanceof TRUE || c instanceof FALSE)
            return c;
        }
    }

    public static Type unCanonicalize(Type t,
                                      Map<InferenceVarType,Integer> map) {
        Map<Integer,InferenceVarType> reverseMap = new HashMap<Integer,InferenceVarType>();
        for (InferenceVarType v : map.keySet()) {
            reverseMap.put(map.get(v), v);
        }
        return unCanon(t, reverseMap);
    }

    public static ConstraintFormula unCanonicalize(ConstraintFormula c,
                                                   Map<InferenceVarType,Integer> map) {
        if (c instanceof SimpleFormula) {
            Map<InferenceVarType,Type> orgUpper = ((SimpleFormula)c).getUpper();
            Map<InferenceVarType,Type> orgLower = ((SimpleFormula)c).getLower();
            Map<InferenceVarType,Type> newUpper = new HashMap<InferenceVarType,Type>();
            Map<InferenceVarType,Type> newLower = new HashMap<InferenceVarType,Type>();
            Map<Integer,InferenceVarType> reverseMap = new HashMap<Integer,InferenceVarType>();
            for (InferenceVarType v : map.keySet()) {
                reverseMap.put(map.get(v), v);
            }
            for (InferenceVarType v : orgUpper.keySet()) {
                newUpper.put((InferenceVarType)unCanon(v, reverseMap),
                             unCanon(orgUpper.get(v), reverseMap));
            }
            for (InferenceVarType v : orgLower.keySet()) {
                newLower.put((InferenceVarType)unCanon(v, reverseMap),
                             unCanon(orgLower.get(v), reverseMap));
            }
            return new SimpleFormula(newUpper, newLower);
        } else {
            return c;
        }
    }

    private static Type unCanon(Type t, Map<Integer,InferenceVarType> map) {
        if (t instanceof DimExpr) {
            return unCanon((DimExpr)t, map);
        } else if (t instanceof AbstractArrowType) {
            return unCanon((AbstractArrowType)t, map);
        } else if (t instanceof NonArrowType) {
            return unCanon((NonArrowType)t, map);
        } else { // if (t instanceof StaticArg)
            return unCanon((StaticArg)t, map);
        }
    }

    private static DimExpr unCanon(DimExpr t,
                                   Map<Integer,InferenceVarType> map) {
        if (t instanceof ExponentType) {
            ExponentType ty = (ExponentType)t;
            return NodeFactory.makeExponentType(ty, unCanon(ty.getBase(),map));
        } else if (t instanceof ProductDim) {
            ProductDim ty = (ProductDim)t;
            return NodeFactory.makeProductDim(ty,
                                              (DimExpr)unCanon(ty.getMultiplier(),map),
                                              (DimExpr)unCanon(ty.getMultiplicand(),map));
        } else if (t instanceof QuotientDim) {
            QuotientDim ty = (QuotientDim)t;
            return NodeFactory.makeQuotientDim(ty,
                                               (DimExpr)unCanon(ty.getNumerator(),map),
                                               (DimExpr)unCanon(ty.getDenominator(),map));
        } else if (t instanceof ExponentDim) {
            ExponentDim ty = (ExponentDim)t;
            return NodeFactory.makeExponentDim(ty,
                                               (DimExpr)unCanon(ty.getBase(),map));
        } else if (t instanceof OpDim) {
            OpDim ty = (OpDim)t;
            return NodeFactory.makeOpDim(ty, (DimExpr)unCanon(ty.getVal(),map));
        } else {
            return t;
        }
    }

    private static AbstractArrowType unCanon(AbstractArrowType t,
                                             Map<Integer,InferenceVarType> map) {
        if (t instanceof ArrowType) {
            ArrowType ty = (ArrowType)t;
            List<Type> newThrows = new ArrayList<Type>();
            newThrows.add(unCanon(Option.unwrap(ty.getThrowsClause()).get(0),map));
            return NodeFactory.makeArrowType(ty, unCanon(ty.getDomain(),map),
                                             unCanon(ty.getRange(),map),
                                             Option.some(newThrows));
        } else { // (t instanceof _RewriteGenericArrowType)
            // ???????????????????????????
            return t;
        }
    }

    private static NonArrowType unCanon(NonArrowType t,
                                        Map<Integer,InferenceVarType> map) {
        if (t instanceof ArrayType) {
            // ???????????????????????????
            return t;
        } else if (t instanceof InferenceVarType) {
            InferenceVarType ty = (InferenceVarType)t;
            int index = ty.getIndex();
            if (index == -1) return NodeFactory.makeInferenceVarType();
            else             return map.get(index);
        } else if (t instanceof MatrixType) {
            // ???????????????????????????
            return t;
        } else if (t instanceof InstantiatedType) {
            InstantiatedType ty = (InstantiatedType)t;
            List<StaticArg> args = new ArrayList<StaticArg>();
            for (StaticArg a : ty.getArgs()) {
                args.add((StaticArg)unCanon(a, map));
            }
            return NodeFactory.makeInstantiatedType(ty, args);
        } else if (t instanceof TupleType) {
            TupleType ty = (TupleType)t;
            List<Type> tys = new ArrayList<Type>();
            for (Type type : ty.getElements()) {
                tys.add(unCanon(type, map));
            }
            return NodeFactory.makeTupleType(ty, tys);
        } else if (t instanceof ArgType) {
            ArgType ty = (ArgType)t;
            List<Type> tys = new ArrayList<Type>();
            for (Type type : ty.getElements()) {
                tys.add(unCanon(type, map));
            }
            Option<VarargsType> varargs = ty.getVarargs();
            if (varargs.isNone()) varargs = Option.<VarargsType>none();
            else varargs = Option.some(unCanon(Option.unwrap(varargs),map));
            List<KeywordType> keywords = new ArrayList<KeywordType>();
            for (KeywordType kwd : ty.getKeywords()) {
                keywords.add(unCanon(kwd, map));
            }
            return NodeFactory.makeArgType(ty, tys, varargs, keywords);
        } else if (t instanceof AndType) {
            AndType ty = (AndType)t;
            return NodeFactory.makeAndType(ty, unCanon(ty.getFirst(),map),
                                           unCanon(ty.getSecond(),map));
        } else if (t instanceof OrType) {
            OrType ty = (OrType)t;
            return NodeFactory.makeOrType(ty, unCanon(ty.getFirst(),map),
                                          unCanon(ty.getSecond(),map));
        } else if (t instanceof FixedPointType) {
            FixedPointType ty = (FixedPointType)t;
            return NodeFactory.makeFixedPointType(ty, unCanon(ty.getBody(),map));
        } else if (t instanceof TaggedDimType) {
            TaggedDimType ty = (TaggedDimType)t;
            return NodeFactory.makeTaggedDimType(ty, unCanon(ty.getType(),map),
                                                 (DimExpr)unCanon(ty.getDim(),map));
        } else if (t instanceof TaggedUnitType) {
            TaggedUnitType ty = (TaggedUnitType)t;
            return NodeFactory.makeTaggedUnitType(ty, unCanon(ty.getType(),map));
        } else {
            return t;
        }
    }

    private static StaticArg unCanon(StaticArg t,
                                     Map<Integer,InferenceVarType> map) {
        if (t instanceof TypeArg) {
            TypeArg ty = (TypeArg) t;
            return NodeFactory.makeTypeArg(ty, unCanon(ty.getType(),map));
        } else if (t instanceof DimArg) {
            DimArg ty = (DimArg) t;
            return NodeFactory.makeDimArg(ty, (DimExpr)unCanon(ty.getDim(),map));
        } else {
            return t;
        }
    }

    private static VarargsType unCanon(VarargsType t,
                                       Map<Integer,InferenceVarType> map) {
        return NodeFactory.makeVarargsType(t, unCanon(t.getType(),map));
    }
    private static KeywordType unCanon(KeywordType t,
                                       Map<Integer,InferenceVarType> map) {
        return NodeFactory.makeKeywordType(t, unCanon(t.getType(),map));
    }
}
