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

package com.sun.fortress.interpreter.useful;

import junit.framework.TestCase;

public class BATJUTest extends TestCase {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BATJUTest.class);
    }

    BATree<String, String> t = new BATree<String, String> (String.CASE_INSENSITIVE_ORDER);
    int pcount;

    public void testEmpty() {
        assertEquals(t.size(), 0);
        assertEquals(t.min(), null);
        assertEquals(t.max(), null);
        t.ok();
    }
    public void testOne() {
        t = t.putNew("a", "b");
        assertEquals(t.size(), 1);
        assertEquals(t.min(), "b");
        assertEquals(t.max(), "b");
        assertEquals(t.get("a"), "b");
        t.ok();
    }
    public void testTwo() {
        t = t.putNew("a", "b");
        t = t.putNew("c", "d");
        assertEquals(t.size(), 2);
        assertEquals(t.min(), "b");
        assertEquals(t.max(), "d");
        assertEquals(t.get("a"), "b");
        assertEquals(t.get("c"), "d");
        t.ok();
    }

    public void testThree() {
        String[] a = {"a", "b", "c"};
        foreachPermutation(a, 0);
    }

    public void testSeven() {
        String[] a = {"a", "b", "c", "d", "e", "f", "g"};
        foreachPermutation(a, 0);
        System.err.println();
    }

    public void testSevenDupe() {
        String[] a = {"a", "b", "c", "d", "e", "f", "g", "d"};
        foreachPermutation(a, 0);
        System.err.println();
    }

    public void test8() {
        String[] a = {"01", "02", "03", "04", "05",
                      "06", "07", "08"
                      };
        foreachPermutation(a, 0);
    }

    public void test8Dupes() {
        String[] a = {"01", "03", "05",
                      "06", "07", "06", "05", "01"
                      };
        foreachPermutation(a, 0);
    }

    public void foreachPermutation(String[] a, int i) {
        if (i == a.length) {
            BATree<String, String> u = t;
            for (int j = 0; j < a.length; j++) {
                int k = u.indexOf(a[j]);
                if (k >= 0) {
                    assertEquals(a[j], u.getKey(k));
                }
                u = u.putNew(a[j], a[j]);
                if (k < 0) {
                    assertEquals(a[j], u.getKey(~k));
                }

                u.ok();
                if (a.length < 4) {
                    System.err.print(" " + a[j]);
                }
            }
            if (a.length < 4) {
                System.err.println();
            }
            //u.ok();
            if (++pcount % 1000 == 0)
                System.err.print(".");
            if (pcount % 100000 == 0)
                System.err.println();

        } else {
            for (int j = i; j < a.length; j++) {
                String t = a[i];
                a[i] = a[j];
                a[j] = t;
                foreachPermutation(a, i+1);
            }
        }
    }

}
