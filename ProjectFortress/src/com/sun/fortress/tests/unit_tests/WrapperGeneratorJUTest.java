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
}
