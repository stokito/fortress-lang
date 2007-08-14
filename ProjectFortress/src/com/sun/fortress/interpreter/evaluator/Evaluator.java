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

import edu.rice.cs.plt.iter.IterUtil;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
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
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsVarDecl;
import com.sun.fortress.nodes.Accumulator;
import com.sun.fortress.nodes.AsExpr;
import com.sun.fortress.nodes.Assignment;
import com.sun.fortress.nodes.AtomicExpr;
import com.sun.fortress.nodes.Binding;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.CaseClause;
import com.sun.fortress.nodes.CaseExpr;
import com.sun.fortress.nodes.CaseParam;
import com.sun.fortress.nodes.CaseParamExpr;
import com.sun.fortress.nodes.CaseParamLargest;
import com.sun.fortress.nodes.CaseParamSmallest;
import com.sun.fortress.nodes.CatchClause;
import com.sun.fortress.nodes.Catch;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.CharLiteral;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.DottedId;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Entry;
import com.sun.fortress.nodes.Exit;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.FloatLiteral;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.Generator;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.IntLiteral;
import com.sun.fortress.nodes.LHS;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LetExpr;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.ListComprehension;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.MapComprehension;
import com.sun.fortress.nodes.MapExpr;
import com.sun.fortress.nodes.ArrayExpr;
import com.sun.fortress.nodes.ArrayElement;
import com.sun.fortress.nodes.ArrayElements;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OperatorParam;
import com.sun.fortress.nodes.Opr;
import com.sun.fortress.nodes.BaseOprStaticArg;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.OprName;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.PostFix;
import com.sun.fortress.nodes.ArrayComprehension;
import com.sun.fortress.nodes.ArrayComprehensionClause;
import com.sun.fortress.nodes.SetComprehension;
import com.sun.fortress.nodes.Spawn;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StringLiteral;
import com.sun.fortress.nodes.SubscriptAssign;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.SubscriptOp;
import com.sun.fortress.nodes.Throw;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.Try;
import com.sun.fortress.nodes.TryAtomicExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.TypecaseClause;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.DimUnitDecl;
import com.sun.fortress.nodes.BaseDimStaticArg;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VoidLiteral;
import com.sun.fortress.nodes.While;
import com.sun.fortress.interpreter.evaluator._WrappedFValue;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.MatchFailure;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;


import java.util.concurrent.Callable;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

public class Evaluator extends EvaluatorBase<FValue> {
    boolean debug = false;
    int transactionNestingCount = 0;

    final public static FVoid evVoid = FVoid.V;

    public FValue eval(Expr e) {
        return e.accept(this);
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
        throw new InterpreterBug(this.getClass().getName() + "." + s
                + " not implemented");
    }

    public FValue NI(String s, AbstractNode n) {
        throw new InterpreterBug(this.getClass().getName() + "." + s
                + " not implemented, input \n" + NodeUtil.dump(n));
    }

