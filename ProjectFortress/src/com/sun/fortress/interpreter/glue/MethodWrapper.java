/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import java.util.ArrayList;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.MethodClosure;
import com.sun.fortress.useful.HasAt;


/**
 * A simple no-frills wrapper for named methods of objects.
 * Caches the lookup, also caches the location (assumption
 * is that all invocations will be tied to a single location).
 */
public class MethodWrapper {
    MethodClosure mc;
    FObject self;
    HasAt at;

    public MethodWrapper(FObject fv, HasAt at, String name) {
        self = (FObject) fv;
        mc = (MethodClosure) self.getSelfEnv().getValue(name);
        this.at = at;
    }

    public FValue call(ArrayList<FValue> l, BetterEnv envForInference) {
        return mc.applyMethod(l,self,at, envForInference);
    }
}
