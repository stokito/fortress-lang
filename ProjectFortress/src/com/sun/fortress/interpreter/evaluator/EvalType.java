/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.FortressException;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.exceptions.ProgramError;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvalType extends NodeAbstractVisitor<FType> {

    Environment env;
    private EvalIndices ___evalIndices;

    private synchronized EvalIndices evalIndices() {
	if (___evalIndices == null) ___evalIndices = new EvalIndices(this);
	return ___evalIndices;
    }

    public FType evalType(Type t) {
	return t.accept(this);
    }

    public FType evalType(TraitSelfType t) {
        return evalType(t.getNamed());
    }

    // Returns the type that has the same name as the trait, in this
    // environment (used in unification).
    // Done this way to ensure that getType is only applied to top-level names.
    public FType evalType(TraitType t) {
	Id q = t.getName();
	try {
	    FType result = env.getType(q);
	    return result;
	}
	catch (FortressException p) {
	    throw p.setContext(q, env);
	}
    }

    public static FType getFTypeFromOption(Option<Type> opt_t, final Environment e, final FType ifMissing) {
	return opt_t.apply(new OptionVisitor<Type, FType>() {
	    public FType forSome(Type t) {
		return getFType(t, e);
	    }

	    public FType forNone() {
		return ifMissing;
	    }
	});
    }

    public FType getFTypeFromOption(Option<Type> opt_t, final FType ifMissing) {
	return opt_t.apply(new OptionVisitor<Type, FType>() {
	    public FType forSome(Type t) {
		return getFType(t);
	    }

	    public FType forNone() {
		return ifMissing;
	    }
	});
    }

    public static FType getFTypeFromList(List<Type> l, Environment e) {
	return getFTypeFromList(l, new EvalType(e));
    }

    public static FType getFTypeFromList(List<Type> l, EvalType et) {
	if (l.size() == 1) {
	    return l.get(0).accept(et);
	}
	return FTypeTuple.make(getFTypeListFromNonEmptyList(l, et));
    }

    public static List<FType> getFTypeListFromList(List<? extends Type> l, Environment e) {
	EvalType et = new EvalType(e);
	return et.getFTypeListFromList(l);
    }

    public List<FType> getFTypeListFromOptionList(Option<List<BaseType>> ol) {
	return ol.apply(new OptionVisitor<List<BaseType>, List<FType>>() {
	    public List<FType> forSome(List<BaseType> l) {
		return getFTypeListFromList(l);
	    }

	    public List<FType> forNone() {
		return Collections.emptyList();
	    }
	});
    }


    public List<FType> getFTypeListFromList(List<? extends Type> l) {
	if (l == null || l.size() == 1 && NodeUtil.isVoidType(l.get(0))) {
	    // Flatten out voids.
	    // Should this be mutable?
	    return Collections.<FType>emptyList();
	}

	return getFTypeListFromNonEmptyList(l, this);
    }

    private static List<FType> getFTypeListFromNonEmptyList(List<? extends Type> l, EvalType et) {
	ArrayList<FType> a = new ArrayList<FType>(l.size());
	for (Type t : l) {
	    a.add(t.accept(et));
	}
	return a;
    }

    public static FType getFType(Type t, Environment e) {
	return t.accept(new EvalType(e));
    }

    public FType getFType(Type t) {
	return t.accept(this);
    }

    public static List<Parameter> paramsToParameters(Environment env, List<Param> params) {
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
	    if (!NodeUtil.isVarargsParam(in_p)) {
		Option<Type> type = NodeUtil.optTypeOrPatternToType(in_p.getIdType());
		ptype = e.getFTypeFromOption(type, FTypeTop.ONLY);
	    } else { // a varargs param
		ptype = FTypeRest.make(e.getFType(in_p.getVarargsType().unwrap()));
	    }
	    Parameter fp = new Parameter(pname, ptype, NodeUtil.isMutable(in_p));
	    fparams.add(i++, fp);
	}
	return fparams;
    }

    static void guardedPutType(String name, FType type, HasAt where, Environment containing) {
	// Referenced from BuildEnvironments, perhaps others.
	try {
	    containing.putType(name, type);
	}
	catch (FortressException pe) {
	    throw pe.setContext(where, containing);
	}
    }

    static void guardedPutNat(String name, Number nat, HasAt where, Environment containing) {
	// Referenced from BuildEnvironments, perhaps others.
	try {
	    containing.putNat(name, nat);
	}
	catch (FortressException pe) {
	    throw pe.setContext(where, containing);
	}
    }

    static void guardedPutBool(String name, Boolean b, HasAt where, Environment containing) {
	// Referenced from BuildEnvironments, perhaps others.
	try {
	    containing.putBool(name, b);
	}
	catch (FortressException pe) {
	    throw pe.setContext(where, containing);
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
		a = BottomType.ONLY;
		guardedPutType(NodeUtil.getName(p), a, what, clenv);
		// TODO need to undefine the nats and bools
		// This means that our choice to use the Java types Boolean etc was wrong,
		// because they lack lattice structure.
	    } else if (NodeUtil.isTypeParam(p)) {
		String whence = null;
		// There's probably some inappropriate ones.
		if (a instanceof FTypeNat) {
		    whence = "nat ";
		} else if (a instanceof FTypeOpr) {
		    whence = "opr ";
		} else if (a instanceof BoolType) {
		    whence = "bool ";
		}
		if (whence != null) {
		    error(within, clenv, errorMsg("When instantiating ",
						  what,
						  "Got ",
						  whence,
						  a,
						  " instead of type for param ",
						  p));
		}
	    } else if (NodeUtil.isNatParam(p)) {
		if (a instanceof IntNat) {
		    long l = ((IntNat) a).getValue();
		    if (l < 0) {
			// Move this check into the param-specific binding code.
			error(p, errorMsg("Negative nats are unNATural: " + l));
		    }

		    guardedPutNat(NodeUtil.getName(p), ((IntNat) a).getNumber(), what, clenv);
		} else if (a instanceof SymbolicNat) {
		    // guardedPutNat(NodeUtil.nameString(p), ((IntNat)a).getNumber(), what, clenv);
		} else {
		    error(within, clenv, errorMsg("Expected Nat, got ", a, " for param ", p, " instantiating ", what));
		}
	    } else if (NodeUtil.isIntParam(p)) {
		if (a instanceof IntNat) {
		    guardedPutNat(NodeUtil.getName(p), ((IntNat) a).getNumber(), what, clenv);
		} else if (a instanceof SymbolicNat) {
		    // guardedPutNat(NodeUtil.getName(p), ((IntNat)a).getNumber(), what, clenv);
		} else {
		    error(within, clenv, errorMsg("Expected Int, got ", a, " for param ", p, " instantiating ", what));
		}
	    } else if (NodeUtil.isBoolParam(p)) {
		if (a instanceof Bool) {
		    guardedPutBool(NodeUtil.getName(p), ((Bool) a).getBooleanValue(), what, clenv);
		} else if (a instanceof SymbolicBool) {
		    // Fall through
		} else {
		    error(within, clenv, errorMsg("Expected Bool, got ", a, " for param ", p, " instantiating ", what));
		}
	    } else if (NodeUtil.isOpParam(p)) {
		if (a instanceof FTypeOpr || a instanceof SymbolicOprType) {
		    // Fall through
		} else {
		    error(within, clenv, errorMsg("Expected Opr, got ", a, " for param ", p, " instantiating ", what));
		}
	    } else if (NodeUtil.isDimParam(p)) {
		NI.nyi("Generic, generic in dimension"); // TODO dimension params
	    } else {
		error(within, clenv, errorMsg("Unexpected generic parameter ", p));
	    }
	    guardedPutType(NodeUtil.getName(p), a, what, clenv);
	}
    }

    public ArrayList<FType> forStaticArgList(List<StaticArg> args) {
	ArrayList<FType> argValues = new ArrayList<FType>(args.size());

	for (int i = 0; i < args.size(); i++) {
	    StaticArg a = args.get(i);
	    FType t = a.accept(this);
	    argValues.add(t);
	}
	return argValues;
    }

    public EvalType(Environment _env) {
	env = _env;
    }

    @Override
    public FType defaultCase(Node n) {
        return bug(n, errorMsg("Can't EvalType this node type " + n.getClass()));
    }

    public FType forOpArg(OpArg b) {
        return FTypeOpr.make(NodeUtil.nameString(b.getId()));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forIntArg(com.sun.fortress.interpreter.nodes.IntArg)
     */
    @Override
    public FType forBoolArg(BoolArg boolArg) {
	BoolExpr i = boolArg.getBoolArg();
	return i.accept(new NodeAbstractVisitor<FType>() {
	    private boolean boolify(BoolExpr ie) {
		FType t = ie.accept(this);
		if (!(t instanceof Bool)) {
		    error(ie, errorMsg("BoolExpr ", ie, " evaluated to ", t, " (instead of Bool)"));
		}
		return ((Bool) t).getBooleanValue();
	    }

	    public FType forBoolBase(BoolBase b) {
		return Bool.make(b.isBoolVal());
	    }

	    public FType forBoolRef(BoolRef n) {
		Id q = n.getName();
		try {
		    FType result = env.getType(q);
		    return result;
		}
		catch (FortressException p) {
		    throw p.setContext(q, env);
		}
	    }

	    public FType forBoolUnaryOp(BoolUnaryOp n) {
		if (n.getOp().getText().equals("NOT")) return Bool.make(!boolify(n.getBoolVal()));
		else return bug(n, errorMsg("EvalType: ", n.getClass(), " is not yet implemented."));
	    }

	    private boolean boolOp(BoolBinaryOp n, String op, boolean left, boolean right) {
		if (op.equals("OR")) return left || right;
		else if (op.equals("AND")) return left && right;
		else if (op.equals("IMPLIES")) return !left || right;
		else if (op.equals("=")) return left == right;
		else bug(n, errorMsg("EvalType: ", n.getClass(), " is not a subtype of BoolExpr."));
		return false;
	    }

	    public FType forBinaryBooluConstraint(BoolBinaryOp n) {
		return Bool.make(boolOp(n, n.getOp().getText(), boolify(n.getLeft()), boolify(n.getRight())));
	    }

	    public FType defaultCase(Node x) {
		return bug(x, errorMsg("EvalType: ", x.getClass(), " is not a subtype of BoolExpr."));
	    }
	});
    }

    public FType forBoolBase(BoolBase b) {
	return Bool.make(b.isBoolVal());
    }

    public FType forVarType(VarType i) {
	try {
	    FType result = env.getType(i);
	    return result;
	}
	catch (FortressException p) {
	    throw p.setContext(i, env);
	}

    }

    @Override
    public FType forArrowType(ArrowType at) {
	Type domain = at.getDomain();
	if (domain instanceof TupleType && !((TupleType) domain).getKeywords().isEmpty()) {
	    return NI.nyi("Can't interpret a type with keywords");
	}
	return FTypeArrow.make(Types.stripKeywords(domain).accept(this), at.getRange().accept(this));
    }

    // Hack!!!
    @Override
    public FType forIntersectionType(IntersectionType it) {
        if (!it.getElements().isEmpty())
            return it.getElements().get(0).accept(this);
        else return NI.nyi("Can't interpret an empty intersection type.");
    }

    // Hack!!!
    @Override
    public FType forUnionType(UnionType ut) {
        if (!ut.getElements().isEmpty())
            return ut.getElements().get(0).accept(this);
        else return NI.nyi("Can't interpret an empty union type.");
    }

    @Override
    public FType forTupleType(TupleType tt) {
	if (NodeUtil.isVoidType(tt)) return FTypeVoid.ONLY;
	else if (!NodeUtil.hasVarargs(tt)) {
	    List<FType> elts = getFTypeListFromList(tt.getElements());
	    return FTypeTuple.make(elts);
	} else {
	    List<FType> elts = getFTypeListFromList(tt.getElements());
	    elts.add(FTypeRest.make(tt.getVarargs().unwrap().accept(this)));
	    return FTypeTuple.make(elts);
	}
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forIntArg(com.sun.fortress.interpreter.nodes.IntArg)
     */
    @Override
    public FType forIntArg(IntArg intArg) {
	IntExpr i = intArg.getIntVal();
	return i.accept(new NodeAbstractVisitor<FType>() {
	    private long longify(IntExpr ie) {
		FType t = ie.accept(this);
		if (!(t instanceof IntNat)) {
		    error(ie, errorMsg("IntExpr ", ie, " evaluated to ", t, " (instead of IntNat)"));
		}
		return ((IntNat) t).getValue();
	    }

	    public FType forIntBase(IntBase n) {
		return IntNat.make(n.getIntVal().getIntVal().intValue());
	    }

	    public FType forIntRef(IntRef n) {
		Id q = n.getName();
		try {
		    FType result = env.getType(q);
		    return result;
		}
		catch (FortressException p) {
		    throw p.setContext(q, env);
		}
	    }

	    public FType forIntBinaryOp(IntBinaryOp n) {
		long left = longify(n.getLeft());
		long right = longify(n.getRight());
		if (n.getOp().getText().equals("+")) return IntNat.make(left + right);
		else if (n.getOp().getText().equals("-")) return IntNat.make(left - right);
		else if (n.getOp().getText().equals(" ")) return IntNat.make(left * right);
		else return bug(n, errorMsg("EvalType: ", n.getClass(), " is not yet implemented."));
	    }

	    public FType defaultCase(Node x) {
		return bug(x, errorMsg("EvalType: ", x.getClass(), " is not a subtype of IntExpr."));
	    }
	});
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forOpParam(com.sun.fortress.interpreter.nodes.OpParam)
     */
    @Override
    public FType forStaticParam(StaticParam x) {
	if (NodeUtil.isOpParam(x)) {
	    // TODO Auto-generated method stub
	    return super.forStaticParam(x);
	} else return bug(x, errorMsg("Can't EvalType this node type."));
    }

    private long nonEmpty(List<? extends Type> value) {
	if (value.size() == 0) error("Empty operands to nat arithmetic");
	return longify(value.get(0));
    }

    private long longify(Type type) {
	FType t = type.accept(this);
	if (!(t instanceof IntNat)) {
	    error(type, errorMsg("StaticArg ", type, " evaluated to ", t, " (instead of IntNat)"));
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
	    return x.getTypeArg().accept(this);
	}
	catch (FortressException pe) {
	    pe.setWhere(x);
	    throw pe;
	}
    }

    public FType forAnyType(AnyType a) {
	return FTypeTop.ONLY;
    }

    public FType forTraitSelfType(TraitSelfType x) {
        return x.getNamed().accept(this);
    }

    public FType forObjectExprType(ObjectExprType x) {
        List<BaseType> extended = x.getExtended();
        if (!extended.isEmpty())
            return extended.get(0).accept(this);
        else return NI.nyi("Can't interpret an empty intersection type.");
    }

    public FType forTraitType(TraitType x) {
	FType ft1;
	try {
	    ft1 = env.getType(x);
	}
	catch (FortressException p) {
	    throw p.setContext(x, env);
	}

	if (ft1 instanceof FTypeGeneric) {
	    FTypeGeneric ftg = (FTypeGeneric) ft1;
	    return ftg.typeApply(x.getArgs(), env, x);
	} else {
	    // It isn't necessarily an error if a TraitType doesn't refer
	    // to an FTypeGeneric. After disambiguation, all trait type references are
	    // TraitTypes (possibly with zero arguments). EricAllen 11/5/2007
	    return ft1;
	}
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forArrayType(com.sun.fortress.interpreter.nodes.IndexedType)
     */
    @Override
    public FType forArrayType(ArrayType x) {
	FType elt_type = x.getElemType().accept(this);
	Indices indices = x.getIndices();

	TypeFixedDimIndices f_indices = (TypeFixedDimIndices) indices.accept(evalIndices());
	List<TypeRange> ltr = f_indices.getRanges();
	return Glue.instantiateGenericType(Driver.getFortressLibrary(),
					   WellKnownNames.arrayTrait(ltr.size()),
					   elt_type,
					   ltr,
					   x);
    }

    @Override
    public FType forMatrixType(MatrixType x) {
	FType elt_type = x.getElemType().accept(this);
	List<ExtentRange> dimensions = x.getDimensions();
	List<TypeRange> typeRanges = new ArrayList<TypeRange>();
	for (ExtentRange extent : dimensions) {
	    typeRanges.add(extentRangeToTypeRange(extent));
	}
	return new FTypeMatrix(elt_type, typeRanges);
    }

    TypeRange extentRangeToTypeRange(ExtentRange extent) {

	Option<StaticArg> b = extent.getBase();
	Option<StaticArg> s = extent.getSize();
	Option<Op> op = extent.getOp();
	FTypeNat natB, natS;
	if (b.isSome()) {
	    FType bt = b.unwrap().accept(this);
	    if (bt instanceof IntNat || bt instanceof SymbolicNat) {
		natB = (FTypeNat) bt;
	    } else {
		natB = error(b.unwrap(), errorMsg(extent,
						  env,
						  "Bad base ",
						  b.unwrap(),
						  "=",
						  bt.getClass().getName(),
						  " ",
						  bt));
	    }
	} else {
	    natB = IntNat.make(0);
	}

	if (s.isSome()) {
	    FType st = s.unwrap().accept(this);
	    if ((st instanceof IntNat || st instanceof SymbolicNat) &&
		(op.isNone() || op.unwrap().getText().equals("#"))) {
		natS = (FTypeNat) st;
	    } else {
		natS = error(s.unwrap(), errorMsg(extent,
						  env,
						  "Bad size ",
						  s.unwrap(),
						  "=",
						  st.getClass().getName(),
						  " ",
						  st));
	    }
	} else {
	    natS = IntNat.make(0);
	}
	return new TypeRange(natB, natS);
    }


}
