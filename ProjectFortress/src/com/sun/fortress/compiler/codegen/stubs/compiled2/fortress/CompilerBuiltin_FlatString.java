package com.sun.fortress.compiler.codegen.stubs.compiled2.fortress;

import com.sun.fortress.compiler.runtimeValues.*;

public class CompilerBuiltin_FlatString implements CompilerBuiltin_String {
    public static FString concatenate(FString a, FString b) {
        return CompilerBuiltin_String_SpringBoard.concatenate(a,b);
    }
}