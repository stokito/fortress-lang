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
import com.sun.fortress.interpreter.evaluator.types.FTypeChar;
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
        install_type(env, "ZZ32", FTypeInt.ONLY);
        install_type(env, "ZZ64", FTypeLong.ONLY);
        install_type(env, "Integral", FTypeIntegral.ONLY);
        install_type(env, "String", FTypeString.ONLY);
        install_type(env, "Boolean", FTypeBool.ONLY);
        install_type(env, "Char", FTypeChar.ONLY);
        install_type(env, "RR64", FTypeFloat.ONLY);
        install_type(env, "Number", FTypeNumber.ONLY);
        // install_type(env, "ZZ32Range", FTypeRange.ONLY);
        install_type(env, "BufferedWriter", FTypeBufferedWriter.ONLY);

        install_type(env, "IntLiteral", FTypeIntLiteral.ONLY);
        install_type(env, "FloatLiteral", FTypeFloatLiteral.ONLY);

        install_value(env, "true", FBool.TRUE);
        install_value(env, "false", FBool.FALSE);

        // Dual identity of true/false
        install_type(env, "true", Bool.make(true));
        install_type(env, "false", Bool.make(false));

        install_type(env, "Any", FTypeTop.ONLY);

        
        install_type(env, "FortressBuiltin.ZZ32", FTypeInt.ONLY);
        install_type(env, "FortressBuiltin.ZZ64", FTypeLong.ONLY);
        install_type(env, "FortressBuiltin.Integral", FTypeIntegral.ONLY);
        install_type(env, "FortressBuiltin.String", FTypeString.ONLY);
        install_type(env, "FortressBuiltin.Boolean", FTypeBool.ONLY);
        install_type(env, "FortressBuiltin.Char", FTypeChar.ONLY);
        install_type(env, "FortressBuiltin.RR64", FTypeFloat.ONLY);
        install_type(env, "FortressBuiltin.Number", FTypeNumber.ONLY);
        // install_type(env, "ZZ32Range", FTypeRange.ONLY);
        install_type(env, "FortressBuiltin.BufferedWriter", FTypeBufferedWriter.ONLY);

        install_type(env, "FortressBuiltin.IntLiteral", FTypeIntLiteral.ONLY);
        install_type(env, "FortressBuiltin.FloatLiteral", FTypeFloatLiteral.ONLY);

        install_value(env, "FortressBuiltin.true", FBool.TRUE);
        install_value(env, "FortressBuiltin.false", FBool.FALSE);

        // Dual identity of true/false
        install_type(env, "FortressBuiltin.true", Bool.make(true));
        install_type(env, "FortressBuiltin.false", Bool.make(false));

        install_type(env, "FortressBuiltin.Any", FTypeTop.ONLY);
        
        
        FTypeNumber.ONLY.addExclude(FTypeString.ONLY);
        FTypeNumber.ONLY.addExclude(FTypeChar.ONLY);
        FTypeNumber.ONLY.addExclude(FTypeBool.ONLY);
        FTypeInt.ONLY.addExclude(FTypeString.ONLY);
        FTypeInt.ONLY.addExclude(FTypeChar.ONLY);
        FTypeInt.ONLY.addExclude(FTypeFloat.ONLY);
        FTypeInt.ONLY.addExclude(FTypeFloatLiteral.ONLY);
        FTypeInt.ONLY.addExclude(FTypeBool.ONLY);
        FTypeInt.ONLY.addExclude(FTypeLong.ONLY);
        FTypeLong.ONLY.addExclude(FTypeString.ONLY);
        FTypeLong.ONLY.addExclude(FTypeFloat.ONLY);
        FTypeLong.ONLY.addExclude(FTypeFloatLiteral.ONLY);
        FTypeLong.ONLY.addExclude(FTypeBool.ONLY);
        FTypeLong.ONLY.addExclude(FTypeChar.ONLY);
        FTypeIntegral.ONLY.addExclude(FTypeString.ONLY);
        FTypeIntegral.ONLY.addExclude(FTypeFloat.ONLY);
        FTypeIntegral.ONLY.addExclude(FTypeFloatLiteral.ONLY);
        FTypeIntegral.ONLY.addExclude(FTypeBool.ONLY);
        FTypeIntegral.ONLY.addExclude(FTypeChar.ONLY);
        FTypeFloat.ONLY.addExclude(FTypeString.ONLY);
        FTypeFloat.ONLY.addExclude(FTypeBool.ONLY);
        FTypeFloat.ONLY.addExclude(FTypeChar.ONLY);
        FTypeString.ONLY.addExclude(FTypeBool.ONLY);
        FTypeString.ONLY.addExclude(FTypeIntLiteral.ONLY);
        FTypeString.ONLY.addExclude(FTypeFloatLiteral.ONLY);
        FTypeString.ONLY.addExclude(FTypeChar.ONLY);
        FTypeChar.ONLY.addExclude(FTypeNumber.ONLY);
        FTypeChar.ONLY.addExclude(FTypeBool.ONLY);
        FTypeChar.ONLY.addExclude(FTypeString.ONLY);
        FTypeChar.ONLY.addExclude(FTypeNumber.ONLY);
        
        
        


    }
}
