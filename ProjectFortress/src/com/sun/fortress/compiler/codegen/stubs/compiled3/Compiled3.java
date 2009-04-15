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
package com.sun.fortress.compiler.codegen.stubs.compiled3;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.compiler.codegen.stubs.compiled3.fortress.*;


public class Compiled3 {

    public static FZZ32 fib(FZZ32 n) {
        if (n.getValue() < 2) 
            return FZZ32.make(1) ;
        else return FZZ32.plus(fib(FZZ32.make(n.getValue()-1)), fib(FZZ32.make(n.getValue()-2)));
    }

    public static void run() {
        CompilerBuiltin.println(fib(FZZ32.make(20)));
    }


    public static void main(String args[]) {
        systemHelper.registerArgs(args);
        run();
    }
}