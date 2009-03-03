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
package com.sun.fortress.compiler.codegen.stubs;


import com.sun.fortress.nodes.*;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nativeHelpers.simplePrintln;
import java.util.*;
import org.objectweb.asm.*;
import edu.rice.cs.plt.tuple.Option;


public class Compile0Stub {

    public static void main(String args[]) {
        run();
    }

    public static void run() {
        simplePrintln.nativePrintln("Hello World\n");
    }
}