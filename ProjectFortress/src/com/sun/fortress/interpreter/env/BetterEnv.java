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

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.sun.fortress.exceptions.CircularDependenceError;
import com.sun.fortress.exceptions.RedefinitionError;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Declaration;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Primitives;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.BATreeNode;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.StringArrayIterator;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Visitor2;


public final class BetterEnv extends BaseEnv implements Iterable<String> 
{

    private BATreeNode<String, FType> type_env;
    private BATreeNode<String, Number> nat_env;
    private BATreeNode<String, Number> int_env;
    private BATreeNode<String, FValue> var_env;
    private BATreeNode<String, Boolean> bool_env;
    private BATreeNode<String, SApi> api_env;
    private BATreeNode<String, SComponent> cmp_env;
    private BATreeNode<String, Declaration> dcl_env;


    static public boolean verboseDump = false;

    /** Names noted for possible future overloading */
    String[] namesPut;
    int namesPutCount;
    boolean blessed; /* until blessed, cannot be copied */
    private boolean topLevel;

    /** Where created */
    HasAt within;

    /** (Lexical) ancestor environment */
    BetterEnv parent;

    private final static Comparator<String> comparator = StringComparer.V;

    public void visit(Visitor2<String, FType> vt,
                      Visitor2<String, Number> vn,
                      Visitor2<String, Number> vi,
                      Visitor2<String, FValue> vv,
                      Visitor2<String, Boolean> vb) {
        if (type_env != null && vt != null) type_env.visit(vt);
        if (nat_env != null && vn != null) nat_env.visit(vn);
        if (int_env != null && vi != null) int_env.visit(vi);
        if (var_env != null && vv != null) var_env.visit(vv);
        if (bool_env != null && bool_env != null) bool_env.visit(vb);

    }

