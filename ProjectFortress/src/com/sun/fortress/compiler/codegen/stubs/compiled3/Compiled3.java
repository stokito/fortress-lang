/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen.stubs.compiled3;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.runtimeSystem.*;
import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.compiler.codegen.stubs.compiled3.fortress.*;

public final class Compiled3 extends FortressExecutable {

    public static FZZ32 fib(FZZ32 n) {
        if (n.getValue() < 2)
            return FZZ32.make(1) ;
        else return FZZ32.plus(fib(FZZ32.make(n.getValue()-1)), fib(FZZ32.make(n.getValue()-2)));
    }

    public static FVoid run() {
        return CompilerBuiltin.println(fib(FZZ32.make(20)));
    }

    public static void main(String args[]) {
        (new Compiled3()).runExecutable(args);
    }

    public void compute() {
        run();
    }
}
