/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.tests.unit_tests;

import java.lang.reflect.Method;
import java.util.*;
import org.objectweb.asm.*;

import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.useful.TestCaseWrapper;
import com.sun.fortress.compiler.nativeInterface.*;

public class WrapperGeneratorJUTest extends TestCaseWrapper {
    public WrapperGeneratorJUTest(String testName) {
        super(testName);
    }
    public WrapperGeneratorJUTest() {
        super("WrapperGeneratorTest");
    }

    public void testPrintln() {
        MyClassLoader loader = new MyClassLoader();
        Class c = FortressTransformer.transform(loader, "com.sun.fortress.nativeHelpers.simplePrintln");
        Class[] parameterTypes = new Class[1];
        try {
            parameterTypes[0] = Class.forName("com.sun.fortress.interpreter.evaluator.values.FString"); 
            Method m = c.getMethod("nativePrintln", parameterTypes);
            FString foo = FString.make("fa la la");
            m.invoke(null, foo);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // This doesn't work yet.  Need to change from fortress.java.lang.Math to java.lang.Math 
    // and put the nativewrapper_cache on the bootclasspath.
//     public void testMath() {
//      MyClassLoader loader = new MyClassLoader();
//         Class c = FortressTransformer.transform(loader, "java.lang.Math");
//         Class[] parameterTypes = new Class[2];
//         try {
//             System.out.println("Step 1");
//             Class ffloat = Class.forName("com.sun.fortress.interpreter.evaluator.values.FFloat"); 
//             System.out.println("Step 2");
//             parameterTypes[0] = ffloat;
//             parameterTypes[1] = ffloat;
//             Method m = c.getMethod("hypot", parameterTypes);
//             System.out.println("Step 3: method = " + m);
//             Object args[] = new FFloat[2];
//             args[0] =  FFloat.make(4.0);
//             args[1] =  FFloat.make(3.0);
//             System.out.println("Step 4: args");
//             FFloat res = (FFloat) m.invoke(null, args);
//             System.out.println("Step 5: res = " + res);
//             System.out.println("res = " + res + " Should be 5 ");
//         } catch (Throwable t) {
//             System.out.println("Caught throwable " + t);
//             throw new RuntimeException(t);
//         }
//     }

        
}
