/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.NI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * LHSToLValue evaluates the provided LHS to the point that we can,
 * without duplication of computation, find both its current value
 * using Evaluator or AtomicEvaluator, and also assign to it using
 * ALHSEvaluator.  Cache computations are represented using
 * _WrappedFValue AST com.sun.fortress.interpreter.nodes.
 */
public class LHSToLValue extends NodeAbstractVisitor<Lhs> {
    Evaluator evaluator;

    LHSToLValue(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    private Expr wrapEval(Expr x, String desc) {
        FValue v = x.accept(evaluator);
        if (v instanceof FObject) {
            return new _WrappedFValue(NodeUtil.getSpan(x), false, v);
        }
        return error(x, evaluator.e, desc);
    }

    public List<Expr> wrapEvalParallel(List<Expr> es) {
        List<FValue> unwrapped = evaluator.evalExprListParallel(es);
        ArrayList<Expr> res = new ArrayList<Expr>(unwrapped.size());
        Iterator<Expr> eIt = es.iterator();
        for (FValue unw : unwrapped) {
            Expr e = eIt.next();
            res.add(new _WrappedFValue(NodeUtil.getSpan(e), false, unw));
        }
        return res;
    }

    /**
     * Evaluate provided Exprs to obtain LValues.
     */
    public List<Lhs> inParallel(List<? extends Lhs> es) {
        // TODO: In parallel!
        ArrayList<Lhs> res = new ArrayList<Lhs>(es.size());
        for (Lhs e : es) {
            res.add(e.accept(this));
        }
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public Lhs forSubscriptExpr(SubscriptExpr x) {
        Expr warray = wrapEval(x.getObj(), "Indexing non-object.");
        List<Expr> wsubs = wrapEvalParallel(x.getSubs());
        return ExprFactory.makeSubscriptExpr(NodeUtil.getSpan(x), warray, wsubs, x.getOp(), x.getStaticArgs());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFieldRef(com.sun.fortress.interpreter.nodes.FieldRef)
     */
    @Override
    public Lhs forFieldRef(FieldRef x) {
        Expr from = wrapEval(x.getObj(), "Non-object in field selection");
        // TODO need to generalize to dotted names.
        return ExprFactory.makeFieldRef(NodeUtil.getSpan(x), from, x.getField());
    }

    public Lhs forVarRef(VarRef x) {
        return x;
    }

    public Lhs forLValue(LValue x) {
        return x;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTupleExpr(com.sun.fortress.interpreter.nodes.TupleExpr)
     */
    @Override
    public Lhs forTupleExpr(TupleExpr x) {
        return NI.nyi("nested tuple in LHS of binding");
    }
}
