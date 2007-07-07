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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.Bool;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeMatrix;
import com.sun.fortress.interpreter.evaluator.types.FTypeNat;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.types.SymbolicNat;
import com.sun.fortress.interpreter.evaluator.types.TypeFixedDimIndices;
import com.sun.fortress.interpreter.evaluator.types.TypeRange;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.nodes.ArrayType;
import com.sun.fortress.interpreter.nodes.ArrowType;
import com.sun.fortress.interpreter.nodes.BaseBoolRef;
import com.sun.fortress.interpreter.nodes.BaseNatRef;
import com.sun.fortress.interpreter.nodes.NodeVisitor;
import com.sun.fortress.interpreter.nodes.BoolParam;
import com.sun.fortress.interpreter.nodes.DimensionParam;
import com.sun.fortress.interpreter.nodes.ExtentRange;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.IdType;
import com.sun.fortress.interpreter.nodes.Indices;
import com.sun.fortress.interpreter.nodes.IntParam;
import com.sun.fortress.interpreter.nodes.MatrixType;
import com.sun.fortress.interpreter.nodes.NatParam;
import com.sun.fortress.interpreter.nodes.OperatorParam;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.ParamType;
import com.sun.fortress.interpreter.nodes.ProductStaticArg;
import com.sun.fortress.interpreter.nodes.VarargsType;
import com.sun.fortress.interpreter.nodes.SimpleTypeParam;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.SumStaticArg;
import com.sun.fortress.interpreter.nodes.TupleType;
import com.sun.fortress.interpreter.nodes.TypeArg;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.VoidType;
import com.sun.fortress.interpreter.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;


public class EvalType extends NodeVisitor<FType> {

    BetterEnv env;
    private EvalIndices ___evalIndices;
    private synchronized EvalIndices evalIndices() {
        if (___evalIndices == null)
            ___evalIndices = new EvalIndices(this);
        return ___evalIndices;
    }

    public FType evalType(TypeRef t) {
        return acceptNode(t);
    }

    public static FType getFTypeFromOption(Option<TypeRef> t, BetterEnv e) {
        if (t.isPresent())
            return getFType(t.getVal(), e);
        else
            return FTypeDynamic.ONLY;
    }

    public  FType getFTypeFromOption(Option<TypeRef> t) {
        if (t.isPresent())
            return getFType(t.getVal());
        else
            return FTypeDynamic.ONLY;
    }

    public static FType getFTypeFromList(List<TypeRef> l, BetterEnv e) {
        return getFTypeFromList(l, new EvalType(e));
    }

    public static FType getFTypeFromList(List<TypeRef> l,  EvalType et) {
        if (l.size() == 1) {
            return l.get(0).accept(et);
        }
        return FTypeTuple.make(getFTypeListFromNonEmptyList(l, et));
    }

    public static List<FType> getFTypeListFromList(List<TypeRef> l, BetterEnv e) {
        EvalType et = new EvalType(e);
        return et.getFTypeListFromList(l);
    }

    public List<FType> getFTypeListFromOptionList(Option<List<TypeRef>> ol) {
        if (ol.isPresent()) {
            List<TypeRef> extl = ol.getVal();
            return this.getFTypeListFromList(extl);
        } else {
            return Collections.<FType>emptyList();
        }
    }


    public List<FType> getFTypeListFromList(List<TypeRef> l) {
        if (l == null || l.size() == 1 && (l.get(0) instanceof VoidType)) {
            // Flatten out voids.
            // Should this be mutable?
            return Collections.<FType>emptyList();
        }

        return getFTypeListFromNonEmptyList(l, this);
    }

    private static List<FType> getFTypeListFromNonEmptyList(List<TypeRef> l, EvalType et) {
        ArrayList<FType> a = new ArrayList<FType>(l.size());
        for (TypeRef t : l) a.add(t.accept(et));
        return a;
    }

    public static FType getFType(TypeRef t, BetterEnv e) {
        return t.accept(new EvalType(e));
    }

    public  FType getFType(TypeRef t) {
        return t.accept(this);
    }

    public static List<Parameter> paramsToParameters(BetterEnv env,
            List<Param> params) {
        if (params.size() == 0) {
            // There must be some way to get the generic parameter attached.
            return Collections.<Parameter>emptyList();
        }
        int i = 0;
        List<Parameter> fparams = new ArrayList<Parameter>(params.size());
        EvalType e = new EvalType(env);
        for (Param in_p : params) {
            Id id = in_p.getName();
            String pname = id.getName();
            Option<TypeRef> type = in_p.getType();
            FType ptype = e.getFTypeFromOption(type);
            Parameter fp = new Parameter(pname, ptype, NodeUtil.isMutable(in_p));
            fparams.add(i++, fp);
        }
        return fparams;
    }

