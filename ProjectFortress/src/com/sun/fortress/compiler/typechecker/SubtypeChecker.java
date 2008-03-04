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
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.lambda.Lambda;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.interpreter.evaluator.ProgramError;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;
import static com.sun.fortress.nodes_util.NodeUtil.getName;
import static com.sun.fortress.nodes_util.NodeUtil.nameString;
import static com.sun.fortress.compiler.typechecker.Types.*;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public abstract class SubtypeChecker {

    protected final TraitTable _table;
    protected final SubtypeCache _cache;
    protected final SubtypeHistory _emptyHistory;
    protected final StaticParamEnv _staticParamEnv;

    public SubtypeChecker(TraitTable table, StaticParamEnv staticParamEnv) {
        _table = table;
        _cache = new SubtypeCache();
        _emptyHistory = new SubtypeHistory();
        _staticParamEnv = staticParamEnv;
    }

    public static SubtypeChecker make(TraitTable table) {
        return new LeafSubtypeChecker(table);
    }

    public SubtypeChecker extend(List<StaticParam> params,
                                 WhereClause whereClause) {
        return new ConsSubtypeChecker(_table, this,
                                      _staticParamEnv.extend(params, whereClause));
    }

    /**
     * Convert the type to a normal form.
     * A normalized type has the following properties:
     *
     * 1) The ArrowType and MatrixType are desugared into InstantiatedType.
     *
     *    ArrayType ::= Type [ ExtentRange(, ExtentRange)* ]
     *    ArrayType(Type element, Indicies indices)
     *    Indices(List<ExtentRange> extents)
     *    ExtentRange(Option<StaticArg> base, Option<StaticArg> size)
     *    trait Array1[\T, nat b0, nat s0\]
     *    trait Array2[\T, nat b0, nat s0, nat b1, nat s1\]
     *    trait Array3[\T, nat b0, nat s0, nat b1, nat s1, nat b2, nat s2\]
     *
     *    MatrixType ::= Type ^ IntExpr
     *                 | Type ^ ( ExtentRange (BY ExtentRange)* )
     *    MatrixType(Type element, List<ExtentRange> dimensions)
     *    trait Matrix[\T extends Number, nat s0, nat s1\]
     *
     *    InstantiatedType(QualifiedIdName name, List<StaticArg> args)
     *
     */
    public static Type normalize(Type t) {
        if (isArray(t)) {
            ArrayType tt = (ArrayType)t;
            Span span = tt.getSpan();
            TypeArg elem = NodeFactory.makeTypeArg(tt.getElement());
            IntArg zero = NodeFactory.makeIntArgVal("0");
            List<ExtentRange> dims = tt.getIndices().getExtents();
            try {
                if (dims.size() == 1) {
                    ExtentRange first = dims.get(0);
                    QualifiedIdName name =
                        NodeFactory.makeQualifiedIdName(span, "FortressLibrary",
                                                        "Array1");
                    StaticArg base;
                    if (first.getBase().isSome())
                         base = Option.unwrap(first.getBase());
                    else base = zero;
                    return NodeFactory.makeInstantiatedType(span, false, name,
                                                            elem, base,
                                                            Option.unwrap(first.getSize()));
                } else if (dims.size() == 2) {
                    ExtentRange first  = dims.get(0);
                    ExtentRange second = dims.get(1);
                    QualifiedIdName name =
                        NodeFactory.makeQualifiedIdName(span, "FortressLibrary",
                                                    "Array2");
                    StaticArg base1;
                    StaticArg base2;
                    if (first.getBase().isSome())
                         base1 = Option.unwrap(first.getBase());
                    else base1 = zero;
                    if (second.getBase().isSome())
                         base2 = Option.unwrap(first.getBase());
                    else base2 = zero;
                    return NodeFactory.makeInstantiatedType(span, false, name,
                                                            elem, base1,
                                                            Option.unwrap(first.getSize()),
                                                            base2,
                                                            Option.unwrap(second.getSize()));
                } else if (dims.size() == 3) {
                    ExtentRange first  = dims.get(0);
                    ExtentRange second = dims.get(1);
                    ExtentRange third  = dims.get(2);
                    QualifiedIdName name =
                        NodeFactory.makeQualifiedIdName(span, "FortressLibrary",
                                                        "Array3");
                    StaticArg base1;
                    StaticArg base2;
                    StaticArg base3;
                    if (first.getBase().isSome())
                         base1 = Option.unwrap(first.getBase());
                    else base1 = zero;
                    if (second.getBase().isSome())
                         base2 = Option.unwrap(first.getBase());
                    else base2 = zero;
                    if (third.getBase().isSome())
                         base3 = Option.unwrap(first.getBase());
                    else base3 = zero;
                    return NodeFactory.makeInstantiatedType(span, false, name,
                                                            elem, base1,
                                                            Option.unwrap(first.getSize()),
                                                            base2,
                                                            Option.unwrap(second.getSize()),
                                                            base3,
                                                            Option.unwrap(third.getSize()));

                }
                return error("Desugaring " + t + " to InstantiatedType is not " +
                             "yet supported.");
            } catch (Exception x) {
                return error("Desugaring " + t + " to InstantiatedType is not " +
                             "yet supported.");
            }
        } else if (isMatrix(t)) {
            MatrixType tt = (MatrixType)t;
            List<ExtentRange> dims = tt.getDimensions();
            if (dims.size() == 2) {
                ExtentRange first  = dims.get(0);
                ExtentRange second = dims.get(1);
                // Or first.getBase() == second.getBase() == 0
                if (first.getBase().isNone() && second.getBase().isNone() &&
                    first.getSize().isSome() && second.getSize().isSome()) {
                    Span span = tt.getSpan();
                    QualifiedIdName name =
                        NodeFactory.makeQualifiedIdName(span, "FortressLibrary",
                                                        "Matrix");
                    return NodeFactory.makeInstantiatedType(span, false, name,
                                                            NodeFactory.makeTypeArg(tt.getElement()),
                                                            Option.unwrap(first.getSize()),
                                                            Option.unwrap(second.getSize()));
                }
            }
            return error("Desugaring " + t + " to InstantiatedType is not yet " +
                         "supported.");
        } return t;
    }

    private Lambda<Type, Type> makeSubst(List<? extends StaticParam> params,
                                         List<? extends StaticArg> args) {
        if (params.size() != args.size()) {
            return error("Mismatched static parameters and static arguments.");
        }

        final Map<QualifiedIdName, Type> typeSubs = new HashMap<QualifiedIdName, Type>();
        final Map<Op, Op> opSubs = new HashMap<Op, Op>();
        final Map<QualifiedIdName, IntExpr> intSubs = new HashMap<QualifiedIdName, IntExpr>();
        final Map<QualifiedIdName, BoolExpr> boolSubs = new HashMap<QualifiedIdName, BoolExpr>();
        final Map<QualifiedIdName, DimExpr> dimSubs = new HashMap<QualifiedIdName, DimExpr>();
        final Map<QualifiedIdName, Expr> unitSubs = new HashMap<QualifiedIdName, Expr>();
        for (Pair<StaticParam, StaticArg> pair : IterUtil.zip(params, args)) {
            final StaticArg a = pair.second();
            pair.first().accept(new NodeAbstractVisitor_void() {
                @Override public void forSimpleTypeParam(SimpleTypeParam p) {
                    if (isTypeArg(a) || isIdArg(a))
                        typeSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                     ((TypeArg) a).getType());
                    else error("A type parameter is instantiated with a " +
                               "non-type argument.");
                }
                @Override public void forOperatorParam(OperatorParam p) {
                    if (isOprArg(a))
                        opSubs.put(p.getName(), ((OprArg)a).getName());
                    else error("An operator parameter is instantiated with a " +
                               "non-operator argument.");
                }
                @Override public void forIntParam(IntParam p) {
                    if (isIntArg(a))
                        intSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                    ((IntArg)a).getVal());
                    else error("An integer parameter is instantiated with a " +
                               "non-integer argument.");
                }
                @Override public void forNatParam(NatParam p) {
                    if (isIntArg(a))
                        intSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                    ((IntArg)a).getVal());
                    else error("A nat parameter is instantiated with a " +
                               "non-nat argument.");
                }
                @Override public void forBoolParam(BoolParam p) {
                    if (isBoolArg(a))
                        boolSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                     ((BoolArg)a).getBool());
                    else error("A bool parameter is instantiated with a " +
                               "non-bool argument.");
                }
                @Override public void forDimensionParam(DimensionParam p) {
                    if (isDimArg(a))
                        dimSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                    ((DimArg)a).getDim());
                    else error("A dimension parameter is instantiated with a " +
                               "non-dimension argument.");
                }
                @Override public void forUnitParam(UnitParam p) {
                    if (isUnitArg(a))
                        unitSubs.put(NodeFactory.makeQualifiedIdName(p.getName()),
                                     ((UnitArg)a).getUnit());
                    else error("A unit parameter is instantiated with a " +
                               "non-unit argument.");
                }
            });
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
                });
            }
        };
    }

    private boolean isStaticParam(IdType t) {
        QualifiedIdName name = t.getName();
        if (name.getApi().isSome()) return false;
        else { // name.getApi().isNone()
            return _staticParamEnv.binding(name.getName()).isSome();
        }
    }

    private List<TraitType> getExtends(IdType t) {
        List<TraitType> _extends = new ArrayList<TraitType>();
        QualifiedIdName name = t.getName();
        if (name.getApi().isNone()) {
            Option<StaticParam> result = _staticParamEnv.binding(name.getName());
            if (result.isSome()) {
                StaticParam sparam = Option.unwrap(result);
                if (isTypeParam(sparam)) {
                    return ((SimpleTypeParam)sparam).getExtendsClause();
                } else return _extends;
            } else return _extends;
        } else return _extends;
    }

    private boolean isValidIdType(Type t) {
        if (isIdType(t)) {
            TypeConsIndex index = _table.typeCons(((IdType)t).getName());
            return (index instanceof TraitIndex ||
                    index instanceof TypeAliasIndex);
        } else return false;
    }

    private boolean isValidTraitType(Type t) {
        if (t instanceof NamedType) {
            TypeConsIndex index = _table.typeCons(((NamedType)t).getName());
            return (index instanceof TraitIndex ||
                    index instanceof TypeAliasIndex);
        } else return (t instanceof AbbreviatedType);
    }

    private boolean isIdType(Type t) {
        return (t instanceof IdType);
    }
    private boolean isArrow(Type t) {
        return (t instanceof AbstractArrowType);
    }
    private boolean isTuple(Type t) {
        return (t instanceof AbstractTupleType);
    }
    private boolean isInst(Type t) {
        return (t instanceof InstantiatedType);
    }
    private static boolean isArray(Type t) {
        return (t instanceof ArrayType);
    }
    private static boolean isMatrix(Type t) {
        return (t instanceof MatrixType);
    }

    private boolean isIdArg(StaticArg t) {
        return (t instanceof IdArg);
    }
    private boolean isTypeArg(StaticArg t) {
        return (t instanceof TypeArg);
    }
    private boolean isIntArg(StaticArg t) {
        return (t instanceof IntArg);
    }
    private boolean isBoolArg(StaticArg t) {
        return (t instanceof BoolArg);
    }
    private boolean isOprArg(StaticArg t) {
        return (t instanceof OprArg);
    }
    private boolean isDimArg(StaticArg t) {
        return (t instanceof DimArg);
    }
    private boolean isUnitArg(StaticArg t) {
        return (t instanceof UnitArg);
    }

    private boolean isOprParam(StaticParam t) {
        return (t instanceof OperatorParam);
    }
    private boolean isBoolParam(StaticParam t) {
        return (t instanceof BoolParam);
    }
    private boolean isDimParam(StaticParam t) {
        return (t instanceof DimensionParam);
    }
    private boolean isIntParam(StaticParam t) {
        return (t instanceof IntParam);
    }
    private boolean isNatParam(StaticParam t) {
        return (t instanceof NatParam);
    }
    private boolean isTypeParam(StaticParam t) {
        return (t instanceof SimpleTypeParam);
    }
    private boolean isUnitParam(StaticParam t) {
        return (t instanceof UnitParam);
    }

    private boolean sameKindStaticParams(StaticParam s, StaticParam t) {
        return ((isTypeParam(s) && isTypeParam(t)) ||
                (isOprParam(s)  && isOprParam(t))  ||
                (isBoolParam(s) && isBoolParam(t)) ||
                (isIntParam(s)  && isIntParam(t))  ||
                (isNatParam(s)  && isNatParam(t))  ||
                (isDimParam(s)  && isDimParam(t))  ||
                (isUnitParam(s) && isUnitParam(t)));
    }

    private boolean equivalent(StaticParam s, StaticParam t, SubtypeHistory h) {
        if (sameKindStaticParams(s, t)) return (getName(s).equals(getName(t)));
        else return false;
    }

    private boolean equivalentStaticParams(List<StaticParam> s,
                                           List<StaticParam> t,
                                           SubtypeHistory h) {
        int index = 0;
        if (s.size() == t.size()) {
            for (StaticParam p : s) {
                if (!(equivalent(p, t.get(index++), h))) return false;
            }
            return true;
        } else return false;
    }

    private boolean equivalent(StaticArg s, StaticArg t, SubtypeHistory h) {
        if (isIdArg(s) && isIdArg(t)) {
            return nameString(((IdArg)s).getName()).equals(((IdArg)t).getName());
        } else if (isTypeArg(s) && isTypeArg(t)) {
            return equivalent(((TypeArg)s).getType(), ((TypeArg)t).getType(), h);
        } else if (isOprArg(s) && isOprArg(t)) {
            return nameString(((OprArg)s).getName()).equals(((OprArg)t).getName());
        } else {
            return false;
        }
    }

    private boolean equivalentStaticArgs(List<StaticArg> s, List<StaticArg> t,
                                         SubtypeHistory h) {
        int index = 0;
        if (s.size() == t.size()) {
            for (StaticArg a : s) {
                if (!(equivalent(a, t.get(index++), h))) return false;
            }
            return true;
        } else return false;
    }

    private boolean equivalent(Type s, Type t, SubtypeHistory h) {
        return (subtype(s,t,h).booleanValue() &&
                subtype(t,s,h).booleanValue());
    }

    private boolean equivalent(Option<StaticArg> s, Option<StaticArg> t,
                               SubtypeHistory h) {
        if (s.isSome()) {
            if (t.isSome()) {
                return equivalent(Option.unwrap(s), Option.unwrap(t), h);
            } else { // t.isNone()
                return false;
            }
        } else { // s.isNone()
            if (t.isSome()) {
                return false;
            } else { // t.isNone()
                return true;
            }
        }
    }

    private boolean equivalent(ExtentRange s, ExtentRange t, SubtypeHistory h) {
        return (equivalent(s.getBase(), t.getBase(), h) &&
                equivalent(s.getSize(), t.getSize(), h));
    }

    private boolean equivalent(List<ExtentRange> s, List<ExtentRange> t,
                               SubtypeHistory h) {
        int index = 0;
        if (s.size() == t.size()) {
            for (ExtentRange p : s) {
                if (!(equivalent(p, t.get(index++), h))) return false;
            }
            return true;
        } else return false;
    }

    private boolean equivalent(Indices s, Indices t, SubtypeHistory h) {
        return equivalent(s.getExtents(), t.getExtents(), h);
    }

    /**
     * Returns whether s is a subtype of t.
     * Assumes s and t satisfy the followings:
     * 1) The following types are not yet supported:
     *
     *        InferenceVarType(Object id, int index = -1);
     *        ArgType(Option<VarargsType> varargs = Option.<VarargsType>none(),
     *                List<KeywordType> keywords = Collections.<KeywordType>emptyList(),
     *                boolean inArrow = false);
     *        abstract DimExpr();
     *            ExponentType(Type base, IntExpr power);
     *            BaseDim();
     *            DimRef(QualifiedIdName name);
     *            ProductDim(DimExpr multiplier, DimExpr multiplicand);
     *            QuotientDim(DimExpr numerator, DimExpr denominator);
     *            OpDim(DimExpr val, Op op);
     *        abstract DimType(Type type);
     *            TaggedDimType(DimExpr dim,
     *                          Option<Expr> unit = Option.<Expr>none());
     *            TaggedUnitType(Expr unit);
     *        AndType(Type first, Type second);
     *        OrType(Type first, Type second);
     *        FixedPointType(QualifiedIdName name, Type body);
     *
     *    if any of the above types appears in s or t,
     *    the subtype checker may return FALSE.
     *
     * 2) Arrow types are partially supported:
     *    their throws clauses, io-ness, and where clauses are not checked and
     *    the static parameters are checked only for the textual equality.
     *
     * 3) The following static arguments are partially supported:
     *
     *        abstract StaticArg();
     *            IdArg(QualifiedIdName name);
     *            TypeArg(Type type);
     *            OprArg(Op name);
     *
     *    if any of the above static arguments appears in s or t,
     *    the subtype checker checks the textual equality of them.
     *
     *    The following static arguments are not supported:
     *
     *            IntArg(IntExpr val);
     *            BoolArg(BoolExpr bool);
     *            DimArg(DimExpr dim);
     *            UnitArg(Expr unit);
     *
     *    if any of the above static arguments appears in s or t,
     *    the subtype checker may return FALSE.
     *
     * 4) The following static parameters are checked for the textual equality:
     *
     *        abstract StaticParam();
     *            OperatorParam(Op name);
     *            abstract IdStaticParam(Id name);
     *                BoolParam();
     *                DimensionParam();
     *                IntParam();
     *                NatParam();
     *
     *    The following static parameters are checked for the textual equality
     *    of their names ignoring other fields:
     *
     *            SimpleTypeParam(List<TraitType> extendsClause =
     *                                Collections.<TraitType>emptyList(),
     *                            boolean absorbs = false);
     *            UnitParam(Option<Type> dim = Option.<Type>none(),
     *                      boolean absorbs = false);
     *
     * 5) For instantiated types, where clauses are not supported.
     */
    public Boolean subtype(Type s, Type t) {
        return subtype(normalize(s), normalize(t), _emptyHistory);
    }

    private Boolean subtype(final Type s, final Type t, SubtypeHistory history) {
        Option<Boolean> cached = cacheContains(s, t);
        if (cached.isSome()) { return Option.unwrap(cached); }
        else if (history.contains(s, t)) { return FALSE; }
        else {
            final SubtypeHistory h = history.extend(s, t);

            // [S-Any] p; Delta |- s <: Any
            if (t.equals(ANY)) { return TRUE; }
            // [S-Bottom] p; Delta |- Bottom <: t
            if (s.equals(BOTTOM)) { return TRUE; }
            // [S-Refl] p; Delta |- s <: s
            if (s.equals(t)) { return TRUE; }
            // [S-Var]  p; Delta |- ALPHA <: Delta(ALPHA)
            // getExtends(IdType s) returns bounds of s if s is a type parameter
            if (isIdType(s) && !isValidIdType(s)) {
                for (TraitType ty : getExtends((IdType)s)) {
                    if (equivalent(t, ty, h)) return true;
                }
                return false;
            }
            // [S-Arrow] p; Delta |- t1 <: s1
            //           p; Delta |- s2 <: t2
            //          ----------------------------------
            //           p; Delta |- s1 -> s2 <: t1 -> t2
            if (isArrow(s) && isArrow(t)) {
                if (s instanceof ArrowType) {
                    if (t instanceof ArrowType) {
                        ArrowType ss = (ArrowType)s;
                        ArrowType tt = (ArrowType)t;
                        return (subtype(tt.getDomain(), ss.getDomain(), h) &&
                                subtype(ss.getRange(), tt.getRange(), h));
                    } else { // t instanceof _RewriteGenericArrowType
                        return FALSE;
                    }
                } else { // s instanceof _RewriteGenericArrowType
                    if (t instanceof ArrowType) {
                        return FALSE;
                    } else { // t instanceof _RewriteGenericArrowType
                        _RewriteGenericArrowType ss = (_RewriteGenericArrowType)s;
                        _RewriteGenericArrowType tt = (_RewriteGenericArrowType)t;
                        return (subtype(tt.getDomain(), ss.getDomain(), h) &&
                                subtype(ss.getRange(), tt.getRange(), h) &&
                                equivalentStaticParams(ss.getStaticParams(),
                                                       tt.getStaticParams(), h));
 }
                }
            }
            // [S-Tuple] p; Delta |- si <: ti
            //           1 <= i <= |(s, ...)| = |(t, ...)|
            //          -----------------------------------
            //           p; Delta |- (s, ...) <: (t, ...)
            if (isTuple(s) && isTuple(t)) {
                if (s instanceof ArgType || t instanceof ArgType) {
                    return FALSE;
                } else { // s instanceof TupleType && s instanceof TupleType
                    TupleType ss = (TupleType)s;
                    TupleType tt = (TupleType)t;
                    List<Type> types = tt.getElements();
                    if (ss.getElements().size() != types.size()) {
                        return FALSE;
                    } else { // ss.getElements().size()==types.size()
                        int index = 0;
                        for (Type ty : ss.getElements()) {
                            if (!subtype(ty, types.get(index++), h))
                                return FALSE;
                        }
                        return TRUE;
                    }
                }
            }
            // [S-Tapp] trait T[\X...\] extends {T'[\t...\]...} _ \in p
            //          1 <= i <= |T'[\t...\]...|
            //         ----------------------------------------------------
            //          p; Delta |- T[\s...\] <: [X...|->s...]T'[\t...\]_i
            if (isValidTraitType(s) && isValidTraitType(t)) {
                // equivalent named types without static arguments
                if (isValidIdType(s) && isValidIdType(t) &&
                    nameString(((IdType)s).getName()).equals(nameString(((IdType)t).getName())))
                    return TRUE;
                if (isInst(s) && isInst(t)) {
                    InstantiatedType ss = (InstantiatedType)s;
                    InstantiatedType tt = (InstantiatedType)t;
                    if (nameString(ss.getName()).equals(nameString(tt.getName())))
                        return equivalentStaticArgs(ss.getArgs(),tt.getArgs(),h);
                }
                TypeConsIndex index = _table.typeCons(((NamedType)s).getName());
                if (index instanceof TraitIndex) {
                    TraitIndex traitIndex = (TraitIndex)index;
                    if (isIdType(s)) {
                        for (TraitTypeWhere _sup : traitIndex.extendsTypes()) {
                            TraitType sup = _sup.getType();
                            if (subtype(sup, t, h)) return TRUE;
                        }
                        return FALSE;
                    } else { // (isInst(s))
                        try {
                            Lambda<Type, Type> subst =
                                makeSubst(traitIndex.staticParameters(),
                                          ((InstantiatedType)s).getArgs());
                            for (TraitTypeWhere _sup : traitIndex.extendsTypes()) {
                                TraitType sup = _sup.getType();
                                if (subtype(subst.value(sup), t, h))
                                    return TRUE;
                            }
                            return FALSE;
                        } catch (ProgramError ex) {
                            return FALSE;
                        }
                    }
                } else if (index instanceof TypeAliasIndex) {
                    return FALSE;
                } else {
                    throw new IllegalStateException("Unexpected index type");
                }
            }
            // [NS-Any]    p; Delta |- Any </: t  (where t =/= Any)
            if (s.equals(ANY) && !t.equals(ANY)) { return FALSE; }
            // [NS-Bottom] p; Delta |- s </: Bottom  (where s =/= Bottom)
            if (t.equals(BOTTOM) && !s.equals(BOTTOM)) { return FALSE; }
            // [NS-Void]   p; Delta |- s </: ()  (where s =/= Bottom /\ s =/= ())
            if (t.equals(VOID) && !s.equals(VOID)) { return FALSE; }
            // [NS-Arrow]  p; Delta |- s </: t1 -> t2  (where s =/= Bottom /\
            //                                               s =/= s1 -> s2)
            if (isArrow(t) && !isArrow(s)) { return FALSE; }
            // [NS-Tuple]  p; Delta |- s </: (t, ...) (where s =/= Bottom /\
            //                                               s =/= (s, ...) )
            if (isTuple(t) && !isTuple(s)) { return FALSE; }
            // [NS-Tapp]   p; Delta |- s </: T'[\...\] (where s =/= Bottom /\
            //                                                s =/= T[\...\] )
            if (isValidTraitType(t) && !isValidTraitType(s)) { return FALSE; }
            return FALSE;
        }
    }

    class SubtypeHistory {
        private final Relation<Type, Type> _entries;
        public SubtypeHistory() {
            _entries = new HashRelation<Type, Type>(false, false);
        }
        private SubtypeHistory(Relation<Type, Type> entries) {
            _entries = entries;
        }
        public int size() { return _entries.size(); }
        public boolean contains(Type s, Type t) {
            return _entries.contains(s, t);
        }
        public SubtypeHistory extend(Type s, Type t) {
            Relation<Type, Type> newEntries = new HashRelation<Type, Type>();
            newEntries.addAll(_entries);
            newEntries.add(s, t);
            return new SubtypeHistory(newEntries);
        }
    }

    protected abstract Option<Boolean> cacheContains(Type s, Type t);

    protected abstract void cachePut(Type s, Type t, Boolean r);

    protected static class SubtypeCache {
        HashMap<Pair<Type,Type>,Boolean> subtypeCache =
            new HashMap<Pair<Type,Type>,Boolean>();

        public void put(Type s, Type t, Boolean r) {
            Pair<Type, Type> types = new Pair(s, t);
            if (!subtypeCache.containsKey(types)) {
                subtypeCache.put(types, r);
            }
        }

        public Option<Boolean> contains(Type s, Type t) {
            Pair<Type, Type> types = new Pair(s, t);
            if (subtypeCache.containsKey(types))
                return Option.some(subtypeCache.get(types));
            else return Option.<Boolean>none();
        }
    }
}
