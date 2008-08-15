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

package com.sun.fortress.interpreter.evaluator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Lhs;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.ArgExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Unpasting;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes._RewriteFieldRef;
import com.sun.fortress.interpreter.evaluator._WrappedFValue;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.useful.NI;

import static com.sun.fortress.exceptions.ProgramError.error;

/**
 * LHSToLValue evaluates the provided LHS to the point that we can,
 * without duplication of computation, find both its current value
 * using Evaluator or AtomicEvaluator, and also assign to it using
 * ALHSEvaluator.  Cache computations are represented using
 * _WrappedFValue AST com.sun.fortress.interpreter.nodes.
 */
public class LHSToLValue extends NodeAbstractVisitor<Lhs>  {
    Evaluator evaluator;

    LHSToLValue(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    private Expr wrapEval(Expr x, String desc) {
        FValue v = x.accept(evaluator);
        if (v instanceof FObject) {
            return new _WrappedFValue(x.getSpan(), false, v);
        }
        return error(x,evaluator.e, desc);
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
        return ExprFactory.makeSubscriptExpr(x.getSpan(), warray, wsubs,
                                             x.getOp(), x.getStaticArgs());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFieldRef(com.sun.fortress.interpreter.nodes.FieldRef)
     */
    @Override
    public Lhs forFieldRef(FieldRef x) {
        Expr from = wrapEval(x.getObj(), "Non-object in field selection");
        // TODO need to generalize to dotted names.
        return ExprFactory.makeFieldRef(x.getSpan(), from, x.getField());
    }


    @Override
    public Lhs for_RewriteFieldRef(_RewriteFieldRef x) {
        Expr from = wrapEval(x.getObj(), "Non-object in field selection");
        // TODO need to generalize to dotted names.
        return new _RewriteFieldRef(x.getSpan(), false, from, x.getField());
    }

    public Lhs forVarRef(VarRef x) {
        return x;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingBind(com.sun.fortress.interpreter.nodes.UnpastingBind)
     */
    @Override
    public Lhs forUnpastingBind(UnpastingBind x) {
        Id name = x.getName();
        List<ExtentRange> dim = x.getDim();
        return super.forUnpastingBind(x);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forUnpastingSplit(com.sun.fortress.interpreter.nodes.UnpastingSplit)
     */
    @Override
    public Lhs forUnpastingSplit(UnpastingSplit x) {
        int dim = x.getDim();
        List<Unpasting> elems = x.getElems();
        return super.forUnpastingSplit(x);
    }

    public Lhs forLValueBind(LValueBind x) {
        return x;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forArgExpr(com.sun.fortress.interpreter.nodes.ArgExpr)
     */
    @Override
    public Lhs forArgExpr(ArgExpr x) {
        return NI.nyi("nested tuple in LHS of binding");
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTupleExpr(com.sun.fortress.interpreter.nodes.TupleExpr)
     */
    @Override
    public Lhs forTupleExpr(TupleExpr x) {
        return NI.nyi("nested tuple in LHS of binding");
    }
}
