/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.useful.Useful;

import java.util.Collections;
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
public abstract class NativeMeth extends NativeApp implements Method {

    /**
     * This is overridable only for perverse cases where we want to
     * use a NativeMeth as both a function and a method.
     */
    public FValue applyToArgs(List<FValue> args) {
        return bug(this, "applyToArgs (functions only) called for method " + this);
    }


    public FValue applyMethod(FObject self) {
        return applyMethod(self, Collections.<FValue>emptyList());
    }

    public FValue applyMethod(FObject self, FValue arg) {
        return applyMethod(self, Collections.singletonList(arg));
    }

    public FValue applyMethod(FObject self, FValue arg, FValue b) {
        return applyMethod(self, Useful.list(arg, b));
    }

    public FValue applyMethod(FObject self, FValue arg, FValue b, FValue c) {
        return applyMethod(self, Useful.list(arg, b, c));
    }

}
