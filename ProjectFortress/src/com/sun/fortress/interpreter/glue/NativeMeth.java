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

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.useful.HasAt;

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

    /** This simply calls through to the 2-argument applyMethod, and
     *  uses the location information to label any FortressError that
     *  is thrown. */
    public final FValue applyMethod(List<FValue> args, FObject selfValue,
                                    HasAt loc, Environment envForInference) {
        return applyMethod(args,selfValue);
    }

    protected abstract FValue applyMethod(List<FValue> args, FObject selfValue);
}
