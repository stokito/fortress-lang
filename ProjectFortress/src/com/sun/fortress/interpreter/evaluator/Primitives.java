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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.Bool;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeBool;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloat;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloatLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntegral;
import com.sun.fortress.interpreter.evaluator.types.FTypeLong;
import com.sun.fortress.interpreter.evaluator.types.FTypeNumber;
import com.sun.fortress.interpreter.evaluator.types.FTypeRange;
import com.sun.fortress.interpreter.evaluator.types.FTypeString;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.types.FTypeBufferedReader;
import com.sun.fortress.interpreter.evaluator.types.FTypeBufferedWriter;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class Primitives {
    static boolean debug = false;

    public static void debugPrint(String debugString) {
        if (debug)
            System.out.print(debugString);
    }

    public static void debugPrintln(String debugString) {
        if (debug)
            System.out.println(debugString);
    }

    public static void install_type(BetterEnv env, String name, FType t) {
        env.putType(name, t);
    }

    public static void install_value(BetterEnv env, String name, FValue v) {
        env.putValue(name, v);
    }

    public static void installPrimitives(BetterEnv env) {
        install_type(env, "ZZ32", FTypeInt.T);
        install_type(env, "ZZ64", FTypeLong.T);
        install_type(env, "Integral", FTypeIntegral.T);
        install_type(env, "String", FTypeString.T);
        install_type(env, "Boolean", FTypeBool.T);
        install_type(env, "RR64", FTypeFloat.T);
        install_type(env, "Number", FTypeNumber.T);
        // install_type(env, "ZZ32Range", FTypeRange.T);
        install_type(env, "BufferedReader", FTypeBufferedReader.T);
        install_type(env, "BufferedWriter", FTypeBufferedWriter.T);

        install_type(env, "IntLiteral", FTypeIntLiteral.T);
        install_type(env, "FloatLiteral", FTypeFloatLiteral.T);

        install_value(env, "true", FBool.TRUE);
        install_value(env, "false", FBool.FALSE);

        // Dual identity of true/false
        install_type(env, "true", new Bool("true", FBool.TRUE));
        install_type(env, "false", new Bool("false", FBool.FALSE));

        install_type(env, "Any", FTypeTop.T);

        FTypeNumber.T.addExclude(FTypeString.T);
        FTypeNumber.T.addExclude(FTypeBool.T);
        FTypeInt.T.addExclude(FTypeString.T);
        FTypeInt.T.addExclude(FTypeFloat.T);
        FTypeInt.T.addExclude(FTypeFloatLiteral.T);
        FTypeInt.T.addExclude(FTypeBool.T);
        FTypeInt.T.addExclude(FTypeLong.T);
        FTypeLong.T.addExclude(FTypeString.T);
        FTypeLong.T.addExclude(FTypeFloat.T);
        FTypeLong.T.addExclude(FTypeFloatLiteral.T);
        FTypeLong.T.addExclude(FTypeBool.T);
        FTypeIntegral.T.addExclude(FTypeString.T);
        FTypeIntegral.T.addExclude(FTypeFloat.T);
        FTypeIntegral.T.addExclude(FTypeFloatLiteral.T);
        FTypeIntegral.T.addExclude(FTypeBool.T);
        FTypeFloat.T.addExclude(FTypeString.T);
        FTypeFloat.T.addExclude(FTypeBool.T);
        FTypeString.T.addExclude(FTypeBool.T);
        FTypeString.T.addExclude(FTypeIntLiteral.T);
        FTypeString.T.addExclude(FTypeFloatLiteral.T);
    }
}
