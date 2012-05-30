/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
        
        public void testFortressMangleNoChange() {
            // First, do no harm; these should be identity-mangles

            String a = "a";
            String ab = "a$b";
            String abc = "a$b$c";
            String a_a = "(La;)La;";
            String ab_ab = "(La/b;)La$b;";
            
            assertEquals(a, Naming.mangleFortressIdentifier(a));
            assertEquals(ab, Naming.mangleFortressIdentifier(ab));
            assertEquals(abc, Naming.mangleFortressIdentifier(abc));
            assertEquals(a_a, Naming.mangleMethodSignature(a_a));
            assertEquals(ab_ab, Naming.mangleMethodSignature(ab_ab));
            
            
        }
        
        public void testFortressMangleNoNest() {
            String a = "a.";
            String ab = "a.b";
            String abc = ".a.b.c.";
            String ab_ab = "(La.b;)La:b;";
            
            assertEquals("\\=a\\,", Naming.mangleFortressIdentifier(a));
            assertEquals("\\=a\\,b", Naming.mangleFortressIdentifier(ab));
            assertEquals("\\,a\\,b\\,c\\,", Naming.mangleFortressIdentifier(abc));
            assertEquals("(L\\=a\\,b;)L\\=a\\!b;", Naming.mangleMethodSignature(ab_ab));
           
        }
        
        public void testFortressMangleNest() {
            String a = "a\u27e6.\u27e7";
            String ab = "a.\u27e6b\u27e7";
            String abc = "a\u27e6b$c\u27e7";
            String ab_ab = "(La\u27e6b$c\u27e7;Lc\u27e6d$e\u27e7;)Lx\u27e6y;z\u27e7;";
            
            assertEquals("\\=a\u27e6\\,\u27e7", Naming.mangleFortressIdentifier(a));
            assertEquals("\\=a\\,\u27e6b\u27e7", Naming.mangleFortressIdentifier(ab));
            assertEquals("\\=a\u27e6b\\%c\u27e7", Naming.mangleFortressIdentifier(abc));
            assertEquals("(L\\=a\u27e6b\\%c\u27e7;L\\=c\u27e6d\\%e\u27e7;)L\\=x\u27e6y\\?z\u27e7;", Naming.mangleMethodSignature(ab_ab));
           
        }
      
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