    public void visit(Visitor2<String, Object> v) {
        if (type_env != null) type_env.visit(v);
        if (nat_env != null) nat_env.visit(v);
        if (int_env != null) int_env.visit(v);
        if (var_env != null) var_env.visit(v);
        if (bool_env != null) bool_env.visit(v);

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

    public static BetterEnv empty() {
        return new BetterEnv("Empty");
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

    // Used by Trait and Object to constructor declared-members environments.
    public BetterEnv(HasAt s) {
        within = s;
    }

    // Only used in testing (EvaluatorJUTest).
    public BetterEnv(BetterEnv existing, String s) {
        this(existing, new HasAt.FromString(s));
    }

    private BetterEnv(BetterEnv containing, HasAt x) {
        this(containing);
        within = x;
    }

    private BetterEnv(BetterEnv existing) {
        if (! existing.blessed)
            bug(within,existing,"Internal error, attempt to copy environment still under construction");
        type_env = existing.type_env;
        nat_env = existing.nat_env;
        int_env = existing.int_env;
        var_env = existing.var_env;
        bool_env = existing.bool_env;
        api_env = existing.api_env;
        cmp_env = existing.cmp_env;
        dcl_env = existing.dcl_env;
        parent = existing;
    }

    /**
     * Creates a new type environment starting with existing (the nominal parent)
     * but with additions added.
     * 
     * @param existing
     * @param additions
     */
    private BetterEnv(BetterEnv existing, BetterEnv additions) {
        if ( !existing.getBlessed() || !additions.getBlessed() )
            bug(within,existing,"Internal error, attempt to copy environment still under construction");
        augment(existing, additions);
        parent = existing;
        bless();
    }
    
    private BetterEnv(BetterEnv existing, Environment additions) {
        this(existing);
        if ( !existing.getBlessed() || !additions.getBlessed() )
            bug(within,existing,"Internal error, attempt to copy environment still under construction");
        
        if (additions instanceof BetterEnv) {
            augment(existing, (BetterEnv) additions);
        } else {
            augment(this, additions);
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
        cmp_env = augment(existing.cmp_env, additions.cmp_env);
        dcl_env = augment(existing.dcl_env, additions.dcl_env);
    }
    
    private static void augment(final Environment existing, final Environment additions) {
        final Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                existing.putType(s, o);
            }
        };
        final Visitor2<String, Number> vn = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                existing.putNat(s, o);
            }
        };
        final Visitor2<String, Number> vi = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                existing.putInt(s, o);
            }
        };
        final Visitor2<String, FValue> vv = new Visitor2<String, FValue>() {
            public void visit(String s, FValue o) {
              
                    FType ft = additions.getVarTypeNull(s);
                    if (ft != null)
                       existing.putValueUnconditionally(s, o, ft);
                    else 
                       existing.putValueUnconditionally(s, o);
            }
        };
        final Visitor2<String, Boolean> vb = new Visitor2<String, Boolean>() {
            public void visit(String s, Boolean o) {
                existing.putBool(s, o);
            }
        };
        
        existing.visit(vt,vn,vi,vv,vb);
        
    }

   private static <Result> BATreeNode<String, Result> augment(
            BATreeNode<String, Result> original,
            BATreeNode<String, Result> toBeAdded) {
        if (original == null) return toBeAdded;
        if (toBeAdded == null) return original;
        return augmentRecursive(original, toBeAdded);
    }

    private static <Result> BATreeNode<String, Result> augmentRecursive(
            BATreeNode<String, Result> original,
            BATreeNode<String, Result> toBeAdded) {

        // I started to write a foreach method and a visitor
        // abstract class, and it was just too much trouble.

        BATreeNode<String, Result> l = toBeAdded.getLeft();
        BATreeNode<String, Result> r = toBeAdded.getRight();

        if (l != null)
            original = augmentRecursive(original, l);
        if (r != null)
            original = augmentRecursive(original, r);
        return original.add(toBeAdded.getKey(), toBeAdded.getValue(), comparator);
    }

     private <Result> Result get(BATreeNode<String, Result> table, String index) {
        Result r = null;
        if (table != null) {
            BATreeNode<String,Result> node = table.getObject(index, comparator);
        if (node != null)
            r = node.getValue();
        }
        return r;
    }

    private <Result> boolean has(BATreeNode<String, Result> table, String index) {
        if (table == null)
            return false;
        BATreeNode<String,Result> node = table.getObject(index, comparator);
        if (node == null)
            return false;
        return true;
    }

    private <Result> BATreeNode<String, Result> put(BATreeNode<String, Result> table, String index, Result value) {
        if (table == null) {
            return new BATreeNode<String, Result> (index, value);
        } else {
            BATreeNode<String, Result> new_table = table.add(index, value, comparator);
            return new_table;
        }
    }

     
 
    private <Result> BATreeNode<String, Result> putUnconditionally(BATreeNode<String, Result> table, String index, Result value, String what) {
        if (table == null) {
            return new BATreeNode<String, Result> (index, value);
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
        if (within!=null) {
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
            dumpOne(a, api_env, "\nApis:\n  ");
            dumpOne(a, cmp_env, "\nComponents:\n  ");
            dumpOne(a, dcl_env, "\nDeclarations:\n  ");
            a.append("\n");
        } else if (parent != null) {
            parent.dump(a);
        }
        return a;
    }

    private <Result> void dumpOne(Appendable a, BATreeNode<String, Result> m, String s) throws IOException {
        if (m != null) {
            a.append(s);
            if (a instanceof StringBuffer) {
                m.recursiveToStringBuffer((StringBuffer)a,false,"\n  ");
            } else {
                StringBuffer sb =
                    m.recursiveToStringBuffer(new StringBuffer(),false,"\n  ");
                a.append(sb.toString());
            }
        }

    }

    public SApi getApiNull(String str) {
        SApi v = get(api_env, str);
        return v;
    }

    public Boolean getBoolNull(String str) {
        Boolean v = get(bool_env, str);
        return v;
    }

    public SComponent getComponentNull(String str) {
        SComponent v = get(cmp_env, str);
        return v;
    }

    public Declaration getDeclNull(String str) {
        Declaration v = get(dcl_env, str);
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


    public boolean hasValue(String str) {
        return has(var_env, str) || has(bool_env, str) || has(nat_env, str);
    }

    public void putApi(String s, SApi api) {
        api_env = put(api_env, s, api);
    }

 
    public void putComponent(String name, SComponent comp) {
        cmp_env = put(cmp_env, name, comp);

    }

    public void putDecl(String str, Declaration f2) {
        dcl_env = put(dcl_env, str, f2);
    }

     public void putType(String str, FType f2) {
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
    
    public void putValue(String str, FValue f2) {
        if (f2 instanceof Fcn)
            putFunction(str, (Fcn) f2, "Var/value", false, false);
        else
            // var_env = putNoShadow(var_env, str, f2, "Var/value");
            putNoShadow(str, f2, "Var/value");
        
     }

    public void putValueNoShadowFn(String str, FValue f2) {
        if (f2 instanceof Fcn)
            putFunction(str, (Fcn) f2, "Var/value", true, false);
        else
            // var_env = putNoShadow(var_env, str, f2, "Var/value");
            putNoShadow(str, f2, "Var/value");
     }
    
    /**
     *
     * @param str
     * @param f2
     */
    public void putFunctionalMethodInstance(String str, FValue f2) {
        if (f2 instanceof Fcn)
            putFunction(str, (Fcn) f2, "Var/value", true, true);
        else
            error(str + " must be a functional method instance ");
     }

    public void putValueUnconditionally(String str, FValue f2) {
        var_env = putUnconditionally(var_env, str, f2, "Var/value");
     }

 
    public Iterator<String> iterator() {
        if (var_env != null)
            return new StringArrayIterator(namesPut, namesPutCount);
        return Collections.<String>emptySet().iterator();
    }

    // Slightly wrong -- returns all, not just the most recently bound.

    public HasAt getAt() {
        return within;
    }

    public void noteName(String s) {
        if (namesPutCount == 0)
            namesPut = new String[2];
        else if (namesPutCount == namesPut.length) {
            String[] next = new String[namesPutCount*2];
            System.arraycopy(namesPut, 0, next, 0, namesPut.length);
            namesPut = next;
        }
        namesPut[namesPutCount++] = s;
    }

    public void removeType(String s) {
        if (type_env == null)
            return;
        type_env = type_env.delete(s, comparator);
    }

    public void removeVar(String s) {
        if (var_env == null)
            return;
        var_env = var_env.delete(s, comparator);
    }

    public Iterable<String> youngestFrame() {
        // TODO Auto-generated method stub
        return this;
    }

}
