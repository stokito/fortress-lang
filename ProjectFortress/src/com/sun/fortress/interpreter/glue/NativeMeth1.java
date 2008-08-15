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

package com.sun.fortress.interpreter.glue;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;

/**
 * A NativeMeth indicates that a method is implemented natively; the
 * type is (like its parent NativeApp) abstract.
 *
 * Programmers writing a NativeMeth should implement getArity and applyMethod.
 *
 * As with NativeApp we don't check the declared type of the method on
 * the Fortress side against the type of the method implemented in the
 * native class.
 */
public abstract class NativeMeth1 extends NativeMeth {
    protected abstract FValue act(FObject self, FValue a);

    public int getArity() {
        return 1;
    }
    protected final FValue applyMethod(List<FValue> args, FObject selfValue) {
        return act(selfValue, args.get(0));
    }
}