    static void guardedPutType(String name, FType type, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putType(name, type);
        } catch (ProgramError pe) {
            pe.setWithin(containing);
            pe.setWhere(where);
            throw pe;
        }
    }

    static void guardedPutNat(String name, Number nat, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putNat(name, nat);
        } catch (ProgramError pe) {
            pe.setWithin(containing);
            pe.setWhere(where);
            throw pe;
        }
    }

    static void guardedPutBool(String name, Boolean b, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putBool(name, b);
        } catch (ProgramError pe) {
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
                        throw new ProgramError("Negative nats are unNATural: " + l);
                    }

                    guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else if (a instanceof SymbolicNat) {
                    // guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    throw new ProgramError(within, clenv, "Expected IntNat, got " + a + " for param " + p + " instantiating " + what);
                }
            } else if (p instanceof IntParam) {
                if (a instanceof IntNat) {
                    guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else if (a instanceof SymbolicNat) {
                    // guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    throw new ProgramError(within, clenv, "Expected IntNat, got " + a + " for param " + p + " instantiating " + what);
                }
            } else if (p instanceof BoolParam) {
                if (a instanceof Bool) {
                    guardedPutBool(NodeUtil.getName(p), ((Bool)a).getBooleanValue(), what, clenv);
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                } else {
                    throw new ProgramError(within, clenv, "Expected Bool, got " + a + " for param " + p + " instantiating " + what);
                }
            } else if (p instanceof SimpleTypeParam) {
                // There's probably some inappropriate ones.
                if (a instanceof IntNat) {
                    throw new ProgramError(within, clenv,
                                           "When instantiating " + what +
                                           "Got nat " + a +
                                           " instead of type for param " + p +
                                           " (should be a nat param).");
                } else {
                    guardedPutType(NodeUtil.getName(p), a, what, clenv);
                }

            } else if (p instanceof OperatorParam) {
                NI.nyi("Generic, generic in operator"); // TODO operator params
            } else if (p instanceof DimensionParam) {
                NI.nyi("Generic, generic in dimension"); // TODO dimension params
            } else {
                throw new ProgramError(within, clenv, "Unexpected generic parameter " + p);
            }
         }
    }

    public ArrayList<FType> forStaticArgList(List<? extends TypeRef> args) {
        ArrayList<FType> argValues = new ArrayList<FType>(args.size());

        for (int i = 0; i < args.size(); i++) {
            TypeRef a = args.get(i);
            FType t = a.accept(this);
            argValues.add(t);
        }
        return argValues;
    }

    public EvalType(BetterEnv _env) {
        env = _env;
    }

    public FType forVoidType(VoidType v) { return FTypeVoid.ONLY; }

    public FType forVarargsType(VarargsType rt) {
        return FTypeRest.make(rt.getType().accept(this));
    }

    public FType forBaseBoolRef(BaseBoolRef b) {
        if (b.isBool()) return new Bool("true", FBool.TRUE);
        else return new Bool("false", FBool.FALSE);
    }

    public FType forIdType(IdType i) {
        try {
            FType result = env.getType(i.getName());
            return result;
        } catch (ProgramError p) {
            p.setWhere(i);
            p.setWithin(env);
            throw p;
        }

    }

    @Override
    public FType forArrowType(ArrowType at) {
        // TODO Keywords, defaults, still TBI
        return FTypeArrow.make(getFTypeFromList(at.getDomain(), this), at.getRange().accept(this));
    }

    @Override
    public FType forTupleType(TupleType tt) {
        // TODO Unusual tuple types
        return getFTypeFromList(tt.getElements(), this);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forBaseNatRef(com.sun.fortress.interpreter.nodes.BaseNatRef)
     */
    @Override
    public FType forBaseNatRef(BaseNatRef x) {
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
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forProductStaticArg(com.sun.fortress.interpreter.nodes.ProductStaticArg)
     */
    @Override
    public FType forProductStaticArg(ProductStaticArg x) {
        List<? extends TypeRef> value = x.getValues();
        long a = nonEmpty(value);
        for (int i = 1; i < value.size(); i++) {
            a *= longify(value.get(i));
        }
        return IntNat.make(a);
    }

    private long nonEmpty(List<? extends TypeRef> value) {
        if (value.size() == 0)
            throw new ProgramError("Empty operands to nat arithmetic");
        return longify(value.get(0));
    }

    private long longify(TypeRef type) {
        FType t = type.accept(this);
        if (!(t instanceof IntNat)) {
            throw new ProgramError(type,"StaticArg " + type + " evaluated to " + t + " (instead of IntNat)");
        }
        return ((IntNat) t).getValue();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSumStaticArg(com.sun.fortress.interpreter.nodes.SumStaticArg)
     */
    @Override
    public FType forSumStaticArg(SumStaticArg x) {
        List<? extends TypeRef> value = x.getValues();
        long a = nonEmpty(value);
        for (int i = 1; i < value.size(); i++) {
            a += longify(value.get(i));
        }
        return IntNat.make(a);
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
        catch (ProgramError pe) {
            pe.setWhere(x);
            throw pe;
        }
    }

    public FType forParamType(ParamType x) {
        TypeRef t = x.getGeneric();
        FType ft1 = t.accept(this);
        if (ft1 instanceof  FTypeGeneric) {
            FTypeGeneric ftg = (FTypeGeneric) ft1;
            return ftg.typeApply(x.getArgs(), env, x);
        } else {
            throw new ProgramError(x, env, "Expected generic type, got " + ft1 + " instead");
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

        Option<? extends TypeRef> b = extent.getBase();
        Option<? extends TypeRef> s = extent.getSize();
        FTypeNat natB, natS;
        if (b.isPresent()) {
            FType bt = b.getVal().accept(this);
            if (bt instanceof IntNat || bt instanceof SymbolicNat) {
                natB = (FTypeNat)bt;
            } else {
                throw new ProgramError(extent,env,"Bad base "+
                                       b.getVal()+"="+
                                       bt.getClass().getName()+" "+bt);
            }
        } else {
            natB = IntNat.make(0);
        }

        if (s.isPresent()) {
            FType st = s.getVal().accept(this);
            if (st instanceof IntNat || st instanceof SymbolicNat) {
                natS = (FTypeNat)st;
            } else {
                throw new ProgramError(extent,env,"Bad size "+
                                       s.getVal()+"="+
                                       st.getClass().getName()+" "+st);
            }
        } else {
            natS = IntNat.make(0);
        }
        return new TypeRange(natB, natS);
    }


}
