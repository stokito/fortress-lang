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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.TaskError;
import com.sun.fortress.interpreter.evaluator.tasks.TupleTask;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.types.FTypeTrait;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FAsIf;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FChar;
import com.sun.fortress.interpreter.evaluator.values.FFloatLiteral;
import com.sun.fortress.interpreter.evaluator.values.FGenerator;
import com.sun.fortress.interpreter.evaluator.values.FGenericFunction;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FRange;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FStringLiteral;
import com.sun.fortress.interpreter.evaluator.values.FTuple;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Fcn;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.GenericMethod;
import com.sun.fortress.interpreter.evaluator.values.GenericSingleton;
import com.sun.fortress.interpreter.evaluator.values.IUOTuple;
import com.sun.fortress.interpreter.evaluator.values.Method;
import com.sun.fortress.interpreter.evaluator.values.MethodClosure;
import com.sun.fortress.interpreter.evaluator.values.MethodInstance;
import com.sun.fortress.interpreter.evaluator.values.OverloadedFunction;
import com.sun.fortress.interpreter.evaluator.values.OverloadedMethod;
import com.sun.fortress.interpreter.evaluator.values.Selectable;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes.AbstractFieldRef;
import com.sun.fortress.nodes.AsExpr;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.Assignment;
import com.sun.fortress.nodes.AtomicExpr;
import com.sun.fortress.nodes.KeywordExpr;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.CaseClause;
import com.sun.fortress.nodes.CaseExpr;
import com.sun.fortress.nodes.CatchClause;
import com.sun.fortress.nodes.Catch;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.CharLiteralExpr;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Exit;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FieldRefForSure;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.FloatLiteralExpr;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.GeneratorClause;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.LHS;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LetExpr;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.ArrayExpr;
import com.sun.fortress.nodes.ArrayElement;
import com.sun.fortress.nodes.ArrayElements;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.MathPrimary;
import com.sun.fortress.nodes.MathItem;
import com.sun.fortress.nodes.ExprMI;
import com.sun.fortress.nodes.ParenthesisDelimitedMI;
import com.sun.fortress.nodes.NonParenthesisDelimitedMI;
import com.sun.fortress.nodes.NonExprMI;
import com.sun.fortress.nodes.ExponentiationMI;
import com.sun.fortress.nodes.SubscriptingMI;
import com.sun.fortress.nodes.Name;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes._RewriteFieldRef;
import com.sun.fortress.nodes._RewriteFnRef;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.ArrayComprehension;
import com.sun.fortress.nodes.ArrayComprehensionClause;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Throw;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Try;
import com.sun.fortress.nodes.TryAtomicExpr;
import com.sun.fortress.nodes.ArgExpr;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.TypecaseClause;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.While;
import com.sun.fortress.interpreter.evaluator._WrappedFValue;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.MatchFailure;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;

