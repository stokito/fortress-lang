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

package com.sun.fortress.interpreter.evaluator;

import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.MethodClosure;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.Applicable;
import com.sun.fortress.nodes.FnAbsDeclOrDecl;


public class BuildObjectEnvironment extends BuildTraitEnvironment {

    public BuildObjectEnvironment(BetterEnv within, BetterEnv methodEnvironment, Set<String> fields) {
        super(within, methodEnvironment, fields);
        // TODO Auto-generated constructor stub
    }

    protected Simple_fcn newClosure(BetterEnv e, Applicable x) {
        return new MethodClosure(e,x);
    }

    protected GenericMethod newGenericClosure(BetterEnv e, FnAbsDeclOrDecl x) {
        return new GenericMethod(e, e, x, false); // TODO need to get notself methods done
    }



}
