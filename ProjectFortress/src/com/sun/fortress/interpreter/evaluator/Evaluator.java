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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.ForLoopTask;
import com.sun.fortress.interpreter.evaluator.tasks.TaskError;
import com.sun.fortress.interpreter.evaluator.tasks.TupleTask;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FFloatLiteral;
import com.sun.fortress.interpreter.evaluator.values.FGenerator;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FRange;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FStringLiteral;
import com.sun.fortress.interpreter.evaluator.values.FThread;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.IUOTuple;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.interpreter.evaluator.values.MethodClosure;
import com.sun.fortress.interpreter.evaluator.values.MethodInstance;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.OverloadedMethod;
import com.sun.fortress.interpreter.evaluator.values.Selectable;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.MethodWrapper;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.nodes.AbsFnDecl;
import com.sun.fortress.interpreter.nodes.AbsVarDecl;
import com.sun.fortress.interpreter.nodes.Accumulator;
import com.sun.fortress.interpreter.nodes.Apply;
import com.sun.fortress.interpreter.nodes.AsExpr;
import com.sun.fortress.interpreter.nodes.Assignment;
import com.sun.fortress.interpreter.nodes.AtomicExpr;
import com.sun.fortress.interpreter.nodes.Binding;
import com.sun.fortress.interpreter.nodes.Block;
import com.sun.fortress.interpreter.nodes.CaseClause;
import com.sun.fortress.interpreter.nodes.CaseExpr;
import com.sun.fortress.interpreter.nodes.CaseParam;
import com.sun.fortress.interpreter.nodes.CaseParamExpr;
import com.sun.fortress.interpreter.nodes.CaseParamLargest;
import com.sun.fortress.interpreter.nodes.CaseParamSmallest;
import com.sun.fortress.interpreter.nodes.CatchClause;
import com.sun.fortress.interpreter.nodes.Catch;
import com.sun.fortress.interpreter.nodes.ChainExpr;
import com.sun.fortress.interpreter.nodes.CharLiteral;
import com.sun.fortress.interpreter.nodes.Do;
import com.sun.fortress.interpreter.nodes.DoFront;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.Enclosing;
import com.sun.fortress.interpreter.nodes.Entry;
import com.sun.fortress.interpreter.nodes.Exit;
import com.sun.fortress.interpreter.nodes.Export;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.ExtentRange;
import com.sun.fortress.interpreter.nodes.FieldSelection;
import com.sun.fortress.interpreter.nodes.FloatLiteral;
import com.sun.fortress.interpreter.nodes.Fn;
import com.sun.fortress.interpreter.nodes.For;
import com.sun.fortress.interpreter.nodes.Fun;
import com.sun.fortress.interpreter.nodes.Generator;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.If;
import com.sun.fortress.interpreter.nodes.IfClause;
import com.sun.fortress.interpreter.nodes.IntLiteral;
import com.sun.fortress.interpreter.nodes.KeywordsExpr;
import com.sun.fortress.interpreter.nodes.LHS;
import com.sun.fortress.interpreter.nodes.LValueBind;
import com.sun.fortress.interpreter.nodes.Label;
import com.sun.fortress.interpreter.nodes.LetExpr;
import com.sun.fortress.interpreter.nodes.LetFn;
import com.sun.fortress.interpreter.nodes.ListComprehension;
import com.sun.fortress.interpreter.nodes.ListExpr;
import com.sun.fortress.interpreter.nodes.LooseJuxt;
import com.sun.fortress.interpreter.nodes.MapComprehension;
import com.sun.fortress.interpreter.nodes.MapExpr;
import com.sun.fortress.interpreter.nodes.MultiDim;
import com.sun.fortress.interpreter.nodes.MultiDimElement;
import com.sun.fortress.interpreter.nodes.MultiDimRow;
import com.sun.fortress.interpreter.nodes.NameDim;
import com.sun.fortress.interpreter.nodes.AbstractNode;
import com.sun.fortress.interpreter.nodes.ObjectExpr;
import com.sun.fortress.interpreter.nodes.Op;
import com.sun.fortress.interpreter.nodes.OperatorParam;
import com.sun.fortress.interpreter.nodes.Opr;
import com.sun.fortress.interpreter.nodes.OprArg;
import com.sun.fortress.interpreter.nodes.OprExpr;
import com.sun.fortress.interpreter.nodes.OprName;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.nodes.PostFix;
import com.sun.fortress.interpreter.nodes.ArrayComprehension;
import com.sun.fortress.interpreter.nodes.ArrayComprehensionClause;
import com.sun.fortress.interpreter.nodes.SetComprehension;
import com.sun.fortress.interpreter.nodes.SetExpr;
import com.sun.fortress.interpreter.nodes.Spawn;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StringLiteral;
import com.sun.fortress.interpreter.nodes.SubscriptAssign;
import com.sun.fortress.interpreter.nodes.SubscriptExpr;
import com.sun.fortress.interpreter.nodes.SubscriptOp;
import com.sun.fortress.interpreter.nodes.Throw;
import com.sun.fortress.interpreter.nodes.TightJuxt;
import com.sun.fortress.interpreter.nodes.Try;
import com.sun.fortress.interpreter.nodes.TryAtomicExpr;
import com.sun.fortress.interpreter.nodes.TupleExpr;
import com.sun.fortress.interpreter.nodes.TypeApply;
import com.sun.fortress.interpreter.nodes.TypeArg;
import com.sun.fortress.interpreter.nodes.TypeCase;
import com.sun.fortress.interpreter.nodes.TypeCaseClause;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.UnitDim;
import com.sun.fortress.interpreter.nodes.UnitVar;
import com.sun.fortress.interpreter.nodes.UnpastingBind;
import com.sun.fortress.interpreter.nodes.UnpastingDim;
import com.sun.fortress.interpreter.nodes.UnpastingSplit;
import com.sun.fortress.interpreter.nodes.VarDecl;
import com.sun.fortress.interpreter.nodes.VarRefExpr;
import com.sun.fortress.interpreter.nodes.VoidLiteral;
import com.sun.fortress.interpreter.nodes.While;
import com.sun.fortress.interpreter.nodes_util.WrappedFValue;
import com.sun.fortress.interpreter.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.MatchFailure;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Pair;
import com.sun.fortress.interpreter.useful.Useful;

