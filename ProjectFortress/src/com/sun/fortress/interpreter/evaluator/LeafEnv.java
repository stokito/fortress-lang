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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes_util.Printer;
import com.sun.fortress.interpreter.useful.NI;


class LeafEnv extends CommonEnv {
    private HashMap<String, FType> type_env;
    private HashMap<String, Number> nat_env;
    private ConcurrentHashMap<String, FValue> var_env;
    private HashMap<String, FType> var_type_env;
    private HashMap<String, Boolean> bool_env;
    private HashMap<String, SApi> api_env;
    private HashMap<String, SComponent> cmp_env;
    private HashMap<String, LeafEnv> pfx_env;
    private HashMap<String, Declaration> dcl_env;

    boolean debug = false;

    public Appendable dump(Appendable a) throws IOException {
        dumpOne(a, type_env, "Types: ");
        dumpOne(a, nat_env, "Nats: ");
        dumpOne(a, var_env, "Vars: ");
        dumpOne(a, var_type_env, "VTypes: ");
        dumpOne(a, bool_env, "Bools: ");
        dumpOne(a, api_env, "Apis: ");
        dumpOne(a, cmp_env, "Components: ");
        dumpOne(a, pfx_env, "Dotteds: ");
        dumpOne(a, dcl_env, "Declarations: ");
        return a;
    }

    /**
     * @param a
     * @throws IOException
     */
    private void dumpOne(Appendable a, Map m, String s) throws IOException {
        if (m != null) {
            a.append(s).append(m.toString()); a.append(Printer.nl);
        }
    }

