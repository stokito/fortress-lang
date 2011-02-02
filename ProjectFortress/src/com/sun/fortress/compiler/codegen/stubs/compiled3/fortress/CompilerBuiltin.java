/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.codegen.stubs.compiled3.fortress;

import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.nativeHelpers.*;

public final class CompilerBuiltin {
    public static FVoid println(FString s) {
        simplePrintln.nativePrintln(s.toString());
        return FVoid.make();
    }

    // Total hack to work around lack of overloading.
    public static FVoid println(FZZ32 n) {
        System.out.println(n.toString());
        return FVoid.make();
    }
}
