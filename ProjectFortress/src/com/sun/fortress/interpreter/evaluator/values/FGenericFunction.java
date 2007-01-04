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

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.nodes.Applicable;
import com.sun.fortress.interpreter.nodes.FnDefOrDecl;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.useful.Factory1P;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Memo1P;
import com.sun.fortress.interpreter.useful.NI;


public class FGenericFunction extends Fcn
                              implements GenericFunctionOrMethod,
                              Factory1P<List<FType>, Simple_fcn, HasAt> {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.FValue#getString()
     */
    @Override
    public String getString() {
        return fndef.toString();
    }

    @Override
    public String toString() {
        return getString();
    }

    protected Simple_fcn newClosure(BetterEnv clenv, List<FType> args) {
        Closure cl = anyAreSymbolic(args) ? new ClosureInstance(clenv, fndef, args, this) : new Closure(clenv, fndef, args);
        return cl.finishInitializing();
    }

    private class Factory implements Factory1P<List<FType>, Simple_fcn, HasAt> {

        public Simple_fcn make(List<FType> args, HasAt within) {
            BetterEnv clenv = new BetterEnv(getEnv(), within);
            List<StaticParam> params = fndef.getStaticParams().getVal();
            EvalType.bindGenericParameters(params, args, clenv, within, fndef);

            return newClosure(clenv, args);
        }


    }

     Memo1P<List<FType>, Simple_fcn, HasAt> memo = new Memo1P<List<FType>, Simple_fcn, HasAt>(new Factory());

    public Simple_fcn make(List<FType> l, HasAt within) {
        return memo.make(l, within);
    }

    FnDefOrDecl fndef;

    public FnDefOrDecl getFnDefOrDecl() {
        return fndef;
    }

    public BetterEnv getEnv() {
        return getWithin();
    }

    public FGenericFunction(BetterEnv e, FnDefOrDecl fndef) {
        super(e);
        this.fndef = fndef;
    }

    public Simple_fcn typeApply(List<StaticArg> args, BetterEnv e, HasAt within) {
        EvalType et = new EvalType(e);
        // TODO Can combine these two functions if we enhance the memo and factory
        // to pass two parameters instead of one.
        ArrayList<FType> argValues = et.forStaticArgList(args);

        return typeApply(args, e, within, argValues);
    }

    /**
     * Same as typeApply, but with the types evaliated already.
     *
     * @param args
     * @param e
     * @param within
     * @param argValues
     * @return
     * @throws ProgramError
     */
    Simple_fcn typeApply(List<StaticArg> args, BetterEnv e, HasAt within, ArrayList<FType> argValues) throws ProgramError {
        List<StaticParam> params = fndef.getStaticParams().getVal();

        // Evaluate each of the args in e, inject into clenv.
        if (args.size() != params.size() ) {
            throw new ProgramError(within, e,  "Generic instantiation (size) mismatch, expected " + params + " got " + args);
        }
        return make(argValues, within);
    }

    @Override
    public FValue applyInner(List<FValue> args, HasAt loc, BetterEnv envForInference) {
        // TODO Auto-generated method stub
        NI.ni();
        return null;
    }

    @Override
    public FnName getFnName() {
        return fndef.getFnName();
    }

    public Applicable getDef() {
        return fndef;
    }



}