import java.util.concurrent.Callable;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class Evaluator extends EvaluatorBase<FValue> {
     boolean debug = false;
    final private static boolean isArgExpr = false;

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
    }

    public FValue NI(String s) {
        return bug(this.getClass().getName() + "." + s + " not implemented");
    }

    public FValue NI(String s, AbstractNode n) {
        return bug(n, this.getClass().getName() + "." + s
                + " not implemented, input \n" + NodeUtil.dump(n));
    }

    public FValue forAsExpr(AsExpr x) {
        final Expr expr = x.getExpr();
        FValue val = expr.accept(this);
        Type ty = x.getType();
        FType fty = EvalType.getFType(ty, e);
        if (val.type().subtypeOf(fty))
            return val;
        else
            return error(x, e, errorMsg("The type of expression ", val.type(),
                                        " is not a subtype of ", fty, "."));
    }

    public FValue forAsIfExpr(AsIfExpr x) {
        final Expr expr = x.getExpr();
        FValue val = expr.accept(this);
        Type ty = x.getType();
        FType fty = EvalType.getFType(ty, e);
        if (val.type().subtypeOf(fty))
            return new FAsIf(val,fty);
        else
            return error(x, e, errorMsg("Type of expression, ", val.type(),
                                        ", not a subtype of ", fty, "."));
    }

    // We ask lhs to accept twice (with this and an LHSEvaluator) in
    // the operator case. Might this cause the world to break?
    public FValue forAssignment(Assignment x) {
        Option<Op> possOp = x.getOpr();
        LHSToLValue getLValue = new LHSToLValue(this);
        List<? extends LHS> lhses = getLValue.inParallel(x.getLhs());
        int lhsSize = lhses.size();
        FValue rhs = x.getRhs().accept(this);

        if (possOp.isSome()) {
            // We created an lvalue for lhses above, so there should
            // be no fear of duplicate evaluation.
            Op opr = Option.unwrap(possOp);
            Fcn fcn = (Fcn) opr.accept(this);
            FValue lhsValue;
            if (lhsSize > 1) {
                // TODO:  An LHS walks, talks, and barks just like
                // an Expr in this context.  Yet it isn't an Expr, and
                // we can't pass the lhses to the numerous functions
                // which expect a List<Expr>---for example ArgExpr
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
                error(x,e,
                      errorMsg("Tuple assignment size mismatch, | lhs | = ",
                               lhsSize, ", | rhs | = ",
                               rhs_tuple.getVals().size()));
            }
        } else if (lhsSize != 1) {
            error(x,e,
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
        FValue res = FortressTaskRunner.doIt (
            new Callable<FValue>() {
                public FValue call() {
                    Evaluator ev = new Evaluator(new BetterEnv(current.e, expr));
                    return expr.accept(ev);
                }
            }
        );
        return res;
    }

    public FValue forTryAtomicExpr(TryAtomicExpr x) {
        final Expr expr = x.getExpr();
        final Evaluator current = new Evaluator(this);
        FValue res = FVoid.V;
        // Inside a transaction tryAtomic is a noop
        if (BaseTask.getThreadState().transactionNesting() > 1) {
            Evaluator ev = new Evaluator(new BetterEnv(current.e, expr));
            return expr.accept(current);
        }
        try {
            res = FortressTaskRunner.doItOnce(
                new Callable<FValue>() {
                    public FValue call() {
                        Evaluator ev = new Evaluator(new BetterEnv(current.e, expr));
                        FValue res1 = expr.accept(ev);
                        return res1;
                    }
                }
            );
        } catch (AbortedException ae) {
            FObject f = (FObject) e.getValue(WellKnownNames.tryatomicFailureException);
            FortressException f_exc = new FortressException(f);
            throw f_exc;
        }
        return res;
    }

    public FValue forKeywordExpr(KeywordExpr x) {
        return NI("forKeywordExpr");
    }

    public FValue forDo(Do x) {
        int s = x.getFronts().size();
        if (s == 0) return FVoid.V;
        if (s == 1) {
            DoFront f = x.getFronts().get(0);
                if (f.getLoc().isSome()) return NI("forAtDo");
                if (f.isAtomic())
                    return forAtomicExpr(new AtomicExpr(x.getSpan(), false,
                                                        f.getExpr()));
             return f.getExpr().accept(this);
       }

       TupleTask[] tasks = new TupleTask[s];
       for (int i = 0; i < s; i++) {
            DoFront f = x.getFronts().get(i);
            if (f.getLoc().isSome()) return NI("forAtDo");
            if (f.isAtomic())
                tasks[i] = new TupleTask(new AtomicExpr(x.getSpan(), false,
                                                        f.getExpr()), this);
            else
                tasks[i] = new TupleTask(f.getExpr(), this);
        }
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        BaseTask currentTask = runner.getCurrentTask();
        TupleTask.forkJoin(tasks);
        runner.setCurrentTask(currentTask);
        for (int i = 0; i < s; i++) {
            if (tasks[i].causedException()) {
                Throwable t = tasks[i].taskException();
                if (t instanceof Error) {
                    throw (Error)t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException)t;
                } else {
                    error(x.getFronts().get(i), errorMsg("Wrapped Exception",t));
                }
            }
        }
        return FVoid.V;
    }

    public FValue forBlock(Block x) {
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
        FValue res = FVoid.V;
        for (Expr exp : exprs) {
            // TODO This will get turned into forLet methods
            if (exp instanceof LetExpr) {
                BetterEnv inner = new BetterEnv(eval.e, exp);
                BuildLetEnvironments be = new BuildLetEnvironments(inner);
                    res = be.doLets((LetExpr) exp);
            } else {
                    res = exp.accept(eval);
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
     /* If we are already in a transaction, don't evaluate in parallel */
        } else if (BaseTask.getThreadState().transactionNesting() > 0) {
            for (Expr exp : exprs) {
            resList.add(exp.accept(this));
            }
        } else if (sz > 1) {
            TupleTask[] tasks = new TupleTask[exprs.size()];
            int count = 0;
            for (Expr e : exprs) {
                tasks[count++] = new TupleTask(e, this);
            }
            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
            BaseTask currentTask = runner.getCurrentTask();
            TupleTask.forkJoin(tasks);
            runner.setCurrentTask(currentTask);

            for (int i = 0; i < count; i++) {
                if (tasks[i].causedException()) {
                    Throwable t = tasks[i].taskException();
                    if (t instanceof Error) {
                        throw (Error)t;
                    } else if (t instanceof RuntimeException) {
                        throw (RuntimeException)t;
                    } else {
                        error(exprs.get(i), errorMsg("Wrapped Exception",t));
                    }
                }
                resList.add(tasks[i].getRes());
            }
        }
        return resList;
    }

    CaseClause findExtremum(CaseExpr x, Fcn fcn ) {
        List<CaseClause> clauses = x.getClauses();
        Iterator<CaseClause> i = clauses.iterator();

        CaseClause c = i.next();
        FValue winner = c.getMatch().accept(this);
        CaseClause res = c;

        for (; i.hasNext();) {
            c = i.next();
            List<FValue> vargs = new ArrayList<FValue>(2);
            FValue current = c.getMatch().accept(this);
            vargs.add(current);
            vargs.add(winner);
            FValue invoke = functionInvocation(vargs, fcn, x);
            if (!(invoke instanceof FBool)) {
                return error(x,errorMsg("Non-boolean result ",invoke,
                                        " in chain, args ", vargs));
            }
            FBool boolres = (FBool) invoke;
            if (boolres.getBool()) {
                winner = current;
                res = c;
            }
        }
        return res;
    }

    public FValue forCaseExpr(CaseExpr x) {
        List<CaseClause> clauses = x.getClauses();
        Option<Expr> param = x.getParam();
        if (param.isNone()) {
            Option<Op> compare = x.getCompare();
            if (compare.isSome()) {
                String op = NodeUtil.nameString(Option.unwrap(compare));
                return forBlock(findExtremum(x,(Fcn) e.getValue(op)).getBody());
            } else
                return error(x,errorMsg("Missing operator for the extremum ",
                                        "expression ", x));
        } else {
            // Evaluate the parameter
            FValue paramValue = Option.unwrap(param).accept(this);
            // Assign a comparison function
            Fcn fcn = (Fcn) e.getValue("=");
            Option<Op> compare = x.getCompare();
            if (compare.isSome())
                fcn = (Fcn) e.getValue(NodeUtil.nameString(Option.unwrap(compare)));

            // Iterate through the cases
            for (Iterator<CaseClause> i = clauses.iterator(); i.hasNext();) {
                CaseClause c = i.next();
                // Evaluate the clause
                FValue match = c.getMatch().accept(this);
                List<FValue> vargs = new ArrayList<FValue>();
                vargs.add(paramValue);
                vargs.add(match);
                if (Glue.extendsGenericTrait(match.type(), "Generator")) {
                    fcn = (Fcn) e.getValue("IN");
                }
                FBool success = (FBool) functionInvocation(vargs, fcn, c);
                if (success.getBool())
                    return forBlock(c.getBody());
            }
            Option<Block> _else = x.getElseClause();
            if (_else.isSome()) {
                // TODO need an Else node to hang a location on
                return forBlock(Option.unwrap(_else));
            }
            FObject f = (FObject) e.getValue(WellKnownNames.matchFailureException);
            FortressException f_exc = new FortressException(f);
            throw f_exc;
        }
    }

    /*
     * Only handles 2-ary definitions of chained operators. That should be
     * perfectly wonderful for the moment.
     */
    public FValue forChainExpr(ChainExpr x) {
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
            FValue invoke = functionInvocation(vargs, fcn, x);
            if (!(invoke instanceof FBool)) {
                return error(x,errorMsg("Non-boolean result ",invoke,
                                        " in chain, args ", vargs));
            }
            boolres = (FBool)invoke;
            idVal = exprVal;
        }
        return boolres;
    }

    private boolean isShadow(Expr expr, String name) {
        return (expr instanceof VarRef &&
                NodeUtil.nameString(((VarRef)expr).getVar()).equals(name));
    }

    private boolean isShadow(Option<String> var, String name) {
        if (var.isSome()) {
            return Option.unwrap(var).equals(name);
        } else { // var.isNone()
            return false;
        }
    }

    private Option<String> getName(Expr expr) {
        if (expr instanceof VarRef) {
            return Option.some(NodeUtil.nameString(((VarRef)expr).getVar()));
        } else { // !(expr instanceof VarRef)
            return Option.<String>none();
        }
    }

    public List<FType> evalTypeCaseBinding(Evaluator ev, Typecase x) {
        Pair<List<Id>, Option<Expr>> bindings = x.getBind();
        List<Id> bindIds = bindings.getA();
        Option<Expr> exprOpt = bindings.getB();
        Expr expr;
        List<FType> res = new ArrayList<FType>();

        if (exprOpt.isNone()) {
            for (Id id : bindIds) {
                FValue val = ExprFactory.makeVarRef(id).accept(ev);
                /* Avoid shadow error when we bind the same var name */
                ev.e.putValueUnconditionally(id.getText(), val);
                res.add(val.type());
            }
        } else { // exprOpt.isSome()
            expr = Option.unwrap(exprOpt);
            if (bindIds.size() == 1) {
                FValue val = expr.accept(ev);
                String name = bindIds.get(0).getText();
                if (isShadow(expr, name)) {
                    /* Avoid shadow error when we bind the same var name */
                    ev.e.putValueUnconditionally(name, val);
                } else {
                    /* But warn about shadowing in all other cases */
                    ev.e.putValue(name, val);
                }
                res.add(val.type());
            } else { // bindIds.size() > 1
                List<Pair<FValue,Option<String>>> vals =
                    new ArrayList<Pair<FValue,Option<String>>>();
                if (expr instanceof TupleExpr) {
                    for (Expr e : ((TupleExpr)expr).getExprs()) {
                        vals.add(new Pair(e.accept(ev), getName(e)));
                    }
                } else { // !(expr instanceof TupleExpr)
                    FValue val = expr.accept(ev);
                    if (!(val instanceof FTuple)) {
                        error(expr, "RHS does not yield a tuple.");
                    }
                    for (FValue v : ((FTuple)val).getVals()) {
                        vals.add(new Pair(v, Option.<String>none()));
                    }
                }
                int index = 0;
                for (Id id : bindIds) {
                    String name = id.getText();
                    Pair<FValue,Option<String>> pair = vals.get(index);
                    FValue val = pair.getA();
                    if (isShadow(pair.getB(), name)) {
                        /* Avoid shadow error when we bind the same var name */
                        ev.e.putValueUnconditionally(name, val);
                    } else {
                        /* But warn about shadowing in all other cases */
                        ev.e.putValue(name, val);
                    }
                    res.add(val.type());
                    index++;
                }
            }
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

    public FValue forAPIName(APIName x) {
        String result = "";
        for (Iterator<Id> i = x.getIds().iterator(); i.hasNext();) {
            result = result.concat(i.next().getText());
            if (i.hasNext())
                result = result.concat(".");
        }
        return FString.make(result);
    }

    public FValue forExit(Exit x) {
        Option<Id> target = x.getTarget();
        Option<Expr> returnExpr = x.getReturnExpr();
        FValue res;
        LabelException e;

        if (returnExpr.isSome()) {
            res = Option.unwrap(returnExpr).accept(new Evaluator(this));
        } else {
            res = FVoid.V;
        }

        if (target.isSome()) {
            String t = Option.unwrap(target).getText();
            e = new NamedLabelException(x, t, res);
        } else {
            e = new LabelException(x,res);
        }
        throw e;
    }

    public FValue forExtentRange(ExtentRange x) {
        return NI("forExtentRange");
    }

    @Override
    public FValue forFieldRef(FieldRef x) {
        return forFieldRefCommon(x, x.getField());
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor#for_RewriteFieldRef(com.sun.fortress.nodes._RewriteFieldRef)
     */
    @Override
    public FValue for_RewriteFieldRef(_RewriteFieldRef x) {
        return forFieldRefCommon(x, x.getField());
    }

    @Override
    public FValue forFieldRefForSure(FieldRefForSure x) {
        return forFieldRefCommon(x, x.getField());
    }

    private FValue forFieldRefCommon(AbstractFieldRef x, Name fld) throws FortressError,
            ProgramError {
        Expr obj = x.getObj();

        FValue fobj = obj.accept(this);
        if (fobj instanceof Selectable) {
            Selectable selectable = (Selectable) fobj;
            /*
             * Selectable was introduced to make it not necessary to know
             * whether a.b was field b of object a, or member b of api a (or api
             * name prefix, extended).
             */
            // TODO Need to distinguish between public/private methods/fields
            try {
                return selectable.select(NodeUtil.nameString(fld));
            } catch (FortressError ex) {
                throw ex.setContext(x, e);
            }
            // } else if (fobj instanceof FObject) {
            // FObject fobject = (FObject) fobj;
            // // TODO Need to distinguish between public/private methods/fields
            // try {
            // return fobject.getSelfEnv().getValue(fld.getName());
            // } catch (FortressError ex) {
            // throw ex.setContext(x,e);
            //        }
        } else {
           return error(x, e,
                         errorMsg("Non-object cannot have field ",
                                  NodeUtil.nameString(fld)));
        }
    }

    public FValue forMethodInvocation(MethodInvocation x) {
        Expr obj = x.getObj();
        Id method = x.getMethod();
        List<StaticArg> sargs = x.getStaticArgs();
        Expr arg = x.getArg();

        FValue fobj = obj.accept(this);
        FObject fobject;
        BetterEnv selfEnv;

        // TODO Need to distinguish between public/private
        // methods/fields
        if (fobj.getValue() instanceof FObject) {
            fobject = (FObject) fobj.getValue();
            if (fobj.type() instanceof FTypeTrait) {
                // fobj instanceof FAsIf, and nontrivial type()
                selfEnv = ((FTypeTrait)fobj.type()).getMembers();
            } else {
                selfEnv = fobject.getSelfEnv();
            }
        } else {
            return error(x, errorMsg("Unexpected receiver in method ",
                                               "invocation, ", fobj));
        }
        FValue cl = selfEnv.getValueNull(NodeUtil.nameString(method));
        if (cl == null) {
            // TODO Environment is split, might not be best choice
            // for error printing.
            String msg = errorMsg("undefined method ", NodeUtil.nameString(method));
            return error(x, selfEnv, msg);
        } else if (sargs.isEmpty() && cl instanceof Method) {
            List<FValue> args = argList(arg.accept(this));
                //evalInvocationArgs(java.util.Arrays.asList(null, arg));
            try {
                return ((Method) cl).applyMethod(args, fobject, x, e);
            } catch (FortressError ex) {
                throw ex.setContext(x, selfEnv);
            } catch (StackOverflowError soe) {
                return error(x,selfEnv, errorMsg("Stack overflow on ",x));
            }
        } else if (cl instanceof OverloadedMethod) {
            return bug(x, selfEnv,   "Don't actually resolve overloading of " +
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
            try {
                return (gm.typeApply(sargs, e, x)).
                        applyMethod(args, fobject, x, e);
            } catch (FortressError ex) {
                throw ex.setContext(x,selfEnv);
            } catch (StackOverflowError soe) {
                return error(x,selfEnv, errorMsg("Stack overflow on ",x));
            }
        } else {
            return error(x, selfEnv,
                           errorMsg("Unexpected method value in method ",
                                    "invocation, ", cl.toString() + "\n" +
                                    NodeUtil.dump(x)));
        }
    }

    public FValue forFnExpr(FnExpr x) {
        Option<Type> return_type = x.getReturnType();
        List<Param> params = x.getParams();
        Closure cl = new Closure(e, x); // , return_type, params);
        cl.finishInitializing();
        return cl;
    }

    public FValue forGeneratorClause(GeneratorClause x) {
        List<Id> names = x.getBind();
        Expr init = x.getInit();
        FValue rval = init.accept(this);
        FRange range = (FRange)rval;
        return new FGenerator(names, range);
    }

    public FValue forId(Id x) {
        return FString.make(x.getText());
    }

    public FValue forIf(If x) {
        List<IfClause> clause = x.getClauses();
        for (Iterator<IfClause> i = clause.iterator(); i.hasNext();) {
            IfClause ifclause = i.next();
            FValue clauseVal = ifclause.getTest().accept(this);
            if (!(clauseVal instanceof FBool)) {
                return error(ifclause,
                             errorMsg("If clause did not return boolean, but ",
                                      clauseVal));
            }
            FBool fbool = (FBool) clauseVal;
            if (fbool.getBool()) {
                return ifclause.getBody().accept(this);
            }
        }
        Option<Block> else_ = x.getElseClause();
        if (else_.isSome()) {
            return Option.unwrap(else_).accept(this);
        }
        else {
            return FVoid.V;
        }
    }

    public FValue forLValueBind(LValueBind x) {
        Id name = x.getName();
        return FString.make(NodeUtil.nameString(name));
    }

    public FValue forLabel(Label x) {
        Id name = x.getName();
        Block body  = x.getBody();
        FValue res = FVoid.V;
        try {
            Evaluator ev = new Evaluator(new BetterEnv(e,body));
            res = ev.forBlock(body);
        } catch (NamedLabelException e) {
            if (e.match(name.getText())) {
                return e.res();
            } else throw e;
        } catch (LabelException e) {
            return e.res();
        }
        return res;
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
            bug(x,"empty juxtaposition");
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

    public FValue forArrayElement(ArrayElement x) {
        // MDEs occur only within ArrayElements, and reset
        // row evaluation to an outercontext (in the scope
        // of the element, that is).
        return bug(x,"Singleton paste?  Can't judge dimensionality without type inference.");
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
            return cl.applyOEConstructor( x, e);
        } else if (v instanceof Constructor) {
            Constructor cl = (Constructor) v;
            // FTypeObject fto = (FTypeObject) ft;
            // cl.setParams(Collections.<Parameter> emptyList());
            // BuildEnvironments.finishObjectTrait(x.getTraits(), fto, e);
            // cl.finishInitializing();
            return cl.applyOEConstructor( x, e);
        } else {
            return bug(x,e, errorMsg("_RewriteObjectExpr ", s,
                                              " has 'constructor' ", v));
        }

        // Option<List<Type>> traits = x.getTraits();
        // List<Decl> defs = x.getDefs();
        // FTypeObject fto = new FTypeObject(genSym(x), e);
        // return BuildEnvironments.anObject(fto, e, traits, defs, x);

    }

    private FValue getOp(OpName op) {
        try {
            return e.getValue(NodeUtil.nameString(op));
        } catch (FortressError ex) {
            throw ex.setContext(op,e);
        }
    }

    public FValue forQualifiedName(QualifiedName n) {
        if (n.getApi().isSome()) {
            return NI.nyi("Qualified name");
        }
        else {
            return n.getName().accept(this);
        }
    }

    public FValue forEnclosing(Enclosing x) {
        return getOp(x);
    }

    public FValue forOp(Op op) {
        return getOp(op);
    }

    private boolean isExponentiation(OprExpr expr) {
        OpRef ref = expr.getOp();
        if (ref.getOps().size() != 1) return false;
        else {
            OpName name = ref.getOps().get(0).getName();
            if (!(name instanceof Op)) return false;
            else return (((Op)name).getText().equals("^") ||
                         OprUtil.isPostfix(name));
        }
    }

    /** Assumes {@code x.getOps()} is a list of length 1.  At the
     * moment it appears that this is true for every OprExpr node that
     * is ever created. */
    public FValue forOprExpr(OprExpr x) {
        OpRef ref = x.getOp();
        if (ref.getOps().size() != 1) {
            return bug(x, errorMsg("OprExpr with multiple operators ",x));
        }
        QualifiedOpName op = ref.getOps().get(0);
        OpName name = op.getName();
        List<Expr> args = x.getArgs();
        FValue fvalue = op.accept(this);
        // Evaluate actual parameters.
        int s = args.size();
        FValue res = FVoid.V;
        List<FValue> vargs;

        if (name instanceof Op && OprUtil.isPostfix(name) &&
            args.size() == 1) {
        // It is a static error if a function argument is immediately followed
        // by a non-expression element.  For example, f(x)!
        // It is a static error if an exponentiation is immediately followed
        // by a non-expression element.  For example, a^b!
            Expr arg = args.get(0);
            if (arg instanceof OprExpr &&
                isExponentiation((OprExpr)arg)) { // a^b!
                vargs = error(arg,
                              "It is a static error if an exponentiation is " +
                              "immediately followed by a non-expression " +
                              "element.");
            } else if (arg instanceof TightJuxt) { // f(x)!
                vargs = Useful.list(forTightJuxt((TightJuxt)arg, true));
            } else if (arg instanceof MathPrimary) { // f(x)^y! y[a](x)!
                vargs = Useful.list(forMathPrimary((MathPrimary)arg, true));
            } else vargs = evalExprListParallel(args);
        } else vargs = evalExprListParallel(args);

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
            error(x, e,
                  errorMsg("Operator ", op.stringName(),
                           " has a non-function value ", fvalue));
        }
        Fcn fcn = (Fcn) fvalue;
        if (s <= 2 || (op.getName() instanceof Enclosing)) {
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

    public FValue forStringLiteralExpr(StringLiteralExpr x) {
        return new FStringLiteral(x.getText());
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
        Option<Enclosing> op = x.getOp();
        // Should evaluate obj.[](subs, getText)
        FValue arr = obj.accept(this);
        if (!(arr instanceof FObject)) {
            error(obj, errorMsg("Value should be an object; got ", arr));
        }
        FObject array = (FObject) arr;
        String opString;
        if (op.isSome()) {
            opString = NodeUtil.nameString(Option.unwrap(op));
        } else {
            opString = "[]";
        }
        FValue ixing = array.getSelfEnv().getValueNull(opString);
        if (ixing == null || !(ixing instanceof Method)) {
            error(x,errorMsg("Could not find appropriate definition of opr [] on ",
                             array));
        }
        Method cl = (Method) ixing;
        List<FValue> subscripts = evalExprListParallel(subs);
        return cl.applyMethod(subscripts, array, x, e);
    }

    public FValue invokeGenericMethod(FObject fobject, Name fld, List<StaticArg> args, List<Expr> exprs, HasAt x) {
        FValue cl = fobject.getSelfEnv().getValueNull(NodeUtil.nameString(fld));
        if (cl == null) {
            // TODO Environment is split, might not be best choice
            // for error printing.
            return error(x, fobject.getSelfEnv(),
                         errorMsg("undefined method/field ",
                                  NodeUtil.nameString(fld)));
        } else if (cl instanceof OverloadedMethod) {

            return bug(x, fobject.getSelfEnv(),
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
            return error(x, fobject.getSelfEnv(),
                         errorMsg("Unexpected Selection result in Juxt of FnRef of Selection, ",
                                  cl));
        }
    }

    private boolean isFunction(FValue val) { return (val instanceof Fcn); }
    private boolean isExpr(MathItem mi)    { return (mi instanceof ExprMI); }
    private boolean isParenthesisDelimited(MathItem mi) {
        return (mi instanceof ParenthesisDelimitedMI);
    }
    private MathItem dummyExpr() {
        Span span = new Span();
        Expr dummyE = ExprFactory.makeVoidLiteralExpr(span);
        return new NonParenthesisDelimitedMI(span, dummyE);
    }

    private List<Pair<MathItem,FValue>> stepTwo(List<Pair<MathItem,FValue>> vs,
                                                boolean isPostfix) {
        if (vs.size() < 1) return error("Reassociation of MathPrimary failed!");
        else if (vs.size() == 1) return vs;
        else { // vs.size() > 1
            int ftnInd = 0;
            FValue ftn = vs.get(ftnInd).getB();
            FValue arg;
            MathItem argE = vs.get(ftnInd+1).getA();
            for (Pair<MathItem,FValue> pair : IterUtil.skipFirst(vs)) {
                // 2. If some function element is immediately followed by
                // an expression element then, find the first such function
                // element, and call the next element the argument.
                argE = pair.getA();
                if (isFunction(ftn) && isExpr(argE)) {
                    arg = pair.getB();
                    // It is a static error if either the argument is not
                    // parenthesized, or the argument is immediately followed by
                    // a non-expression element.
                    if (!isParenthesisDelimited(argE) ||
                        (ftnInd+2 < vs.size() &&
                         !isExpr(vs.get(ftnInd+2).getA())))
                       return error(((ExprMI)argE).getExpr(),
                                    "It is a static error if either the " +
                                    "argument is not parenthesized, or the " +
                                    "argument is immediately followed by a " +
                                    "non-expression element.");
                    // Otherwise, replace the function and argument with a
                    // single element that is the application of the function to
                    // the argument.  This new element is an expression.
                    Pair<MathItem,FValue> app =
                        new Pair(dummyExpr(),
                                 functionInvocation(arg,(Fcn)ftn,argE));
                    vs.set(ftnInd, app);
                    vs.remove(ftnInd+1);
                    // It is a static error if a function argument is immediately
                    // followed by a postfix operator.  For example, y[a](x)!
                    if (isPostfix && vs.size() == 2 &&
                        isFunction(vs.get(0).getB()) && isExpr(vs.get(1).getA()))
                        return error(((ExprMI)vs.get(1).getA()).getExpr(),
                                     "It is a static error if a " +
                                     "function argument is immediately " +
                                     "followed by a postfix operator.");
                    // Reassociate the resulting sequence (which is one element
                    // shorter).
                    return reassociate(vs, isPostfix);
                }
                ftn = pair.getB();
                ftnInd++;
            }
            return vs;
        }
    }

    private FValue mathItemApplication(NonExprMI opr, FValue front,
                                       MathItem loc) {
        if (opr instanceof ExponentiationMI) {
            ExponentiationMI expo = (ExponentiationMI)opr;
            Op op = expo.getOp();
            FValue fvalue = op.accept(this);
            if (!isFunction(fvalue))
                return error(op, errorMsg("Operator ", op.stringName(),
                                          " has a non-function value ", fvalue));
            Option<Expr> expr = expo.getExpr();
            Fcn fcn = (Fcn)fvalue;
            if (expr.isSome()) { // ^ Exponent
                FValue exponent = Option.unwrap(expr).accept(this);
                return functionInvocation(Useful.list(front, exponent), fcn, op);
            } else { // ExponentOp
                return functionInvocation(Useful.list(front), fcn, op);
            }
        } else { // opr instanceof SubscriptingMI
            SubscriptingMI sub = (SubscriptingMI)opr;
            Enclosing op = sub.getOp();
            List<Expr> subs = sub.getExprs();
            if (!(front instanceof FObject))
                error(loc, errorMsg("Value should be an object; got ", front));
            FObject array = (FObject)front;
            String opString = NodeUtil.nameString(op);
            FValue ixing = array.getSelfEnv().getValueNull(opString);
            if (ixing == null || !(ixing instanceof Method))
                error(loc, errorMsg("Could not find appropriate definition of ",
                                    "the operator ", opString, " on ", array));
            return ((Method)ixing).applyMethod(evalExprListParallel(subs), array,
                                               loc, e);
        }
    }

    private List<Pair<MathItem,FValue>> stepThree(List<Pair<MathItem,FValue>> vs,
                                                  boolean isPostfix) {
        if (vs.size() < 1) return error("Reassociation of MathPrimary failed!");
        else if (vs.size() == 1) return vs;
        else { // vs.size() > 1
            int frontInd = 0;
            FValue front = vs.get(frontInd).getB();
            MathItem opr;
            for (Pair<MathItem,FValue> pair : IterUtil.skipFirst(vs)) {
                // 3. If there is any non-expression element (it cannot be the
                // first element)
                opr = pair.getA();
                if (!isExpr(opr)) {
                    // then replace the first such element and the element
                    // immediately preceding it (which must be an expression)
                    // with a single element that does the appropriate operator
                    // application.  This new element is an expression.
                    Pair<MathItem,FValue> app =
                        new Pair(dummyExpr(),
                                 mathItemApplication((NonExprMI)opr,front,opr));
                    vs.set(frontInd, app);
                    vs.remove(frontInd+1);
                    // Reassociate the resulting sequence (which is one element
                    // shorter).
                    return reassociate(vs, isPostfix);
                }
                front = pair.getB();
                frontInd++;
            }
            return vs;
        }
    }

    private List<Pair<MathItem,FValue>> reassociate(List<Pair<MathItem,FValue>> vs, boolean isPostfix) {
        if (vs.size() == 1) return vs;
        return stepThree(stepTwo(vs, isPostfix), isPostfix);
    }

    private FValue tightJuxt(FValue first, FValue second, MathItem loc) {
        if (isFunction(first)) return functionInvocation(second,(Fcn)first,loc);
        else return functionInvocation(Useful.list(first, second),
                                       e.getValue("juxtaposition"), loc);
    }

    private FValue leftAssociate(List<Pair<MathItem,FValue>> vs) {
        // vs.size() > 1
        FValue result = vs.get(0).getB();
        for (Pair<MathItem,FValue> i : IterUtil.skipFirst(vs)) {
            result = tightJuxt(result, i.getB(), i.getA());
        }
        return result;
    }

    public FValue forMathPrimary(MathPrimary x) {
        return forMathPrimary(x, false);
    }

    private FValue forMathPrimary(MathPrimary x, boolean isPostfix) {
        Expr front = x.getFront();
        FValue fval = front.accept(this);
        List<MathItem> rest = x.getRest();
        if (!rest.isEmpty()) {
            List<Pair<MathItem,FValue>> vs =
                Useful.list(new Pair<MathItem,FValue>(null, fval));
            // It is a static error if an exponentiation is immediately followed
            // by a non-expression element.
            boolean isExponentiation = false;
            for (MathItem mi : rest) {
                if (mi instanceof ExprMI)
                    vs.add(new Pair(mi, ((ExprMI)mi).getExpr().accept(this)));
                else { // mi instanceof NonExprMI
                    if (isExponentiation)
                       return error(x, "It is a static error if an " +
                                    "exponentiation is immediately followed " +
                                    "by a subscripting or an exponentiation.");
                    vs.add(new Pair(mi, null));
                    isExponentiation = (mi instanceof ExponentiationMI);
                }
            }
            if (isPostfix && isExponentiation) {
                return error(x, "It is a static error if an exponentiation is " +
                             "immediately followed by a postfix operator.");
            }
            if (vs.size() == 1) fval = vs.get(0).getB();
            else
                // vs.size() > 1
                // 4. Otherwise, left-associate the sequence, which has only
                // expression elements, only the last of which may be a function.
                fval = leftAssociate(reassociate(vs, isPostfix));
        }
        return fval;
    }

    Name fldName(AbstractFieldRef arf) {
        if (arf instanceof FieldRef) {
            return ((FieldRef)arf).getField();
        }
        if (arf instanceof FieldRefForSure) {
            return ((FieldRefForSure)arf).getField();

        }
        if (arf instanceof _RewriteFieldRef) {
            return ((_RewriteFieldRef)arf).getField();

        }
        return bug("Unexpected AbstractFieldRef " + arf);

    }

    /** Assumes wrapped FnRefs have ids fields of length 1. */
    public FValue forTightJuxt(TightJuxt x) {
        return forTightJuxt(x, false);
    }

    private FValue forTightJuxt(TightJuxt x, boolean isPostfix) {
        List<Expr> exprs = x.getExprs();
        if (exprs.size() == 0)
            bug(x,e,"empty juxtaposition");
        Expr fcnExpr = exprs.get(0);

        if (fcnExpr instanceof FieldRef) {
            // In this case, open code the FieldRef evaluation
            // so that the object can be preserved. Alternate
            // strategy might be to generate a closure from
            // the field selection.
            FieldRef fld_sel = (FieldRef) fcnExpr;
            Expr obj = fld_sel.getObj();
            Id fld = fld_sel.getField();
            FValue fobj = obj.accept(this);
            return juxtMemberSelection(x, fobj, fld, exprs);

        } else if (fcnExpr instanceof _RewriteFieldRef) {
            // In this case, open code the FieldRef evaluation
            // so that the object can be preserved. Alternate
            // strategy might be to generate a closure from
            // the field selection.
            _RewriteFieldRef fld_sel = (_RewriteFieldRef) fcnExpr;
            Expr obj = fld_sel.getObj();
            Name fld = fld_sel.getField();
            if (fld instanceof Id) {
                FValue fobj = obj.accept(this);
                return juxtMemberSelection(x, fobj, (Id) fld, exprs);
            } else {
                NI.nyi("Field selector of dotted");
            }
        } else if (fcnExpr instanceof _RewriteFnRef) {
            // Only come here if there are static args -- must be generic
            // Note that ALL method references have been rewritten into
            // this.that form, so that bare var-ref is a function
            _RewriteFnRef rfr = (_RewriteFnRef) fcnExpr;
            Expr fn = rfr.getFn();
            List<StaticArg> args = rfr.getStaticArgs();
            if (fn instanceof AbstractFieldRef) {
                AbstractFieldRef arf = (AbstractFieldRef) fn;
                FValue fobj = arf.getObj().accept(this);
                if (fobj instanceof FObject) {
                    try {
                        return invokeGenericMethod((FObject) fobj, fldName(arf), args, exprs, x);
                    } catch (FortressError ex) {
                        throw ex.setContext(x,e);
                    }
                } else {
                    error(x,errorMsg("Non-Object target ",fobj," of method call ",x));
                }
            } else if (fn instanceof VarRef) {
                // FALL OUT TO REGULAR FUNCTION CASE!
            } else {
                return bug(x,"_RewriteFnRef with unexpected fn " + fn);
            }
        } else if (fcnExpr instanceof FnRef) {
            return bug(fcnExpr,"FnRefs are supposed to be gone from the AST \n" + x.toStringVerbose() );
        }

        FValue fnVal = fcnExpr.accept(this);
        if (fnVal instanceof MethodClosure) {
            return bug(x,"Unexpected application of " + fcnExpr);
        } else if (fnVal instanceof Fcn) {
            if (isPostfix) {
                // It is a static error if a function argument is immediately
                // followed by a non-expression element.  For example, f(x)!
                return error(x, "It is a static error if a function argument " +
                             "is immediately followed by a non-expression " +
                             "element.");
            } else {
                return finishFunctionInvocation(exprs, fnVal, x);
            }
        } else {
            // When a tight juxtaposition is not a function application,
            // fake it as a loose juxtaposition.  It's clearly a hack.
            // Please fix it if you know how to do it.  -- Sukyoung
            return forLooseJuxt(new LooseJuxt(x.getSpan(), x.getExprs()));
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
            FValue cl = fobject.getSelfEnv().getValueNull(NodeUtil.nameString(fld));
            if (cl == null)
                // TODO Environment is split, might not be best choice for error
                // printing.
                return error(x, fobject.getSelfEnv(),
                             errorMsg("undefined method/field ",
                                      NodeUtil.nameString(fld)));
            else if (cl instanceof Method) {
                return ((Method) cl).applyMethod(evalInvocationArgs(exprs),
                        fobject, x, e);
            } else if (cl instanceof Fcn) {
                Fcn fcl = (Fcn) cl;
                // Ordinary closure, assigned to a field.
                return finishFunctionInvocation(exprs, fcl, x);
            } else {
                // TODO seems like we could be multiplying, too.
                String msg = errorMsg("Tight juxtaposition of non-function ",
                                      NodeUtil.nameString(fld));
                return error(x, fobject.getSelfEnv(), msg);
            }
        } else {
            // TODO Could be a fragment of a component/api name, too.
            return error(x,
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
        Evaluator ev = new Evaluator(this, x);
        Block body = x.getBody();
        FValue res = FVoid.V;
        try {
            res = body.accept(this);
        } catch (FortressException exc) {
            Option<Catch> _catchClause = x.getCatchClause();
            FType excType = exc.getException().type();
            if (_catchClause.isSome()) {
                Catch _catch = Option.unwrap(_catchClause);
                Id name = _catch.getName();
                List<CatchClause> clauses = _catch.getClauses();
                e.putValue(name.getText(), exc.getException());

                for (CatchClause clause : clauses) {
                    TraitType match = clause.getMatch();
                    Block catchBody = clause.getBody();
                    FType foo = EvalType.getFType(match, e);
                    if (excType.subtypeOf(foo)) {
                        res = catchBody.accept(this);
                        return res;
                    }
                }
            }
            List<TraitType> forbid = x.getForbid();
            for (TraitType forbidType : forbid) {
                if (excType.subtypeOf(EvalType.getFType(forbidType,e))) {
                  FType ftype = e.getTypeNull(WellKnownNames.forbiddenException);
                  List<FValue> args = new ArrayList<FValue>();
                  args.add(exc.getException());
                  Constructor c = (Constructor) e.getValue(WellKnownNames.forbiddenException);
                  // Can we get a better HasAt?
                  HasAt at = new HasAt.FromString(WellKnownNames.forbiddenException);
                  FObject f = (FObject) c.apply(args, at, e);
                  FortressException f_exc = new FortressException(f);
                  throw f_exc;
                }
            }
        } finally {
            Option<Block> finallyClause = x.getFinallyClause();
            if (finallyClause.isSome()) {
                Block b = Option.unwrap(finallyClause);
                b.accept(this);
            }
        }
        return res;
    }

    public FValue forArgExpr(ArgExpr x) {
        List<Expr> exprs = x.getExprs();
        /*
        if (!isArgExpr)
            error(x, "Tuples are not allowed to have varargs or keyword expressions.");
        */
        return FTuple.make(evalExprListParallel(exprs));
    }

    public FValue forTupleExpr(TupleExpr x) {
        List<Expr> exprs = x.getExprs();
        return FTuple.make(evalExprListParallel(exprs));
    }

    public FValue forTypecase(Typecase x) {
        Evaluator ev = new Evaluator(this, x);
        List<FType> res = evalTypeCaseBinding(ev, x);
        FValue result = FVoid.V;
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
        if (el.isSome()) {
            Block elseClauses = Option.unwrap(el);
            result = evalExprList(elseClauses.getExprs(), elseClauses, ev);
            return result;
        } else {
            throw new MatchFailure();
        }
    }

    public FValue forUnpastingBind(UnpastingBind x) {
        return NI("forUnpastingBind");
    }

    public FValue forUnpastingSplit(UnpastingSplit x) {
        return NI("forUnpastingSplit");
    }

    public FValue forVarRef(VarRef x) {
        Iterable<Id> names = NodeUtil.getIds(x.getVar());

        if (!ProjectProperties.noStaticAnalysis) {
            FValue res = e.getValueNull(IterUtil.last(names).getText());
            if (res == null)
                error(x, e, errorMsg("undefined variable ", IterUtil
                        .last(names).getText()));
            return res;

        } else {

            FValue res = e.getValueNull(IterUtil.first(names).getText());
            if (res == null)
                error(x, e, errorMsg("undefined variable ", IterUtil.first(
                        names).getText()));

            for (Id fld : IterUtil.skipFirst(names)) {
                if (res instanceof Selectable) {
                    /*
                     * Selectable was introduced to make it not necessary to
                     * know whether a.b was field b of object a, or member b of
                     * api a (or api name prefix, extended).
                     */
                    // TODO Need to distinguish between public/private
                    // methods/fields
                    try {
                        res = ((Selectable) res).select(fld.getText());
                    } catch (FortressError ex) {
                        throw ex.setContext(x, e);
                    }
                } else {
                    res = error(x, e, errorMsg("Non-object cannot have field ",
                            fld.getText()));
                }
            }
            return res;
        }
    }

    public FValue forVoidLiteralExpr(VoidLiteralExpr x) {
        return FVoid.V;
    }

    public FValue forWhile(While x) {
        Expr body = x.getBody();
        Expr test = x.getTest();
        FBool res = (FBool) test.accept(this);
        while (res.getBool() != false) {
            body.accept(this);
            res = (FBool) test.accept(this);
        }
        return FVoid.V;
    }

    public FValue forThrow(Throw throw1) {
        Expr ex = throw1.getExpr();
        FObject v = (FObject) ex.accept(this);
        FortressException f_exc = new FortressException(v);
        throw f_exc;
        // neverReached
    }

    public FValue forCharLiteralExpr(CharLiteralExpr x) {
        return FChar.make(x.getVal());
    }

    public FValue forFloatLiteralExpr(FloatLiteralExpr x) {
        return new FFloatLiteral(x.getText());
    }

    public FValue forIntLiteralExpr(IntLiteralExpr x) {
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
        QualifiedIdName name = x.getFns().get(0);
        FValue g = forVarRef(new VarRef(name.getSpan(), name));
        List<StaticArg> args = x.getStaticArgs();
        if (g instanceof FGenericFunction) {
            return ((FGenericFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericConstructor) {
            return ((GenericConstructor) g).typeApply(args, e, x);
        } else if (g instanceof OverloadedFunction) {
            return((OverloadedFunction) g).typeApply(args, e, x);
        } else {
            return error(x, e, errorMsg("Unexpected FnRef value, ",g));
        }
    }

    @Override
    public FValue for_RewriteFnRef(_RewriteFnRef x) {
        Expr name = x.getFn();
        FValue g = name.accept(this);
        List<StaticArg> args = x.getStaticArgs();
        if (args.size() == 0)
            return g;
        if (g instanceof FGenericFunction) {
            return ((FGenericFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericConstructor) {
            return ((GenericConstructor) g).typeApply(args, e, x);
        } else if (g instanceof OverloadedFunction) {
            return((OverloadedFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericSingleton) {
            return ((GenericSingleton) g).typeApply(args, e, x);
        } else {
            return error(x, e, errorMsg("Unexpected _RewriteFnRef value, ",g));
        }
    }

    public FValue for_WrappedFValue(_WrappedFValue w) {
        return w.getFValue();
    }
}
