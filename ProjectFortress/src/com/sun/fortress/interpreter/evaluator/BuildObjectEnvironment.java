/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.MethodClosure;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.FnDecl;

import java.util.Set;


public class BuildObjectEnvironment extends BuildTraitEnvironment {


    public BuildObjectEnvironment(Environment within,
                                  Environment methodEnvironment,
                                  FType definer,
                                  Set<String> fields) {
        super(within, methodEnvironment, definer, fields);
        // TODO Auto-generated constructor stub
    }

    protected Simple_fcn newClosure(Environment e, Applicable x) {
        return new MethodClosure(containing, x, definer);
    }

    protected GenericMethod newGenericClosure(Environment e, FnDecl x) {
        return new GenericMethod(containing, e, x, definer, false);
    }


}
