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

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.nodes_util.NodeUtil;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.StringLiteral;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes.VarRefExpr;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.Useful;


/**
 * A NativeApp indicates that an action is implemented natively; the
 * type itself is abstract.
 *
 * We might want some way to sanity-check the expected type of a
 * native thing against the actual type declared in the applicable
 * object passed in.  Right now we just check arities.
 */
public abstract class NativeApp implements Applicable {
    protected Applicable a;

    /**
     * A NativeApp has a fixed arity by default.  This defines that
     * fixed arity.  This is used in the checking performed by
     * setParams and by applyInner.  If you need a multi-arity
     * function you will need to override those two methods as
     * well. */
    public abstract int getArity();

    /**
     * Set the delegate to a, after performing an arity check and
     * making sure the return type is defined.  Override this only if
     * defining a function whose arity is not fixed, or you wish to
     * perform additional sanity checks on the Fortress-side function
     * definition.
     */
    protected void init(Applicable a) {
        if (this.a!=null) {
            throw new InterpreterError("Duplicate NativeApp.init call.");
        }
        this.a = a;
        int aty = a.getParams().size();
        if (aty != getArity()) {
            throw new ProgramError(a,"Arity of type "+aty
                                   +" does not match native arity "+getArity());
        }
        if (a.getReturnType()==null || !a.getReturnType().isPresent()) {
            throw new ProgramError(a,"Please specify a Fortress return type.");
        }
    }

    /* Except for getBody() these just delegate to a. */
    public Expr getBody() { return null; }
    public List<Param> getParams() { return a.getParams(); }
    public Option<TypeRef> getReturnType() { return a.getReturnType(); }
    public Option<List<StaticParam>> getStaticParams() {
        return a.getStaticParams();
    }
    public FnName getFnName() { return a.getFnName(); }
    public List<WhereClause> getWhere() { return a.getWhere(); }
    public String at() { return NodeUtil.getAt(a); }
    public String stringName() { return NodeUtil.stringName(a); }

    public String toString() {
        return (NodeUtil.stringName(a)+"(native " + this.getClass().getSimpleName()+")");
    }

    /**
     * Actually apply the native function to the passed-in arguments.
     * Called by Closure.applyInner.  Arity and type checking have already
     * occurred and can be assumed.  For fixed-arity primitives this
     * method ought to simply unpack the arguments and chain to
     * another method defined in the subclass.  RuntimeExceptions and
     * Errors are caught and wrapped by applyInner, which also adds
     * location information to ProgramErrors.
     */
    public abstract FValue applyToArgs(List<FValue> args);

    /* Does the passed-in method body represent a native function?  If
     * so, return the corresponding native action.
     * Otherwise, return null.
     *
     * Right now this does a naive pattern-match for the expression:
     *
     * builtinPrimitive("name.of.java.class.as.literal.String")
     *
     * Using Java reflection we load and construct an instance of this
     * class, failing somewhat-gracefully if any part of the attempt
     * fails.  The error checking is gratuitously detailed so the
     * library hacker can sort out what is going on when things break.
     */
    public static Applicable checkAndLoadNative(Applicable defn) {
        Option<Expr> optBody = NodeUtil.getBody(defn);
        if (!optBody.isPresent()) return defn;
        else if (!(optBody.getVal() instanceof TightJuxt)) return defn;
        Expr body = optBody.getVal();
        List<Expr> juxts = ((TightJuxt)body).getExprs();
        if (juxts.size()!=2) return defn;
        Expr fn = juxts.get(0);
        Expr arg = juxts.get(1);
        if (!(fn instanceof VarRefExpr &&
              arg instanceof StringLiteral)) return defn;
        VarRefExpr func = (VarRefExpr)fn;
        if (!func.getVar().getName().equals("builtinPrimitive")) return defn;
        String str = ((StringLiteral)arg).getText();
        try {
            // System.err.println("Loading primitive class "+str);
            Class nativeAct = Class.forName(str);
            NativeApp res = (NativeApp)nativeAct.newInstance();
            res.init(defn);
            return res;
        } catch (java.lang.ClassNotFoundException x) {
            throw new ProgramError(defn,"Native class "+str
                                   +" not found.",x);
        } catch (java.lang.InstantiationException x) {
            throw new ProgramError(defn,"Native class "+str
                                   +" has no nullary constructor.",x);
        } catch (java.lang.IllegalAccessException x) {
            throw new ProgramError(defn,"Native class "+str
                                   +" cannot be accessed.",x);
        } catch (java.lang.ClassCastException x) {
            throw new ProgramError(defn,"Native class "+str
                                   +" is not a NativeApp.",x);
        }
    }
}