    final public String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            dump(sb);
            if (sb.length() == 0) return "EMPTY";
            return sb.toString();
        } catch (IOException ex) {
            return NI.np();
        }
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#debugPrint(java.lang.String)
     */
    public void debugPrint(String debugString) {
        if (debug)
            System.out.println(debugString);
    }

    public LeafEnv() {
    }

    // Copy the environment for a let binding

    private FValue lookup_helper(String str) {
        if (var_env != null) {
            FValue res = var_env.get(str);
            if (res != null) {
                return res;
            }
        }
        if (nat_env != null) {
            Number res = nat_env.get(str);
            if (res != null) {
                return FInt.make(res.intValue());
            }
        }
        if (bool_env != null) {
            Boolean res = bool_env.get(str);
            if (res != null) {
                return FBool.make(res.booleanValue());
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getValue(java.lang.String)
     */
    public FValue getValueNull(String str) {
        FValue res = lookup_helper(str);
        return res;
    }


    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getVarType(java.lang.String)
     */
    public FType getVarTypeNull(String str) {
        // Return the type that is paired with the definition, if there is one.
        if (var_env != null && var_env.containsKey(str)) {
            return var_type_env == null ? null : var_type_env.get(str);
        }

        return null;
    }

    static boolean has(Map m, String str) {
        return m != null && m.containsKey(str);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#hasValue(java.lang.String)
     */
    public boolean hasValue(String str) {
        return has(var_env, str) || has(bool_env, str) || has(nat_env, str);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#hasType(java.lang.String)
     */
    public boolean hasType(String str) {
        return has(type_env, str);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putValue(java.lang.String, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    public void putValue(String str, FValue f2) {
         put_helper(str, f2);
    }

    public void putValueUnconditionally(String str, FValue f2) {
        if (var_env == null) var_env = new ConcurrentHashMap<String, FValue>();
        var_env.put(str, NI.nnf(f2));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putValue(java.lang.String, com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putVariable(String str, FValue f2, FType ft) {
        put_helper(str, f2);
        if (var_type_env == null) var_type_env = new HashMap<String, FType>();
        var_type_env.put(str, NI.nnf(ft));
     }

    private void put_helper(String str, FValue f2) {
        if (var_env == null) var_env = new ConcurrentHashMap<String, FValue>();
        else if (var_env.containsKey(str))
            throw new RedefinitionError("value", str, var_env.get(str), f2);
        var_env.put(str, NI.nnf(f2));
    }


    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getRunMethod()
     */
    public Closure getRunMethod() {
        return (Closure) getValue("run");
    }

    /* Types */

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getType(java.lang.String)
     */
    public FType getTypeNull(String str) {
        FType res = null;
        if (type_env != null)
            res = type_env.get(str);
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putType(java.lang.String, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putDecl(String str, Declaration f2) {
        if (dcl_env == null) dcl_env = new HashMap<String, Declaration>();
        dcl_env.put(str, NI.nnf(f2));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getType(java.lang.String)
     */
    public Declaration getDeclNull(String str) {
        Declaration res = null;
        if (dcl_env != null)
            res = dcl_env.get(str);
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putType(java.lang.String, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putType(String str, FType f2) {
        if (type_env == null) type_env = new HashMap<String, FType>();
        else if (type_env.containsKey(str))
            throw new RedefinitionError("type", str, var_env.get(str), f2);
        type_env.put(str, NI.nnf(f2));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getNat(java.lang.String)
     */
    public Number getNatNull(String str) {
        Number res = null;
        if (nat_env != null)
            res = nat_env.get(str);
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putNat(java.lang.String, java.lang.Number)
     */
    public void putNat(String str, Number f2) {
        if (nat_env == null) nat_env = new HashMap<String, Number>();
        else if (nat_env.containsKey(str))
            throw new RedefinitionError("nat parameter", str, nat_env.get(str), f2);
        nat_env.put(str, NI.nnf(f2));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getBool(java.lang.String)
     */
    public Boolean getBoolNull(String str) {
        Boolean res = null;
        if (bool_env != null)
            res = bool_env.get(str);
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putBool(java.lang.String, java.lang.Boolean)
     */
    public void putBool(String str, Boolean f2) {
        if (bool_env == null) bool_env = new HashMap<String, Boolean>();
        else if (bool_env.containsKey(str))
            throw new RedefinitionError("bool parameter", str, bool_env.get(str), f2);
        bool_env.put(str, NI.nnf(f2));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getApi(java.lang.String)
     */
    public SApi getApiNull(String str) {
        SApi res = null;
        if (api_env != null)
            res = api_env.get(str);
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putApi(java.lang.String, com.sun.fortress.interpreter.evaluator.scopes.SApi)
     */
    public void putApi(String s, SApi api) {
        if (api_env == null) api_env = new HashMap<String, SApi>();
        api_env.put(s, NI.nnf(api));
    }

    /* DottedId variants.  This attempts to follow the rules set
     * forth in JLS 6.5, "Determining the meaning of a name",
     * adapted for Fortress.
     */

    protected Environment deDot(DottedId d) {
        List<String> names = d.getNames();
        return deDot(names.subList(0, names.size()-1));
    }

    protected String last(DottedId d) {
        List<String> names = d.getNames();
        return names.get(names.size()-1);
    }

    /**
     * Walk back through a table of dotted to see if it can be
     * resolved in this environment or one of our ancestors.
     * If the list is empty, return the current environment
     * (empty searches are seen to succeed trivially).
     *
     * @param names
     * @return
     */

    protected LeafEnv deDot(List<String> names) {
        LeafEnv e = this;
        if (names.size() > 0) {
            int l = names.size();
            for (int i = 0; i < l - 1; i++) {
                if (e.pfx_env == null)
                    break;
                e = e.pfx_env.get(names.get(i));
                if (e == null)
                    break;
            }
         }
        return e;
    }

    /* An Api name is unambiguous. */
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getApi(com.sun.fortress.interpreter.nodes.DottedId)
     */
    public SApi getApiNull(DottedId d) {
        return deDot(d).getApi(last(d));
     }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putApi(com.sun.fortress.interpreter.nodes.DottedId, com.sun.fortress.interpreter.evaluator.scopes.SApi)
     */
    public void putApi(DottedId d, SApi x) {
        deDot(d).putApi(last(d), x);
     }

    /* TypeRef names take the form ID or Api.ID */
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getType(com.sun.fortress.interpreter.nodes.DottedId)
     */
    public FType getTypeNull(DottedId d) {
        return deDot(d).getType(last(d));
     }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putType(com.sun.fortress.interpreter.nodes.DottedId, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putType(DottedId d, FType x) {
        deDot(d).putType(last(d), x);
     }

    /* Variables/values -- these are more complex.
     * Api.var
     * Expr.field
     * Expr.method
     *
     * These may not ever appear in this form.
     */
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getValue(com.sun.fortress.interpreter.nodes.DottedId)
     */
    public FValue getValueNull(DottedId d) {
        return deDot(d).getValue(last(d));
     }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putValue(com.sun.fortress.interpreter.nodes.DottedId, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    public void putValue(DottedId d, FValue x) {
        deDot(d).putValue(last(d), x);
     }


    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getComponent(java.lang.String)
     */
    public SComponent getComponentNull(String str) {
        SComponent res = null;
        if (cmp_env != null)
            res = cmp_env.get(str);
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putComponent(java.lang.String, com.sun.fortress.interpreter.evaluator.scopes.SComponent)
     */
    public void putComponent(String s, SComponent api) {
        if (cmp_env == null) cmp_env = new HashMap<String, SComponent>();
        cmp_env.put(s, api);
    }

    /* An Component name is unambiguous. */
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#getComponent(com.sun.fortress.interpreter.nodes.DottedId)
     */
    public SComponent getComponentNull(DottedId d) {
        return deDot(d).getComponent(last(d));
     }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putComponent(com.sun.fortress.interpreter.nodes.DottedId, com.sun.fortress.interpreter.evaluator.scopes.SComponent)
     */
    public void putComponent(DottedId d, SComponent x) {
        deDot(d).putComponent(last(d), x);
     }



    HashMap<String, LeafEnv> pfxEnv() {
        return pfx_env;
    }

    public boolean casValue(String str, FValue old_value, FValue new_value) {
        return var_env.replace(str, old_value, NI.nnf(new_value));
    }

    @Override
    public Iterator<String> iterator() {
        if (var_env != null)
            return var_env.keySet().iterator();
        return Collections.<String>emptySet().iterator();
    }
}
