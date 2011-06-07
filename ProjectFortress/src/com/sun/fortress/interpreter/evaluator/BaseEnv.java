/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.CircularDependenceError;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.exceptions.RedefinitionError;
import com.sun.fortress.interpreter.env.BetterEnvWithTopLevel;
import com.sun.fortress.interpreter.env.IndirectionCell;
import com.sun.fortress.interpreter.env.LazilyEvaluatedCell;
import com.sun.fortress.interpreter.env.ReferenceCell;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.StringArrayIterator;
import com.sun.fortress.useful.Visitor2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A BaseEnv supplies (enforces!) some overloadings that
 * every environment must support.
 */

abstract public class BaseEnv implements Environment, Iterable<String> {

    public Environment getHomeEnvironment(IdOrOpOrAnonymousName ioooan) {
        Option<APIName> oapi = ioooan.getApiName();
        if (oapi.isNone()) return this;
        return getApi(oapi.unwrap());
    }

    public Environment getApi(APIName a) {
        String s = NodeUtil.nameString(a);
        return getTopLevel().getApi(s);
    }

    public Environment getApi(List<Id> ids) {
        String s = com.sun.fortress.useful.Useful.<Id>dottedList(ids);
        return getTopLevel().getApi(s);
    }

    public Environment getApi(String s) {
        Environment e1 = getTopLevel();
        Environment e = e1.getApiNull(s);
        if (e == null) {
            //e = e1.getApiNull(s);
            return error(errorMsg("Missing api name ", s));
        }
        return e;
    }

    public void putApi(String apiName, Environment env) {
        /* Should override in the generated top level environment */
    }

    public Environment extend() {
        return tlExtend();
    }

    public Environment extend(Environment additions) {
        return tlExtend(additions);
    }

    public Environment extendAt(HasAt x) {
        return tlExtendAt(x);
    }

    /**
     * Extends a top-level environment.
     *
     * @return
     */
    protected Environment tlExtend() {
        return new BetterEnvWithTopLevel(this, this.getAt());
    }

    protected Environment tlExtend(Environment additions) {
        BetterEnvWithTopLevel bewtl = new BetterEnvWithTopLevel(this, this.getAt());
        bewtl.augment(additions);
        bewtl.bless();
        return bewtl;
    }

    protected Environment tlExtendAt(HasAt x) {
        return new BetterEnvWithTopLevel(this, x);

    }

    public void visit(Visitor2<String, FType> vt,
                      Visitor2<String, Number> vn,
                      Visitor2<String, Number> vi,
                      Visitor2<String, FValue> vv,
                      Visitor2<String, Boolean> vb) {
        // TODO Auto-generated method stub
    }

    public void visit(Visitor2<String, Object> nameCollector) {
        // TODO Auto-generated method stub
    }

    /**
     * Names noted for possible future overloading
     */
    private String[] namesPut;
    private int namesPutCount;

    private boolean blessed = false; /* until blessed, cannot be copied */
    private boolean topLevel = false;

    public boolean debug = false;
    public boolean verboseDump = false;

    /**
     * Where created
     */
    protected HasAt within;

    public void debugPrint(String debugString) {
        if (debug) System.out.println(debugString);

    }

    static public String string(FValue f1) {
        return ((FString) f1).getString();
    }

    public void bless() {
        blessed = true;
    }

    public boolean getBlessed() {
        return blessed;
    }

