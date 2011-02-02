/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.codegen.stubs.compiled2;

import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.compiler.codegen.stubs.compiled2.fortress.*;

public class CompilerSystem {
    public static final class args implements CompilerBuiltin.Object {
        public static FZZ32 length() {return FZZ32.make(simpleSystem.nativeArgcount());}
        public FString elem(FZZ32 n) {
            return FString.make(simpleSystem.nativeArg(n.getValue()));
        }
    }
    public static final args default_args = new args();
}
