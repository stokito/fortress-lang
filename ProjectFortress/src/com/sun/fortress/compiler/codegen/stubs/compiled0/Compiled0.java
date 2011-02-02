/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen.stubs.compiled0;

import com.sun.fortress.nativeHelpers.simplePrintln;

public class Compiled0 {

    public static void main(String args[]) {
        run();
    }

    public static void run() {
        simplePrintln.nativePrintln("Hello World\n");
    }
}
