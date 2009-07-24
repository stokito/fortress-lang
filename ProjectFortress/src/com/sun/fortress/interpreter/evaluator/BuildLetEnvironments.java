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

import com.sun.fortress.exceptions.FortressException;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.env.IndirectionCell;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FunctionClosure;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.Iterator;
import java.util.List;

public class BuildLetEnvironments extends NodeAbstractVisitor<FValue> {

    boolean firstPass = true;

    Environment containing;

    public BuildLetEnvironments(Environment within) {
        this.containing = within;
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forLetFn(com.sun.fortress.interpreter.nodes.LetFn)
     */
    @Override
    public FValue forLetFn(LetFn x) {
        List<FnDecl> fns = x.getFns();
        List<Expr> body = x.getBody();

        for (int i = 0; i < fns.size(); i++) {
            FnDecl fn = fns.get(i);

            IdOrOpOrAnonymousName name = NodeUtil.getName(fn);
            String fname = NodeUtil.nameString(name);
            List<Param> params = NodeUtil.getParams(fn);
            Option<Type> retType = NodeUtil.getReturnType(fn);

            FunctionClosure cl = new FunctionClosure(containing, fn);
            try {
                containing.putValue(fname, cl);
            }
            catch (FortressException pe) {
                throw pe.setContext(x, containing);
            }
            // TODO Local functions cannot be Enclosing, can they?
            // Local functions cannot be any operator including Enclosing.
            FType ft = EvalType.getFTypeFromOption(retType, containing, FTypeTop.ONLY);
            List<Parameter> fparams = EvalType.paramsToParameters(containing, params);
            cl.setParamsAndReturnType(fparams, ft);
        }
        return (new Evaluator(containing)).evalExprList(body, x);
    }

    public FValue forLocalVarDecl(LocalVarDecl x) {
        List<LValue> lhs = x.getLhs();
        Option<Expr> rhs = x.getRhs();
        List<Expr> body = x.getBody();
        //FValue res = Evaluator.evVoid;

        Evaluator new_eval = new Evaluator(containing);
        if (rhs.isSome()) {
            if (lhs.size() == 1) {
                FValue val = rhs.unwrap().accept(new_eval);
                LHSEvaluator lhs_eval = new LHSEvaluator(new_eval, val);
                lhs.get(0).accept(lhs_eval);
            } else {
                FValue val = rhs.unwrap().accept(new_eval);

                if (!(val instanceof FTuple)) {
                    error(x, containing, errorMsg("RHS does not yield a tuple"));
                }

                FTuple rTuple = (FTuple) val;
                Iterator<LValue> i = lhs.iterator();
                Iterator<FValue> j = rTuple.getVals().iterator();
                while (i.hasNext() && j.hasNext()) {
                    LValue lval = i.next();
                    FValue rval = j.next();

                    LHSEvaluator lhs_eval = new LHSEvaluator(new_eval, rval);
                    lval.accept(lhs_eval);
                }
                if (i.hasNext()) {
                    LValue lval = i.next();
                    error(lval, containing, errorMsg("Extra lvalue"));
                } else if (j.hasNext()) {
                    FValue rval = j.next();
                    error(rhs.unwrap(), containing, errorMsg("Extra rvalue"));
                }
            }
        } else {
            EvalType eval_type = new EvalType(containing);
            for (LValue lvb : lhs) {
                if (lvb.isMutable() && lvb.getIdType().isSome()) {
                    FValue fv = lvb.accept(new_eval);
                    FType fvt = lvb.getIdType().unwrap().accept(eval_type);
                    containing.putVariable(fv.getString(), fvt);
                } else {
                    containing.putValue(lvb.accept(new_eval), new IndirectionCell());
                }
            }

        }
        return new_eval.evalExprList(body, x);
    }

    public FValue doLets(LetExpr exp) {
        return exp.accept(this);
    }

}
