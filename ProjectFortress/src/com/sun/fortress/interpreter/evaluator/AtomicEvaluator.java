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

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.nodes.Assignment;
import com.sun.fortress.interpreter.nodes.Block;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.LHS;
import com.sun.fortress.interpreter.nodes.NodeVisitor;
import com.sun.fortress.interpreter.nodes.Op;
import com.sun.fortress.interpreter.nodes.Option;

import dstm2.Thread;
import dstm2.Transaction;

public class AtomicEvaluator extends EvaluatorBase<FValue> implements
        NodeVisitor<FValue> {

    AtomicEvaluator(Evaluator evaluator) {
        // To simplify life, we extend EvaluatorBase to get access to
        // some com.sun.fortress.interpreter.useful methods.  'com.sun.fortress.interpreter.evaluator' is semi-redundant.
        super(evaluator.e);
        this.evaluator = evaluator;
    }

    Evaluator evaluator;

    boolean debug = false;

    public void debugPrint(String debugString) {
        if (debug)
            System.out.println(debugString);
    }

    // We ask lhs to accept twice (with this and an LHSEvaluator) in
    // the operator case. Might this cause the world to break?
    public FValue forAssignment(Assignment x) {
        // debugPrint("forAssignment " + x);
        Option<Op> possOp = x.getOp();
        List<? extends LHS> lhses = x.getLhs();
        int lhsSize = lhses.size();

        startover: while (true) {
            FValue rhs = x.getRhs().accept(evaluator);
            Iterator<FValue> rhs_it = null;

            // Assignment might be single-single, or tuple-tuple.
            // Make sure that everything matches up, and initialize
            // rhs_it-erator if it is needed.

            if (lhsSize > 1 && rhs instanceof FTuple) {
                FTuple rhs_tuple = (FTuple) rhs;
                rhs_it = rhs_tuple.getVals().iterator();
                // Verify, now, that LHS and RHS sizes match.
                if (rhs_tuple.getVals().size() != lhsSize) {
                    throw new ProgramError(x,
                            "Tuple assignment size mismatch, | lhs | = "
                                    + lhses.size() + ", | rhs | = "
                                    + rhs_tuple.getVals().size());
                }
            } else if (lhsSize != 1) {
                throw new ProgramError(x,
                            "Tuple assignment size mismatch, | lhs | = "
                                    + lhses.size() + ", rhs is not a tuple");
            }

            if (possOp.isPresent()) {

                // This is not good enough yet. We need to
                // create an "lvalue" for the various possible LHSes,
                // where the lvalue has had all its side-effects evaluated
                // out, so that we can obtain a value, and then assign a value.
                // TODO this will need careful study for the case of fields,
                // getters, and setters.

                // TODO HELP! We need transactions, we simply cannot generalize
                // all this w.r.t. side-effects etc.  Right now, if there are side-effects,
                // they will simply NOT BE UNDONE!

                if (lhses.size() != 1)
                    throw new InterpreterError(x, evaluator.e,
                                    "Don't yet implement multiple variable "+
                                    "atomic assignment, sorry");

                for (LHS lhs : lhses) {
                    if (rhs_it != null) {
                        rhs = rhs_it.next();
                    }
                    Op op = possOp.getVal();
                    FValue lhsValue = lhs.accept(evaluator);
                    Fcn fcn = (Fcn) op.accept(evaluator);

                    List<FValue> vargs = new ArrayList<FValue>(2);
                    vargs.add(lhsValue);
                    vargs.add(rhs);
                    FBool resValue = (FBool) lhs.accept(new LHSAtomicEvaluator(
                            evaluator, lhsValue, functionInvocation(vargs, fcn,
                                    x)));

                    if (!resValue.getBool().booleanValue())
                        continue startover;

                }
                return FVoid.V;
            } else {
                throw new InterpreterError(x, evaluator.e,
                        "Atomics not implemented yet");

            }
        }

    }

    FValue doBlock(List<Expr> exprs) {
	FValue res = FVoid.V;
        for (Expr expr : exprs) {
            res = expr.accept(evaluator);
       }
        return res;
    }

    public FValue forBlock(Block x) {
        debugPrint("forBlock " + x);
        final List<Expr> exprs = x.getExprs();
        FValue res = Thread.doIt(new Callable<FValue>() { public FValue call() {
                           return doBlock(exprs);}

                     });
        return res;
    }
}