import java.util.concurrent.Callable;

public class Evaluator extends EvaluatorBase<FValue> {
    boolean debug = false;
    int transactionNestingCount = 0;

    final public static FVoid evVoid = FVoid.V;

    public FValue eval(Expr e) {
        return acceptNode(e);
    }

    /**
     * Creates a new evaluator in a primitive environment.
     *
     */
    public Evaluator(HasAt within) {
        this(BetterEnv.primitive(within));
    }

    /**
     * Creates a new com.sun.fortress.interpreter.evaluator in a nested scope. The "frame" created for the
     * extension to the environment is tagged by within.
     *
     * @param ev
     * @param within
     */
    public Evaluator(Evaluator ev, HasAt within) {
        this(new BetterEnv(ev.e, within));
    }

    /**
     * Creates a new com.sun.fortress.interpreter.evaluator in the specified environment.
     */
    public Evaluator(BetterEnv e) {
        super(e);
        this.e.bless();
    }

    /**
     * Useful for evaluators that differ only a little.
     *
     * @param e2
     */
    protected Evaluator(Evaluator e2) {
        super(e2.e);
        debug = e2.debug;
        transactionNestingCount = e2.transactionNestingCount;
    }

    public void debugPrint(String debugString) {
        if (debug)
            System.out.println(debugString);
    }

    public FValue NI(String s) {
        throw new InterpreterError(this.getClass().getName() + "." + s
                + " not implemented");
    }

    public FValue NI(String s, AbstractNode n) {
        throw new InterpreterError(this.getClass().getName() + "." + s
                + " not implemented, input \n" + n.dump());
    }

    public FValue forAccumulator(Accumulator x) {
        return NI("forAccumulator");
    }

    public FValue forApply(Apply x) {
        return NI("forApply");
    }

    public FValue forAsExpr(AsExpr x) {
        return NI("forAsExpr");
    }

    // We ask lhs to accept twice (with this and an LHSEvaluator) in
    // the operator case. Might this cause the world to break?
    public FValue forAssignment(Assignment x) {
        // debugPrint("forAssignment " + x);
        Option<Op> possOp = x.getOp();
        LHSToLValue getLValue = new LHSToLValue(this);
        List<? extends LHS> lhses = getLValue.inParallel(x.getLhs());
        int lhsSize = lhses.size();
        FValue rhs = x.getRhs().accept(this);

        if (possOp.isPresent()) {
            // We created an lvalue for lhses above, so there should
            // be no fear of duplicate evaluation.
            Op op = possOp.getVal();
            Fcn fcn = (Fcn) op.accept(this);
            FValue lhsValue;
            if (lhsSize > 1) {
                // TODO:  An LHS walks, talks, and barks just like
                // an Expr in this context.  Yet it isn't an Expr, and
                // we can't pass the lhses to the numerous functions
                // which expect a List<Expr>---for example TupleExpr
                // or evalExprListParallel!  This is extremely annoying!
                List<FValue> lhsComps = new ArrayList<FValue>(lhsSize);
                for (LHS lhs : lhses) {
                    // This should occur in parallel!!!
                    lhsComps.add(lhs.accept(this));
                }
                lhsValue = FTuple.make(lhsComps);
            } else {
                lhsValue = lhses.get(0).accept(this);
            }
            List<FValue> vargs = new ArrayList<FValue>(2);
            vargs.add(lhsValue);
            vargs.add(rhs);
            rhs = functionInvocation(vargs, fcn, x);
        }

        // Assignment must be single-anything, or tuple-tuple with
        // matched sizes.
        Iterator<FValue> rhsIt = null;
        if (lhsSize > 1 && rhs instanceof FTuple) {
            FTuple rhs_tuple = (FTuple) rhs;
            rhsIt = rhs_tuple.getVals().iterator();
            // Verify, now, that LHS and RHS sizes match.
            if (rhs_tuple.getVals().size() != lhsSize) {
                throw new ProgramError(x,e,
                        "Tuple assignment size mismatch, | lhs | = "
                                + lhsSize + ", | rhs | = "
                                + rhs_tuple.getVals().size());
            }
        } else if (lhsSize != 1) {
            throw new ProgramError(x,e,
                                   "Tuple assignment size mismatch, | lhs | = "
                                   + lhsSize + ", rhs is not a tuple");
        }

        for (LHS lhs : lhses) {
            if (rhsIt != null) {
                rhs = rhsIt.next();
            }
            lhs.accept(new ALHSEvaluator(this, rhs));
        }
        return FVoid.V;
    }

    public FValue forAtomicExpr(AtomicExpr x) {
        final Expr e = x.getExpr();
        final Evaluator current = new Evaluator(this);
        transactionNestingCount += 1;

        FValue res = BaseTask.doIt (
            new Callable<FValue>() {
                public FValue call() {
                    Evaluator ev = new Evaluator(new BetterEnv(current.e, e));
                    return e.accept(ev);
                }
            }
        );
        transactionNestingCount -= 1;
        return res;
    }

    public FValue forTryAtomicExpr(TryAtomicExpr x) {
        return NI("forTryAtomicExpr");
    }

    public FValue forBinding(Binding x) {
        return NI("forBinding");
    }

