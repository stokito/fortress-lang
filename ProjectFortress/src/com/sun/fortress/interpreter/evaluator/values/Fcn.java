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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;


abstract public class Fcn extends FConstructedValue {
    /**
     * Need to know the environment so we can resolve
     * overloading/shadowing properly.
     */
    BetterEnv within;
    public BetterEnv getWithin() {
        return within;
    }

    protected Fcn(BetterEnv within) {
        this.within = within;
        within.bless();
    }

    final public FValue apply(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        args = conditionallyUnwrapTupledArgs(args);
        return applyInner(args, loc, envForInference);

    }

    public boolean isMethod() {
        return false;
    }

    protected List<FValue> conditionallyUnwrapTupledArgs(List<FValue> args) {
        // TODO This ought not be necessary.
        if (args.size() == 1 && (args.get(0) instanceof FTuple)) {
            args =  ((FTuple) args.get(0)).getVals();
         }
        return args;
    }

    abstract public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference);

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    abstract public FnName getFnName();

    /**
     * Returns the name if this "function" is regarded as a method.
     * Ought to throw an exception if it cannot be a method.
     */
    public String asMethodName() {
        return NodeUtil.nameString(getFnName());
    }

    static boolean anyAreSymbolic(List<FType> args) {
        for (FType t : args)
            if (t.isSymbolic())
                return true;
        return false;
    }

}
