/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.Pair;
import edu.rice.cs.plt.tuple.Option;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * A NativeApp indicates that an action is implemented natively; the
 * type itself is abstract.
 * <p/>
 * Programmers writing a NativeApp should implement getArity and ApplyToArgs.
 * <p/>
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
     * well.
     */
    public abstract int getArity();

    /**
     * Set the delegate to a, after performing an arity check and
     * making sure the return type is defined.  Override this only if
     * defining a function whose arity is not fixed, or you wish to
     * perform additional sanity checks on the Fortress-side function
     * definition.
     */
    protected void init(Applicable app, boolean isFunctionalMethod) {
        if (this.a != null) {
            bug("Duplicate NativeApp.init call.");
        }
        this.a = app;
        int aty = NodeUtil.getParams(app).size();
        // Dock functional methods by 1 for the self parameter.
        // This lets us treat methods and functional methods identically
        // on the native Java side, which simplifies life immensely.
        if (isFunctionalMethod) aty--;
        if (aty != getArity()) {
            error(app, "Arity of type " + aty + " does not match native arity " + getArity());
        }
        if (NodeUtil.getReturnType(app) == null || NodeUtil.getReturnType(app).isNone()) {
            error(app, "Please specify a Fortress return type.");
        }
    }

    /* Except for getBody() these just delegate to a. */
    public Expr getBody() {
        return null;
    }

    public FnHeader getHeader() {
        return a.getHeader();
    }

    public List<Param> getParams() {
        return a.getHeader().getParams();
    }

    public Option<Type> getReturnType() {
        return a.getHeader().getReturnType();
    }

    public List<StaticParam> getStaticParams() {
        return a.getHeader().getStaticParams();
    }

    public IdOrOpOrAnonymousName getName() {
        return a.getHeader().getName();
    }

    public Option<WhereClause> getWhereClause() {
        return a.getHeader().getWhereClause();
    }

    public String at() {
        return a.at();
    }

    public String stringName() {
        return a.stringName();
    }

    public String toString() {
        return (a.stringName() + "(native " + this.getClass().getSimpleName() + ")");
    }

    public <RetType> RetType accept(NodeVisitor<RetType> visitor) {
        return visitor.forId(NodeFactory.makeId(NodeFactory.makeSpan(""), ""));
    }

    public void accept(NodeVisitor_void visitor) {
    }

    public int generateHashCode() {
        return 0;
    }

    public String serialize() {
        return bug(this, errorMsg("Cannot serialize NativeApp ", this));
    }

    public void serialize(java.io.Writer writer) {
        bug(this, errorMsg("Cannot serialize NativeApp ", this));
    }

    public void walk(TreeWalker w) {
        bug(this, errorMsg("Cannot walk NativeApp ", this));
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
    public static Applicable checkAndLoadNative(Applicable defn, boolean isFunctionalMethod) {
        Option<Expr> optBody = NodeUtil.getBody(defn);
        if (optBody.isNone()) return defn;
        Expr body = optBody.unwrap();
        Expr fn;
        Expr arg;
        if (body instanceof Juxt && ((Juxt) body).isTight()) {
            List<Expr> juxts = ((Juxt) body).getExprs();
            if (juxts.size() != 2) return defn;
            fn = juxts.get(0);
            arg = juxts.get(1);
        } else if (body instanceof MathPrimary) {
            MathPrimary mp = (MathPrimary) body;
            List<MathItem> args = mp.getRest();
            if (args.size() != 1) return defn;
            fn = mp.getFront();
            MathItem mi = args.get(0);
            if (mi instanceof ExprMI) arg = ((ExprMI) mi).getExpr();
            else return defn;
        } else // (!(body instanceof TightJuxt || body instanceof MathPrimary))
            return defn;
        if (!(fn instanceof VarRef)) return defn;
        if (!(arg instanceof StringLiteralExpr)) return defn;
        Id name = ((VarRef) fn).getVarId();
        if (!name.getText().equals("builtinPrimitive")) return defn;
        String str = ((StringLiteralExpr) arg).getText();
        Pair<String, Applicable> key = new Pair<String, Applicable>(str, defn);
        synchronized (cache) {
            NativeApp res = cache.get(key);
            if (res != null) return res;
            try {
                // System.err.println("Loading primitive class "+str);
                Class nativeAct = Class.forName(str);
                res = (NativeApp) nativeAct.newInstance();
                res.init(defn, isFunctionalMethod);
                cache.put(key, res);
                return res;
            }
            catch (java.lang.ClassNotFoundException x) {
                return bug(defn, "Native class " + str + " not found.", x);
            }
            catch (java.lang.InstantiationException x) {
                return bug(defn, "Native class " + str + " has no nullary constructor.", x);
            }
            catch (java.lang.IllegalAccessException x) {
                return bug(defn, "Native class " + str + " cannot be accessed.", x);
            }
            catch (java.lang.ClassCastException x) {
                return bug(defn, "Native class " + str + " is not a NativeApp.", x);
            }
        }
    }

    public static Applicable checkAndLoadNative(Applicable defn) {
        return checkAndLoadNative(defn, false);
    }

    static public void reset() {
        cache = new Hashtable<Pair<String, Applicable>, NativeApp>();
        NativeConstructor.unregisterAllConstructors();
    }

    static Map<Pair<String, Applicable>, NativeApp> cache;
}
