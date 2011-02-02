/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FunctionClosure;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;

import java.io.IOException;
import java.util.List;


public interface Environment {

    public final static int TOP_LEVEL = 0;

    /**
     * Assign to a pre-existing variable wherever it happens
     * to occur.
     */
    public abstract void assignValue(HasAt loc, String str, FValue f2);

    public void bless();

    public abstract void debugPrint(String debugString);

    /**
     * Appends to a all the information in this environment.
     *
     * @param a
     */
    public abstract Appendable dump(Appendable a) throws IOException;

    public Environment extend();

    public Environment extend(Environment additions);

    public Environment extendAt(HasAt x);

    public abstract Environment genericLeafEnvHack(Environment genericEnv);

    public abstract HasAt getAt();

    public boolean getBlessed();

    public abstract Boolean getBool(String str); // 0 refs

    public abstract Boolean getBoolNull(String s); // 2 trivial refs

    //public boolean hasType(String str);

    public abstract FunctionClosure getClosure(String toBeRun);

    public abstract Number getIntNull(String s); // 1 trivial ref

    public abstract Number getNat(String str);
    // used only by natFromGeneric (applied to trait environments)
    // which is used only by arrayRank and lengthAlongArrayAxis

    public abstract Number getNatNull(String s);
    // used only by getNat (above)

    public abstract FunctionClosure getRunClosure();
    // top-level environment reference

    /* Type names take the form ID or Api.ID */

    public abstract FType getType(Id d); // 3
    // BoolRef, IntRef, and the TL name of a (generic) trait.

    public abstract FType getTypeNull(VarType q); // 2

    public abstract FType getType(VarType q); // 2

    // forTraitType, forVarType (these have lexical depth)
    public abstract FType getType(TraitType q); // toplevel, possible api

    public abstract FType getRootType(String str); // 10 refs

    public abstract FType getLeafType(String str); // 4 refs

    public abstract FType getTypeNull(Id name); // 3
    // two can easily be converted to use index.

    public abstract FType getLeafTypeNull(String name); // 16 refs

    public abstract FType getRootTypeNull(String name); // 4 refs

    public FType getTypeNull(String name); // 3 refs -- keep this name because of compiled code.

    /**
     * Level-tagged version of getTypeNull
     *
     * @param name
     * @param level
     * @return
     */
    public FType getTypeNull(String name, int level);

    //public abstract FValue getValue(FValue f1);

    /* Variables/values -- these are more complex.
     * Api.var
     * Expr.field
     * Expr.method
     *
     * These may not ever appear in this form.
     */
    //public abstract FValue getValue(Id d);

    /**
     * Get a value from this environment (not a parent).
     * Throws an Error if not found.
     *
     * @param str
     */
    public abstract FValue getLeafValue(String str); // 25 refs

    /**
     * Get a value from top-level environment.
     * Throws an Error if not found.
     *
     * @param str
     */
    public abstract FValue getRootValue(String str); // 12 refs

    /**
     * Be prepared for a null if the value is missing!
     *
     * @param s
     */
    public abstract FValue getLeafValueNull(String s); // 6 refs

    public abstract FValue getRootValueNull(String s); // 5 refs

    /**
     * Similar to the string version, but able to deal with
     * depth information on the VarRef.
     *
     * @param vr
     * @return
     */
    public abstract FValue getValueNull(VarRef vr); // 2 refs

    public abstract FValue getValueNull(FunctionalRef vr); // 2 refs

    public abstract FValue getValueNull(IdOrOp name, int l); // 3 refs

    public abstract FValue getValue(Id name, int l); // 0 refs

    public abstract FValue getValue(Op name, int l); // 1 ref

    public abstract FValue getValue(VarRef vr); // 0 refs

    public abstract FValue getValue(FunctionalRef vr); // 2 refs

    // Reference from tests, BaseEnv, and BuildApiEnvironment
    // BaseEnv accesses are internal-use-only, BAE references link snapping etc.
    public abstract FValue getValueRaw(String s); // 20 refs

    public abstract FType getVarTypeNull(String str); // 2 refs
    // Leaf in baseenv,
    // Can be level-indexed in LHSEvaluator

    public abstract Environment installPrimitives();

    public boolean isTopLevel();

    // A notable name -- for overloading later, I think.
    public abstract void noteName(String s);

    public abstract void putBool(String str, Boolean f2);

    public void putBoolRaw(String str, Boolean f2);

    public abstract void putFunctionalMethodInstance(String fndodname, FValue cl); // Fcn?

    public abstract void putInt(String add_as, Number cnnf);

    //public abstract void putValueShadowFn(String fname, FValue cl);

    public void putIntRaw(String str, Number f2);

    public abstract void putNat(String str, Number f2);

    public void putNatRaw(String str, Number f2);

    public abstract void putType(Id d, FType x);

    /**
     * Put a type in the top-most scope.
     * Return true if successful, false if already defined.
     *
     * @param str
     * @param f2
     */
    public abstract void putType(String str, FType f2);

    public abstract void putTypeRaw(String str, FType f2);

    /**
     * Put a value in the top-most scope.
     * Return true if successful, false if already defined.
     */
    public abstract void putValue(FValue f1, FValue f2);

    public abstract void putValue(Id d, FValue x);

    /**
     * Put a value in the top-most scope.
     *
     * @param str
     * @param f2
     */
    public abstract void putValue(String str, FValue f2);

    public abstract void putValueNoShadowFn(String fndodname, FValue cl); // Fcn?

    public void putValueRaw(String str, FValue f2);

    public abstract void putValueRaw(String name, FValue value, FType ft);

    public abstract void putVariable(String string, FType fvt);

    public abstract void putVariable(String s, FValue value);

    /**
     * Put a value in the top-most scope.
     * Return true if successful, false if already defined.
     *
     * @param str
     * @param f2
     */
    public abstract void putVariable(String str, FValue f2, FType ft);

    // An untyped variable...
    public abstract void putVariablePlaceholder(String sname);

    public abstract void removeType(String s);

    public abstract void removeVar(String name);

    public abstract void setTopLevel();

    // Fix the untyped variable
    public abstract void storeType(HasAt x, String sname, FType ft);

    public void visit(Visitor2<String, FType> vt,
                      Visitor2<String, Number> vn,
                      Visitor2<String, Number> vi,
                      Visitor2<String, FValue> vv,
                      Visitor2<String, Boolean> vb);

    public abstract void visit(Visitor2<String, Object> nameCollector);

    /**
     * Returns the names of vars in the most recently added frame (everything
     * added since this environment was created with a call to "extend()" ).
     */

    public Iterable<String> youngestFrame();

    /**
     * Level-tagged version of getValueRaw
     *
     * @param s
     * @param level
     * @return
     */
    public FValue getValueRaw(String s, int level);

    public Environment getApi(String s);

    public Environment getApi(APIName s);

    public Environment getApi(List<Id> s);

    public Environment getApiNull(String apiName);

    public void putApi(String apiName, Environment env);

    public Environment getHomeEnvironment(IdOrOpOrAnonymousName ioooan);

    public Environment getTopLevel();
}
