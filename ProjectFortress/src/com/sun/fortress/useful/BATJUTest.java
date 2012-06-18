/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Random;

public class BATJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BATJUTest.class);
    }

    BATree<String, String> t = new BATree<String, String>(String.CASE_INSENSITIVE_ORDER);
    int pcount;

    String[] animals = {
            "alpaca", "ant", "auk", "bat", "beetle", "bison", "buffalo", "camel", "cat", "cavy", "crab", "deer",
            "dingo", "dodo", "dog", "dove", "eagle", "eel", "eland", "elephant", "elk", "emu", "finch", "gar",
            "giraffe", "gnu", "guanaco", "gull", "hawk", "hedgehog", "hyena", "ibis", "iguana", "jackal", "jaguar",
            "kangaroo", "koala", "lemur", "leopard", "llama", "manatee", "mule", "narwhal", "nutria", "octopus",
            "osprey", "ostrich", "owl", "penguin", "pigeon", "piranha", "puffin", "quagga", "quail", "quokka", "rat",
            "ray", "seal", "shark", "snake", "spider", "tern", "tiger", "turtle", "unicorn", "vicuna", "vole",
            "vulture", "walrus", "warthog", "worm", "xiphias", "yak", "zebra"
    };


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

    // Too dadgum slow
    //    public void test8() {
    //        String[] a = {"01", "02", "03", "04", "05",
    //                      "06", "07", "08"
    //                      };
    //        foreachPermutation(a, 0);
    //    }
    //
    //    public void test8Dupes() {
    //        String[] a = {"01", "03", "05",
    //                      "06", "07", "06", "05", "01"
    //                      };
    //        foreachPermutation(a, 0);
    //    }

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

    public void testAddsDescending() {
        int l = animals.length;

        for (int i = 0; i < l; i++) {
            int j = l - 1 - i;
            t.put(animals[j], animals[j]);
            assertEquals(t.size(), i + 1);
            assertEquals(t.min(), animals[j]);
            assertEquals(t.max(), animals[l - 1]);
            t.ok();
        }
        t.ok();
    }

    public void testAddsDeletesRandom() {

        Random r = new Random(0x12345555);

        for (int k = 0; k < 1000; k++) {

            testAddsDescending();
            int l = animals.length;
            for (int i = 0; i < l; i++) {
                int j = r.nextInt(l - i);
                String s = t.getKey(j);
                t.remove(s);
                assertEquals(t.size(), l - i - 1);
                t.ok();
            }
        }
    }


}
