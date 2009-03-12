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

package com.sun.fortress.interpreter.glue;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

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
public abstract class NativeMeth extends NativeApp implements Method {

    /** This is overridable only for perverse cases where we want to
     * use a NativeMeth as both a function and a method. */
    public FValue applyToArgs(List<FValue> args) {
        return bug(this,"applyToArgs (functions only) called for method "+this);
    }


    public FValue applyMethod(FObject self) {
        return applyMethod(self, Collections.<FValue>emptyList());
    }

    public FValue applyMethod(FObject self, FValue arg) {
        return applyMethod(self, Collections.singletonList(arg));
    }

    public FValue applyMethod(FObject self, FValue arg, FValue b) {
        return applyMethod(self, Useful.list(arg,b));
    }

    public FValue applyMethod(FObject self, FValue arg, FValue b, FValue c) {
        return applyMethod(self, Useful.list(arg,b,c));
    }

}