    public FValue forDo(Do x) {
        // debugPrint("forDo " + x);
        if (x.getFronts().size() == 0)
            return evVoid;
        else if (x.getFronts().size() > 1)
            return NI("forParallelDo");
        else { // (x.getFronts().size() == 1)
            DoFront f = x.getFronts().get(0);
            if (f.getAt().isPresent()) return NI("forAtDo");
            if (f.isAtomic())
                return forAtomicExpr(new AtomicExpr(x.getSpan(),f.getExpr()));
            return f.getExpr().accept(this);
        }
    }

    public FValue forBlock(Block x) {
        // debugPrint("forBlock " + x);
        List<Expr> exprs = x.getExprs();
        return evalExprList(exprs, x);
    }

    /**
     * Returns a list containing the evaluation of exprs.
     * The expressions in the list should not be Lets.
     *
     * @param exprs
     * @return
     */
     <T extends Expr> List<FValue> evalExprList(List<T> exprs) {
        List<FValue> res = new ArrayList<FValue>(exprs.size());
        return evalExprList(exprs, res);
    }

    /**
     * Appends the evaluation of exprs to res.
     * The expressions in the list should not be Lets.
     *
     * @param <T>
     * @param exprs
     * @param res
     * @return
     */
    <T extends Expr> List<FValue> evalExprList(List<T> exprs, List<FValue> res) {
        for (Expr expr : exprs) {
            res.add(expr.accept(this));
        }
        return res;
    }

    /**
     * Returns the evaluation of a list of (general) exprs, returning the
     * result of evaluating the last expr in the list.
     * Does the "right thing" with LetExprs.
     */
    public FValue evalExprList(List<Expr> exprs, AbstractNode tag) {
        FValue res = evVoid;
        Evaluator eval = this;
        for (Expr exp : exprs) {
            // TODO This will get turned into forLet methods
            if (exp instanceof LetExpr) {
                BetterEnv inner = new BetterEnv(eval.e, exp);
                BuildLetEnvironments be = new BuildLetEnvironments(inner);
                try {
                    res = be.doLets((LetExpr) exp);
                } catch (ProgramError ex) {
                    throw ex; /* Skip the wrapper */
                } catch (RuntimeException ex) {
                    throw ex; // new ProgramError(exp, inner, "Wrapped
                    // exception", ex);
                }
            } else {
                try {
                    res = exp.accept(eval);
                } catch (ProgramError ex) {
                    throw ex; /* Skip the wrapper */
                } catch (RuntimeException ex) {
                    throw ex; // new ProgramError(exp, eval.e, "Wrapped
                    // exception", ex);
                }
            }
        }
        return res;
    }


    <T extends Expr> List<FValue> evalExprListParallel(List<T> exprs) {
        // Added some special-case code to avoid explosion of TupleTasks.
        int sz = exprs.size();
        ArrayList<FValue> resList = new ArrayList<FValue>(sz);
        if (sz==1) {
            resList.add(exprs.get(0).accept(this));
 } else if (transactionNestingCount > 0) {
   for (Expr exp : exprs) {
       resList.add(exp.accept(this));
   }
        } else if (sz > 1) {
            TupleTask[] tasks = new TupleTask[exprs.size()];
            int count = 0;
            BaseTask currentTask = BaseTask.getCurrentTask();
            for (Expr e : exprs) {
                tasks[count++] = new TupleTask(e, this, currentTask);
            }
            TupleTask.coInvoke(tasks);
            for (int i = 0; i < count; i++) {
                if (tasks[i].causedException) {
                    Throwable t = tasks[i].getException();
                    if (t instanceof Error) {
                        throw (Error)t;
                    } else if (t instanceof RuntimeException) {
                        throw (RuntimeException)t;
                    } else {
                        throw new ProgramError(exprs.get(i),
                                               "Wrapped Exception",t);
                    }
                }
                resList.add(tasks[i].getRes());
            }
        }
        return resList;
    }

    public FValue forCaseClause(CaseClause x) {
        return NI("forCaseClause");
    }

    CaseClause findLargest(List<CaseClause> clauses) {
        Iterator<CaseClause> i = clauses.iterator();
        CaseClause c = i.next();
        FValue max = c.getMatch().accept(this);
        CaseClause res = c;

        for (; i.hasNext();) {
            c = i.next();
            FValue current = c.getMatch().accept(this);
            if (current.getInt() > max.getInt()) {
                max = current;
                res = c;
            }
        }
        return res;
    }

    CaseClause findSmallest(List<CaseClause> clauses) {
        Iterator<CaseClause> i = clauses.iterator();
        CaseClause c = i.next();
        FValue min = c.getMatch().accept(this);
        CaseClause res = c;

        for (; i.hasNext();) {
            c = i.next();
            FValue current = c.getMatch().accept(this);
            if (current.getInt() < min.getInt()) {
                min = current;
                res = c;
            }
        }
        return res;
    }

    public FValue forCaseExpr(CaseExpr x) {
        List<CaseClause> clauses = x.getClauses();
        CaseParam param = x.getParam();

        if (param instanceof CaseParamLargest) {
            CaseClause y = findLargest(clauses);
            return evalExprList(y.getBody(), y);
        } else if (param instanceof CaseParamSmallest) {
            CaseClause y = findSmallest(clauses);
            return evalExprList(y.getBody(), y);
        } else {
            // Evaluate the parameter
            FValue paramValue = param.accept(this);
            // Assign a comparison function
            Fcn fcn = (Fcn) e.getValue("=");
            Option<Op> Compare = x.getCompare();
            if (Compare.isPresent())
                fcn = (Fcn) e.getValue(Compare.getVal().getName());

            // Iterate through the cases
            for (Iterator<CaseClause> i = clauses.iterator(); i.hasNext();) {
                CaseClause c = i.next();
                // Evaluate the clause
                FValue match = c.getMatch().accept(this);
                List<FValue> vargs = new ArrayList<FValue>();
                vargs.add(paramValue);
                vargs.add(match);
                // If it is a range, check that the param is in the range
                // Otherwise use comparison operation
                Fcn actual = (match instanceof FRange) ? (Fcn) e
                        .getValue("elementOf") : fcn;

                FBool success = (FBool) functionInvocation(vargs, actual, c);
                if (success.getBool())
                    return evalExprList(c.getBody(), c);
            }
            Option<List<Expr>> _else = x.getElseClause();
            if (_else.isPresent()) {
                // TODO need an Else node to hang a location on
                return evalExprList(_else.getVal(), x);
            }
            return evVoid;
        }
    }

