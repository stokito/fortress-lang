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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

abstract public class Fcn extends FValue {
    /**
     * Need to know the environment so we can resolve
     * overloading/shadowing properly.
     */
    BetterEnv within;

    /**
     * Need to make type information mutable due to
     * multi-phase initialization protocol.
     */
    private volatile FType ftype;

    public BetterEnv getWithin() {
        return within;
    }

    /**
     * Getter for ftype.  Should always be non-null, but right now
     * FGenericFunction never calls setFtype and this returns null in
     * that case.  This leads to extensive bugs particularly when a
     * generic function is overloaded along with non-generic siblings,
     * or when a generic function is passed as an argument to an
     * overloaded function without providing an explicit type
     * instantiation.  Delete "&& false" to enable checking if you're
     * trying to fix this bug.
     */
    public FType type() {
        if (ftype==null && false) {
            throw new NullPointerException(errorMsg("No type information for ", this));
        }
        return ftype;
    }

    public void setFtype(FType ftype) {
        if (this.ftype != null)
            throw new IllegalStateException("Cannot set twice");
        this.ftype = ftype;
    }

    public void setFtypeUnconditionally(FType ftype) {
        this.ftype = ftype;
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

    abstract public SimpleName getFnName();

    /**
     * Returns the name if this "function" is regarded as a method.
     * Ought to throw an exception if it cannot be a method.
     */
    public String asMethodName() {
        return NodeUtil.nameString(getFnName());
    }

}
