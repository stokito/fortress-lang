/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen.stubs.compiled1;

import com.sun.fortress.compiler.runtimeValues.FString;

public class Compiled1 {

    public static void main(String args[]) {
        run();
    }

    public static void run() {
        CompilerBuiltin.println(FString.make("Hello Word\n"));
    }
}