    public FValue forCaseParamExpr(CaseParamExpr x) {
        Expr expr = x.getExpr();
        return expr.accept(this);
    }

    public FValue forCaseParamLargest(CaseParamLargest x) {
        // Should not be called
        return NI("forCaseParamSmallest");
    }

    public FValue forCaseParamSmallest(CaseParamSmallest x) {
        // Should not be called
        return NI("forCaseParamSmallest");
    }

    public FValue forCatchClause(CatchClause x) {
        return NI("forCatchClause");
    }

    public FValue forCatchExpr(Catch x) {
        return NI("forCatchExpr");
    }

    /*
     * Only handles 2-ary definitions of chained operators. That should be
     * perfectly wonderful for the moment.
     */
    public FValue forChainExpr(ChainExpr x) {
        // debugPrint("forChainExpr " + x);
        Expr first = x.getFirst();
        List<Pair<Op, Expr>> links = x.getLinks();
        FValue idVal = first.accept(this);
        FBool boolres = FBool.TRUE;
        Iterator<Pair<Op, Expr>> i = links.iterator();
        List<FValue> vargs = new ArrayList<FValue>(2);
        vargs.add(idVal);
        vargs.add(idVal);
        while (boolres.getBool() && i.hasNext()) {
            Pair<Op, Expr> link = i.next();
            Fcn fcn = (Fcn) link.getA().accept(this);
            FValue exprVal = link.getB().accept(this);
            vargs.set(0, idVal);
            vargs.set(1, exprVal);
            boolres = (FBool) functionInvocation(vargs, fcn, x);
            idVal = exprVal;
        }
        return boolres;
    }

    // We shouldn't be walking over components in an expression Evaluator.
    // All definitions should be accumulated in an earlier pass and given
    // to this Evaluator as arguments to its constructor. An Evaluator should
    // be responsible solely for evaluating expressions. An Evaluator should be
    // called on
    // each top-level expression of a component and on the body of the
    // "run" method of a component when it is executed.
    // I'll leave this code uncommented for now, as it is probably used as an
    // entry point
    // for our tests. -- Eric
    // public FValue forComponent(Component x) {
    // BuildEnvironments be = new BuildEnvironments(e);
    // x.accept(be);
    // return evVoid;
    // }

    public List<FType> evalTypeCaseBinding(Evaluator ev,
            TypeCase x) {
        List<Binding> bindings = x.getBind();
        List<FType> res = new ArrayList<FType>();
        for (Iterator<Binding> i = bindings.iterator(); i.hasNext();) {
            Binding bind = i.next();
            String name = bind.getName().getName();
            Expr init = bind.getInit();
            FValue val = init.accept(ev);
            if (init instanceof VarRefExpr
                    && ((VarRefExpr) init).getVar().getName().equals(name)) {
                /* Avoid shadow error when we bind the same var name */
                ev.e.putValueUnconditionally(name, val);
            } else {
                /* But warn about shadowing in all other cases */
                ev.e.putValue(name, val);
            }
            res.add(val.type());
        }
        return res;
    }

    private boolean moreSpecificHelper(TypeCaseClause candidate,
            TypeCaseClause current, Evaluator ev) {
        List<TypeRef> candType = candidate.getMatch();
        List<TypeRef> curType = current.getMatch();
        List<FType> candMatch = EvalType.getFTypeListFromList(candType, ev.e);
        List<FType> curMatch = EvalType.getFTypeListFromList(curType, ev.e);
        boolean res = FTypeTuple.moreSpecificThan(candMatch, curMatch);
        return res;
    }

    // This takes a candidate type clause and list of the best matches so far.
    // It walks down the list comparing the candidate with each match. The
    // winner
    // gets added to the result list. If the candidate beats more than one of
    // the
    // best matches so far, it should only get added to the result list once.

    public List<TypeCaseClause> BestMatches(TypeCaseClause candidate,
            List<TypeCaseClause> bestSoFar, Evaluator ev) {
        List<TypeCaseClause> result = new ArrayList<TypeCaseClause>();
        boolean addedCandidate = false;

        for (Iterator<TypeCaseClause> i = bestSoFar.iterator(); i.hasNext();) {
            TypeCaseClause current = i.next();
            if (moreSpecificHelper(candidate, current, ev)) {
                if (!addedCandidate) {
                    result.add(candidate);
                    addedCandidate = true;
                }
            } else if (moreSpecificHelper(current, candidate, ev)) {
                result.add(current);
            } else {
                result.add(current);
                if (!addedCandidate) {
                    result.add(candidate);
                    addedCandidate = true;
                }
            }
        }
        return result;
    }

    public FValue forDottedId(DottedId x) {
        // debugPrint("forDotted " + x);
        String result = "";
        for (Iterator<String> i = x.getNames().iterator(); i.hasNext();) {
            result = result.concat(i.next());
            if (i.hasNext())
                result = result.concat(".");
        }
        return FString.make(result);
    }

    public FValue forEntry(Entry x) {
        return NI("forEntry");
    }

    public FValue forExit(Exit x) {
        return NI("forExit");
    }

    // No need to evaluate this; it's not an expression. -- Eric
    // We need to do something with it -- CHF
    public FValue forExport(Export x) {
        // debugPrint("forExport " + x);
        List<DottedId> name = x.getNames();
        return null;
    }

