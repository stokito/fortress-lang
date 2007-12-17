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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.useful.HasAt;


public interface Environment  {

    public abstract void debugPrint(String debugString);

    /**
     * Appends to a all the information in this environment.
     *
     * @param a
     */
    public abstract Appendable dump(Appendable a) throws IOException ;

    /**
     * Get a value from this environment or a parent.
     * Throws an Error if not found.
     * @param str
     */
    public abstract FValue getValue(String str);

    public abstract FValue getValue(FValue f1);

    /**
     * Get a value from this environment or a parent.
     * Throws an Error if not found.
     * @param str
     */
    public abstract FType getVarType(String str);
    public abstract FType getVarTypeNull(String str);

    public abstract boolean hasValue(String str);

    /**
     * Put a value in the top-most scope.
     * Return true if successful, false if already defined.
     * @param str
     * @param f2
     */
    public abstract void putValue(String str, FValue f2);

    /**
     * Put a value in the top-most scope.
     * Return true if successful, false if already defined.
     * @param str
     * @param f2
     */
    public abstract void putVariable(String str, FValue f2, FType ft);

    /**
     * Put a value in the top-most scope.
     * Return true if successful, false if already defined.
     */
    public abstract void putValue(FValue f1, FValue f2);

    /**
     * Assign to a pre-existing variable wherever it happens
     * to occur.
     */
    public abstract void assignValue(HasAt loc, String str, FValue f2);

    // public abstract void assignValue(FValue f1, FValue f2);

    public abstract Boolean casValue(String str, FValue old_value, FValue new_value);

    public abstract Boolean casValue(FValue f1, FValue old_value, FValue new_value);

    public abstract Closure getRunClosure();

    public abstract FType getType(String str);

    /**
     * Put a type in the top-most scope.
     * Return true if successful, false if already defined.
     * @param str
     * @param f2
     */
    public abstract void putType(String str, FType f2);

    public boolean hasType(String str);

    public abstract Declaration getDecl(String str);

    public abstract void putDecl(String str, Declaration f2);

    public abstract Number getNat(String str);

    public abstract void putNat(String str, Number f2);

    public abstract Boolean getBool(String str);

    public abstract void putBool(String str, Boolean f2);

    /**
     * @return Returns the api_env.
     */
    public abstract SApi getApi(String str);

    public abstract void putApi(String s, SApi api);

    /* An Api name is unambiguous. */
    public abstract SApi getApi(APIName d);

    public abstract void putApi(APIName d, SApi x);

    /* Type names take the form ID or Api.ID */
    public abstract FType getType(QualifiedIdName d);

    public abstract void putType(QualifiedIdName d, FType x);

    /* Variables/values -- these are more complex.
     * Api.var
     * Expr.field
     * Expr.method
     *
     * These may not ever appear in this form.
     */
    public abstract FValue getValue(QualifiedIdName d);

    public abstract void putValue(QualifiedIdName d, FValue x);

    public abstract void putComponent(APIName name, SComponent comp);

    public abstract void putComponent(String name, SComponent comp);

    public abstract SComponent getComponent(APIName name);

    public abstract SComponent getComponent(String name);

    /**
     * Be prepared for a null if the value is missing!
     * @param s
     */
    public abstract FValue getValueNull(String s);

    public abstract FType getTypeNull(String name);

    public abstract Number getNatNull(String s);

    public abstract FType getTypeNull(QualifiedIdName name);

    public abstract BetterEnv genericLeafEnvHack(BetterEnv genericEnv, HasAt within);

}
