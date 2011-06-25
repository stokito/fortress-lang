/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen.stubs.compiled1;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.compiler.runtimeValues.*;

import java.util.*;
import org.objectweb.asm.*;
import edu.rice.cs.plt.tuple.Option;

public class CompilerBuiltin {

    public interface Object {}
    public interface String extends Object {}
    public static class FlatString implements String {}
    public interface Number {}
    public static class RR32 implements Number {}
    public static class RR64 implements Number {}
    public static class ZZ32 implements Number {}
    public static class ZZ64 implements Number {}
    public static class Character implements Object {}

    public static com.sun.fortress.compiler.runtimeValues.FVoid println(FString s) {
        simplePrintln.nativePrintln(s.toString());
        return com.sun.fortress.compiler.runtimeValues.FVoid.make();
    }
}