    public FValue forExtentRange(ExtentRange x) {
        return NI("forExtentRange");
    }

    public FValue forFieldSelection(FieldSelection x) {
        Expr obj = x.getObj();
        Id fld = x.getId();
        FValue fobj = obj.accept(this);
        if (fobj instanceof Selectable) {
            Selectable selectable = (Selectable) fobj;
            /*
             * Selectable was introduced to make it not necessary
             * to know whether a.b was field b of object a, or member
             * b of api a (or api name prefix, extended).
             */
//          TODO Need to distinguish between public/private methods/fields
            try {
                return selectable.select(fld.getName());
            } catch (ProgramError ex) {
                ex.setWithin(e);
                ex.setWhere(x);
                throw ex;
            }
//        } else if (fobj instanceof FObject) {
//            FObject fobject = (FObject) fobj;
//            // TODO Need to distinguish between public/private methods/fields
//            try {
//                return fobject.getSelfEnv().getValue(fld.getName());
//            } catch (ProgramError ex) {
//                ex.setWithin(e);
//                ex.setWhere(x);
//                throw ex;
//            }
        } else {
            throw new ProgramError(x, e, "Non-object cannot have field "
                    + fld.getName());
        }

    }

    public FValue forFn(Fn x) {
        Closure cl = new Closure(e, x);
        return cl.finishInitializing();
    }

    public FValue forAbsFnDecl(AbsFnDecl x) {
        return NI("forFnDecl");
    }


    public FValue forFor(For x) {
        // debugPrint("forFor " + x);
        List<Generator> gens = x.getGens();
        Generator gen = gens.get(0);
        FGenerator fgen = (FGenerator) gen.accept(this);
        DoFront df = x.getBody();
        Expr body = x;
        if (df.isAtomic()) {
            NI("forAtomicDo");
        }
        if (df.getAt().isPresent()) {
            NI("forAtDo");
        }
        if (gens.size() > 1) {
            gens = gens.subList(1,gens.size());
            body = new For(x.getSpan(),gens,df);
        }
        new ForLoopTask(fgen, body, this, BaseTask.getCurrentTask()).run();
        return evVoid;
    }

    public FValue forFun(Fun x) {
        return x.getName().accept(this);
    }

    public FValue forGenerator(Generator x) {
        // debugPrint("forGenerator " + x);
        List<Id> ids = x.getBind();
        Expr init = x.getInit();
        FValue rval = init.accept(this);
        FRange range = (FRange)rval;
        return new FGenerator(ids, range);
    }

    public FValue forId(Id x) {
        return FString.make(x.getName());
    }

    public FValue forIf(If x) {
        List<IfClause> clause = x.getClauses();
        for (Iterator<IfClause> i = clause.iterator(); i.hasNext();) {
            IfClause ifclause = i.next();
            FBool fbool = (FBool) ifclause.getTest().accept(this);
            if (fbool.getBool())
                return ifclause.getBody().accept(this);
            ;
        }
        Option<Expr> else_ = x.getElseClause();
        if (else_.isPresent()) {
            Expr else_expr = else_.getVal();
            return else_expr.accept(this);
        }
        return evVoid;
    }

    public FValue forIfClause(IfClause x) {
        return NI("This ought not be called.");
    }

    public FValue forKeywordsExpr(KeywordsExpr x) {
        return NI("forKeywordsExpr");
    }

    public FValue forLValueBind(LValueBind x) {
        Id name = x.getName();
        String s = name.getName();
        return FString.make(s);
    }

    public FValue forLabel(Label x) {
        /* Don't forget to use a new evaluator/environment for the inner block. */
        throw new InterpreterError(x,"label construct not yet implemented.");
    }

    public FValue forLetFn(LetFn x) {
        throw new InterpreterError(x,"forLetFn not implemented.");
    }

    public FValue forListComprehension(ListComprehension x) {
        throw new InterpreterError(x,"list comprehensions not yet implemented.");
    }

    public FValue forListExpr(ListExpr x) {
        throw new InterpreterError(x,"list expressions not implemented.");
    }

    private FValue juxtApplyStack(Stack<FValue> fns, FValue times, AbstractNode loc) {
        FValue tos = fns.pop();
        while (!fns.empty()) {
            FValue f = fns.pop();

            if (f instanceof Fcn) {
                tos = functionInvocation(argList(tos), f, loc);
            } else {
                tos = functionInvocation(Useful.list(f, tos), times, loc);
            }
        }
        return tos;
    }

    public FValue forLooseJuxt(LooseJuxt x) {
        // This is correct except for one minor detail:
        // We should treat names from another scope as if they were functions.
        // Right now we evaluate them and make the function/non-function
        // distinction based on the resulting getText.
        // We do not handle functional methods, and overlap therewith, at all.
        List<Expr> exprs = x.getExprs();
        FValue times = e.getValue("juxtaposition");
        if (exprs.size() == 0)
            throw new InterpreterError(x,"empty juxtaposition");
        List<FValue> evaled = evalExprListParallel(exprs);
        Boolean inFunction = true;
        Stack<FValue> stack = new Stack<FValue>();
        for (FValue e : evaled) {
            if (e instanceof Fcn) {
                if (!inFunction) {
                    stack.push(juxtApplyStack(stack, times, x));
                    inFunction = true;
                }
                stack.push(e);
            } else if (inFunction) {
                inFunction = false;
                stack.push(e);
            } else {
                FValue r = stack.pop();
                r = functionInvocation(Useful.list(r, e), times, x);
                stack.push(r);
            }
        }
        return juxtApplyStack(stack, times, x);
    }

    public FValue forMapComprehension(MapComprehension x) {
        return NI("forMapComprehension");
    }

    public FValue forMapExpr(MapExpr x) {
        return NI("forMapExpr");
    }

