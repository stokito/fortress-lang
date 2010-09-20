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

package com.sun.fortress.nativeHelpers;

public class simplePrintln {

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
}
