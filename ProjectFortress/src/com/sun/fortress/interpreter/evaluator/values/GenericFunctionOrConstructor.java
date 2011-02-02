/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvaluatorBase;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.useful.BATreeEC;

import java.util.List;

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
    public FValue applyInnerPossiblyGeneric(List<FValue> args) {
        Simple_fcn foo = cache.get(args);
        if (foo == null) {
            foo = EvaluatorBase.inferAndInstantiateGenericFunction(args, this, getWithin());
            cache.syncPut(args, foo);
        }
        return foo.applyInnerPossiblyGeneric(args);
    }


    abstract protected Simple_fcn getSymbolic() throws Error, ProgramError;

}