    public FValue forMultiDimElement(MultiDimElement x) {
        // MDEs occur only within MultiDimRows, and reset
        // row evaluation to an outercontext (in the scope
        // of the element, that is).
        throw new InterpreterError(x,"Singleton paste?  Can't judge dimensionality without type inference.");
        // Evaluator notInPaste = new Evaluator(this);
        // return x.getElement().accept(notInPaste);
    }

    /**
     * Evaluates a pasting, outermost.
     *
     * In an outer context, the across-all-dimensions correctness and size
     * information for the IUOTuples can be correctly determined. Until then, it
     * is too early to expand scalars up to full rank.
     */
    public FValue forMultiDimRow(MultiDimRow x) {
        List<MultiDim> elements = x.getElements();
        EvaluatorInPaste eip = new EvaluatorInPaste(this);
        List<FValue> values = eip.evalExprList(elements);
        IUOTuple paste = new IUOTuple(values, x);
        // Finish the pasting now.
        paste.finish();
        return paste;
    }

    public FValue forNameDim(NameDim x) {
        return NI("forNameDim");
    }

    public FValue forObjectExpr(ObjectExpr x) {
        String s = x.getGenSymName();
        // FType ft = e.getType(s);
        // System.out.println("forObjectExpr "+s);
        FValue v = e.getValue(s);

        if (v instanceof GenericConstructor) {
            GenericConstructor gc = (GenericConstructor) v;
            List<StaticArg> args = x.getStaticArgs();
            Constructor cl = (Constructor) gc.typeApply(args, e, x);
            return cl.applyConstructor(java.util.Collections
                    .<FValue> emptyList(), x, e);
        } else if (v instanceof Constructor) {
            Constructor cl = (Constructor) v;
            // FTypeObject fto = (FTypeObject) ft;
            // cl.setParams(Collections.<Parameter> emptyList());
            // BuildEnvironments.finishObjectTrait(x.getTraits(), fto, e);
            // cl.finishInitializing();
            return cl.applyConstructor(java.util.Collections
                    .<FValue> emptyList(), x, e);
        } else {
            throw new InterpreterError(x,e,"ObjectExpr " + s + " has 'constructor' " + v);
        }

        // Option<List<TypeRef>> traits = x.getTraits();
        // List<Decl> defs = x.getDefs();
        // FTypeObject fto = new FTypeObject(genSym(x), e);
        // return BuildEnvironments.anObject(fto, e, traits, defs, x);

    }

    /*
     * In a sane world, we would not have both OprName and Op com.sun.fortress.interpreter.nodes. My guess is
     * that this is an ugly holdover from two distinct algebraic types in the
     * old code.
     */
    private FValue getOpr(OprName op) {
        try {
            return e.getValue(NodeUtil.getName(op));
        } catch (ProgramError ex) {
            ex.setWhere(op);
            ex.setWithin(e);
            throw ex;
        }
    }

    public FValue forEnclosing(Enclosing x) {
        return getOpr(x);
    }

    public FValue forPostFix(PostFix x) {
        return getOpr(x);
    }

    public FValue forOp(Op op) {
        try {
            return e.getValue(op.getName());
        } catch (ProgramError ex) {
            ex.setWhere(op);
            ex.setWithin(e);
            throw ex;
        }
    }

    public FValue forOperatorParam(OperatorParam x) {
        return NI("forOperatorParam");
    }

    // Why does this extra layer of indirection exist?
    public FValue forOpr(Opr x) {
        return getOpr(x);
    }

    public FValue forOprExpr(OprExpr x) {
        // debugPrint("forOprExpr " + x);
        OprName op = x.getOp();
        List<Expr> args = x.getArgs();
        FValue fvalue = op.accept(this);
        // Evaluate actual parameters.
        int s = args.size();
        FValue res = evVoid;
        List<FValue> vargs = evalExprListParallel(args);

        /*
         * It *seems* that the reason we have to have our own version of
         * application here is that we have no simple way to decide for any
         * possible fvalue whether it should be applied at arbitrary arity, or
         * just as a unary or binary call. It would be a lovely simplification
         * if we could make this ball of hair go away (especially since it's not
         * right at the moment!). - Jan
         *
         * The current take on this particular hack is that we treat operators
         * as binary and left-associative, unless they are enclosing. If they
         * are enclosing we perform a full-arity application.
         */

        if (!(fvalue instanceof Fcn)) {
            throw new ProgramError(x, e, "Operator " + op.stringName()
                    + " has a non-function value " + fvalue);
        }
        Fcn fcn = (Fcn) fvalue;
        if (s <= 2 || (op instanceof Enclosing)) {
            res = functionInvocation(vargs, fcn, x);
        } else {
            List<FValue> argPair = new ArrayList<FValue>(2);
            argPair.add(vargs.get(0));
            argPair.add(vargs.get(1));
            res = functionInvocation(argPair, fcn, x);
            for (int i = 2; i < s; i++) {
                argPair.set(0, res);
                argPair.set(1, vargs.get(i));
                res = functionInvocation(argPair, fcn, x);
            }
        }
        return res;
    }

    public FValue forArrayCompClause(ArrayComprehensionClause x) {
        return NI("forArrayCompClause");
    }

    public FValue forArrayComprehension(ArrayComprehension x) {
        return NI("forArrayComprehension");
    }

    public FValue forSetComprehension(SetComprehension x) {
        return NI("forSetComprehension");
    }

    public FValue forSetExpr(SetExpr x) {
        List<Expr> elements = x.getElements();
        // Evaluate the elements.
        List<FValue> evaled = evalExprListParallel(elements);
        // Iterate over the elements to pick the most general type
        Set<FType> bestGuess = FType.join(evaled);
        FType bestGuessType = Useful.<FType> singleValue(bestGuess);
        Simple_fcn f = Glue.instantiateGenericConstructor(e,
                WellKnownNames.setMaker, bestGuessType, x);
        // Now invoke f.
        FValue theArray = functionInvocation(Collections.<FValue> emptyList(),
                f, x);

        MethodWrapper mw = new MethodWrapper((FObject) theArray, x, "add");
        ArrayList<FValue> singleton = new ArrayList<FValue>(1);
        singleton.add(null);
        for (FValue v : evaled) {
            singleton.set(0, v);
            mw.call(singleton, e);
        }
        return theArray;
    }