    public void setTopLevel() {
        topLevel = true;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    protected void augment(final Environment additions) {
        final Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                putTypeRaw(s, o);
            }
        };
        final Visitor2<String, Number> vn = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                putNatRaw(s, o);
            }
        };
        final Visitor2<String, Number> vi = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                putIntRaw(s, o);
            }
        };
        final Visitor2<String, FValue> vv = new Visitor2<String, FValue>() {
            public void visit(String s, FValue o) {

                FType ft = additions.getVarTypeNull(s);
                if (ft != null) putValueRaw(s, o, ft);
                else putValueRaw(s, o);
            }
        };
        final Visitor2<String, Boolean> vb = new Visitor2<String, Boolean>() {
            public void visit(String s, Boolean o) {
                putBoolRaw(s, o);
            }
        };

        additions.visit(vt, vn, vi, vv, vb);

    }

    abstract public Appendable dump(Appendable a) throws IOException;

    protected final void putNoShadow(String index, FValue value, String what) {
        FValue fvo = getValueRaw(index);
        if (fvo == null || index.equals("outcome")) {
            putValueRaw(index, value);
        } else if (fvo instanceof IndirectionCell) {
            // TODO Need to push the generic type Result into IndirectionCell, etc.
            // This yucky code is "correct" because IndirectionCell extends FValue,
            // and "it happens to be true" that this code will never be instantiated
            // above or below FValue in the type hierarchy.
            // Strictly speaking, this might be wrong if it permits
            // = redefinition of a mutable cell (doesn't seem to).
            IndirectionCell ic = (IndirectionCell) fvo;
            if (ic instanceof ReferenceCell) {
                throw new RedefinitionError("Mutable variable", index, fvo, value);
            } else {
                ic.storeValue((FValue) value);
            }
        } else {
            throw new RedefinitionError(what, index, fvo, value);
        }
    }

    protected final void putFunction(String index,
                                     Fcn value,
                                     String what,
                                     boolean shadowIfDifferent,
                                     boolean overloadIsOK) {
        FValue fvo = getValueRaw(index);
        if (fvo == null) {
            putValueRaw(index, value);
            noteName(index);
        } else {
            if (fvo instanceof IndirectionCell) {
                // TODO Need to push the generic type Result into IndirectionCell, etc.
                // This yucky code is "correct" because IndirectionCell extends FValue,
                // and "it happens to be true" that this code will never be instantiated
                // above or below FValue in the type hierarchy.
                // Strictly speaking, this might be wrong if it permits
                // = redefinition of a mutable cell (doesn't seem to).
                IndirectionCell ic = (IndirectionCell) fvo;
                if (ic instanceof ReferenceCell) {
                    throw new RedefinitionError("Mutable variable", index, fvo, value);
                } else if (!ic.isInitialized()) {
                    ic.storeValue((FValue) value);
                    return;
                } else {
                    // ic is an initialized value cell, not a true function.
                    // do not overload.
                    throw new RedefinitionError(what, index, fvo, value);
                }
            }

            /* ic is a function, do an overloading on it.
            * Because of wholesale symbol import via linking,
            * it is possible to combine a pair of overloadings.
            *
            * This is all going to get simpler in the future,
            * when overloading gets more complicated (allowing mixed
            * generic and non-generic overloading).
            */

            if (!(fvo instanceof Fcn)) System.err.println("Eek!");
            Fcn fcn_fvo = (Fcn) fvo;
            if (!shadowIfDifferent && // true for functional methods
                fcn_fvo.getWithin() != value.getWithin() &&
                !(fcn_fvo.getWithin().isTopLevel() && value.getWithin().isTopLevel()) // for imports from another api
                    ) {
                /*
                * If defined in a different environment, shadow
                * instead of overloading.
                */
                putValueRaw(index, value);
                noteName(index);
                return;
            }

            /*
            * Lots of overloading combinations
            */
            OverloadedFunction ovl = null;
            if (fvo instanceof SingleFcn) {
                SingleFcn gm = (SingleFcn) fvo;
                ovl = new OverloadedFunction(gm.getFnName(), this);
                ovl.addOverload(gm);
                putValueRaw(index, ovl);

            } else if (fvo instanceof OverloadedFunction) {
                ovl = (OverloadedFunction) fvo;
            } else {
                throw new RedefinitionError(what, index, fvo, value);
            }
            if (value instanceof SingleFcn) {
                ovl.addOverload((SingleFcn) value, overloadIsOK);
            } else if (value instanceof OverloadedFunction) {
                ovl.addOverloads((OverloadedFunction) value);
            } else {
                error(errorMsg("Overload of ", ovl, " with inconsistent ", value));
            }
            /*
            * The overloading occurs in the original table, unless a new overload
            * was created (see returns of "table.add" above).
            */

        }

    }

    public void assignValue(HasAt loc, String str, FValue value) {
        FValue v = getValueRaw(str);
        if (v instanceof ReferenceCell) {
            ReferenceCell rc = (ReferenceCell) v;
            FType ft = rc.getType();
            if (ft != null) {
                if (!ft.typeMatch(value)) {
                    String m = errorMsg("Type mismatch assigning ",
                                        value,
                                        " (type ",
                                        value.type(),
                                        ") to ",
                                        str,
                                        " (type ",
                                        ft,
                                        ")");
                    error(loc, m);
                    return;
                }
            }
            rc.assignValue(value);
            return;
        }
        if (v == null) error(loc, this, "Cannot assign to unbound variable " + str);
        error(loc, this, "Cannot assign to immutable " + str);
    }

    public void storeType(HasAt loc, String str, FType f2) {
        FValue v = getValueRaw(str);
        if (v instanceof ReferenceCell) {
            ((ReferenceCell) v).storeType(f2);
            return;
        }
        if (v == null) error(loc, this, "Type stored to unbound variable " + str);
        error(loc, this, "Type stored to immutable variable " + str);

    }

    final public Boolean getBool(String str) {
        Boolean x = getBoolNull(str);
        if (x == null) return error(errorMsg("Missing boolean ", str));
        else return x;
    }

    abstract public Boolean getBoolNull(String str);

    final public Number getNat(String str) {
        Number x = getNatNull(str);
        if (x == null) return error(errorMsg("Missing nat ", str));
        else return x;
    }

    abstract public Number getNatNull(String str);

    public FunctionClosure getRunClosure() {
        return (FunctionClosure) getRootValue("run");
    }

    final public FType getType(VarType q) {
        FType x = getTypeNull(q);
        if (x == null) {
            // System.err.println(this.toString());
            return error(errorMsg("Missing type ", q));
        } else return x;
    }

    final public FType getType(TraitType q) {
        Environment e = getTopLevel();
        FType x = e.getTypeNull(q.getName());
        if (x == null) {
            // System.err.println(this.toString());
            return error(errorMsg("Missing type ", q));
        } else return x;
    }

    final public FType getTypeNull(VarType q) {
        Environment e = toContainingObjectEnv(this, q.getLexicalDepth());
        FType x = e.getTypeNull(q.getName());
        return x;
    }

    final public FType getType(Id q) {
        FType x = getTypeNull(q);
        if (x == null) {
            // System.err.println(this.toString());
            return error(errorMsg("Missing type ", q));
        } else return x;
    }

    final public FType getRootType(String str) {
        FType x = getTypeNull(str);
        if (x == null) return error(errorMsg("Missing type ", str));
        else return x;
    }

    final public FType getLeafType(String str) {
        return getRootType(str); // temp hack
    }

    public FType getLeafTypeNull(String name) {
        return getTypeNull(name); // temp hack
    }

    public FType getRootTypeNull(String name) {
        return getTypeNull(name); // temp hack
    }

    final public FType getTypeNull(Id name) {
        String local = NodeUtil.nameSuffixString(name);
        Option<APIName> opt_api = name.getApiName();

        if (opt_api.isSome()) {
            APIName api = opt_api.unwrap();
            // Circular dependence etc will be signalled in API.
            Environment api_e = getApi(api);
            return api_e.getRootTypeNull(local);
        } else {
            FType v = getTypeNull(local); // TODO A PROBLEM
            return v;
        }
    }

    abstract public FType getTypeNull(String str);

    //    final public  FValue getValue(FValue f1) {
    //        return getValue(string(f1));
    //    }

    //    final public  FValue getValue(Id q)  {
    //        FValue x = getValueNull(q);
    //        if (x == null)
    //            return error(errorMsg("Missing value ", q));
    //        else
    //            return x;
    //    }

    final public FValue getLeafValue(String str) {
        FValue x = getLeafValueNull(str); // leaf
        return getValueTail(str, x);
    }

    final public FValue getRootValue(String str) {
        FValue x = getRootValueNull(str); // root
        return getValueTail(str, x);
    }

    final public FValue getValue(VarRef vr) {
        FValue x = getValueNull(vr);
        return getValueTail(vr, x);
    }

    final public FValue getValue(FunctionalRef vr) {
        FValue x = getValueNull(vr);
        return getValueTail(vr, x);
    }

    private FValue getValueTail(Object str, FValue x) {
        if (x == null) {
            return error(errorMsg("Missing value: ", str, " in environment:\n", this));
        } else {
            return x;
        }
    }

    public FValue getLeafValueNull(String s) {
        FValue v = getValueRaw(s);
        return getValueNullTail(s, v);
    }

    public FValue getRootValueNull(String s) {
        FValue v = getValueRaw(s);
        return getValueNullTail(s, v);
    }

    static private BASet<String> missedNames = new BASet<String>(com.sun.fortress.useful.StringHashComparer.V);

    final public FValue getValueNull(VarRef vr) {
        Id name = vr.getVarId();
        int l = vr.getLexicalDepth();
        return getValueNull(name, l);
    }

    final public FValue getValueNull(FunctionalRef vr) {
        IdOrOp name = vr.getNames().get(0);
        int l = vr.getLexicalDepth();
        return getValueNull(name, l);
    }

    final public FValue getValueNull(IdOrOp name, int l) throws CircularDependenceError {
        // String s = NodeUtil.nameString(name);
        String local = NodeUtil.nameSuffixString(name);
        Option<APIName> opt_api = name.getApiName();
        return getValueNullTail(name, l, local, opt_api);

    }

    final public FValue getValue(Id name, int l) throws CircularDependenceError {
        //String s = NodeUtil.nameString(name);
        String local = NodeUtil.nameSuffixString(name);
        Option<APIName> opt_api = name.getApiName();
        return getValueTail(name, l, local, opt_api);
    }

    final public FValue getValue(Op name, int l) throws CircularDependenceError {
        // String s = NodeUtil.nameString(name);
        String local = NodeUtil.nameSuffixString(name);
        Option<APIName> opt_api = name.getApiName();
        return getValueTail(name, l, local, opt_api);

    }

    private FValue getValueTail(IdOrOp name, int l, String local, Option<APIName> opt_api) throws OptionUnwrapException,
                                                                                                  CircularDependenceError {
        FValue v = getValueNullTail(name, l, local, opt_api);
        if (v != null) return v;
        return opt_api.isSome() ?
               (FValue) error(errorMsg("Missing value: ", local, " in api:\n", opt_api.unwrap())) :
               (FValue) error(errorMsg("Missing value: ", local, " in environment:\n", this));
    }

    private FValue getValueNullTail(IdOrOp name, int l, String local, Option<APIName> opt_api) throws
                                                                                               OptionUnwrapException,
                                                                                               CircularDependenceError {
        if (opt_api.isSome()) {
            if (l != TOP_LEVEL) {
                bug("Non-top-level reference to imported " + name);
            }
            APIName api = opt_api.unwrap();
            // Circular dependence etc will be signalled in API.
            Environment api_e = getApi(api);
            return api_e.getRootValueNull(local); // root
        } else {
            FValue v = getValueRaw(local, l);
            return getValueNullTail(local, v);
        }
    }

    private FValue getValueNullTail(String s, FValue v) throws CircularDependenceError {
        if (v == null) return v;
        if (v instanceof IndirectionCell) {
            try {
                FValue ov = v;
                v = ((IndirectionCell) v).getValueNull();
                if (ov instanceof LazilyEvaluatedCell) {
                    putValueRaw(s, v);
                }
            }
            catch (CircularDependenceError ce) {
                ce.addParticipant(s);
                throw ce;
            }
        }
        return v;
    }

    public FType getVarTypeNull(String str) {
        FValue v = getValueRaw(str);
        if (v == null) return null;
        if (v instanceof ReferenceCell) {
            return ((ReferenceCell) v).getType();
        }
        return null;
    }

    public Environment installPrimitives() {
        Primitives.installPrimitives(this);
        return this;
    }

    public void putType(Id name, FType x) {
        putType(NodeUtil.nameString(name), x);
    }


    final public void putValue(FValue f1, FValue f2) {
        putValue(string(f1), f2);
    }

    public void putValue(Id name, FValue x) {
        putValue(NodeUtil.nameString(name), x);
    }

    // TODO this needs to be level-disambiguated
    public FunctionClosure getClosure(String s) {
        return (FunctionClosure) getLeafValue(s);
    }

    public void putVariable(String str, FValue f2) {
        putValue(str, new ReferenceCell(FTypeTop.ONLY, f2));
    }

    public void putVariablePlaceholder(String str) {
        putValue(str, new ReferenceCell());
    }

    public void putValueRaw(String str, FValue f2, FType ft) {
        putValueRaw(str, new ReferenceCell(ft, f2));
    }

    public void putVariable(String str, FValue f2, FType ft) {
        putValue(str, new ReferenceCell(ft, f2));
    }

    public void putVariable(String str, FType ft) {
        putValue(str, new ReferenceCell(ft));
    }

    public Environment genericLeafEnvHack(Environment genericEnv) {
        return extend(genericEnv);
    }

    public void putBool(String str, Boolean f2) {
        putBoolRaw(str, f2);
        putValueRaw(str, FBool.make(f2));
    }

    public void putNat(String str, Number f2) {
        putNatRaw(str, f2);
        putValueRaw(str, FInt.make(f2.intValue()));
    }

    public void putInt(String str, Number f2) {
        putIntRaw(str, f2);
        putValueRaw(str, FInt.make(f2.intValue()));
    }

    public void putType(String str, FType f2) {
        putTypeRaw(str, f2);
    }


    public void putValue(String str, FValue f2) {
        if (f2 instanceof Fcn) putFunction(str, (Fcn) f2, "Var/value", false, false);
        else
            // var_env = putNoShadow(var_env, str, f2, "Var/value");
            putNoShadow(str, f2, "Var/value");

    }

    public void putValueNoShadowFn(String str, FValue f2) {
        if (f2 instanceof Fcn) putFunction(str, (Fcn) f2, "Var/value", true, false);
        else
            // var_env = putNoShadow(var_env, str, f2, "Var/value");
            putNoShadow(str, f2, "Var/value");
    }

    /**
     * @param str
     * @param f2
     */
    public void putFunctionalMethodInstance(String str, FValue f2) {
        if (f2 instanceof Fcn) {
            putFunction(str, (Fcn) f2, "Var/value", true, true);
        } else error(str + " must be a functional method instance ");
    }


    public void noteName(String s) {
        if (namesPutCount == 0) namesPut = new String[2];
        else if (namesPutCount == namesPut.length) {
            String[] next = new String[namesPutCount * 2];
            System.arraycopy(namesPut, 0, next, 0, namesPut.length);
            namesPut = next;
        }
        namesPut[namesPutCount++] = s;
    }

    public Iterator<String> iterator() {
        if (namesPutCount > 0) return new StringArrayIterator(namesPut, namesPutCount);
        return Collections.<String>emptySet().iterator();
    }

    // Slightly wrong -- returns all, not just the most recently bound.

    public HasAt getAt() {
        return within;
    }

    /**
     * Level-tagged version of getTypeNull
     *
     * @param name
     * @param level
     * @return
     */
    public FType getTypeNull(String name, int level) {
        Environment e = toContainingObjectEnv(this, level);
        return e.getTypeNull(name);
    }

    /**
     * Level-tagged version of getValueRaw
     *
     * @param s
     * @param level
     * @return
     */
    public FValue getValueRaw(String s, int level) {
        Environment e = toContainingObjectEnv(this, level);
        return e.getValueRaw(s);
    }

    public Environment getApiNull(String apiName) {
        return null;
    }

    public Iterable<String> youngestFrame() {
        return this;
    }

    public Environment getTopLevel() {
        return this;
    }

    /**
     * Retrieves the environment in which a name was defined,
     * if it was a surrounding object.  Currently accomplished
     * by chaining up the list of $self/$parent entries.
     */

    static Environment toContainingObjectEnv(Environment e, int negative_object_depth) {
        if (-negative_object_depth > 1) {
            // -1 means "self" -- contained in "e", not here.
            // This assumes $self can be found in the current environment.
            // Next statement maps -1 to zero, -2 to one, -3 to two, etc.
            negative_object_depth = -1 - negative_object_depth;
            FObject obj = (FObject) e.getLeafValue(WellKnownNames.secretSelfName);
            e = obj.getSelfEnv();
            while (negative_object_depth > 0) {
                negative_object_depth--;
                obj = (FObject) e.getLeafValue(WellKnownNames.secretParentName);
                e = obj.getSelfEnv();
            }
        } else if (negative_object_depth == 0) {
            e = e.getTopLevel();
        } else if (negative_object_depth < 0) {
            // True only for MIN_VALUE
            return e;
        }
        return e;
    }


}
