/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleOverload {

//    public static void foo(int i, int j, int k, int l) {
//        System.out.println("IIII " + i + " " + j + " " + k + " " + l);
//    }
//
//    public static void foo(int i, int j, Number k, int l) {
//        System.out.println("IILI " + i + " " + j + " " + k + " " + l);
//    }
//
//    public static void foo(int i, int j, int k, Number l) {
//        System.out.println("IIIL " + i + " " + j + " " + k + " " + l);
//    }
//
//    public static void foo(int i, int j, Number k, Number l) {
//        System.out.println("IILL " + i + " " + j + " " + k + " " + l);
//    }
//
//    public static void foo(int i, int j, float k, float l) {
//        System.out.println("IIFF " + i + " " + j + " " + k + " " + l);
//    }

//    public static void foo(int i, int j, double k, double l) {
//        System.out.println("IIDD " + i + " " + j + " " + k + " " + l);
//    }
//
//    public static void foo(int i, String j, double k, double l) {
//        System.out.println("ISDD " + i + " " + j + " " + k + " " + l);
//    }


    public static String bar() {
        return "bar";
    }

    public static String baz(String s) {
        return "baz " + s;
    }

    public static String baz(int i) {
        return "baz " + i;
    }

    public static String duo(String s, String t) {
        return "baz " + (s + t);
    }

    public static String duo(String s) {
        return "baz " + s;
    }

    public static String duo(int i, int j) {
        return "baz " + (i + j);
    }

}
