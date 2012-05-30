/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.List;

/**
 * A NativeMeth indicates that a method is implemented natively; the
 * type is (like its parent NativeApp) abstract.
 * <p/>
 * Programmers writing a NativeMeth should implement getArity and applyMethod.
 * <p/>
 * As with NativeApp we don't check the declared type of the method on
 * the Fortress side against the type of the method implemented in the
 * native class.
 */
public abstract class NativeMeth3 extends NativeMeth {
    public abstract FValue applyMethod(FObject self, FValue a, FValue b, FValue c);

    public int getArity() {
        return 3;
    }

    public final FValue applyMethod(FObject selfValue, List<FValue> args) {
        return applyMethod(selfValue, args.get(0), args.get(1), args.get(2));
    }
}
