/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.compiler;

import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.TestCaseWrapper;
import com.sun.fortress.useful.UsefulJUTest;

public class NamingCzarJUTest extends TestCaseWrapper {

    public NamingCzarJUTest() {
    }

    public NamingCzarJUTest(String arg0) {
        super(arg0);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        junit.swingui.TestRunner.run(NamingCzarJUTest.class);
    }

    public void testForeign() {
        assertEquals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32;", 
                     NamingCzar.jvmTypeDesc(NamingCzar.fortressTypeForForeignJavaType("I"), null));
        assertEquals("Lcom/sun/fortress/compiler/runtimeValues/FJavaString;",
                NamingCzar.jvmTypeDesc(NamingCzar.fortressTypeForForeignJavaType("Ljava/lang/String;"), null));
    }
    
    public void testNameMangling() {
        String input = "/.;$<>[]:\\%\\%\\";
        String mangledInput = "\\|\\,\\?\\%\\^\\_\\{\\}\\!\\-%\\-%\\";
        String s = Naming.mangleIdentifier(input);
        assertEquals(s, mangledInput);
        input = "hello" + input;
        mangledInput = "\\=" + "hello" + mangledInput;
        s = Naming.mangleIdentifier(input);
        assertEquals(s, mangledInput);
    }

    
}
