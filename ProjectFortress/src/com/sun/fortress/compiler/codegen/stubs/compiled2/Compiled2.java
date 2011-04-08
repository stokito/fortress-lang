/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen.stubs.compiled2;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.compiler.codegen.stubs.compiled2.fortress.*;


public class Compiled2 {

    public static void run() {
        CompilerBuiltin.println(
            CompilerBuiltin_FlatString.concatenate(
               CompilerBuiltin_FlatString.concatenate(FString.make("Hello "),
                                                      CompilerSystem.default_args.elem(FZZ32.make(0))),
               FString.make("!")));
    }

    public static FVoid tempRun() {
        CompilerBuiltin.println(CompilerSystem.default_args.elem(FZZ32.make(0)));
        return FVoid.make();
    }

    public static void main(String args[]) {
        systemHelper.registerArgs(args);
        tempRun();
    }
}
