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

import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.IndirectionCell;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.nodes.NodeVisitor;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FnDecl;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.LValue;
import com.sun.fortress.interpreter.nodes.LValueBind;
import com.sun.fortress.interpreter.nodes.LetExpr;
import com.sun.fortress.interpreter.nodes.LetFn;
import com.sun.fortress.interpreter.nodes.LocalVarDecl;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.TypeRef;



public class BuildLetEnvironments extends NodeVisitor<FValue> {

    boolean firstPass = true;

    BetterEnv containing;

    public BuildLetEnvironments(BetterEnv within) {
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

            FnName name = fn.getFnName();
            //Expr expr = fn.getBody();
            String fname = name.name();
            List<Param> params = fn.getParams();
            Option<TypeRef> retType = fn.getReturnType();

            Closure cl = new Closure(containing, fn);
            try {
                containing.putValue(fname, cl);
            } catch (ProgramError pe) {
                pe.setWithin(containing);
                pe.setWhere(x);
            }
            // TODO Local functions cannot be Enclosing, can they?
            FType ft = EvalType.getFTypeFromOption(retType,containing);
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
        if (rhs.isPresent()) {
            if (lhs.size() == 1) {
                FValue val = rhs.getVal().accept(new_eval);
                LHSEvaluator lhs_eval = new LHSEvaluator(new_eval, val);
                lhs.get(0).accept(lhs_eval);
            } else {
                FValue val = rhs.getVal().accept(new_eval);

                if (!(val instanceof FTuple)) {
                    throw new ProgramError(x, containing,
                                           "RHS does not yield a tuple");
                }

                FTuple rTuple = (FTuple) val;
                Iterator<LValue> i = lhs.iterator();
                for (Iterator<FValue> j = rTuple.getVals().iterator();
                        j.hasNext();) {
                    LValue lval = i.next();
                    FValue rval = j.next();

                    LHSEvaluator lhs_eval = new LHSEvaluator(new_eval, rval);
                    lval.accept(lhs_eval);
                }
            }
        } else {
            EvalType eval_type = new EvalType(containing);
            for (LValue lval : lhs) {
                if (lval instanceof LValueBind) {
                    LValueBind lvb = (LValueBind) lval;
                    if (lvb.getMutable()) {
                        FValue fv = lval.accept(new_eval);
                        FType fvt = lvb.getType().getVal().accept(eval_type);
                        containing.putVariable(fv.getString(),fvt);
                    } else {
                        containing.putValue(lval.accept(new_eval), new IndirectionCell());

                    }
                } else {
                    containing.putValue(lval.accept(new_eval), new IndirectionCell());
                }
            }

        }
        return new_eval.evalExprList(body, x);
    }

    public FValue doLets(LetExpr exp) {
        return exp.accept(this);
    }

}