    public FValue forSpawn(Spawn x) {
        Expr body = x.getBody();
        return new FThread(body, this);
    }

    public FValue forStringLiteral(StringLiteral x) {
        return new FStringLiteral(x.getText());
    }

    public FValue forSubscriptAssign(SubscriptAssign x) {
        // Didn't come here when it seemed like we should have.
        // See LHSEvaluator.forSubscriptExpr
        return NI("forSubscriptAssign");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public FValue forSubscriptExpr(SubscriptExpr x) {
        Expr obj = x.getObj();
        List<Expr> subs = x.getSubs();
        // Should evaluate obj.[](subs, getText)
        FValue arr = obj.accept(this);
        if (!(arr instanceof FObject)) {
            throw new ProgramError(obj, "Value should be an object; got " + arr);
        }
        FObject array = (FObject) arr;
        FValue ixing = array.getSelfEnv().getValue("[]");
        if (ixing == null || !(ixing instanceof Method)) {
            throw new ProgramError(x,
                    "Could not find appropriate definition of opr [] on "
                            + array);
        }
        Method cl = (Method) ixing;
        List<FValue> subscripts = evalExprListParallel(subs);
        return cl.applyMethod(subscripts, array, x, e);
    }

    public FValue forSubscriptOp(SubscriptOp x) {
        return NI("forSubscriptOp");
    }

    public FValue forTightJuxt(TightJuxt x) {
        // debugPrint("forTightJuxt " + x);
        // Assume for now that tight juxtaposition is function application fixme
        // chf
        List<Expr> exprs = x.getExprs();
        if (exprs.size() == 0)
            throw new InterpreterError(x,e,"empty juxtaposition");
        Expr fcnExpr = exprs.get(0);

        if (fcnExpr instanceof FieldSelection) {
            // In this case, open code the FieldSelection evaluation
            // so that the object can be preserved. Alternate
            // strategy might be to generate a closure from
            // the field selection.
            FieldSelection fld_sel = (FieldSelection) fcnExpr;
            Expr obj = fld_sel.getObj();
            Id fld = fld_sel.getId();
            FValue fobj = obj.accept(this);
            return juxtFieldSelection(x, fobj, fld, exprs);
        } else if (fcnExpr instanceof TypeApply) {
            // Peek into the type-apply, see if it actually a generic method
            // being instantiated.
            TypeApply tax = (TypeApply) fcnExpr;
            Expr expr = tax.getExpr();
            List<StaticArg> args = tax.getArgs();
            if (expr instanceof FieldSelection) {

                FieldSelection fld_sel = (FieldSelection) expr;
                Expr obj = fld_sel.getObj();
                Id fld = fld_sel.getId();
                FValue fobj = obj.accept(this);

                // "Function" is type-apply of field-selection;
                // currently that can only be instantiation of
                // a generic method.

                if (fobj instanceof FObject) {
                    FObject fobject = (FObject) fobj;
                    // TODO Need to distinguish between public/private
                    // methods/fields
                    FValue cl = fobject.getSelfEnv()
                            .getValueNull(fld.getName());
                    if (cl == null) {
                        // TODO Environment is split, might not be best choice
                        // for error printing.
                        throw new ProgramError(x, fobject.getSelfEnv(),
                                "undefined method/field "
                                + fld.getName());
                    } else if (cl instanceof OverloadedMethod) {

                        throw new InterpreterError(x, fobject.getSelfEnv(),
                                "Don't actually resolve overloading of generic methods yet.");

                    } else if (cl instanceof MethodInstance) {
                        // What gets retrieved is the symbolic instantiation of
                        // the generic method.
                        // This is ever-so-slightly wrong -- we need to not
                        // create an "instance"
                        // if the parameters are non-symbolic.
                        GenericMethod gm = ((MethodInstance) cl).getGenerator();
                        return (gm.typeApply(args, e, x)).applyMethod(
                                evalInvocationArgs(exprs), fobject, x, e);

                    } else {
                        throw new ProgramError(x, fobject.getSelfEnv(),
                                "Unexpected Selection result in Juxt of TypeApply of Selection, "
                                        + cl);
                    }
                } else {
                    throw new ProgramError(x,
                            "Unexpected Selection LHS in Juxt of TypeApply of Selection, "
                                    + fobj);

                }
            } else {
                // Fall out into normal case.
            }

        }

        FValue fnVal = fcnExpr.accept(this);
        if (fnVal instanceof MethodClosure) {
            return NI.nyi("Functional method application");
        } else {
            return finishFunctionInvocation(exprs, fnVal, x);
        }

    }

    /**
     * @param x
     * @param fobj
     * @param fld
     * @param exprs
     * @return
     * @throws ProgramError
     */
    private FValue juxtFieldSelection(TightJuxt x, FValue fobj, Id fld,
            List<Expr> exprs) throws ProgramError {
        if (fobj instanceof FObject) {
            FObject fobject = (FObject) fobj;
            // TODO Need to distinguish between public/private methods/fields
            FValue cl = fobject.getSelfEnv().getValueNull(fld.getName());
            if (cl == null)
                // TODO Environment is split, might not be best choice for error
                // printing.
                throw new ProgramError(x, fobject.getSelfEnv(),
                        "undefined method/field " + fld.getName());
            else if (cl instanceof Method) {
                return ((Method) cl).applyMethod(evalInvocationArgs(exprs),
                        fobject, x, e);
            } else if (cl instanceof Fcn) {
                Fcn fcl = (Fcn) cl;
                // Ordinary closure, assigned to a field.
                return finishFunctionInvocation(exprs, fcl, x);
            } else {
                // TODO seems like we could be multiplying, too.
                throw new ProgramError(x, fobject.getSelfEnv(),
                        "Tight juxtaposition of non-function " + fld.getName());
            }
        } else {
            // TODO Could be a fragment of a component/api name, too.
            throw new ProgramError(x, "" + fobj + "." + fld
                    + " but not object.something");
        }
    }

    /**
     * @param exprs
     * @param fcnExpr
     * @return
     */
    private FValue finishFunctionInvocation(List<Expr> exprs, FValue foo,
            AbstractNode loc) {
        return functionInvocation(evalInvocationArgs(exprs), foo, loc);
    }

    /**
     * Evaluates the tail of the list, EXCLUDING THE FIRST ELEMENT.
     *
     * @param exprs
     * @return
     */
    List<FValue> evalInvocationArgs(List<Expr> exprs) {
        List<FValue> rest = evalExprListParallel(exprs.subList(1, exprs.size()));
        if (rest.size()==1) {
            FValue val = rest.get(0);
            if (val instanceof FVoid) {
                rest = new ArrayList<FValue>(0);
            } else if (val instanceof FTuple) {
                rest = ((FTuple) val).getVals();
            }
        }
        return rest;
    }

    private List<FValue> argList(FValue arg) {
        if (arg instanceof FTuple) {
            return ((FTuple) arg).getVals();
        } else {
            return Useful.list(arg);
        }
    }

    public FValue forTry(Try x) {
        return NI("forTry");
    }

    public FValue forTupleExpr(TupleExpr x) {
        debugPrint("forTuple " + x);
        List<Expr> exprs = x.getExprs();
        return FTuple.make(evalExprListParallel(exprs));
    }

    public FValue forTypeCase(TypeCase x) {
        Evaluator ev = new Evaluator(this, x);
        List<FType> res = evalTypeCaseBinding(ev, x);
        FValue result = evVoid;
        List<TypeCaseClause> clauses = x.getClauses();

        for (TypeCaseClause c : clauses) {
            List<TypeRef> match = c.getMatch();
            /* Technically, match and res need not be tuples; they could be
               singletons and the subtype test below ought to be correct. */
            FType matchTuple = EvalType.getFTypeFromList(match, ev.e);
            FType resTuple = FTypeTuple.make(res);

            if (resTuple.subtypeOf(matchTuple)) {
                List<Expr> body = c.getBody();
                result = evalExprList(body, c);
                return result;
            }
        }

        Option<List<Expr>> el = x.getElseClause();
        if (el.isPresent()) {
            List<Expr> elseClauses = el.getVal();
            // TODO really ought to have a node, with a location, for this list
            result = evalExprList(elseClauses, x);
            return result;
        } else {
            throw new MatchFailure();
        }
    }

    public FValue forTypeCaseClause(TypeCaseClause x) {
        return NI("forTypeClause");
    }

    public FValue forTypeArg(TypeArg x) {
        return NI("forTypeRefArg");
    }

    public FValue forUnitDim(UnitDim x) {
        return NI("forUnitDim");
    }

    public FValue forUnitVar(UnitVar x) {
        return NI("forUnitVar");
    }

    public FValue forUnpastingBind(UnpastingBind x) {
        return NI("forUnpastingBind");
    }

    public FValue forUnpastingDim(UnpastingDim x) {
        return NI("forUnpastingDim");
    }

    public FValue forUnpastingSplit(UnpastingSplit x) {
        return NI("forUnpastingSplit");
    }

    public FValue forAbsVarDecl(AbsVarDecl x) {
        return NI("forVarDecl");
    }

    public FValue forVarDecl(VarDecl x) {
        return NI("forVarDef");
    }

    public FValue forVarRefExpr(VarRefExpr x) {
        // debugPrint("forVarRefExpr " + x);
        Id var = x.getVar();
        String s = var.getName();
        // debugPrint("forVarRefExpr " + s);

        FValue res = e.getValueNull(s);

        if (res == null)
            throw new ProgramError(x, e, "undefined variable " + s);
        return res;
    }

    public FValue forVoidLiteral(VoidLiteral x) {
        return FVoid.V;
    }

    public FValue forWhile(While x) {
        // debugPrint("forWhile " + x);
        Expr body = x.getBody();
        Expr test = x.getTest();
        FBool res = (FBool) test.accept(this);
        while (res.getBool() != false) {
            // debugPrint("While loop iter");
            body.accept(this);
            res = (FBool) test.accept(this);
        }
        return FVoid.V;
    }

    public FValue forThrow(Throw throw1) {
        return NI("forThrow");
    }

    public FValue forCharLiteral(CharLiteral x) {
        return NI("forCharLiteral");
    }

    public FValue forFloatLiteral(FloatLiteral x) {
        return new FFloatLiteral(x.getText());
    }

    public FValue forIntLiteral(IntLiteral x) {
        return FIntLiteral.make(x.getVal());
    }

    public FValue forOprArg(OprArg x) {
        return NI("forOprArg");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forTypeApply(com.sun.fortress.interpreter.nodes.TypeApply)
     */
    @Override
    public FValue forTypeApply(TypeApply x) {
        Expr expr = x.getExpr();
        FValue g = expr.accept(this);
        List<StaticArg> args = x.getArgs();
        if (g instanceof FGenericFunction) {
            return ((FGenericFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericConstructor) {
            return ((GenericConstructor) g).typeApply(args, e, x);
        } else if (g instanceof OverloadedFunction) {
            return((OverloadedFunction) g).typeApply(args, e, x);
        }
        return super.forTypeApply(x);
    }

    @Override
    public FValue forWrappedFValue(WrappedFValue w) {
        return w.getFValue();
    }
}
