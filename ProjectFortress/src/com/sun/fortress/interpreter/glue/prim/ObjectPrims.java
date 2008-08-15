/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.glue.prim;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.glue.NativeMeth0;

/**
 * Functions from String.
 */
public class ObjectPrims {

protected static abstract class O2S extends NativeMeth0 {
    protected abstract java.lang.String f(FObject o);
    protected final FString act(FObject self) {
        return FString.make(f(self));
    }
}

public static final class ToString extends O2S {
    protected final java.lang.String f(FObject o) {
        return (o.toString());
    }
}

}
