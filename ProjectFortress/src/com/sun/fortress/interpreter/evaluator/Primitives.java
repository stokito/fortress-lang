/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.types.Bool;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class Primitives {
    static boolean debug = false;

    private static void debugPrint(String debugString) {
        if (debug) System.out.print(debugString);
    }

    private static void debugPrintln(String debugString) {
        if (debug) System.out.println(debugString);
    }

    private static void install_type(Environment env, String name, FType t) {
        env.putType(name, t);
    }

    private static void install_value(Environment env, String name, FValue v) {
        env.putValue(name, v);
    }

    public static void installPrimitives(Environment env) {

        // Dual identity of true/false
        install_type(env, "true", Bool.make(true));
        install_type(env, "false", Bool.make(false));

        install_type(env, "Any", FTypeTop.ONLY);

        // Dual identity of true/false
        install_type(env, "AnyType.true", Bool.make(true));
        install_type(env, "AnyType.false", Bool.make(false));

        install_type(env, "AnyType.Any", FTypeTop.ONLY);

    }
}
