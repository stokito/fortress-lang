package com.sun.fortress.compiler.codegen.stubs.compiled3.fortress;

import com.sun.fortress.compiler.runtimeValues.*;
import com.sun.fortress.nativeHelpers.*;

public class CompilerBuiltin {
    public static void println(FString s) {
        simplePrintln.nativePrintln(s.toString());
    }

    // Total hack to work around lack of overloading.
    public static void println(FZZ32 n) {
        System.out.println(n.toString());
    }
}