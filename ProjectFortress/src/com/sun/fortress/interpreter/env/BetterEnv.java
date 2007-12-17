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

package com.sun.fortress.interpreter.env;

import com.sun.fortress.nodes_util.NodeUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.sun.fortress.interpreter.evaluator.CircularDependenceError;
import com.sun.fortress.interpreter.evaluator.CommonEnv;
import com.sun.fortress.interpreter.evaluator.Declaration;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Primitives;
import com.sun.fortress.interpreter.evaluator.RedefinitionError;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.useful.BATreeNode;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Visitor2;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;


public final class BetterEnv extends CommonEnv implements Environment, Iterable<String>  {

    private BATreeNode<String, FType> type_env;
    private BATreeNode<String, Number> nat_env;
    private BATreeNode<String, Number> int_env;
    private BATreeNode<String, FValue> var_env;
    private BATreeNode<String, Boolean> bool_env;
    private BATreeNode<String, SApi> api_env;
    private BATreeNode<String, SComponent> cmp_env;
    private BATreeNode<String, Declaration> dcl_env;


    static public boolean verboseDump = true;

    /** Names noted for possible future overloading */
    String[] namesPut;
    int namesPutCount;
    boolean blessed; /* until blessed, cannot be copied */
    private boolean topLevel;

    /** Where created */
    HasAt within;

    /** (Lexical) ancestor environment */
    BetterEnv parent;

    static boolean debug = false;

    private final static Comparator<String> comparator = StringComparer.V;

    public BetterEnv installPrimitives() {
        Primitives.installPrimitives(this);
        return this;
    }

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

    public static BetterEnv primitive() {
        return empty().installPrimitives();
    }

    public static BetterEnv primitive(String x) {
        return (new BetterEnv(x)).installPrimitives();
    }

    public static BetterEnv primitive(HasAt x) {
        return (new BetterEnv(x)).installPrimitives();
    }

    public BetterEnv(String s) {
        this(new HasAt.FromString(s));
    }

    public BetterEnv(HasAt s) {
        within = s;
    }