    public FValue forAccumulator(Accumulator x) {
        return NI("forAccumulator");
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
                        errorMsg("Tuple assignment size mismatch, | lhs | = ",
                                 lhsSize, ", | rhs | = ",
                                 rhs_tuple.getVals().size()));
            }
        } else if (lhsSize != 1) {
            throw new ProgramError(x,e,
                    errorMsg("Tuple assignment size mismatch, | lhs | = ",
                                   lhsSize, ", rhs is not a tuple"));
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
        final Expr expr = x.getExpr();
        final Evaluator current = new Evaluator(this);
        transactionNestingCount += 1;

        FValue res = FortressTaskRunner.doIt (
            new Callable<FValue>() {
                public FValue call() {
                    Evaluator ev = new Evaluator(new BetterEnv(current.e, expr));
                    return expr.accept(ev);
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
            if (f.getLoc().isPresent()) return NI("forAtDo");
            if (f.isAtomic())
                return forAtomicExpr(new AtomicExpr(x.getSpan(), false,
                                                    f.getExpr()));
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

    public FValue evalExprList(List<Expr> exprs, AbstractNode tag) {
        return evalExprList(exprs, tag, this);
    }

    /**
     * Returns the evaluation of a list of (general) exprs, returning the
     * result of evaluating the last expr in the list.
     * Does the "right thing" with LetExprs.
     */
    public FValue evalExprList(List<Expr> exprs, AbstractNode tag,
                               Evaluator eval) {
        FValue res = evVoid;
        for (Expr exp : exprs) {
            // TODO This will get turned into forLet methods
            if (exp instanceof LetExpr) {
                BetterEnv inner = new BetterEnv(eval.e, exp);
                BuildLetEnvironments be = new BuildLetEnvironments(inner);
                try {
                    res = be.doLets((LetExpr) exp);
                } catch (FortressError ex) {
                    throw ex; /* Skip the wrapper */
                } catch (RuntimeException ex) {
                    throw ex; // new ProgramError(exp, inner, "Wrapped
                    // exception", ex);
                }
            } else {
                try {
                    res = exp.accept(eval);
                } catch (FortressError ex) {
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
                    Throwable t = tasks[i].getTaskException();
                    if (t instanceof Error) {
                        throw (Error)t;
                    } else if (t instanceof RuntimeException) {
                        throw (RuntimeException)t;
                    } else {
                        throw new ProgramError(exprs.get(i),
                                errorMsg("Wrapped Exception",t));
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
            return forBlock(y.getBody());
        } else if (param instanceof CaseParamSmallest) {
            CaseClause y = findSmallest(clauses);
            return forBlock(y.getBody());
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
                    return forBlock(c.getBody());
            }
            Option<Block> _else = x.getElseClause();
            if (_else.isPresent()) {
                // TODO need an Else node to hang a location on
                return forBlock(_else.getVal());
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

    public List<FType> evalTypeCaseBinding(Evaluator ev,
            Typecase x) {
        List<Binding> bindings = x.getBind();
        List<FType> res = new ArrayList<FType>();
        for (Iterator<Binding> i = bindings.iterator(); i.hasNext();) {
            Binding bind = i.next();
            String name = bind.getId().getName();
            Expr init = bind.getInit();
            FValue val = init.accept(ev);
            if (init instanceof VarRef
                    && NodeUtil.getName(((VarRef) init).getVar()).equals(name)) {
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

    private boolean moreSpecificHelper(TypecaseClause candidate,
            TypecaseClause current, Evaluator ev) {
        List<Type> candType = candidate.getMatch();
        List<Type> curType = current.getMatch();
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

    public List<TypecaseClause> BestMatches(TypecaseClause candidate,
            List<TypecaseClause> bestSoFar, Evaluator ev) {
        List<TypecaseClause> result = new ArrayList<TypecaseClause>();
        boolean addedCandidate = false;

        for (Iterator<TypecaseClause> i = bestSoFar.iterator(); i.hasNext();) {
            TypecaseClause current = i.next();
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
        for (Iterator<Id> i = x.getNames().iterator(); i.hasNext();) {
            result = result.concat(i.next().getName());
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
        List<DottedId> name = x.getDottedIds();
        return null;
    }

    public FValue forExtentRange(ExtentRange x) {
        return NI("forExtentRange");
    }

    public FValue forFieldRef(FieldRef x) {
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
            } catch (FortressError ex) {
                ex.setWithin(e);
                ex.setWhere(x);
                throw ex;
            }
//        } else if (fobj instanceof FObject) {
//            FObject fobject = (FObject) fobj;
//            // TODO Need to distinguish between public/private methods/fields
//            try {
//                return fobject.getSelfEnv().getValue(fld.getName());
//            } catch (FortressError ex) {
//                ex.setWithin(e);
//                ex.setWhere(x);
//                throw ex;
//            }
        } else {
            throw new ProgramError(x, e,
                    errorMsg("Non-object cannot have field ", fld.getName()));
        }

    }

    public FValue forMethodInvocation(MethodInvocation x) {
        Expr obj = x.getObj();
        Id method = x.getId();
        List<StaticArg> sargs = x.getStaticArgs();
        Expr arg = x.getArg();
        
        FValue fobj = obj.accept(this);
        if (fobj instanceof FObject) {
            FObject fobject = (FObject) fobj;
            // TODO Need to distinguish between public/private
            // methods/fields
            FValue cl = fobject.getSelfEnv().getValueNull(method.getName());
            if (cl == null) {
                // TODO Environment is split, might not be best choice
                // for error printing.
                throw new ProgramError(x, fobject.getSelfEnv(),
                                       errorMsg("undefined method ", method.getName()));
            } else if (sargs.isEmpty() && cl instanceof Method) {
                List<FValue> args = argList(arg.accept(this));
                    //evalInvocationArgs(java.util.Arrays.asList(null, arg));
                return ((Method) cl).applyMethod(args, fobject, x, e);
            } else if (cl instanceof OverloadedMethod) {
                throw new InterpreterBug(x, fobject.getSelfEnv(),
                                         "Don't actually resolve overloading of " +
                                         "generic methods yet.");
            } else if (cl instanceof MethodInstance) {
                // What gets retrieved is the symbolic instantiation of
                // the generic method.
                // This is ever-so-slightly wrong -- we need to not
                // create an "instance"
                // if the parameters are non-symbolic.
                GenericMethod gm = ((MethodInstance) cl).getGenerator();
                List<FValue> args = argList(arg.accept(this));
                    //evalInvocationArgs(java.util.Arrays.asList(null, arg));
                return (gm.typeApply(sargs, e, x)).applyMethod(args, fobject, x, e);
            } else {
                throw new ProgramError(x, fobject.getSelfEnv(),
                                       errorMsg("Unexpected method value in method ",
                                                "invocation, ", cl));
            }
        } else {
            throw new ProgramError(x, errorMsg("Unexpected receiver in method ",
                                               "invocation, ", fobj));
        }
    }

    public FValue forFnExpr(FnExpr x) {
        Option<Type> return_type = x.getReturnType();
        List<Param> params = x.getParams();
        Closure cl = new Closure(e, x); // , return_type, params);
        cl.finishInitializing();
        return cl;
    }

    public FValue forAbsFnDecl(AbsFnDecl x) {
        return NI("forAbsFnDecl");
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
        Option<Block> else_ = x.getElseClause();
        if (else_.isPresent()) {
            Block else_expr = else_.getVal();
            return else_expr.accept(this);
        }
        return evVoid;
    }

    public FValue forIfClause(IfClause x) {
        return NI("This ought not be called.");
    }

    public FValue forLValueBind(LValueBind x) {
        Id name = x.getId();
        String s = name.getName();
        return FString.make(s);
    }

    public FValue forLabel(Label x) {
        /* Don't forget to use a new evaluator/environment for the inner block. */
        throw new InterpreterBug(x,"label construct not yet implemented.");
    }

    public FValue forLetFn(LetFn x) {
        throw new InterpreterBug(x,"forLetFn not implemented.");
    }

    public FValue forListComprehension(ListComprehension x) {
        throw new InterpreterBug(x,"list comprehensions not yet implemented.");
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
            throw new InterpreterBug(x,"empty juxtaposition");
        List<FValue> evaled = evalExprListParallel(exprs);
        Boolean inFunction = true;
        Stack<FValue> stack = new Stack<FValue>();
        for (FValue val : evaled) {
            if (val instanceof Fcn) {
                if (!inFunction) {
                    stack.push(juxtApplyStack(stack, times, x));
                    inFunction = true;
                }
                stack.push(val);
            } else if (inFunction) {
                inFunction = false;
                stack.push(val);
            } else {
                FValue r = stack.pop();
                r = functionInvocation(Useful.list(r, val), times, x);
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

    public FValue forArrayElement(ArrayElement x) {
        // MDEs occur only within ArrayElements, and reset
        // row evaluation to an outercontext (in the scope
        // of the element, that is).
        throw new InterpreterBug(x,"Singleton paste?  Can't judge dimensionality without type inference.");
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
    public FValue forArrayElements(ArrayElements x) {
        List<ArrayExpr> elements = x.getElements();
        EvaluatorInPaste eip = new EvaluatorInPaste(this);
        List<FValue> values = eip.evalExprList(elements);
        IUOTuple paste = new IUOTuple(values, x);
        // Finish the pasting now.
        paste.finish();
        return paste;
    }

    public FValue for_RewriteObjectExpr(_RewriteObjectExpr x) {
        String s = x.getGenSymName();
        // FType ft = e.getType(s);
        // System.out.println("for_RewriteObjectExpr "+s);
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
            throw new InterpreterBug(x,e,"_RewriteObjectExpr " + s + " has 'constructor' " + v);
        }

        // Option<List<Type>> traits = x.getTraits();
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
        } catch (FortressError ex) {
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
        } catch (FortressError ex) {
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

    /** Assumes {@code x.getOps()} is a list of length 1. */
    public FValue forOprExpr(OprExpr x) {
        // debugPrint("forOprExpr " + x);
        OprName op = x.getOps().get(0);
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
            throw new ProgramError(x, e,
                    errorMsg("Operator ", op.stringName(),
                             " has a non-function value ", fvalue));
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

    /*
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
    */

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
            throw new ProgramError(obj,
                    errorMsg("Value should be an object; got " + arr));
        }
        FObject array = (FObject) arr;
        FValue ixing = array.getSelfEnv().getValueNull("[]");
        if (ixing == null || !(ixing instanceof Method)) {
            throw new ProgramError(x,
                    errorMsg("Could not find appropriate definition of opr [] on ",
                             array));
        }
        Method cl = (Method) ixing;
        List<FValue> subscripts = evalExprListParallel(subs);
        return cl.applyMethod(subscripts, array, x, e);
    }

    public FValue forSubscriptOp(SubscriptOp x) {
        return NI("forSubscriptOp");
    }

    /** Assumes wrapped FnRefs have ids fields of length 1. */
    public FValue forTightJuxt(TightJuxt x) {
        // debugPrint("forTightJuxt " + x);
        // Assume for now that tight juxtaposition is function application fixme
        // chf
        List<Expr> exprs = x.getExprs();
        if (exprs.size() == 0)
            throw new InterpreterBug(x,e,"empty juxtaposition");
        Expr fcnExpr = exprs.get(0);
        
        // Translate compound VarRefs to FieldRefs
        if (fcnExpr instanceof VarRef &&
            ((VarRef)fcnExpr).getVar().getNames().size() > 1) {
            fcnExpr = ExprFactory.makeFieldRef((VarRef)fcnExpr);
        }
        
        if (fcnExpr instanceof FieldRef) {
            // In this case, open code the FieldRef evaluation
            // so that the object can be preserved. Alternate
            // strategy might be to generate a closure from
            // the field selection.
            FieldRef fld_sel = (FieldRef) fcnExpr;
            Expr obj = fld_sel.getObj();
            Id fld = fld_sel.getId();
            FValue fobj = obj.accept(this);
            return juxtMemberSelection(x, fobj, fld, exprs);
            
        } else if (fcnExpr instanceof FnRef) {
            // We must evaluate the receiver separately so we can get a handle on it
            FnRef tax = (FnRef) fcnExpr;
            List<Id> names = tax.getIds().get(0).getNames();
            if (names.size() > 1) {
                VarRef receiverVar = ExprFactory.makeVarRef(IterUtil.skipLast(names));
                Id fld = IterUtil.last(names);
                List<StaticArg> args = tax.getStaticArgs();
                FValue fobj = forVarRef(receiverVar);
                
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
                                               errorMsg("undefined method/field ",
                                                        fld.getName()));
                    } else if (cl instanceof OverloadedMethod) {
                        
                        throw new InterpreterBug(x, fobject.getSelfEnv(),
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
                                               errorMsg("Unexpected Selection result in Juxt of FnRef of Selection, ",
                                                        cl));
                    }
                } else {
                    throw new ProgramError(x,
                                           errorMsg("Unexpected Selection LHS in Juxt of FnRef of Selection, ",
                                                    fobj));
                    
                }
            }
            // else fall through
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
    private FValue juxtMemberSelection(TightJuxt x, FValue fobj, Id fld,
            List<Expr> exprs) throws ProgramError {
        if (fobj instanceof FObject) {
            FObject fobject = (FObject) fobj;
            // TODO Need to distinguish between public/private methods/fields
            FValue cl = fobject.getSelfEnv().getValueNull(fld.getName());
            if (cl == null)
                // TODO Environment is split, might not be best choice for error
                // printing.
                throw new ProgramError(x, fobject.getSelfEnv(),
                        errorMsg("undefined method/field ", fld.getName()));
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
                        errorMsg("Tight juxtaposition of non-function ", fld.getName()));
            }
        } else {
            // TODO Could be a fragment of a component/api name, too.
            throw new ProgramError(x,
                    errorMsg("", fobj, ".", fld,
                             " but not object.something"));
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

    public FValue forTypecase(Typecase x) {
        Evaluator ev = new Evaluator(this, x);
        List<FType> res = evalTypeCaseBinding(ev, x);
        FValue result = evVoid;
        List<TypecaseClause> clauses = x.getClauses();

        for (TypecaseClause c : clauses) {
            List<Type> match = c.getMatch();
            /* Technically, match and res need not be tuples; they could be
               singletons and the subtype test below ought to be correct. */
            FType matchTuple = EvalType.getFTypeFromList(match, ev.e);
            FType resTuple = FTypeTuple.make(res);

            if (resTuple.subtypeOf(matchTuple)) {
                Block body = c.getBody();
                result = evalExprList(body.getExprs(), body, ev);
                return result;
            }
        }

        Option<Block> el = x.getElseClause();
        if (el.isPresent()) {
            Block elseClauses = el.getVal();
            // TODO really ought to have a node, with a location, for this list
            result = forBlock(elseClauses);
            return result;
        } else {
            throw new MatchFailure();
        }
    }

    public FValue forTypecaseClause(TypecaseClause x) {
        return NI("forTypeClause");
    }

    public FValue forTypeArg(TypeArg x) {
        return NI("forTypeArg");
    }

    public FValue forBaseDimStaticArg(BaseDimStaticArg x) {
        return NI("forBaseDimStaticArg");
    }

    public FValue forDimUnitDecl(DimUnitDecl x) {
        return NI("forDimUnitDecl");
    }

    public FValue forUnpastingBind(UnpastingBind x) {
        return NI("forUnpastingBind");
    }

    public FValue forUnpastingSplit(UnpastingSplit x) {
        return NI("forUnpastingSplit");
    }

    public FValue forAbsVarDecl(AbsVarDecl x) {
        return NI("forAbsVarDecl");
    }

    public FValue forVarDecl(VarDecl x) {
        return NI("forVarDecl");
    }

    public FValue forVarRef(VarRef x) {
        // debugPrint("forVarRef " + x);
        List<Id> names = x.getVar().getNames();
        if (names.isEmpty())
            throw new InterpreterBug(x, e, "empty variable name");

        FValue res = e.getValueNull(names.get(0).getName());
        if (res == null)
            throw new ProgramError(x, e, errorMsg("undefined variable ",
                                                  names.get(0).getName()));

        for (Id fld : IterUtil.skipFirst(names)) {
            if (res instanceof Selectable) {
                /*
                 * Selectable was introduced to make it not necessary
                 * to know whether a.b was field b of object a, or member
                 * b of api a (or api name prefix, extended).
                 */
                // TODO Need to distinguish between public/private methods/fields
                try {
                    res = ((Selectable) res).select(fld.getName());
                } catch (FortressError ex) {
                    ex.setWithin(e);
                    ex.setWhere(x);
                    throw ex;
                }
            } else {
                throw new ProgramError(x, e, errorMsg("Non-object cannot have field ",
                                                      fld.getName()));
            }
        }
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

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forFnRef(com.sun.fortress.interpreter.nodes.FnRef)
     */
    /** Assumes {@code x.getIds()} is a list of length 1. */
    @Override
    public FValue forFnRef(FnRef x) {
        DottedId id = x.getIds().get(0);
        FValue g = forVarRef(new VarRef(id.getSpan(), id));
        List<StaticArg> args = x.getStaticArgs();
        if (g instanceof FGenericFunction) {
            return ((FGenericFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericConstructor) {
            return ((GenericConstructor) g).typeApply(args, e, x);
        } else if (g instanceof OverloadedFunction) {
            return((OverloadedFunction) g).typeApply(args, e, x);
        } else {
            throw new ProgramError(x, e, errorMsg("Unexpected FnRef value, ", g));
        }
    }

    public FValue for_WrappedFValue(_WrappedFValue w) {
        return w.getFValue();
    }
}
