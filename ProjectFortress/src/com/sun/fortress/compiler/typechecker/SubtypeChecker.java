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
import static com.sun.fortress.compiler.Types.*;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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
     * 1) The ArrowType and MatrixType are desugared into TraitType.
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
     *    TraitType(Id name, List<StaticArg> args)
     *
     */
    public static Type normalize(Type t) {
        if (isArray(t)) {
            ArrayType tt = (ArrayType)t;
            Span span = tt.getSpan();
            TypeArg elem = NodeFactory.makeTypeArg(tt.getType());
            IntArg zero = NodeFactory.makeIntArgVal("0");
            List<ExtentRange> dims = tt.getIndices().getExtents();
            try {
                if (dims.size() == 1) {
                    ExtentRange first = dims.get(0);
                    Id name = NodeFactory.makeId(span, "FortressLibrary", "Array1");
                    StaticArg base;
                    if (first.getBase().isSome())
                         base = first.getBase().unwrap();
                    else base = zero;
                    if (first.getSize().isSome())
                        return NodeFactory.makeTraitType(span, false, name,
                                                         elem, base,
                                                         first.getSize().unwrap());
                    else return bug(t, "Missing size.");
                } else if (dims.size() == 2) {
                    ExtentRange first  = dims.get(0);
                    ExtentRange second = dims.get(1);
                    Id name = NodeFactory.makeId(span, "FortressLibrary", "Array2");
                    StaticArg base1;
                    StaticArg base2;
                    if (first.getBase().isSome())
                         base1 = first.getBase().unwrap();
                    else base1 = zero;
                    if (second.getBase().isSome())
                         base2 = second.getBase().unwrap();
                    else base2 = zero;
                    if (first.getSize().isSome()) {
                        if (second.getSize().isSome()) {
                            return NodeFactory.makeTraitType(span, false, name,
                                                             elem, base1,
                                                             first.getSize().unwrap(),
                                                             base2,
                                                             second.getSize().unwrap());
                        } else return bug(second, "Missing size.");
                    } else return bug(first, "Missing size.");
                } else if (dims.size() == 3) {
                    ExtentRange first  = dims.get(0);
                    ExtentRange second = dims.get(1);
                    ExtentRange third  = dims.get(2);
                    Id name = NodeFactory.makeId(span, "FortressLibrary", "Array3");
                    StaticArg base1;
                    StaticArg base2;
                    StaticArg base3;
                    if (first.getBase().isSome())
                         base1 = first.getBase().unwrap();
                    else base1 = zero;
                    if (second.getBase().isSome())
                         base2 = second.getBase().unwrap();
                    else base2 = zero;
                    if (third.getBase().isSome())
                         base3 = third.getBase().unwrap();
                    else base3 = zero;
                    if (first.getSize().isSome()) {
                        if (second.getSize().isSome()) {
                            if (third.getSize().isSome()) {
                                return NodeFactory.makeTraitType(span, false, name,
                                                                 elem, base1,
                                                                 first.getSize().unwrap(),
                                                                 base2,
                                                                 second.getSize().unwrap(),
                                                                 base3,
                                                                 third.getSize().unwrap());
                            } else return bug(third, "Missing size.");
                        } else return bug(second, "Missing size.");
                    } else return bug(first, "Missing size.");
                }
                return error("Desugaring " + t + " to TraitType is not " +
                             "yet supported.");
            } catch (Exception x) {
                return error("Desugaring " + t + " to TraitType is not " +
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
                    Id name = NodeFactory.makeId(span, "FortressLibrary", "Matrix");
                    return NodeFactory.makeTraitType(span, false, name,
                                                     NodeFactory.makeTypeArg(tt.getType()),
                                                     first.getSize().unwrap(),
                                                     second.getSize().unwrap());
                }
            }
            return error("Desugaring " + t + " to TraitType is not yet " +
                         "supported.");
        } return t;
    }

    private Lambda<Type, Type> makeSubst(List<? extends StaticParam> params,
                                         List<? extends StaticArg> args) {
        if (params.size() != args.size()) {
            return error("Mismatched static parameters and static arguments.");
        }

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
                    if (isTypeArg(a))
                        typeSubs.put(p.getName(),
                                     ((TypeArg) a).getType());
                    else error("A type parameter is instantiated with a " +
                               "non-type argument.");
                }
                @Override public void forOpParam(OpParam p) {
                    if (isOpArg(a))
                        opSubs.put(p.getName(), ((OpArg)a).getName());
                    else error("An operator parameter is instantiated with a " +
                               "non-operator argument.");
                }
                @Override public void forIntParam(IntParam p) {
                    if (isIntArg(a))
                        intSubs.put(p.getName(),
                                    ((IntArg)a).getVal());
                    else error("An integer parameter is instantiated with a " +
                               "non-integer argument.");
                }
                @Override public void forNatParam(NatParam p) {
                    if (isIntArg(a))
                        intSubs.put(p.getName(),
                                    ((IntArg)a).getVal());
                    else error("A nat parameter is instantiated with a " +
                               "non-nat argument.");
                }
                @Override public void forBoolParam(BoolParam p) {
                    if (isBoolArg(a))
                        boolSubs.put(p.getName(),
                                     ((BoolArg)a).getBool());
                    else error("A bool parameter is instantiated with a " +
                               "non-bool argument.");
                }
                @Override public void forDimParam(DimParam p) {
                    if (isDimArg(a))
                        dimSubs.put(p.getName(),
                                    ((DimArg)a).getDim());
                    else error("A dimension parameter is instantiated with a " +
                               "non-dimension argument.");
                }
                @Override public void forUnitParam(UnitParam p) {
                    if (isUnitArg(a))
                        unitSubs.put(p.getName(),
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

    private boolean isStaticParam(VarType t) {
        Id name = t.getName();
        if (name.getApi().isSome()) return false;
        else { // name.getApi().isNone()
            return _staticParamEnv.binding(name).isSome();
        }
    }

    private List<BaseType> getExtends(VarType t) {
        List<BaseType> _extends = new ArrayList<BaseType>();
        Id name = t.getName();
        if (name.getApi().isNone()) {
            Option<StaticParam> result = _staticParamEnv.binding(name);
            if (result.isSome()) {
                StaticParam sparam = result.unwrap();
                if (isTypeParam(sparam)) {
                    return ((TypeParam)sparam).getExtendsClause();
                } else return _extends;
            } else return _extends;
        } else return _extends;
    }

    private boolean isValidVarType(Type t) {
        if (isVarType(t)) {
            TypeConsIndex index = _table.typeCons(((VarType)t).getName());
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

    private boolean isVarType(Type t) {
        return (t instanceof VarType);
    }
    private boolean isArrow(Type t) {
        return (t instanceof AbstractArrowType);
    }
    private boolean isTuple(Type t) {
        return (t instanceof AbstractTupleType);
    }
    private boolean isInst(Type t) {
        return (t instanceof TraitType);
    }
    private static boolean isArray(Type t) {
        return (t instanceof ArrayType);
    }
    private static boolean isMatrix(Type t) {
        return (t instanceof MatrixType);
    }
    private boolean isUnion(Type t) {
        return (t instanceof UnionType);
    }
    private boolean isIntersection(Type t) {
        return (t instanceof IntersectionType);
    }
    private boolean isVarargTupleType(Type t) {
        return (t instanceof VarargTupleType);
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
    private boolean isOpArg(StaticArg t) {
        return (t instanceof OpArg);
    }
    private boolean isDimArg(StaticArg t) {
        return (t instanceof DimArg);
    }
    private boolean isUnitArg(StaticArg t) {
        return (t instanceof UnitArg);
    }

    private boolean isOpParam(StaticParam t) {
        return (t instanceof OpParam);
    }
    private boolean isBoolParam(StaticParam t) {
        return (t instanceof BoolParam);
    }
    private boolean isDimParam(StaticParam t) {
        return (t instanceof DimParam);
    }
    private boolean isIntParam(StaticParam t) {
        return (t instanceof IntParam);
    }
    private boolean isNatParam(StaticParam t) {
        return (t instanceof NatParam);
    }
    private boolean isTypeParam(StaticParam t) {
        return (t instanceof TypeParam);
    }
    private boolean isUnitParam(StaticParam t) {
        return (t instanceof UnitParam);
    }

    private boolean sameKindStaticParams(StaticParam s, StaticParam t) {
        return ((isTypeParam(s) && isTypeParam(t)) ||
                (isOpParam(s)  && isOpParam(t))  ||
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
        if (isTypeArg(s) && isTypeArg(t)) {
            return equivalent(((TypeArg)s).getType(), ((TypeArg)t).getType(), h);
        } else if (isOpArg(s) && isOpArg(t)) {
            return nameString(((OpArg)s).getName()).equals(((OpArg)t).getName());
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
                return equivalent(s.unwrap(), t.unwrap(), h);
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

    private List<Type> pad(List<Type> types, Type padType, int length) {
        assert(length >= types.size());
        return IterUtil.asList(IterUtil.compose(types, IterUtil.copy(padType, length-types.size())));
    }
    
    public Type join(Type... ts) {
        return join(IterUtil.make(ts));
    }
    
    public Type join(Iterable<? extends Type> ts) {
        // eliminate duplicates but preserve order
        // (a better implementation would eliminate subtypes)
        Set<Type> elts = new LinkedHashSet<Type>();
        for (Type t : ts) { elts.add(t); }
        switch (elts.size()) {
            case 0: return BOTTOM;
            case 1: return IterUtil.first(elts);
            default: return new UnionType(IterUtil.asList(elts));
        }
    }

    public Type meet(Type... ts) {
        return meet(IterUtil.make(ts));
    }
    
    public Type meet(Iterable<? extends Type> ts) {
        // eliminate duplicates but preserve order
        // (a better implementation would eliminate supertypes)
        Set<Type> elts = new LinkedHashSet<Type>();
        for (Type t : ts) { elts.add(t); }
        switch (elts.size()) {
            case 0: return ANY;
            case 1: return IterUtil.first(elts);
            default: return new IntersectionType(IterUtil.asList(elts));
        }
    }

    /**
     * Returns whether s is a subtype of t.
     * Assumes s and t satisfy the followings:
     * 1) The following types are not yet supported:
     *
     *        InferenceVarType(Object id, int index = -1);
     *        VarargTupleType(Type varargs);
     *        abstract DimExpr();
     *            ExponentType(Type base, IntExpr power);
     *            BaseDim();
     *            DimRef(Id name);
     *            ProductDim(DimExpr multiplier, DimExpr multiplicand);
     *            QuotientDim(DimExpr numerator, DimExpr denominator);
     *            OpDim(DimExpr val, Op op);
     *        abstract DimType(Type type);
     *            TaggedDimType(DimExpr dim,
     *                          Option<Expr> unit = Option.<Expr>none());
     *            TaggedUnitType(Expr unit);
     *        abstract BoundType(List<Type> elements);
     *            IntersectionType();
     *            UnionType();
     *        FixedPointType(Id name, Type body);
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
     *            TypeArg(Type type);
     *            OpArg(Op name);
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
     *            OpParam(Op name);
     *            abstract IdStaticParam(Id name);
     *                BoolParam();
     *                DimParam();
     *                IntParam();
     *                NatParam();
     *
     *    The following static parameters are checked for the textual equality
     *    of their names ignoring other fields:
     *
     *            TypeParam(List<BaseType> extendsClause =
     *                                Collections.<BaseType>emptyList(),
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
        if (cached.isSome()) { return cached.unwrap(); }
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
            // getExtends(VarType s) returns bounds of s if s is a type parameter
            if (isVarType(s) && !isValidVarType(s)) {
                for (BaseType ty : getExtends((VarType)s)) {
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
                        return (subdomain(tt.getDomain(), ss.getDomain(), h) &&
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
                        return (subdomain(tt.getDomain(), ss.getDomain(), h) &&
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
                if (s instanceof VarargTupleType && t instanceof VarargTupleType) {
                    VarargTupleType ss = (VarargTupleType)s;
                    VarargTupleType tt = (VarargTupleType)t;
                    List<Type> stypes = ss.getElements();
                    List<Type> ttypes = tt.getElements();
                    int padLength = Math.max(stypes.size(), ttypes.size());
                    List<Type> spadded = pad(stypes, ss.getVarargs(), padLength);
                    List<Type> tpadded = pad(ttypes, tt.getVarargs(), padLength);
                    for (Pair<Type, Type> p : IterUtil.zip(spadded, tpadded)) {
                        if (!subtype(p.first(), p.second(), history)) {
                            return FALSE;
                        }
                    }
                    // Check that varargs are subtypes
                    if (!subtype(ss.getVarargs(), tt.getVarargs())) {
                        return FALSE;
                    }
                    return TRUE;
                } else if (s instanceof VarargTupleType || t instanceof VarargTupleType) {
                    // only one is a VarargTupleType
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
                if (isValidVarType(s) && isValidVarType(t) &&
                    nameString(((VarType)s).getName()).equals(nameString(((VarType)t).getName())))
                    return TRUE;
                if (isInst(s) && isInst(t)) {
                    TraitType ss = (TraitType)s;
                    TraitType tt = (TraitType)t;
                    if (nameString(ss.getName()).equals(nameString(tt.getName())))
                        return equivalentStaticArgs(ss.getArgs(),tt.getArgs(),h);
                }
                TypeConsIndex index = _table.typeCons(((NamedType)s).getName());
                if (index instanceof TraitIndex) {
                    TraitIndex traitIndex = (TraitIndex)index;
                    if (isVarType(s)) {
                        for (TraitTypeWhere _sup : traitIndex.extendsTypes()) {
                            BaseType sup = _sup.getType();
                            if (subtype(sup, t, h)) return TRUE;
                        }
                        return FALSE;
                    } else { // (isInst(s))
                        try {
                            Lambda<Type, Type> subst =
                                makeSubst(traitIndex.staticParameters(),
                                          ((TraitType)s).getArgs());
                            for (TraitTypeWhere _sup : traitIndex.extendsTypes()) {
                                BaseType sup = _sup.getType();
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
            // [NS-Union1] p; Delta |- s1 <: t
            //             -------------------------
            //             p; Delta |- s1 OR s2 <: t
            // [NS-Union2] p; Delta |- s2 <: t
            //             -------------------------
            //             p; Delta |- s1 OR s2 <: t
            if (isUnion(s)) {
                boolean result = false;
                for (Type sElt : ((UnionType) s).getElements()) {
                    result |= subtype(sElt, t, h);
                    if (result) { break; }
                }
                return result;
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
    
    private Boolean subdomain(Domain s, Domain t, SubtypeHistory history) {
        if (subtype(stripKeywords(s), stripKeywords(t), history)) {
            Map<Id, Type> sMap = extractKeywords(s);
            Map<Id, Type> tMap = extractKeywords(t);
            if (tMap.keySet().containsAll(sMap.keySet())) {
                boolean result = true;
                for (Map.Entry<Id, Type> entry : sMap.entrySet()) {
                    Type tType = tMap.get(entry.getKey());
                    result &= subtype(entry.getValue(), tType, history);
                    if (!result) { break; }
                }
                return result;
            }
        }
        return false;
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
            Pair<Type, Type> types = Pair.make(s, t);
            if (!subtypeCache.containsKey(types)) {
                subtypeCache.put(types, r);
            }
        }

        public Option<Boolean> contains(Type s, Type t) {
            Pair<Type, Type> types = Pair.make(s, t);
            if (subtypeCache.containsKey(types))
                return Option.some(subtypeCache.get(types));
            else return Option.none();
        }
    }
}
