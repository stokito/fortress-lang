/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simplePrintln {

    public static void nativePrint(String s) {
        System.out.print(s);
    }
    
    public static void nativePrint(int i) {
        System.out.print(String.valueOf(i));
    }

    public static void nativePrintln(String s) {
        System.out.println(s);
    }
    
    public static void nativePrintln(int i) {
        System.out.println(String.valueOf(i));
    }

    public static void nativePrintlnWithThreadInfo(String s) {
        System.out.println(Thread.currentThread() + " " + s);
    }

    public static void nativePrintlnWithThreadInfo(int i) {
        System.out.println(Thread.currentThread() + " " + String.valueOf(i));
    }

    public static void nativeErrorPrint(String s) {
        System.err.print(s);
    }
    
    public static void nativeErrorPrint(int i) {
        System.err.print(String.valueOf(i));
    }

    public static void nativeErrorPrintln(String s) {
        System.err.println(s);
    }
    
    public static void nativeErrorPrintln(int i) {
        System.err.println(String.valueOf(i));
    }

    public static void nativeErrorPrintlnWithThreadInfo(String s) {
        System.err.println(Thread.currentThread() + " " + s);
    }

    public static void nativeErrorPrintlnWithThreadInfo(int i) {
        System.err.println(Thread.currentThread() + " " + String.valueOf(i));
    }
}
