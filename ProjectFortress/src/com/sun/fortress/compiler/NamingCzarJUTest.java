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
package com.sun.fortress.compiler;

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
        NamingCzar nc = NamingCzar.only;
        assertEquals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32;", 
                     nc.boxedImplDesc(nc.fortressTypeForForeignJavaType("I"), null));
        assertEquals("Lcom/sun/fortress/compiler/runtimeValues/FString;",
                nc.boxedImplDesc(nc.fortressTypeForForeignJavaType("Ljava/lang/String;"), null));
    }
    
}
