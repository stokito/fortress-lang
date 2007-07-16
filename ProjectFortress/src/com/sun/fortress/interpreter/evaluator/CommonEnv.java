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
import java.util.Iterator;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.scopes.SApi;
import com.sun.fortress.interpreter.evaluator.scopes.SComponent;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.NI;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

/**
 * A CommonEnv enforces some default implementations of methods that filter
 * out null values, and provides differently named methods (not appearing
 * in the Environment interface) for getting at those nulls.
 */

abstract public class CommonEnv extends BaseEnv implements Environment {
    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.Environment#genericLeafEnvHack(com.sun.fortress.interpreter.evaluator.SpineEnv)
     */
    public BetterEnv genericLeafEnvHack(BetterEnv genericEnv, HasAt within) {
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
        if (x == null)
            throw new ProgramError(errorMsg("Missing value ", str));
        else
            return x;
    }

    abstract public  FType getVarTypeNull(String str);

    final public  FType getVarType(String str) {
        FType x = getVarTypeNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing type of ", str));
        else
            return x;
    }

    abstract public  boolean casValue(String str, FValue old_value, FValue new_value) ;

    abstract public  Closure getRunMethod() ;

    abstract public  FType getTypeNull(String str) ;
    final public  FType getType(String str)  {
        FType x = getTypeNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing type ", str));
        else
            return x;
    }

    abstract public  Declaration getDeclNull(String str) ;
    final public  Declaration getDecl(String str)  {
        Declaration x = getDeclNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing declaration ", str));
        else
            return x;
    }

    abstract public  Number getNatNull(String str);
    final public  Number getNat(String str) {
        Number x = getNatNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing nat ", str));
        else
            return x;
    }

    abstract public  Boolean getBoolNull(String str) ;
    final public  Boolean getBool(String str)  {
        Boolean x = getBoolNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing boolean ", str));
        else
            return x;
    }

    abstract public  SApi getApiNull(String str) ;
    final public  SApi getApi(String str)  {
        SApi x = getApiNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing api ", str));
        else
            return x;
    }

    abstract public  SApi getApiNull(DottedId d) ;
    final public  SApi getApi(DottedId d)  {
        SApi x = getApiNull(d);
        if (x == null)
            throw new ProgramError(errorMsg("Missing api ", d));
        else
            return x;
    }

    abstract public  SComponent getComponentNull(String str) ;
    final public  SComponent getComponent(String str)  {
        SComponent x = getComponentNull(str);
        if (x == null)
            throw new ProgramError(errorMsg("Missing component ", str));
        else
            return x;
    }

    abstract public  SComponent getComponentNull(DottedId d) ;
    final public  SComponent getComponent(DottedId d)  {
        SComponent x = getComponentNull(d);
        if (x == null)
            throw new ProgramError(errorMsg("Missing component ", d));
        else
            return x;
    }

    abstract public  FType getTypeNull(DottedId d) ;
    final public  FType getType(DottedId d)  {
        FType x = getTypeNull(d);
        if (x == null)
            {
                System.err.println(this.toString());
                throw new ProgramError(errorMsg("Missing type ", d));
            }
        else
            return x;
    }

    abstract public  FValue getValueNull(DottedId d) ;
    final public  FValue getValue(DottedId d)  {
        FValue x = getValueNull(d);
        if (x == null)
            throw new ProgramError(errorMsg("Missing value ", d));
        else
            return x;
    }
}
