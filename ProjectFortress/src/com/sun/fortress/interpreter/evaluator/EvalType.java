/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.Bool;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeMatrix;
import com.sun.fortress.interpreter.evaluator.types.FTypeNat;
import com.sun.fortress.interpreter.evaluator.types.FTypeOpr;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.types.SymbolicNat;
import com.sun.fortress.interpreter.evaluator.types.TypeFixedDimIndices;
import com.sun.fortress.interpreter.evaluator.types.TypeRange;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class EvalType extends NodeAbstractVisitor<FType> {

    BetterEnv env;
    private EvalIndices ___evalIndices;
    private synchronized EvalIndices evalIndices() {
        if (___evalIndices == null)
            ___evalIndices = new EvalIndices(this);
        return ___evalIndices;
    }

    public FType evalType(Type t) {
        return t.accept(this);
    }

    public FType evalType(DottedId t) {
        return forDottedId(t);
    }

    public static FType getFTypeFromOption(Option<Type> t, final BetterEnv e) {
        return t.apply(new OptionVisitor<Type, FType>() {
            public FType forSome(Type t) { return getFType(t, e); }
            public FType forNone() { return FTypeDynamic.ONLY; }
        });
    }

    public  FType getFTypeFromOption(Option<Type> t) {
        return t.apply(new OptionVisitor<Type, FType>() {
            public FType forSome(Type t) { return getFType(t); }
            public FType forNone() { return FTypeDynamic.ONLY; }
        });
    }

    public static FType getFTypeFromList(List<Type> l, BetterEnv e) {
        return getFTypeFromList(l, new EvalType(e));
    }

    public static FType getFTypeFromList(List<Type> l,  EvalType et) {
        if (l.size() == 1) {
            return l.get(0).accept(et);
        }
        return FTypeTuple.make(getFTypeListFromNonEmptyList(l, et));
    }

    public static List<FType> getFTypeListFromList(List<? extends Type> l, BetterEnv e) {
        EvalType et = new EvalType(e);
        return et.getFTypeListFromList(l);
    }

    public List<FType> getFTypeListFromOptionList(Option<List<TraitType>> ol) {
        return ol.apply(new OptionVisitor<List<TraitType>, List<FType>>() {
            public List<FType> forSome(List<TraitType> l) {
                return getFTypeListFromList(l);
            }
            public List<FType> forNone() { return Collections.emptyList(); }
        });
    }


    public List<FType> getFTypeListFromList(List<? extends Type> l) {
        if (l == null || l.size() == 1 && (l.get(0) instanceof VoidType)) {
            // Flatten out voids.
            // Should this be mutable?
            return Collections.<FType>emptyList();
        }

        return getFTypeListFromNonEmptyList(l, this);
    }

    private static List<FType> getFTypeListFromNonEmptyList(List<? extends Type> l, EvalType et) {
        ArrayList<FType> a = new ArrayList<FType>(l.size());
        for (Type t : l) a.add(t.accept(et));
        return a;
    }

    public static FType getFType(Type t, BetterEnv e) {
        return t.accept(new EvalType(e));
    }

    public  FType getFType(Type t) {
        return t.accept(this);
    }

    public FType getFType(VarargsType t) {
        return t.accept(this);
    }

    public static List<Parameter> paramsToParameters(BetterEnv env,
            List<Param> params) {
        if (params.size() == 0) {
            // There must be some way to get the generic parameter attached.
            return Collections.<Parameter>emptyList();
        }
        return paramsToParameters(new EvalType(env), params);
    }

    public static List<Parameter> paramsToParameters(EvalType e,
            List<Param> params) {
        if (params.size() == 0) {
            // There must be some way to get the generic parameter attached.
            return Collections.<Parameter>emptyList();
        }
        int i = 0;
        List<Parameter> fparams = new ArrayList<Parameter>(params.size());
        for (Param in_p : params) {
            Id id = in_p.getId();
            String pname = id.getName();
            FType ptype;
            if (in_p instanceof NormalParam) {
                Option<Type> type = ((NormalParam)in_p).getType();
                ptype = e.getFTypeFromOption(type);
            }
            else { // in_p instanceof VarargsParam
                ptype = e.getFType(((VarargsParam)in_p).getVarargsType());
            }
            // TOP?  or Dynamic?
            if (ptype instanceof FTypeDynamic)
                ptype = FTypeTop.ONLY;
            Parameter fp = new Parameter(pname, ptype, NodeUtil.isMutable(in_p));
            fparams.add(i++, fp);
        }
        return fparams;
    }

    static void guardedPutType(String name, FType type, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putType(name, type);
        } catch (FortressError pe) {
            pe.setWithin(containing);
            pe.setWhere(where);
            throw pe;
        }
    }

    static void guardedPutNat(String name, Number nat, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putNat(name, nat);
        } catch (FortressError pe) {
            pe.setWithin(containing);
            pe.setWhere(where);
            throw pe;
        }
    }

    static void guardedPutBool(String name, Boolean b, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putBool(name, b);
        } catch (FortressError pe) {
            pe.setWithin(containing);
            pe.setWhere(where);
            throw pe;
        }
    }

    /**
     * @param params Parameters that need binding
     * @param args   Instance arguments to use
     * @param clenv  Environment into which bindings should be put
     * @param within CompilationUnit node where the instantiation occurs (for error reporting)
     * @param what   What generic thing is getting instantiated (for error reporting)
     * @throws ProgramError
     */
    public static void bindGenericParameters(List<StaticParam> params,
                                             List<FType> args,
                                             Environment clenv,
                                             HasAt within,
                                             HasAt what) throws ProgramError {
        for (int i = 0; i < args.size(); i++) {
            FType a = args.get(i);
            StaticParam p = params.get(i);

            if (a == BottomType.ONLY || a == null) {
                guardedPutType(NodeUtil.getName(p), BottomType.ONLY, what, clenv);
                // TODO need to undefine the nats and bools
                // This means that our choice to use the Java types Boolean etc was wrong,
                // because they lack lattice structure.
            } else if (p instanceof NatParam) {
                if (a instanceof IntNat) {
                    long l = ((IntNat)a).getValue();
                    if (l < 0) {
                        // Move this check into the param-specific binding code.
                        error(p, errorMsg("Negative nats are unNATural: " + l));
                    }

                    guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else if (a instanceof SymbolicNat) {
                    // guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    error(within, clenv,
                          errorMsg("Expected Nat, got ", a, " for param ", p,
                                   " instantiating ", what));
                }
            } else if (p instanceof IntParam) {
                if (a instanceof IntNat) {
                    guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else if (a instanceof SymbolicNat) {
                    // guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    error(within, clenv,
                          errorMsg("Expected Int, got ", a, " for param ", p,
                                   " instantiating ", what));
                }
            } else if (p instanceof BoolParam) {
                if (a instanceof Bool) {
                    guardedPutBool(NodeUtil.getName(p), ((Bool)a).getBooleanValue(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    error(within, clenv,
                          errorMsg("Expected Bool, got ", a, " for param ", p,
                                   " instantiating ", what));
                }
            } else if (p instanceof SimpleTypeParam) {
                // There's probably some inappropriate ones.
                if (a instanceof FTypeNat) {
                    error(within, clenv,
                          errorMsg("When instantiating ", what,
                                   "Got nat ", a,
                                   " instead of type for param ", p));
                } else if (a instanceof FTypeOpr) {
                    error(within, clenv,
                          errorMsg("When instantiating ", what,
                                   "Got opr ", a,
                                   " instead of type for param ", p));
                } else {
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                }

            } else if (p instanceof OperatorParam) {
                if (a instanceof FTypeOpr) {
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    error(within, clenv,
                          errorMsg("Expected Opr, got ", a, " for param ", p,
                                   " instantiating ", what));
                }
            } else if (p instanceof DimensionParam) {
                NI.nyi("Generic, generic in dimension"); // TODO dimension params
            } else {
                error(within, clenv,
                      errorMsg("Unexpected generic parameter ", p));
            }
         }
    }

    public ArrayList<FType> forStaticArgList(List<? extends Type> args) {
        ArrayList<FType> argValues = new ArrayList<FType>(args.size());

        for (int i = 0; i < args.size(); i++) {
            Type a = args.get(i);
            FType t = a.accept(this);
            argValues.add(t);
        }
        return argValues;
    }

    public EvalType(BetterEnv _env) {
        env = _env;
    }

    public FType forBaseOprStaticArg(BaseOprStaticArg b) {
        return new FTypeOpr(NodeUtil.getName(b.getFnName()));
    }

    public FType forVoidType(VoidType v) { return FTypeVoid.ONLY; }

    public FType forVarargsType(VarargsType rt) {
        return FTypeRest.make(rt.getType().accept(this));
    }

    public FType forBaseBoolStaticArg(BaseBoolStaticArg b) {
        if (b.isBool()) return new Bool("true", FBool.TRUE);
        else return new Bool("false", FBool.FALSE);
    }

    public FType forIdType(IdType i) {
        try {
            FType result = env.getType(i.getDottedId());
            return result;
        } catch (FortressError p) {
            p.setWhere(i);
            p.setWithin(env);
            throw p;
        }

    }

    @Override
    public FType forArrowType(ArrowType at) {
        // TODO Keywords, defaults, still TBI
        return FTypeArrow.make(at.getDomain().accept(this), at.getRange().accept(this));
    }

    @Override
    public FType forTupleType(TupleType tt) {
        // TODO Unusual tuple types
        return getFTypeFromList(tt.getElements(), this);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forBaseNatStaticArg(com.sun.fortress.interpreter.nodes.BaseNatStaticArg)
     */
    @Override
    public FType forBaseNatStaticArg(BaseNatStaticArg x) {
        return IntNat.make(x.getValue());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forOperatorParam(com.sun.fortress.interpreter.nodes.OperatorParam)
     */
    @Override
    public FType forOperatorParam(OperatorParam x) {
        // TODO Auto-generated method stub
        return super.forOperatorParam(x);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forProductType(com.sun.fortress.interpreter.nodes.ProductType)
     */
    @Override
    public FType forProductType(ProductType x) {
        return IntNat.make(longify(x.getMultiplier()) *
                           longify(x.getMultiplicand()));
        }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forProductStaticArg(com.sun.fortress.interpreter.nodes.ProductStaticArg)
     */
    @Override
    public FType forProductStaticArg(ProductStaticArg x) {
        return IntNat.make(longify(x.getMultiplier()) *
                           longify(x.getMultiplicand()));
        }

    private long nonEmpty(List<? extends Type> value) {
        if (value.size() == 0)
            error("Empty operands to nat arithmetic");
        return longify(value.get(0));
    }

    private long longify(Type type) {
        FType t = type.accept(this);
        if (!(t instanceof IntNat)) {
            error(type,
                  errorMsg("StaticArg ", type, " evaluated to ", t,
                           " (instead of IntNat)"));
        }
        return ((IntNat) t).getValue();
    }

    private long longify(DimUnitExpr type) {
        FType t = type.accept(this);
        if (!(t instanceof IntNat)) {
            error(type,
                  errorMsg("StaticArg ", type, " evaluated to ", t,
                           " (instead of IntNat)"));
        }
        return ((IntNat) t).getValue();
    }

    public FType forBaseDimId(BaseDimId id) {
        throw new InterpreterBug(id,
                                 errorMsg("Evaluating BaseDimId ", id,
                                          " is not yet implemented."));
    }

    public FType forBaseUnitId(BaseUnitId id) {
        throw new InterpreterBug(id,
                                 errorMsg("Evaluating BaseUnitId ", id,
                                          " is not yet implemented."));
    }

    public FType forDimUnitId(DimUnitId i) {
        try {
            FType result = env.getType(i.getDottedId());
            return result;
        } catch (FortressError p) {
            p.setWhere(i);
            p.setWithin(env);
            throw p;
        }
    }

    public FType forProductDimUnit(ProductDimUnit du) {
        return IntNat.make(longify(du.getMultiplier()) *
                           longify(du.getMultiplicand()));
    }

    public FType forQuotientDimUnit(QuotientDimUnit du) {
        throw new InterpreterBug(du,
                                 errorMsg("Evaluating QuotientDimUnit ",
                                          du, " is not yet implemented."));
    }

    public FType forChangeDimUnit(ChangeDimUnit du) {
        throw new InterpreterBug(du,
                                 errorMsg("Evaluating OpDimUnit ",
                                          du, " is not yet implemented."));
    }

    public FType forInversionDimUnit(InversionDimUnit du) {
        throw new InterpreterBug(du,
                                 errorMsg("Evaluating InversionDimUnit ",
                                          du, " is not yet implemented."));
    }

    public FType forExponentDimUnit(ExponentDimUnit du) {
        throw new InterpreterBug(du,
                                 errorMsg("Evaluating ExponentDimunit ",
                                          du, " is not yet implemented."));
    }

    public FType forOpDimUnit(OpDimUnit du) {
        throw new InterpreterBug(du,
                                 errorMsg("Evaluating OpDimUnit ",
                                          du, " is not yet implemented."));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSumStaticArg(com.sun.fortress.interpreter.nodes.SumStaticArg)
     */
    @Override
    public FType forSumStaticArg(SumStaticArg x) {
        return IntNat.make(longify(x.getLeft()) + longify(x.getRight()));
        }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forMinusStaticArg(com.sun.fortress.interpreter.nodes.MinusStaticArg)
     */
    @Override
    public FType forMinusStaticArg(MinusStaticArg x) {
        return IntNat.make(longify(x.getLeft()) - longify(x.getRight()));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTypeArg(com.sun.fortress.interpreter.nodes.StaticArg)
     */
    @Override
    public FType forStaticArg(StaticArg x) {
        // TODO Auto-generated method stub
        return super.forStaticArg(x);
    }

    public FType forTypeArg(TypeArg x) {
        try {
            return x.getType().accept(this);
        }
        catch (FortressError pe) {
            pe.setWhere(x);
            throw pe;
        }
    }

    public FType forDottedId(DottedId d) {
        try {
            FType result = env.getType(d);
            return result;
        } catch (FortressError p) {
            p.setWhere(d);
            p.setWithin(env);
            throw p;
        }

    }

    public FType forInstantiatedType(InstantiatedType x) {
       FType ft1 = forDottedId(x.getDottedId());
        if (ft1 instanceof  FTypeGeneric) {
            FTypeGeneric ftg = (FTypeGeneric) ft1;
            return ftg.typeApply(x.getArgs(), env, x);
        } else {
            throw new ProgramError(x, env,
                                   errorMsg("Expected generic type, got ", ft1,
                                            " instead"));
        }
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forArrayType(com.sun.fortress.interpreter.nodes.IndexedType)
     */
    @Override
    public FType forArrayType(ArrayType x) {
        FType elt_type = x.getElement().accept(this);
        Indices indices = x.getIndices();

        TypeFixedDimIndices f_indices = (TypeFixedDimIndices) indices.accept(evalIndices());
        List<TypeRange> ltr = f_indices.getRanges();
        return Glue.instantiateGenericType(env, "Array" + ltr.size(), elt_type, ltr, x);
    }

    @Override
    public FType forMatrixType(MatrixType x) {
        FType elt_type = x.getElement().accept(this);
        List<ExtentRange> dimensions = x.getDimensions();
        List<TypeRange> typeRanges = new ArrayList<TypeRange>();
        for (ExtentRange extent : dimensions) {
            typeRanges.add(extentRangeToTypeRange(extent));
        }
        return new FTypeMatrix(elt_type, typeRanges);
    }

    TypeRange extentRangeToTypeRange(ExtentRange extent) {

        Option<? extends Type> b = extent.getBase();
        Option<? extends Type> s = extent.getSize();
        FTypeNat natB, natS;
        if (b.isSome()) {
            FType bt = Option.unwrap(b).accept(this);
            if (bt instanceof IntNat || bt instanceof SymbolicNat) {
                natB = (FTypeNat)bt;
            } else {
                throw new ProgramError(Option.unwrap(b),
                                       errorMsg(extent,env,"Bad base ",
                                                Option.unwrap(b),"=",
                                                bt.getClass().getName(), " ",
                                                bt));
            }
        } else {
            natB = IntNat.make(0);
        }

        if (s.isSome()) {
            FType st = Option.unwrap(s).accept(this);
            if (st instanceof IntNat || st instanceof SymbolicNat) {
                natS = (FTypeNat)st;
            } else {
                throw new ProgramError(Option.unwrap(s),
                                       errorMsg(extent,env,"Bad size ",
                                                Option.unwrap(s), "=",
                                                st.getClass().getName(), " ",
                                                st));
            }
        } else {
            natS = IntNat.make(0);
        }
        return new TypeRange(natB, natS);
    }


}
