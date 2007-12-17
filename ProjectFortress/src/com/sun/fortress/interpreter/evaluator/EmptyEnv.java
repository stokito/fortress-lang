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
import java.util.Iterator;

import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class EmptyEnv extends CommonEnv {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#casValue(java.lang.String, com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    @Override
    public Boolean casValue(String str, FValue old_value, FValue new_value) {
        return bug("Empty environment does not support cas");
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getApiNull(com.sun.fortress.interpreter.nodes.APIName)
     */
    @Override
    public SApi getApiNull(APIName d) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getApiNull(java.lang.String)
     */
    @Override
    public SApi getApiNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getBoolNull(java.lang.String)
     */
    @Override
    public Boolean getBoolNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getNatNull(java.lang.String)
     */
    @Override
    public Number getNatNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getRunMethod()
     */
    @Override
    public Closure getRunClosure() {
        return bug("Empty environment does not have run method");
   }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getTypeNull(com.sun.fortress.interpreter.nodes.QualifiedIdName)
     */
    @Override
    public FType getTypeNull(QualifiedIdName q) {
        // TODO Auto-generated method stub
        return null;
    }

    public Declaration getDeclNull(APIName d) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getTypeNull(java.lang.String)
     */
    @Override
    public FType getTypeNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getValueNull(com.sun.fortress.interpreter.nodes.QualifiedIdName)
     */
    @Override
    public FValue getValueNull(QualifiedIdName q) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getValueNull(java.lang.String)
     */
    @Override
    public FValue getValueNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.CommonEnv#getVarTypeNull(java.lang.String)
     */
    @Override
    public FType getVarTypeNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putApi(com.sun.fortress.interpreter.nodes.APIName, com.sun.fortress.interpreter.evaluator.scopes.SApi)
     */
    public void putApi(APIName d, SApi x) {
        bug(d, "Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putApi(java.lang.String, com.sun.fortress.interpreter.evaluator.scopes.SApi)
     */
    public void putApi(String s, SApi api) {
        bug("Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putBool(java.lang.String, java.lang.Boolean)
     */
    public void putBool(String str, Boolean f2) {
        bug("Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putComponent(com.sun.fortress.interpreter.nodes.APIName, com.sun.fortress.interpreter.evaluator.scopes.SComponent)
     */
    public void putComponent(APIName name, SComponent comp) {
        bug(name, "Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putNat(java.lang.String, java.lang.Number)
     */
    public void putNat(String str, Number f2) {
        bug("Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putType(com.sun.fortress.interpreter.nodes.QualifiedIdName, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putType(QualifiedIdName q, FType x) {
        bug(q, "Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putType(java.lang.String, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putType(String str, FType f2) {
        bug("Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putValue(com.sun.fortress.interpreter.nodes.QualifiedIdName, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    public void putValue(QualifiedIdName q, FValue x) {
        bug(q, "Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putValue(java.lang.String, com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.types.FType)
     */
    public void putVariable(String str, FValue f2, FType ft) {
        bug("Empty environment does not support this operation");

    }



    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#putValue(java.lang.String, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    public void putValue(String str, FValue f2) {
        bug("Empty environment does not support this operation");

    }

    public void putValueUnconditionally(String str, FValue f2) {
        bug("Empty environment does not support this operation");

    }



    public void debugPrint(String debugString) {
        System.err.println("Empty environment: " + debugString);
    }



    public boolean hasValue(String str) {
        return false;
    }


    public Appendable dump(Appendable a) throws IOException {
        return a;
    }



    @Override
    public SComponent getComponentNull(String str) {
        return null;
    }



    @Override
    public SComponent getComponentNull(APIName d) {
        return null;
    }



    public void putComponent(String name, SComponent comp) {
        bug("Empty environment does not support this operation");
    }



    @Override
    public Declaration getDeclNull(String str) {
        // TODO Auto-generated method stub
        return null;
    }

    public void putDecl(String str, Declaration f2) {
        bug("Empty environment does not support this operation");

    }



    public boolean hasType(String str) {
        return false;
    }



    @Override
    public Iterator<String> iterator() {

        return Collections.<String>emptySet().iterator();
    }

}
