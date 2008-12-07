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
package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvaluatorBase;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.useful.BATreeEC;
import com.sun.fortress.useful.HasAt;

abstract public class GenericFunctionOrConstructor extends SingleFcn implements GenericFunctionOrMethod {

    BATreeEC<List<FValue>, List<FType>, Simple_fcn> cache =
        new BATreeEC<List<FValue>, List<FType>, Simple_fcn>(FValue.asTypesList);

    volatile Simple_fcn symbolicInstantiation;

    public GenericFunctionOrConstructor(Environment within) {
        super(within);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean needsInference() {
        return true;
    }

    @Override
    public List<FType> getDomain() {
        return getSymbolic().getDomain();

    }

    @Override
    public FType getRange() {
        return getSymbolic().getRange();

    }

    @Override
    public FValue applyInnerPossiblyGeneric(List<FValue> args, HasAt site) {
        Simple_fcn foo = cache.get(args);
        if (foo == null) {
            foo = EvaluatorBase.inferAndInstantiateGenericFunction(args, this, site, getWithin());
            cache.syncPut(args, foo);
        }
        return foo.applyPossiblyGeneric(args, site);
    }



    abstract protected Simple_fcn getSymbolic() throws Error, ProgramError;

}
