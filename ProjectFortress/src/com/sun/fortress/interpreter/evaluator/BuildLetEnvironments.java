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

import com.sun.fortress.nodes_util.NodeUtil;
import java.util.Iterator;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.IndirectionCell;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LetExpr;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class BuildLetEnvironments extends NodeAbstractVisitor<FValue> {

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
        List<FnDef> fns = x.getFns();
        List<Expr> body = x.getBody();

        for (int i = 0; i < fns.size(); i++) {
            FnDef fn = fns.get(i);

            SimpleName name = fn.getName();
            //Expr expr = fn.getBody();
            String fname = NodeUtil.nameString(name);
            List<Param> params = fn.getParams();
            Option<Type> retType = fn.getReturnType();

            Closure cl = new Closure(containing, fn);
            try {
                containing.putValue(fname, cl);
            } catch (FortressError pe) {
                throw pe.setContext(x,containing);
            }
            // TODO Local functions cannot be Enclosing, can they?
            // Local functions cannot be any operator including Enclosing.
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
        if (rhs.isSome()) {
            if (lhs.size() == 1) {
                FValue val = Option.unwrap(rhs).accept(new_eval);
                LHSEvaluator lhs_eval = new LHSEvaluator(new_eval, val);
                lhs.get(0).accept(lhs_eval);
            } else {
                FValue val = Option.unwrap(rhs).accept(new_eval);

                if (!(val instanceof FTuple)) {
                    error(x, containing,
                          errorMsg("RHS does not yield a tuple"));
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
                    if (lvb.isMutable()) {
                        FValue fv = lval.accept(new_eval);
                        FType fvt = Option.unwrap(lvb.getType()).accept(eval_type);
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
