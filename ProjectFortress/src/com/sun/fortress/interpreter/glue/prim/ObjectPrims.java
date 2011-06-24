/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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

        public final FString applyMethod(FObject self) {
            return FString.make(f(self));
        }
    }

    public static final class ToString extends O2S {
        protected final java.lang.String f(FObject o) {
            return (o.toString());
        }
    }

    public static final class ClassName extends O2S {
        protected final java.lang.String f(FObject o) {
            return (o.type().toString());
        }
    }

}
