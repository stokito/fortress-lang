/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class BalancedApplicativeTreeEquivalenceClasssesJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BalancedApplicativeTreeEquivalenceClasssesJUTest.class);
    }

    static class E implements EquivalenceClass<String, Number> {

        public int compare(String x, Number y) {
            int i = Integer.parseInt(x);
            return i - y.intValue();
        }

        public int compareLeftKeys(String x, String y) {
            int i = Integer.parseInt(x);
            int j = Integer.parseInt(y);
            return i - j;
        }

        public int compareRightKeys(Number x, Number y) {
            return x.intValue() - y.intValue();
        }

        public Number translate(String x) {
            return Integer.valueOf(x);
        }

    }

    E e = new E();

    BATreeEC<String, Number, String> t = new BATreeEC<String, Number, String>(e);

    int pcount;

    public void testEmpty() {
        assertEquals(t.size(), 0);
        assertEquals(t.min(), null);
        assertEquals(t.max(), null);
        t.ok();
    }

    public void testOne() {
        t = t.putNew("5", "five");
        assertEquals(t.size(), 1);
        assertEquals(t.min(), "five");
        assertEquals(t.max(), "five");
        assertEquals(t.get("5"), "five");
        assertEquals(t.get("0005"), "five");
        t.ok();
    }

    public void testTwo() {
        t = t.putNew("5", "Five");
        t = t.putNew("6", "Six");
        t = t.putNew("05", "five");
        t = t.putNew("06", "six");
        assertEquals(t.size(), 2);
        assertEquals(t.min(), "five");
        assertEquals(t.max(), "six");
        assertEquals(t.get("5"), "five");
        assertEquals(t.get("6"), "six");
        assertEquals(t.get("05"), "five");
        assertEquals(t.get("06"), "six");
        t.ok();
    }

    public void testThree() {
        String[] a = {"1", "2", "3"};
        foreachPermutation(a, 0);
    }

    public void testSeven() {
        String[] a = {"1", "2", "3", "4", "5", "6", "7"};
        foreachPermutation(a, 0);
        System.err.println();
    }

    public void testSevenDupe() {
        String[] a = {"1", "2", "3", "4", "5", "6", "7", "4"};
        foreachPermutation(a, 0);
        System.err.println();
    }

    public void test8() {
        String[] a = {"01", "02", "03", "04", "05", "06", "07", "08"};
        foreachPermutation(a, 0);
    }

    public void test8Dupes() {
        String[] a = {"01", "03", "05", "06", "07", "06", "05", "01"};
        foreachPermutation(a, 0);
    }

    public void foreachPermutation(String[] a, int i) {
        if (i == a.length) {
            BATreeEC<String, Number, String> u = t;
            for (int j = 0; j < a.length; j++) {
                int k = u.indexOf(a[j]);
                if (k >= 0) {
                    assertEquals(e.translate(a[j]), u.getKey(k));
                }
                u = u.putNew(a[j], a[j]);
                if (k < 0) {
                    assertEquals(e.translate(a[j]), u.getKey(~k));
                }

                u.ok();
                if (a.length < 4) {
                    System.err.print(" " + a[j]);
                }
            }
            if (a.length < 4) {
                System.err.println();
            }
            // u.ok();
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
