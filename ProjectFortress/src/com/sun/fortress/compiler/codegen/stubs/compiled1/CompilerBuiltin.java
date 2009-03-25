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
    public class FlatString implements String {}
    public interface Number {}
    public class RR32 implements Number {}
    public class RR64 implements Number {}
    public class ZZ32 implements Number {}
    public class ZZ64 implements Number {}

    public static void println(FString s) {
        simplePrintln.nativePrintln(s.toString());
    }
}