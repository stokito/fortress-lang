/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.codegen.stubs.compiled2.fortress;

import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.nativeHelpers.*;

public class CompilerBuiltin {
    public static void println(FString s) {
        simplePrintln.nativePrintln(s.toString());
    }

    public static interface Object extends AnyType.Any {
        public static class SpringBoard {
        }
    }
}
