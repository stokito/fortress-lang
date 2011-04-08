/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.codegen.stubs.compiled2.fortress;

import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.compiler.runtimeValues.*;

public class CompilerBuiltin_String_SpringBoard implements CompilerBuiltin_String {
    public static FString concatenate(FString a, FString b) {
        return FString.make(simpleConcatenate.nativeConcatenate(a.toString(), b.toString()));
    }

}
