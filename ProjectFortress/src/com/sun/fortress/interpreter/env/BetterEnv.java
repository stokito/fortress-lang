/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.useful.BATreeNode;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.StringHashComparer;
import com.sun.fortress.useful.Visitor2;

import java.io.IOException;
import java.util.Comparator;


public class BetterEnv extends BaseEnv {

    private BATreeNode<String, FType> type_env;
    private BATreeNode<String, Number> nat_env;
    private BATreeNode<String, Number> int_env;
    private BATreeNode<String, FValue> var_env;
    private BATreeNode<String, Boolean> bool_env;
    private BATreeNode<String, Environment> api_env;

    /**
     * (Lexical) ancestor environment
     */
    protected Environment parent;

    private final static Comparator<String> comparator = StringHashComparer.V;

    public void visit(Visitor2<String, FType> vt,
                      Visitor2<String, Number> vn,
                      Visitor2<String, Number> vi,
                      Visitor2<String, FValue> vv,
                      Visitor2<String, Boolean> vb) {
        if (type_env != null && vt != null) type_env.visit(vt);
        if (nat_env != null && vn != null) nat_env.visit(vn);
        if (int_env != null && vi != null) int_env.visit(vi);
        if (var_env != null && vv != null) var_env.visit(vv);
        if (bool_env != null && vb != null) bool_env.visit(vb);

    }

    public void visit(Visitor2<String, Object> v) {
        if (type_env != null) type_env.visit(v);
        if (nat_env != null) nat_env.visit(v);
        if (int_env != null) int_env.visit(v);
        if (var_env != null) var_env.visit(v);
        if (bool_env != null) bool_env.visit(v);

    }

    private static BetterEnv empty() {
        return new BetterEnv("Empty");
    }

    public static BetterEnv empty(String s) {
        return new BetterEnv(s);
    }

    public static BetterEnv blessedEmpty() {
        BetterEnv r = empty();
        r.bless();
        return r;
    }

    public static Environment primitive() {
        return empty().installPrimitives();
    }

    public static Environment primitive(String x) {
        return (new BetterEnv(x)).installPrimitives();
    }

    public static Environment primitive(HasAt x) {
        return (new BetterEnv(x)).installPrimitives();
    }

    private BetterEnv(String s) {
        this(new HasAt.FromString(s));
    }

    // Used by Trait and Object to construct declared-members environments.
    public BetterEnv(HasAt s) {
        within = s;
    }

    // Only used in testing (EvaluatorJUTest).
    public BetterEnv(BetterEnv existing, String s) {
        this(existing, new HasAt.FromString(s));
    }

    protected BetterEnv(BetterEnv containing, HasAt x) {
        this(containing);
        within = x;
    }

    private BetterEnv(BetterEnv existing) {
        if (!existing.getBlessed()) bug(within,
                                        existing,
                                        "Internal error, attempt to copy environment still under construction");
        type_env = existing.type_env;
        nat_env = existing.nat_env;
        int_env = existing.int_env;
        var_env = existing.var_env;
        bool_env = existing.bool_env;
        api_env = existing.api_env;
        parent = existing;
    }

    /**
     * Creates a new type environment starting with existing (the nominal parent)
     * but with additions added.
     *
     * @param existing
     * @param additions
     */
    protected BetterEnv(BetterEnv existing, BetterEnv additions) {
        if (!existing.getBlessed() || !additions.getBlessed()) bug(within,
                                                                   existing,
                                                                   "Internal error, attempt to copy environment still under construction");
        augment(existing, additions);
        parent = existing;
        bless();
    }

    protected BetterEnv(BetterEnv existing, Environment additions) {
        this(existing);
        if (!existing.getBlessed() || !additions.getBlessed()) bug(within,
                                                                   existing,
                                                                   "Internal error, attempt to copy environment still under construction");

        if (additions instanceof BetterEnv) {
            augment(existing, (BetterEnv) additions);
        } else {
            augment(additions);
        }


        bless();
    }

    private void augment(BetterEnv existing, BetterEnv additions) {
        type_env = augment(existing.type_env, additions.type_env);
        nat_env = augment(existing.nat_env, additions.nat_env);
        int_env = augment(existing.int_env, additions.int_env);
        var_env = augment(existing.var_env, additions.var_env);
        bool_env = augment(existing.bool_env, additions.bool_env);
        api_env = augment(existing.api_env, additions.api_env);
    }

    private static <Result> BATreeNode<String, Result> augment(BATreeNode<String, Result> original,
                                                               BATreeNode<String, Result> toBeAdded) {
        if (original == null) return toBeAdded;
        if (toBeAdded == null) return original;
        return augmentRecursive(original, toBeAdded);
    }

