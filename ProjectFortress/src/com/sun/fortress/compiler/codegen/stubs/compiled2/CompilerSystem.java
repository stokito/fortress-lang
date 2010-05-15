/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
