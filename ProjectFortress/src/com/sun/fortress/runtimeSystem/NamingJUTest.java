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
package com.sun.fortress.runtimeSystem;

import com.sun.fortress.useful.TestCaseWrapper;

import junit.framework.TestCase;

public class NamingJUTest extends TestCaseWrapper {

        String[] inputs = {
            "\\=",
            "",
            "cat",
            "\\d\\o\\g\\",
            "%e%m%u%",
            ";$",
            "\\%$",
        };
        
        String[] outputs = {
            "\\-=",
            "\\=",
            "cat",
            "\\d\\o\\g\\",
            "%e%m%u%",
            "\\?\\%",
            "\\-%\\%",
        };
        
        public void testMangle() {
            for (int i = 0; i < inputs.length; i++) {
                String a = outputs[i];
                String b = Naming.mangleIdentifier(inputs[i]);
                System.out.println("'"+a+"' '"+b+"'");
                assertEquals(a,b);
            }
        }
        public void testUnmangle() {
            for (int i = 0; i < inputs.length; i++) {
                String a = inputs[i];
                String b = Naming.deMangle(outputs[i]);
                System.out.println("'"+a+"' '"+b+"'");
                if (!a.equals(b))
                    b = Naming.deMangle(outputs[i]);
                assertEquals(a,b);
            }
        }
        public void testCat2() {
            for (int i = 0; i < inputs.length; i++) {
                for (int j = 0; j < inputs.length; j++) {
                    String in = inputs[i] + inputs[j];
                    String a = Naming.mangleIdentifier(in);
                    String b = Naming.catMangled(outputs[i], outputs[j]);
                    System.out.println("'"+a+"' '"+b+"'");
                    if (! a.equals(b)) {
                        b = Naming.catMangled(outputs[i], outputs[j]);
                    }
                    assertEquals(a,b);
                    String d = Naming.deMangle(b);
                    assertEquals(in,d);
                }
            }
        }
        public void testCat3() {
            for (int i = 0; i < inputs.length; i++) {
                for (int j = 0; j < inputs.length; j++) {
                    for (int k = 0; k < inputs.length; k++) {

                    String in = inputs[i] + inputs[j] + inputs[k];
                    String a = Naming.mangleIdentifier(in);
                    String b = Naming.catMangled(Naming.catMangled(outputs[i], outputs[j]), outputs[k]);
                    String c = Naming.catMangled(outputs[i], Naming.catMangled(outputs[j], outputs[k]));
                    System.out.println("'"+a+"' '"+b+"'"+"' '"+c+"'");
                    if (! a.equals(b)) {
                        b = Naming.catMangled(Naming.catMangled(outputs[i], outputs[j]), outputs[k]);
                    }
                    if (! a.equals(c)) {
                        c = Naming.catMangled(outputs[i], Naming.catMangled(outputs[j], outputs[k]));
                    }
                    assertEquals(a,b);
                    assertEquals(a,c);
                    String d = Naming.deMangle(b);
                    assertEquals(in,d);
                    String e = Naming.deMangle(c);
                    assertEquals(in,e);

                }
            }
        }
    }
        
        public void testRemoveNthSigParameter() {
            String sig = "(LA;LB;LC;)LD;";
            String bcd = Naming.removeNthSigParameter(sig, 0);
            String acd = Naming.removeNthSigParameter(sig, 1);
            String abd = Naming.removeNthSigParameter(sig, 2);
            
            assertEquals("(LB;LC;)LD;", bcd);
            assertEquals("(LA;LC;)LD;", acd);
            assertEquals("(LA;LB;)LD;", abd);
        }
}
