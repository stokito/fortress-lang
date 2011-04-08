/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
/*
 * Created on Jun 8, 2007
 *
 */
package com.sun.fortress.useful;

import java.util.ArrayList;
import java.util.List;

public class VarArgs {
    public static <T> Iterable<T> make(T x1) {
        List<T> a = new ArrayList<T>(1);
        a.add(x1);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2) {
        List<T> a = new ArrayList<T>(2);
        a.add(x1);
        a.add(x2);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3) {
        List<T> a = new ArrayList<T>(3);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4) {
        List<T> a = new ArrayList<T>(4);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5) {
        List<T> a = new ArrayList<T>(5);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6) {
        List<T> a = new ArrayList<T>(6);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6, T x7) {
        List<T> a = new ArrayList<T>(7);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6, T x7, T x8) {
        List<T> a = new ArrayList<T>(8);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6, T x7, T x8, T x9) {
        List<T> a = new ArrayList<T>(9);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6, T x7, T x8, T x9, T x10) {
        List<T> a = new ArrayList<T>(10);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        a.add(x10);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6, T x7, T x8, T x9, T x10, T x11) {
        List<T> a = new ArrayList<T>(11);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        a.add(x10);
        a.add(x11);
        return a;
    }

    public static <T> Iterable<T> make(T x1, T x2, T x3, T x4, T x5, T x6, T x7, T x8, T x9, T x10, T x11, T x12) {
        List<T> a = new ArrayList<T>(12);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        a.add(x10);
        a.add(x11);
        a.add(x12);
        return a;
    }

    public static <T> Iterable<T> make(T x1,
                                       T x2,
                                       T x3,
                                       T x4,
                                       T x5,
                                       T x6,
                                       T x7,
                                       T x8,
                                       T x9,
                                       T x10,
                                       T x11,
                                       T x12,
                                       T x13) {
        List<T> a = new ArrayList<T>(13);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        a.add(x10);
        a.add(x11);
        a.add(x12);
        a.add(x13);
        return a;
    }

    public static <T> Iterable<T> make(T x1,
                                       T x2,
                                       T x3,
                                       T x4,
                                       T x5,
                                       T x6,
                                       T x7,
                                       T x8,
                                       T x9,
                                       T x10,
                                       T x11,
                                       T x12,
                                       T x13,
                                       T x14) {
        List<T> a = new ArrayList<T>(14);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        a.add(x10);
        a.add(x11);
        a.add(x12);
        a.add(x13);
        a.add(x14);
        return a;
    }

    public static <T> Iterable<T> make(T x1,
                                       T x2,
                                       T x3,
                                       T x4,
                                       T x5,
                                       T x6,
                                       T x7,
                                       T x8,
                                       T x9,
                                       T x10,
                                       T x11,
                                       T x12,
                                       T x13,
                                       T x14,
                                       T x15) {
        List<T> a = new ArrayList<T>(15);
        a.add(x1);
        a.add(x2);
        a.add(x3);
        a.add(x4);
        a.add(x5);
        a.add(x6);
        a.add(x7);
        a.add(x8);
        a.add(x9);
        a.add(x10);
        a.add(x11);
        a.add(x12);
        a.add(x13);
        a.add(x14);
        a.add(x15);
        return a;
    }
}
