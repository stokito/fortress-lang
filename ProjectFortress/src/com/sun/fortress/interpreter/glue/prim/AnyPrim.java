/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeFn2;

/**
 * Functions on Any type.
 */
public class AnyPrim {

    public static boolean sequiv(FValue x, FValue y) {
        if (x == y) return true;
        return x.seqv(y);
    }

    public static final class SEquiv extends NativeFn2 {
        protected FValue applyToArgs(FValue x, FValue y) {
            return FBool.make(AnyPrim.sequiv(x, y));
        }
    }

}