    private static <Result> BATreeNode<String, Result> augmentRecursive(BATreeNode<String, Result> original,
                                                                        BATreeNode<String, Result> toBeAdded) {

        // I started to write a foreach method and a visitor
        // abstract class, and it was just too much trouble.

        BATreeNode<String, Result> l = toBeAdded.getLeft();
        BATreeNode<String, Result> r = toBeAdded.getRight();

        if (l != null) original = augmentRecursive(original, l);
        if (r != null) original = augmentRecursive(original, r);
        return original.add(toBeAdded.getKey(), toBeAdded.getValue(), comparator);
    }

    private <Result> Result get(BATreeNode<String, Result> table, String index) {
        Result r = null;
        if (table != null) {
            BATreeNode<String, Result> node = table.getObject(index, comparator);
            if (node != null) r = node.getValue();
        }
        return r;
    }

    private <Result> BATreeNode<String, Result> put(BATreeNode<String, Result> table, String index, Result value) {
        if (table == null) {
            return new BATreeNode<String, Result>(index, value);
        } else {
            BATreeNode<String, Result> new_table = table.add(index, value, comparator);
            return new_table;
        }
    }


    public Environment extend(BetterEnv additions) {
        return new BetterEnv(this, additions);
    }

    public Environment extend(Environment additions) {
        if (additions instanceof BetterEnv) {
            return new BetterEnv(this, (BetterEnv) additions);
        }
        return new BetterEnv(this, additions);
    }

    public Environment extendAt(HasAt x) {
        return new BetterEnv(this, x);
    }

    public Environment extend() {
        return new BetterEnv(this, this.getAt());
    }

    public Appendable dump(Appendable a) throws IOException {
        if (within != null) {
            a.append(within.at());
            a.append("\n");
        } else {
            a.append("Not within anything.\n");
        }
        if (verboseDump) {
            dumpOne(a, type_env, "Types:\n  ");
            dumpOne(a, nat_env, "\nNats:\n  ");
            dumpOne(a, int_env, "\nInts:\n  ");
            dumpOne(a, var_env, "\nVars:\n  ");
            dumpOne(a, bool_env, "\nBools:\n  ");
            // Add api_env a little later.
            a.append("\n");
        } else if (getParent() != null) {
            getParent().dump(a);
        }
        return a;
    }

    private <Result> void dumpOne(Appendable a, BATreeNode<String, Result> m, String s) throws IOException {
        if (m != null) {
            a.append(s);
            if (a instanceof StringBuffer) {
                m.recursiveToStringBuffer((StringBuffer) a, false, "\n  ");
            } else {
                StringBuffer sb = m.recursiveToStringBuffer(new StringBuffer(), false, "\n  ");
                a.append(sb.toString());
            }
        }

    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            dump(sb);
        }
        catch (java.io.IOException e) {
        }
        return sb.toString();
    }

    public Boolean getBoolNull(String str) {
        Boolean v = get(bool_env, str);
        return v;
    }

    public Number getNatNull(String str) {
        Number v = get(nat_env, str);
        return v;
    }

    public Number getIntNull(String str) {
        Number v = get(int_env, str);
        return v;
    }

    public FType getTypeNull(String name) {
        return get(type_env, name);
    }

    /**
     * Completely uninterpreted value, only to be used when
     * initializing from imports.
     *
     * @param s
     */
    public FValue getValueRaw(String s) {
        FValue v = get(var_env, s);
        return v;
    }

    public void putTypeRaw(String str, FType f2) {
        type_env = put(type_env, str, f2);
    }

    public void putBoolRaw(String str, Boolean f2) {
        bool_env = put(bool_env, str, f2);
    }

    public void putNatRaw(String str, Number f2) {
        nat_env = put(nat_env, str, f2);
    }

    public void putIntRaw(String str, Number f2) {
        int_env = put(int_env, str, f2);
    }

    public void putValueRaw(String str, FValue f2) {
        var_env = put(var_env, str, f2);
    }

    public Environment getApiNull(String apiName) {
        Environment e = get(api_env, apiName);
        return e;
    }

    public void putApi(String apiName, Environment env) {
        api_env = put(api_env, apiName, env);
    }


    public void removeType(String s) {
        if (type_env == null) return;
        type_env = type_env.delete(s, comparator);
    }

    public void removeVar(String s) {
        if (var_env == null) return;
        var_env = var_env.delete(s, comparator);
    }

    public Environment getTopLevel() {
        return bug("This should have been an environment with a Top Level");
    }

    public BATreeNode<String, Number> getNat_env() {
        return nat_env;
    }

    public Environment getParent() {
        return parent;
    }


}
