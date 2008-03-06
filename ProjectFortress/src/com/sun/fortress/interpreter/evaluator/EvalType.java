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
import com.sun.fortress.interpreter.evaluator.types.SymbolicOprType;
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
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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

    public FType evalType(QualifiedIdName t) {
        return forQualifiedIdName(t);
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

    public static List<Parameter> paramsToParameters(EvalType e, List<Param> params) {
        if (params.size() == 0) {
            // There must be some way to get the generic parameter attached.
            return Collections.<Parameter>emptyList();
        }
        int i = 0;
        List<Parameter> fparams = new ArrayList<Parameter>(params.size());
        for (Param in_p : params) {
            Id idName = in_p.getName();
            String pname = NodeUtil.nameString(idName);
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
            throw pe.setContext(where,containing);
        }
    }

    static void guardedPutNat(String name, Number nat, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putNat(name, nat);
        } catch (FortressError pe) {
            throw pe.setContext(where, containing);
        }
    }

    static void guardedPutBool(String name, Boolean b, HasAt where, Environment containing) {
        // Referenced from BuildEnvironments, perhaps others.
        try {
            containing.putBool(name, b);
        } catch (FortressError pe) {
            throw pe.setContext(where,containing);
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
                    // guardedPutNat(NodeUtil.nameString(p), ((IntNat)a).getNumber(), what, clenv);
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
                } else if (a instanceof SymbolicOprType) {
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

    public FType forOprArg(OprArg b) {
        return FTypeOpr.make(NodeUtil.nameString(b.getName()));
    }

    public FType forVoidType(VoidType v) { return FTypeVoid.ONLY; }

    public FType forVarargsType(VarargsType rt) {
        return FTypeRest.make(rt.getType().accept(this));
    }

    public FType forBoolArg(BoolArg b) {
        return b.getBool().accept(this);
    }

    public FType forBoolConstant(BoolConstant b) {
        return Bool.make(b.isBool());
    }

    public FType forIdType(IdType i) {
        try {
            FType result = env.getType(i.getName());
            return result;
        } catch (FortressError p) {
            throw p.setContext(i,env);
        }

    }

    @Override
    public FType forArrowType(ArrowType at) {
        // TODO Keywords, defaults, still TBI
        return FTypeArrow.make(at.getDomain().accept(this), at.getRange().accept(this));
    }

    @Override
    public FType forArgType(ArgType tt) {
        // TODO Unusual tuple types
        return getFTypeFromList(tt.getElements(), this);
    }

    public FType forTupleType(TupleType tt) {
        // TODO Unusual tuple types
        return getFTypeFromList(tt.getElements(), this);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forIntArg(com.sun.fortress.interpreter.nodes.IntArg)
     */
    @Override
    public FType forIntArg(IntArg x) {
        IntExpr i = x.getVal();
        return i.accept(new NodeAbstractVisitor<FType>() {
            private long longify(IntExpr ie) {
                FType t = ie.accept(this);
                if (!(t instanceof IntNat)) {
                    error(ie, errorMsg("IntExpr ", ie, " evaluated to ", t,
                                       " (instead of IntNat)"));
                }
                return ((IntNat)t).getValue();
            }
            public FType forNumberConstraint(NumberConstraint n) {
                return IntNat.make(n.getVal().getVal().intValue());
            }
            public FType forIntRef(IntRef n) {
                QualifiedIdName q = n.getName();
                try {
                    FType result = env.getType(q);
                    return result;
                } catch (FortressError p) {
                    throw p.setContext(q, env);
                }
            }
            public FType forSumConstraint(SumConstraint n) {
                return IntNat.make(longify(n.getLeft()) + longify(n.getRight()));
            }
            public FType forMinusConstraint(MinusConstraint n) {
                return IntNat.make(longify(n.getLeft()) - longify(n.getRight()));
            }
            public FType forProductConstraint(ProductConstraint n) {
                return IntNat.make(longify(n.getLeft()) * longify(n.getRight()));
            }
            public FType defaultCase(Node x) {
                return bug(x, "EvalType: " + x.getClass() +
         " is not a subtype of IntExpr.");
            }
        });
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forOperatorParam(com.sun.fortress.interpreter.nodes.OperatorParam)
     */
    @Override
    public FType forOperatorParam(OperatorParam x) {
        // TODO Auto-generated method stub
        return super.forOperatorParam(x);
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

    public FType forQualifiedIdName(QualifiedIdName q) {
        try {
            FType result = env.getType(q);
            return result;
        } catch (FortressError p) {
            throw p.setContext(q,env);
        }

    }

    public FType forInstantiatedType(InstantiatedType x) {
       FType ft1 = forQualifiedIdName(x.getName());
        if (ft1 instanceof  FTypeGeneric) {
            FTypeGeneric ftg = (FTypeGeneric) ft1;
            return ftg.typeApply(x.getArgs(), env, x);
        } else {
            // It isn't necessarily an error if an InstantiatedType doesn't refer
            // to an FTypeGeneric. After disambiguation, all trait type references are
            // InstantiatedTypes (possibly with zero arguments). EricAllen 11/5/2007
            return ft1;
//            return error(x, env, errorMsg("Expected generic type, got ", ft1,
//                                          " instead"));
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
                natB = error(Option.unwrap(b),
                             errorMsg(extent,env,"Bad base ",
                                      Option.unwrap(b),"=",
                                      bt.getClass().getName(), " ", bt));
            }
        } else {
            natB = IntNat.make(0);
        }

        if (s.isSome()) {
            FType st = Option.unwrap(s).accept(this);
            if (st instanceof IntNat || st instanceof SymbolicNat) {
                natS = (FTypeNat)st;
            } else {
                natS = error(Option.unwrap(s),
                             errorMsg(extent,env,"Bad size ",
                                      Option.unwrap(s), "=",
                                      st.getClass().getName(), " ", st));
            }
        } else {
            natS = IntNat.make(0);
        }
        return new TypeRange(natB, natS);
    }


}
