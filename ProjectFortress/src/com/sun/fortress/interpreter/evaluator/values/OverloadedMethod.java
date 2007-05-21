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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.Fun;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Useful;


public class OverloadedMethod extends OverloadedFunction implements Method {

    public OverloadedMethod(String fnName, BetterEnv within) {
        super(new Fun(fnName), within);
        // TODO Auto-generated constructor stub
    }

    public OverloadedMethod(String fnName, Set<? extends Simple_fcn> ssf,
            BetterEnv within) {
        super(new Fun(fnName), ssf, within);
        // TODO Auto-generated constructor stub
    }

   static int lastBest;

   public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc, BetterEnv envForInference) {

        int best = bestMatchIndex(args, loc, envForInference);
        lastBest = best;

        if (best == -1) {
            // TODO add checks for COERCE, right here.
            throw new ProgramError(loc,  within,
                         "Failed to find matching overload, args = " +
                         Useful.listInParens(args) + ", overload = " + this);
        }

        return ((Method)overloads.get(best).getFn()).applyMethod(args, selfValue, loc, envForInference);
    }



}
