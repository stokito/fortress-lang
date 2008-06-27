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

package com.sun.fortress.interpreter.glue.prim;

import static com.sun.fortress.exceptions.ProgramError.error;

/* import java.lang.String; /* SPARE COPY  */
import java.lang.String; /*  ECLIPSE MAY REMOVE THIS INCORRECTLY */

import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.NativeFn0;

/**
 * Functions from String.
 */
public class StringPrim {

public static final class Print extends Util.S2V {
    protected void f(String x) { System.out.print(x); }
}
public static final class Println extends Util.S2V {
    protected void f(String x) { System.out.println(x); }
}

public static final class Match extends Util.SS2B {
    protected boolean f(String regex, String some){
        return some.matches(regex);
    }
}

public static final class PrintTaskTrace extends NativeFn0 {
    protected FValue act() {
        BaseTask.printTaskTrace();
        return FVoid.V;
    }
}

public static final class PrintThreadInfo extends Util.S2V {
    protected void f(String x) {
          System.out.println(" Thread " + java.lang.Thread.currentThread().getName() + " operating on value " + x);
    }
}

public static final class ThrowError extends Util.S2V {
    protected void f(String x) {
        String msg = " Thread " + java.lang.Thread.currentThread().getName() + " got error " + x;
        error(msg);
    }
}

public static final class GetEnvironment extends Util.SS2S {
    protected String f(String name, String defaultValue) {
        return com.sun.fortress.repository.ProjectProperties.get(name,defaultValue);
    }
}
}
