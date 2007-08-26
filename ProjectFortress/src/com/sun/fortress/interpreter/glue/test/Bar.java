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

package com.sun.fortress.interpreter.glue.test;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.useful.HasAt;

public class Bar extends NativeApp implements Method {

    @Override
    public FValue applyToArgs(List<FValue> args) {
        // TODO Auto-generated method stub
        throw new Error();
    }

    @Override
    public int getArity() {
        // Glitch -- the arity must match the SYNTACTIC arity of the method.
        return 2;
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc,
            BetterEnv envForInference) {
        FValue x = selfValue.getSelfEnv().getValue("x");
        FValue s = args.get(0);
        return FString.make(s.getString() + x.getString());
    }

}
