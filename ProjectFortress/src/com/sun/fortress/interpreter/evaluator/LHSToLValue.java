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
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.NodeAbstractVisitor;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.ExtentRange;
import com.sun.fortress.interpreter.nodes.FieldSelection;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.LHS;
import com.sun.fortress.interpreter.nodes.LValueBind;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.nodes.SubscriptExpr;
import com.sun.fortress.interpreter.nodes.TupleExpr;
import com.sun.fortress.interpreter.nodes.Unpasting;
import com.sun.fortress.interpreter.nodes.UnpastingBind;
import com.sun.fortress.interpreter.nodes.UnpastingSplit;
import com.sun.fortress.interpreter.nodes.VarRefExpr;
import com.sun.fortress.interpreter.nodes._WrappedFValue;
import com.sun.fortress.interpreter.nodes_util.ExprFactory;
import com.sun.fortress.interpreter.useful.NI;


/**
 * LHSToLValue evaluates the provided LHS to the point that we can,
 * without duplication of computation, find both its current value
 * using Evaluator or AtomicEvaluator, and also assign to it using
 * ALHSEvaluator.  Cache computations are represented using
 * _WrappedFValue AST com.sun.fortress.interpreter.nodes.
 */
public class LHSToLValue extends NodeAbstractVisitor<LHS>  {
    Evaluator evaluator;

    LHSToLValue(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    private Expr wrapEval(Expr x, String desc) {
        FValue v = x.visit(evaluator);
        if (v instanceof FObject) {
            return new _WrappedFValue(x.getSpan(), false, v);
        }
        throw new ProgramError(x,evaluator.e,desc);
    }

    public List<Expr> wrapEvalParallel(List<Expr> es) {
        List<FValue> unwrapped = evaluator.evalExprListParallel(es);
        ArrayList<Expr> res = new ArrayList<Expr>(unwrapped.size());
        Iterator<Expr> eIt = es.iterator();
        for (FValue unw : unwrapped) {
            Expr e = eIt.next();
            res.add(new _WrappedFValue(e.getSpan(), false, unw));
        }
        return res;
    }

    /**
     * Evaluate provided Exprs to obtain LValues.
     */
    public List<LHS> inParallel(List<? extends LHS> es) {
        // TODO: In parallel!
        ArrayList<LHS> res = new ArrayList<LHS>(es.size());
        for (LHS e : es) {
            res.add(e.visit(this));
        }
        return res;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public LHS forSubscriptExpr(SubscriptExpr x) {
        Expr warray = wrapEval(x.getObj(), "Indexing non-object.");
        List<Expr> wsubs = wrapEvalParallel(x.getSubs());
        return ExprFactory.makeSubscriptExpr(x.getSpan(), warray, wsubs);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFieldSelection(com.sun.fortress.interpreter.nodes.FieldSelection)
     */
    @Override
    public LHS forFieldSelection(FieldSelection x) {
        Expr from = wrapEval(x.getObj(), "Non-object in field selection");
        // TODO need to generalize to dotted names.
        return new FieldSelection(x.getSpan(), false, from, x.getId());
    }

    public LHS forVarRefExpr(VarRefExpr x) {
        return x;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingBind(com.sun.fortress.interpreter.nodes.UnpastingBind)
     */
    @Override
    public LHS forUnpastingBind(UnpastingBind x) {
        Id id = x.getName();
        Option<List<ExtentRange>> dim = x.getDim();
        return super.forUnpastingBind(x);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingSplit(com.sun.fortress.interpreter.nodes.UnpastingSplit)
     */
    @Override
    public LHS forUnpastingSplit(UnpastingSplit x) {
        int dim = x.getDim();
        List<Unpasting> elems = x.getElems();
        return super.forUnpastingSplit(x);
    }

    public LHS forLValueBind(LValueBind x) {
        return x;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTupleExpr(com.sun.fortress.interpreter.nodes.TupleExpr)
     */
    @Override
    public LHS forTupleExpr(TupleExpr x) {
        return NI.nyi("nested tuple in LHS of binding");
    }
}
