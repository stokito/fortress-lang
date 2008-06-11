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
import java.util.Iterator;

import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * A BaseEnv supplies (enforces!) some overloadings that
 * every environment must support.
 */

abstract public class BaseEnv implements Environment {

    static public String string(FValue f1) {
        return ((FString) f1).getString();
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#assignValue(com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    // final public void assignValue(FValue f1, FValue f2) {
    //     assignValue(string(f1), f2);
    // }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#casValue(com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue, com.sun.fortress.interpreter.evaluator.values.FValue)
     */

    final public void putValue(FValue f1, FValue f2) {
        putValue(string(f1), f2);
    }

    final public  FValue getValue(FValue f1) {
        return getValue(string(f1));
    }

    public void assignValue(HasAt loc, String str, FValue f2) {
        // TODO track down references, catch error, and fix.
        if (hasValue(str)) putValueUnconditionally(str, f2);
        else error(loc,this, errorMsg("Cannot assign to unbound ", str));
    }

    public Environment genericLeafEnvHack(Environment genericEnv, HasAt within) {
        return NI.na("This only works for SpineEnv");
    }

    abstract public void debugPrint(String debugString);

    abstract public  Appendable dump(Appendable a) throws IOException;

    abstract public  FValue getValueNull(String str) ;

    abstract public void putValueUnconditionally(String str, FValue v);

    /**
     * Used to obtain names of variables defined in this environment.
     */
    abstract public Iterator<String> iterator();

    final public  FValue getValue(String str) {
        FValue x = getValueNull(str);
        if (x == null) {
            return error(errorMsg("Missing value: ", str," in environment:\n",this));
        } else {
            return x;
        }
    }

    abstract public  FType getVarTypeNull(String str);

    final public  FType getVarType(String str) {
        FType x = getVarTypeNull(str);
        if (x == null)
            return error(errorMsg("Missing type of ", str));
        else
            return x;
    }

    abstract public  Closure getRunClosure() ;

    abstract public  FType getTypeNull(String str) ;
    final public  FType getType(String str)  {
        FType x = getTypeNull(str);
        if (x == null)
            return error(errorMsg("Missing type ", str));
        else
            return x;
    }

    abstract public  Declaration getDeclNull(String str) ;
    final public  Declaration getDecl(String str)  {
        Declaration x = getDeclNull(str);
        if (x == null)
            return error(errorMsg("Missing declaration ", str));
        else
            return x;
    }

    abstract public  Number getNatNull(String str);
    final public  Number getNat(String str) {
        Number x = getNatNull(str);
        if (x == null)
            return error(errorMsg("Missing nat ", str));
        else
            return x;
    }

    abstract public  Boolean getBoolNull(String str) ;
    final public  Boolean getBool(String str)  {
        Boolean x = getBoolNull(str);
        if (x == null)
            return error(errorMsg("Missing boolean ", str));
        else
            return x;
    }

    abstract public  SApi getApiNull(String str) ;
    final public  SApi getApi(String str)  {
        SApi x = getApiNull(str);
        if (x == null)
            return error(errorMsg("Missing api ", str));
        else
            return x;
    }

    abstract public  SApi getApiNull(APIName d) ;
    final public  SApi getApi(APIName d)  {
        SApi x = getApiNull(d);
        if (x == null)
            return error(errorMsg("Missing api ", d));
        else
            return x;
    }

    abstract public  SComponent getComponentNull(String str) ;
    final public  SComponent getComponent(String str)  {
        SComponent x = getComponentNull(str);
        if (x == null)
            return error(errorMsg("Missing component ", str));
        else
            return x;
    }

    abstract public  SComponent getComponentNull(APIName d) ;
    final public  SComponent getComponent(APIName d)  {
        SComponent x = getComponentNull(d);
        if (x == null)
            return error(errorMsg("Missing component ", d));
        else
            return x;
    }

    abstract public  FType getTypeNull(Id q) ;
    final public  FType getType(Id q)  {
        FType x = getTypeNull(q);
        if (x == null)
            {
                // System.err.println(this.toString());
                return error(errorMsg("Missing type ", q));
            }
        else
            return x;
    }

    abstract public  FValue getValueNull(Id q) ;
    final public  FValue getValue(Id q)  {
        FValue x = getValueNull(q);
        if (x == null)
            return error(errorMsg("Missing value ", q));
        else
            return x;
    }
}
