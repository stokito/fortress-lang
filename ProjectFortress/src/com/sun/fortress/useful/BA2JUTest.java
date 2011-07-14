/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;

public class BA2JUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BA2JUTest.class);
    }

    static Comparator<Integer> IC = new Comparator<Integer>() {
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    };

    static Integer ONE = Integer.valueOf(1);
    static Integer TWO = Integer.valueOf(2);
    static Integer THREE = Integer.valueOf(3);

    BA2Tree<String, Integer, String> t = new BA2Tree<String, Integer, String>(String.CASE_INSENSITIVE_ORDER, IC);

    int pcount;

    public void testEmpty() {
        assertEquals(t.size(), 0);
        assertEquals(t.min(), null);
        assertEquals(t.max(), null);
        t.ok();
    }

    public void testOne() {
        t = t.putNew("a", ONE, "b");
        assertEquals(t.size(), 1);
        assertEquals(t.min(), "b");
        assertEquals(t.max(), "b");
        assertEquals(t.get("a", ONE), "b");
        t.ok();
    }

    public void testTwo() {
        t = t.putNew("a", ONE, "b");
        t = t.putNew("c", TWO, "d");
        assertEquals(t.size(), 2);
        assertEquals(t.min(), "b");
        assertEquals(t.max(), "d");
        assertEquals(t.get("a", ONE), "b");
        assertEquals(t.get("c", TWO), "d");
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
        String[] a = {
                "01", "02", "03", "04", "05", "06", "07", "08"
        };
        foreachPermutation(a, 0);
    }

    public void test8Dupes() {
        String[] a = {
                "01", "03", "05", "06", "07", "06", "05", "01"
        };
        foreachPermutation(a, 0);
    }

    public void foreachPermutation(String[] a, int i) {
        if (i == a.length) {
            BA2Tree<String, Integer, String> u = t;
            for (int j = 0; j < a.length; j++) {
                int k = u.indexOf(a[j], ONE);
                if (k >= 0) {
                    assertEquals(new Pair<String, Integer>(a[j], ONE), u.getKey(k));
                }
                u = u.putNew(a[j], ONE, a[j]);
                if (k < 0) {
                    assertEquals(new Pair<String, Integer>(a[j], ONE), u.getKey(~k));
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
            if (++pcount % 1000 == 0) System.err.print(".");
            if (pcount % 100000 == 0) System.err.println();

        } else {
            for (int j = i; j < a.length; j++) {
                String str = a[i];
                a[i] = a[j];
                a[j] = str;
                foreachPermutation(a, i + 1);
            }
        }
    }

}
