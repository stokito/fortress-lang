package com.sun.fortress.compiler.codegen.stubs.compiled2.fortress;

import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.nativeHelpers.*;

public class CompilerBuiltin {
    public static void println(FString s) {
        simplePrintln.nativePrintln(s.toString());
    }
}