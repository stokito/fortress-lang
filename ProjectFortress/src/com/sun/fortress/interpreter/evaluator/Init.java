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
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.types.BottomType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeBool;
import com.sun.fortress.interpreter.evaluator.types.FTypeBufferedWriter;
import com.sun.fortress.interpreter.evaluator.types.FTypeChar;
import com.sun.fortress.interpreter.evaluator.types.FTypeDynamic;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloat;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloatLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntegral;
import com.sun.fortress.interpreter.evaluator.types.FTypeLong;
import com.sun.fortress.interpreter.evaluator.types.FTypeNumber;
import com.sun.fortress.interpreter.evaluator.types.FTypeOpr;
import com.sun.fortress.interpreter.evaluator.types.FTypeOverloadedArrow;
import com.sun.fortress.interpreter.evaluator.types.FTypeRange;
import com.sun.fortress.interpreter.evaluator.types.FTypeRest;
import com.sun.fortress.interpreter.evaluator.types.FTypeString;
import com.sun.fortress.interpreter.evaluator.types.FTypeStringLiteral;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.values.GenericFunctionOrMethod;
import com.sun.fortress.interpreter.glue.NativeApp;


/*
 * Created on Oct 27, 2006
 *
 */

public class Init {

    // For leak detection.
    static int countdown=4;
    
    public static void initializeEverything() {
        FTypeArrow.reset();
        FTypeTuple.reset();
        FTypeOpr.reset();
        FTypeRest.reset();
        FTypeOverloadedArrow.reset();
        FortressTests.reset();
        IntNat.reset();
        GenericFunctionOrMethod.FunctionsAndState.reset();

        FTypeVoid.ONLY.resetState();
        FTypeTop.ONLY.resetState();
        FTypeDynamic.ONLY.resetState();

        FTypeBool.ONLY.resetState();
        FTypeBufferedWriter.ONLY.resetState();
        FTypeChar.ONLY.resetState();

        FTypeFloat.ONLY.resetState();
        FTypeFloatLiteral.ONLY.resetState();
        FTypeInt.ONLY.resetState();
        FTypeIntegral.ONLY.resetState();
        FTypeIntLiteral.ONLY.resetState();
        FTypeLong.ONLY.resetState();
        FTypeNumber.ONLY.resetState();
        FTypeIntegral.ONLY.resetState();
        FTypeString.ONLY.resetState();
        FTypeStringLiteral.ONLY.resetState();
        
        BottomType.ONLY.resetState();
        FTypeRange.ONLY.resetState();

        FTypeIntLiteral.ONLY.resetState();
        FTypeFloatLiteral.ONLY.resetState();
        FTypeStringLiteral.ONLY.resetState();
        
        NativeApp.reset();

        // For leak detection; runs 4 tests, then cleans the heap, and hangs.
        if (false && countdown-- == 0) {
            for (int i = 0; i < 10; i++)
                System.gc();
            try {
                Thread.sleep(1000000000);
            } catch (InterruptedException ex) {
                
            }
        }
        
    }

}
