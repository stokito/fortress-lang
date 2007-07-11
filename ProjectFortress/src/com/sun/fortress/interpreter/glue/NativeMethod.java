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

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeArrow;
import com.sun.fortress.interpreter.evaluator.values.PartiallyDefinedMethod;


public class NativeMethod extends PartiallyDefinedMethod {
    List<FType> domain;

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.PartiallyDefinedMethod#completeClosure(com.sun.fortress.interpreter.evaluator.Environment)
     */
//    @Override
//    public MethodClosure completeClosure(BetterEnv com.sun.fortress.interpreter.env) {
//        // Not 100% sure about this.
//        return this;
//    }

    public List<FType> getDomain() {
        return domain;
    }

    NativeMethod(BetterEnv env, FType t, String name, List<FType> domain, FType range) {
        super(env, env, new NativeApplicable(name));
        this.domain = domain;
        // setParamsAndReturnType() ?
        setFtype(FTypeArrow.make(domain, range));
    }
}
