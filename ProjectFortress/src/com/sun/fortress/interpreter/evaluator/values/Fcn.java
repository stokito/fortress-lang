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

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.util.List;

import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.UnificationError;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

abstract public class Fcn extends FValue {
    /**
     * Need to know the environment so we can resolve
     * overloading/shadowing properly.
     */
    Environment within;

    /**
     * Need to make type information mutable due to
     * multi-phase initialization protocol.
     */
    private volatile FType ftype;

    protected Fcn(Environment within) {
        this.within = within;
        within.bless();
    }

    public Environment getWithin() {
        return within;
    }
    
    public boolean needsInference() {
        return false;
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
            throw new NullPointerException(errorMsg("No type information for ", this,
                                                    " ", this.getClass()));
        }
        return ftype;
    }

    /**
     * Finish initializing Fcn, if necessary.
     *
     * There used to be a HasFinishInitializing interface for this,
     * but after cleaning up the code a bit it became clear that we
     * should just let it apply to any Fcn.
     */
    public void finishInitializing() {
        // By default, nothing need be done.
    }

    public void setFtype(FType ftype) {
        if (this.ftype != null)
            throw new IllegalStateException("Cannot set twice");
        setFtypeUnconditionally(ftype);
    }

    public  void setFtypeUnconditionally(FType ftype) {
        this.ftype = ftype;
    }

    protected FValue check(FValue x) {
            return x;
    }

    final public FValue applyPossiblyGeneric(List<FValue> args, HasAt site) {
        List<FValue> unwrapped = conditionallyUnwrapTupledArgs(args);
        try {
            return check(applyInnerPossiblyGeneric(unwrapped, site));
        } catch (UnificationError u) {
            if (unwrapped != args) {
                try {
                    return check(applyInnerPossiblyGeneric(args, site));
                } catch (UnificationError u1) {
                    throw u;
                }
            }
            throw u;
        }
    }

    protected List<FValue> conditionallyUnwrapTupledArgs(List<FValue> args) {
        // TODO This ought not be necessary.
        if (args.size() == 1 && (args.get(0) instanceof FTuple)) {
            args =  ((FTuple) args.get(0)).getVals();
        }
        return args;
    }

    abstract public FValue applyInnerPossiblyGeneric(List<FValue> args, HasAt site);

    public boolean hasSelfDotMethodInvocation() {
        return false;
    }

    abstract public IdOrOpOrAnonymousName getFnName();

    /**
     * Returns the name if this "function" is regarded as a method.
     * For functions, just returns the name.
     */
    public String asMethodName() {
        return NodeUtil.nameString(getFnName());
    }


    public static FValue functionInvocation(FValue arg, FValue foo, HasAt loc) {
        return functionInvocation(Useful.list(arg), foo, loc);
    }

    public FValue functionInvocation(FValue arg, HasAt loc) {
        return this.functionInvocation(Useful.list(arg), loc);
    }

    public static FValue functionInvocation(List<FValue> args, FValue foo,
                                            HasAt loc) {
        if (foo instanceof Fcn) {
            return ((Fcn)foo).functionInvocation(args, loc);
        } else {
            return bug(loc, errorMsg("Not a Fcn: ", foo));
        }
    }

    public FValue functionInvocation(List<FValue> args, HasAt site) {
        try {
            // We used to do redundant checks for genericity here, but
            // now we reply on foo.apply to do type inference if necessary.
            return this.applyPossiblyGeneric(args, site);
        } catch (FortressException ex) {
            throw ex.setWhere(site);
        } catch (StackOverflowError soe) {
            return error(site,errorMsg("Stack overflow on ",site));
        }
    }

}