    public BetterEnv(BetterEnv existing, String s) {
        this(existing, new HasAt.FromString(s));
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

    public BetterEnv(BetterEnv existing, BetterEnv additions) {
        if ( !existing.blessed || !additions.blessed )
            bug(within,existing,"Internal error, attempt to copy environment still under construction");
        type_env = augment(existing.type_env, additions.type_env);
        nat_env = augment(existing.nat_env, additions.nat_env);
        int_env = augment(existing.int_env, additions.int_env);
        var_env = augment(existing.var_env, additions.var_env);
        bool_env = augment(existing.bool_env, additions.bool_env);
        api_env = augment(existing.api_env, additions.api_env);
        cmp_env = augment(existing.cmp_env, additions.cmp_env);
        dcl_env = augment(existing.dcl_env, additions.dcl_env);
        parent = existing;
        bless();
    }

    public BetterEnv(BetterEnv containing, HasAt x) {
        this(containing);
        within = x;
    }

    static <Result> BATreeNode<String, Result> augment(
            BATreeNode<String, Result> original,
            BATreeNode<String, Result> toBeAdded) {
        if (original == null) return toBeAdded;
        if (toBeAdded == null) return original;
        return augmentRecursive(original, toBeAdded);
    }

    static <Result> BATreeNode<String, Result> augmentRecursive(
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

    static public String string(FValue f1) {
        return ((FString) f1).getString();
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

    private <Result> BATreeNode<String, Result> put(BATreeNode<String, Result> table, String index, Result value, String what) {
        if (table == null) {
            return new BATreeNode<String, Result> (index, value);
        } else {
            BATreeNode<String, Result> new_table = table.add(index, value, comparator);
//            if (new_table.getWeight() == table.getWeight()) {
//                BATreeNode<String, Result> original = table.getObject(index, comparator);
//                if (original == null) {
//                    bug("Duplicate entry in table, but not in table.");
//                }
//                throw new RedefinitionError(what, index, original.getValue(), value);
//            }
            return new_table;
        }
    }

    private <Result> BATreeNode<String, Result> putNoShadow(BATreeNode<String, Result> table, String index, Result value, String what) {
        if (table == null) {
            return new BATreeNode<String, Result> (index, value);
        } else {
            BATreeNode<String, Result> new_table = table.add(index, value, comparator);
            if (new_table.getWeight() == table.getWeight()) {
                BATreeNode<String, Result> original = table.getObject(index, comparator);
                if (original == null) {
                    bug("Duplicate entry in table, but not in table.");
                }
                Result fvo = original.getValue();
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
                    } else {
                        ic.storeValue((FValue) value);
                        return table;
                    }
                }
                throw new RedefinitionError(what, index, fvo, value);
            }
            return new_table;
        }
    }

    private BATreeNode<String, FValue> putFunction(BATreeNode<String, FValue> table, String index, Fcn value, String what, boolean shadowIfDifferent, boolean overloadIsOK) {
        if (table == null) {
            noteName(index);
            return new BATreeNode<String, FValue> (index, value);
        } else {
            BATreeNode<String, FValue> new_table = table.add(index, value, comparator);
            if (new_table.getWeight() == table.getWeight()) {
                BATreeNode<String, FValue> original = table.getObject(index, comparator);
                if (original == null) {
                    bug("Duplicate entry in table, but not in table.");
                }
                FValue fvo = original.getValue();

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
                    } else if (! ic.isInitialized()) {
                        ic.storeValue((FValue) value);
                        return table;
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

                if (!(fvo instanceof Fcn))
                    System.err.println("Eek!");
                Fcn fcn_fvo = (Fcn) fvo;
                if (! shadowIfDifferent && // true for functional methods
                    fcn_fvo.getWithin() != value.getWithin() &&
                    ! (fcn_fvo.getWithin().isTopLevel() &&  value.getWithin().isTopLevel()) // for imports from another api
                    ) {
                    /*
                     * If defined in a different environment, shadow
                     * instead of overloading.
                     */
                    noteName(index);
                    return new_table;
                }

                /*
                 * Lots of overloading combinations
                 */
                if (fvo instanceof SingleFcn) {
                    SingleFcn gm = (SingleFcn)fvo;
                    OverloadedFunction gms = new OverloadedFunction(gm.getFnName(), this);
                    gms.addOverload(gm);
                     if (value instanceof SingleFcn) {
                         gms.addOverload((SingleFcn) value, overloadIsOK);
                    } else if (value instanceof OverloadedFunction) {
                        gms.addOverloads((OverloadedFunction) value);
                    } else {
                        error(errorMsg("Overload of Simple_fcn ", fvo,
                                       " with inconsistent ", value));
                    }
                     return table.add(index, gms, comparator);
                 } else if (fvo instanceof OverloadedFunction) {
                   if (value instanceof SingleFcn) {
                       ((OverloadedFunction)fvo).addOverload((SingleFcn) value, overloadIsOK);
                    } else if (value instanceof OverloadedFunction) {
                        ((OverloadedFunction)fvo).addOverloads((OverloadedFunction) value);
                    } else {
                        error(errorMsg("Overload of OverloadedFunction ", fvo,
                                       " with inconsistent ", value));
                    }


               } else {
                    throw new RedefinitionError(what, index, original.getValue(), value);
                }
                /*
                 * The overloading occurs in the original table, unless a new overload
                 * was created (see returns of "table.add" above).
                 */
                return table;
            } else {
                noteName(index);
            }
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

    public void assignValue(HasAt loc, String str, FValue f2) {
        FValue v = get(var_env, str);
        if (v instanceof ReferenceCell) {
            ((ReferenceCell)v).assignValue(f2);
            return;
        }
        if (v == null)
            error(loc, this, "Cannot assign to unbound variable " + str);
        error(loc, this, "Cannot assign to immutable " + str);
    }

    public void storeType(HasAt loc, String str, FType f2) {
        FValue v = get(var_env, str);
        if (v instanceof ReferenceCell) {
            ((ReferenceCell)v).storeType(f2);
            return;
        }
        if (v == null)
            error(loc, this, "Type stored to unbound variable " + str);
        error(loc, this, "Type stored to immutable variable " + str);

    }


//    public void assignValue(FValue f1, FValue f2) {
//        assignValue(string(f1), f2);
//
//    }

    public Boolean casValue(String str, FValue old_value, FValue new_value) {
        return bug("Cas on envs no longer supported");
    }


    public void debugPrint(String debugString) {
        if (debug)
            System.out.println(debugString);

    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            dump(sb);
        } catch (IOException never) {

        }
        return sb.toString();
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

    public BetterEnv genericLeafEnvHack(BetterEnv genericEnv, HasAt loc) {
        return new BetterEnv(this, genericEnv);
    }

    public SApi getApiNull(String str) {
        SApi v = get(api_env, str);
        return v;
    }

    public SApi getApiNull(APIName d) {
        return getApiNull(NodeUtil.nameString(d));
    }

    public Boolean getBoolNull(String str) {
        Boolean v = get(bool_env, str);
        return v;
    }

    public SComponent getComponentNull(String str) {
        SComponent v = get(cmp_env, str);
        return v;
    }

    public SComponent getComponentNull(APIName name) {
        return getComponentNull(NodeUtil.nameString(name));
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

    public Closure getRunClosure() {
        return (Closure) getValue("run");
    }

    public Closure getClosure(String s) {
        return (Closure) getValue(s);
    }

    public FType getTypeNull(String name) {
        return get(type_env, name);
    }

    public FType getTypeNull(QualifiedIdName name) {
        return getTypeNull(NodeUtil.nameString(name));
    }

    public FValue getValueNull(QualifiedIdName name) {
        return getValueNull(NodeUtil.nameString(name));
    }

    public FValue getValueNull(String s) {
        FValue v = get(var_env, s);
        if (v == null)
            return v;
        if (v instanceof IndirectionCell) {
            try {
                v = ((IndirectionCell) v).getValue();
            } catch (CircularDependenceError ce) {
                ce.addParticipant(s);
                throw ce;
            }
            // Ought to snap link if original was not ReferenceCell
            if (v == null) {
                error("Variable " + s + " has no value");
            }
        }
        return v;
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

    public FType getVarTypeNull(String str) {
        FValue v = get(var_env, str);
        if (v == null)
            return null;
        if (v instanceof ReferenceCell) {
            return ((ReferenceCell) v).getType();
        }
        return null;
    }

    public boolean hasType(String str) {
        return has(var_env, str);
    }

    public boolean hasValue(String str) {
        return has(var_env, str) || has(bool_env, str) || has(nat_env, str);
    }

    public void putApi(String s, SApi api) {
        api_env = put(api_env, s, api, "API");
    }

    public void putApi(APIName d, SApi x) {
        putApi(NodeUtil.nameString(d), x);

    }

    public void putBool(String str, Boolean f2) {
        bool_env = put(bool_env, str, f2, "Boolean type parameter");
        var_env = put(var_env, str, FBool.make(f2), "Nat param as var/value");
   }

    public void putComponent(APIName name, SComponent comp) {
        putComponent(NodeUtil.nameString(name), comp);

    }

    public void putComponent(String name, SComponent comp) {
        cmp_env = put(cmp_env, name, comp, "Component");

    }

    public void putDecl(String str, Declaration f2) {
        dcl_env = put(dcl_env, str, f2, "Declaration");

    }

    public void putNat(String str, Number f2) {
        nat_env = put(nat_env, str, f2, "Nat type parameter");
        var_env = put(var_env, str, FInt.make(f2.intValue()), "Nat param as var/value");
    }

    public void putInt(String str, Number f2) {
        int_env = put(int_env, str, f2, "Int type parameter");
        var_env = put(var_env, str, FInt.make(f2.intValue()), "Int param as var/value");
    }

    public void putType(String str, FType f2) {
        type_env = put(type_env, str, f2, "Type");
    }

    public void putType(QualifiedIdName name, FType x) {
        putType(NodeUtil.nameString(name), x);
    }

    public void putValue(String str, FValue f2) {
        if (f2 instanceof Fcn)
            var_env = putFunction(var_env, str, (Fcn) f2, "Var/value", false, false);
        else
            var_env = putNoShadow(var_env, str, f2, "Var/value");
     }

    public void putValueShadowFn(String str, FValue f2) {
        if (f2 instanceof Fcn)
            var_env = putFunction(var_env, str, (Fcn) f2, "Var/value", false, false);
        else
            var_env = putNoShadow(var_env, str, f2, "Var/value");
     }

    public void putValueNoShadowFn(String str, FValue f2) {
        if (f2 instanceof Fcn)
            var_env = putFunction(var_env, str, (Fcn) f2, "Var/value", true, false);
        else
            var_env = putNoShadow(var_env, str, f2, "Var/value");
     }

    /**
     * 
     * @param str
     * @param f2
     */
    public void putFunctionalMethodInstance(String str, FValue f2) {
        if (f2 instanceof Fcn)
            var_env = putFunction(var_env, str, (Fcn) f2, "Var/value", true, true);
        else
            error(str + " must be a functional method instance ");
     }

    public void putVariable(String str, FValue f2) {
        putValue(str, new ReferenceCell(FTypeDynamic.ONLY, f2));
     }

    public void putVariablePlaceholder(String str) {
        putValue(str, new ReferenceCell());
     }

    public void putValueUnconditionally(String str, FValue f2) {
        var_env = putUnconditionally(var_env, str, f2, "Var/value");
     }

    public void putValueUnconditionally(String str, FValue f2, FType ft) {
        putValueUnconditionally(str, new ReferenceCell(ft, f2));
     }

    public void putVariable(String str, FValue f2, FType ft) {
        putValue(str, new ReferenceCell(ft, f2));
    }

    public void putVariable(String str, FType ft) {
        putValue(str, new ReferenceCell(ft));
    }

    public void putValue(QualifiedIdName name, FValue x) {
        putValue(NodeUtil.nameString(name), x);
    }

    public Iterator<String> iterator() {
        if (var_env != null)
            return new Iter(namesPut, namesPutCount);
        return Collections.<String>emptySet().iterator();
    }

    // Slightly wrong -- returns all, not just the most recently bound.

    static class Iter implements Iterator<String> {
        String[] a;
        int n;
        int i;

        Iter(String[] a, int n) {
            this.a = a;
            this.n = n;
        }

        public boolean hasNext() {
            return i < n;
        }

        public String next() {
            if (hasNext()) {
                return a[i++];
            }
            return null;
        }

        public void remove() {
            bug("Applicative data structures cannot be changed!");
        }

    }

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

  




}
