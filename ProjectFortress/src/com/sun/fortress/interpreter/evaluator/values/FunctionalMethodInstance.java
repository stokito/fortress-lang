/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Applicable;

import java.util.List;

public class FunctionalMethodInstance extends FunctionalMethod {

    public FunctionalMethodInstance(Environment e,
                                    Applicable fndef,
                                    List<FType> args,
                                    FGenericFunction generator,
                                    int self_parameter_index,
                                    FTraitOrObjectOrGeneric self_parameter_type) {
        super(e, fndef, args, self_parameter_index, self_parameter_type);
        this.generator = generator;
    }

    FGenericFunction generator;

    public FGenericFunction getGenerator() {
        return generator;
    }


}
