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
