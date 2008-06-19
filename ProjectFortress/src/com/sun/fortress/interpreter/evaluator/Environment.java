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

package com.sun.fortress.interpreter.evaluator;

import java.io.IOException;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Visitor2;


public interface Environment  {

    /**
     * Assign to a pre-existing variable wherever it happens
     * to occur.
     */
    public abstract void assignValue(HasAt loc, String str, FValue f2);

    public void bless() ;

    public abstract void debugPrint(String debugString);

    /**
     * Appends to a all the information in this environment.
     *
     * @param a
     */
    public abstract Appendable dump(Appendable a) throws IOException ;

    public Environment extend() ;
    public Environment extend(Environment additions) ;

    public Environment extendAt(HasAt x) ;

    public abstract Environment genericLeafEnvHack(Environment genericEnv, HasAt within);

    /* An Api name is unambiguous. */
    public abstract SApi getApi(APIName d);

    /**
     * @return Returns the api_env.
     */
    public abstract SApi getApi(String str);

    public abstract HasAt getAt();

    public boolean getBlessed() ;

    public abstract Boolean getBool(String str);

    public abstract Boolean getBoolNull(String s);

    //public boolean hasType(String str);

    public abstract Closure getClosure(String toBeRun);

    public abstract SComponent getComponent(APIName name);

    public abstract SComponent getComponent(String name);

    public abstract Number getIntNull(String s);

    public abstract Number getNat(String str);

    public abstract Number getNatNull(String s);

    public abstract Closure getRunClosure();

    /* Type names take the form ID or Api.ID */
    public abstract FType getType(Id d);

    public abstract FType getType(String str);

    public abstract FType getTypeNull(Id name);

    public abstract FType getTypeNull(String name);

    public abstract FValue getValue(FValue f1);

    /* Variables/values -- these are more complex.
     * Api.var
     * Expr.field
     * Expr.method
     *
     * These may not ever appear in this form.
     */
    public abstract FValue getValue(Id d);

    /**
     * Get a value from this environment or a parent.
     * Throws an Error if not found.
     * @param str
     */
    public abstract FValue getValue(String str);

    /**
     * Be prepared for a null if the value is missing!
     * @param s
     */
    public abstract FValue getValueNull(String s);

    public abstract FValue getValueRaw(String s);

    /**
     * Get a value from this environment or a parent.
     * Throws an Error if not found.
     * @param str
     */
    public abstract FType getVarType(String str);

    public abstract FType getVarTypeNull(String str);

    public abstract boolean hasValue(String str);

    public abstract Environment installPrimitives();

    public boolean isTopLevel();
    
    // A notable name -- for overloading later, I think.
    public abstract void noteName(String s);

    public abstract void putApi(APIName d, SApi x);

    public abstract void putApi(String s, SApi api);
    
    public abstract void putBool(String str, Boolean f2);

    public void putBoolRaw(String str, Boolean f2) ;
    
    public abstract void putComponent(APIName name, SComponent comp);

    public abstract void putComponent(String name, SComponent comp);

    public abstract void putFunctionalMethodInstance(String fndodname, FValue cl); // Fcn?

    public abstract void putInt(String add_as, Number cnnf);

    //public abstract void putValueShadowFn(String fname, FValue cl);

    public void putIntRaw(String str, Number f2);

    public abstract void putNat(String str, Number f2);

    public void putNatRaw(String str, Number f2) ;

    public abstract void putType(Id d, FType x);

    /**
     * Put a type in the top-most scope.
     * Return true if successful, false if already defined.
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
     * Return true if successful, false if already defined.
     * @param str
     * @param f2
     */
    public abstract void putValue(String str, FValue f2);

    public abstract void putValueNoShadowFn(String fndodname, FValue cl); // Fcn?

    public void putValueRaw(String str, FValue f2) ;

    public abstract void putValueRaw(String name, FValue value, FType ft);

    public abstract void putVariable(String string, FType fvt);

    public abstract void putVariable(String s, FValue value);

    /**
     * Put a value in the top-most scope.
     * Return true if successful, false if already defined.
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
    
    public Iterable<String> youngestFrame() ;
    

    
}
