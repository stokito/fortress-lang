/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.*;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.TupleTask;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;

public class Evaluator extends EvaluatorBase<FValue> {
    boolean debug = false;

    public FValue eval(Expr e) {
        return e.accept(this);
    }

    /**
     * Creates a new evaluator in a primitive environment.
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
        this(ev.e.extendAt(within));
    }

    /**
     * Creates a new com.sun.fortress.interpreter.evaluator in the specified environment.
     */
    public Evaluator(Environment e) {
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
        return bug(n, this.getClass().getName() + "." + s + " not implemented, input \n" + NodeUtil.dump(n));
    }

    public FValue defaultCase(Node n) {
        return bug(n, errorMsg("Cannot evaluate the node ", n, " of type ", n.getClass()));
    }

    public FValue forAsExpr(AsExpr x) {
        final Expr expr = x.getExpr();
        FValue val = expr.accept(this);
        Type ty = x.getAnnType();
        FType fty = EvalType.getFType(ty, e);
        if (val.type().subtypeOf(fty)) return val;
        else return error(x, e, errorMsg("The type of expression ", val.type(), " is not a subtype of ", fty, "."));
    }

    public FValue forAsIfExpr(AsIfExpr x) {
        final Expr expr = x.getExpr();
        FValue val = expr.accept(this);
        Type ty = x.getAnnType();
        FType fty = EvalType.getFType(ty, e);
        if (val.type().subtypeOf(fty)) return new FAsIf(val, fty);
        else return error(x, e, errorMsg("Type of expression, ", val.type(), ", not a subtype of ", fty, "."));
    }

    // We ask lhs to accept twice (with this and an LHSEvaluator) in
    // the operator case. Might this cause the world to break?

    public FValue forAssignment(Assignment x) {
        Option<FunctionalRef> possOp = x.getAssignOp();
        LHSToLValue getLValue = new LHSToLValue(this);
        List<? extends Lhs> lhses = getLValue.inParallel(x.getLhs());
        int lhsSize = lhses.size();
        FValue rhs = x.getRhs().accept(this);

        if (possOp.isSome()) {
            // We created an lvalue for lhses above, so there should
            // be no fear of duplicate evaluation.
            FunctionalRef opr_ = possOp.unwrap();
            //Op opr = opr_.getOriginalName();
            Fcn fcn = (Fcn) getOp(opr_);// opr.accept(this);
            FValue lhsValue;
            if (lhsSize > 1) {
                // TODO:  An LHS walks, talks, and barks just like
                // an Expr in this context.  Yet it isn't an Expr, and
                // we can't pass the lhses to the numerous functions
                // which expect a List<Expr>---for example,
                // TupleExpr with a varargs or keywords
                // or evalExprListParallel!  This is extremely annoying!
                List<FValue> lhsComps = new ArrayList<FValue>(lhsSize);
                for (Lhs lhs : lhses) {
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
            rhs = fcn.functionInvocation(vargs, x);
        }

        // Assignment must be single-anything, or tuple-tuple with
        // matched sizes.
        Iterator<FValue> rhsIt = null;
        if (lhsSize > 1 && rhs instanceof FTuple) {
            FTuple rhs_tuple = (FTuple) rhs;
            rhsIt = rhs_tuple.getVals().iterator();
            // Verify, now, that LHS and RHS sizes match.
            if (rhs_tuple.getVals().size() != lhsSize) {
                error(x, e, errorMsg("Tuple assignment size mismatch, | lhs | = ",
                                     lhsSize,
                                     ", | rhs | = ",
                                     rhs_tuple.getVals().size()));
            }
        } else if (lhsSize != 1) {
            error(x, e, errorMsg("Tuple assignment size mismatch, | lhs | = ", lhsSize, ", rhs is not a tuple"));
        }

        for (Lhs lhs : lhses) {
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
        FValue res = FVoid.V;
        try {
            res = FortressTaskRunner.doIt(new Callable<FValue>() {
                public FValue call() {
                    Evaluator ev = new Evaluator(current.e.extendAt(expr));
                    return expr.accept(ev);
                }
            });
        }
        catch (RuntimeException re) {
            throw re;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    public FValue forTryAtomicExpr(TryAtomicExpr x) {
        final Expr expr = x.getExpr();
        final Evaluator current = new Evaluator(this);
        FValue res = FVoid.V;
        // Inside a transaction tryAtomic is a noop
        // Why is this?  It seems inconsistent with the rest of our transaction story.  chf
        if (FortressTaskRunner.inATransaction()) {
            Evaluator ev = new Evaluator(current.e.extendAt(expr));
            return expr.accept(ev);
        }
        try {
            res = FortressTaskRunner.doItOnce(new Callable<FValue>() {
                public FValue call() {
                    Evaluator ev = new Evaluator(current.e.extendAt(expr));
                    FValue res1 = expr.accept(ev);
                    return res1;
                }
            });
        }
        catch (AbortedException ae) {
            FObject f = (FObject) Driver.getFortressLibrary().getRootValue(WellKnownNames.tryatomicFailureException);
            FortressError f_exc = new FortressError(x, e, f);
            throw f_exc;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    public FValue forKeywordExpr(KeywordExpr x) {
        return NI("forKeywordExpr");
    }

    public FValue forDo(Do x) {
        FValue res;
        int s = x.getFronts().size();
        //      System.out.println("forDo with s = " + s + " x = " + x);

        if (s == 0) return FVoid.V;
        if (s == 1) {
            Block f = x.getFronts().get(0);
            if (f.getLoc().isSome()) {
                Expr regionExp = f.getLoc().unwrap();
                regionExp.accept(this);
            }
            if (f.isAtomicBlock()) {
                res = forAtomicExpr(ExprFactory.makeAtomicExpr(NodeUtil.getSpan(x), f));
            } else {
                res = f.accept(new Evaluator(this));
            }
            return res;
        }

        List<TupleTask> tasks = new ArrayList<TupleTask>();

        for (int i = 0; i < s; i++) {
            Block f = x.getFronts().get(i);

            if (f.getLoc().isSome()) {
                Expr regionExp = f.getLoc().unwrap();
                regionExp.accept(this);
            }
            if (f.isAtomicBlock()) tasks.add(new TupleTask(ExprFactory.makeAtomicExpr(NodeUtil.getSpan(x), f), this));
            else {
                tasks.add(new TupleTask(f, new Evaluator(this)));
            }
        }

        BaseTask currentTask = FortressTaskRunner.getTask();
        TupleTask.invokeAll(tasks);
        FortressTaskRunner.setCurrentTask(currentTask);

        for (TupleTask t : tasks) {
            FValue ignore = t.getResOrException();
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

    /**
     * Returns the evaluation of a list of (general) exprs, returning the
     * result of evaluating the last expr in the list.
     * Does the "right thing" with LetExprs.
     */
    public FValue evalExprList(List<Expr> exprs, AbstractNode tag) {
        FValue res = FVoid.V;
        for (Expr exp : exprs) {
            // TODO This will get turned into forLet methods
            if (exp instanceof LetExpr) {
                Environment inner = this.e.extendAt(exp);
                BuildLetEnvironments be = new BuildLetEnvironments(inner);
                res = be.doLets((LetExpr) exp);
            } else {
                res = exp.accept(this);
            }
        }
        return res;
    }

    <T extends Expr> List<FValue> evalExprListParallel(List<T> exprs) {
        // Added some special-case code to avoid explosion of TupleTasks.
        int sz = exprs.size();
        ArrayList<FValue> resList = new ArrayList<FValue>(sz);
        ArrayList<TupleTask> TupleTasks = new ArrayList<TupleTask>(sz);
        if (sz < 2 || !TupleTask.worthSpawning()) {
            evalExprList(exprs, resList);
        } else {
            for (Expr expr : exprs)
            // We want to add things to the front of the list.
            {
                TupleTasks.add(0, new TupleTask(expr, this));
            }

            BaseTask currentTask = FortressTaskRunner.getTask();
            TupleTask.invokeAll(TupleTasks);
            FortressTaskRunner.setCurrentTask(currentTask);

            for (TupleTask task : TupleTasks) {
                resList.add(0, task.getResOrException());
            }
        }

        if (resList.size() != exprs.size()) bug(
                "We should have the same number of results as we did exprs resList = " + resList + " exprs = " + exprs);
        return resList;
    }

    CaseClause findExtremum(CaseExpr x, Fcn fcn) {
        List<CaseClause> clauses = x.getClauses();
        Iterator<CaseClause> i = clauses.iterator();

        CaseClause c = i.next();
        FValue winner = c.getMatchClause().accept(this);
        CaseClause res = c;

        while (i.hasNext()) {
            c = i.next();
            List<FValue> vargs = new ArrayList<FValue>(2);
            FValue current = c.getMatchClause().accept(this);
            vargs.add(current);
            vargs.add(winner);
            FValue invoke = fcn.functionInvocation(vargs, x);
            if (!(invoke instanceof FBool)) {
                return error(x, errorMsg("Non-boolean result ", invoke, " in chain, args ", vargs));
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
            Option<FunctionalRef> compare = x.getCompare();
            if (compare.isSome()) {
                FValue op = getOp(compare.unwrap());
                return forBlock(findExtremum(x, (Fcn) op).getBody());
            } else return error(x, errorMsg("Missing operator for the extremum ", "expression ", x));
        } else {
            // Evaluate the parameter
            FValue paramValue = param.unwrap().accept(this);
            // Assign a comparison function
            Fcn fcn = (Fcn) e.getValue(x.getEqualsOp());
            Option<FunctionalRef> compare = x.getCompare();
            if (compare.isSome()) fcn = (Fcn) getOp(compare.unwrap());

            // Iterate through the cases
            for (CaseClause c : clauses) {
                // Evaluate the clause
                FValue match = c.getMatchClause().accept(this);
                List<FValue> vargs = new ArrayList<FValue>();
                vargs.add(paramValue);
                vargs.add(match);
                if (Glue.extendsGenericTrait(match.type(), WellKnownNames.containsTypeName)) {
                    fcn = (Fcn) Driver.getFortressLibrary().getRootValue(WellKnownNames.containsMatchName);
                }
                FBool success = (FBool) fcn.functionInvocation(vargs, c);
                if (success.getBool()) return forBlock(c.getBody());
            }
            Option<Block> _else = x.getElseClause();
            if (_else.isSome()) {
                // TODO need an Else node to hang a location on
                return forBlock(_else.unwrap());
            }
            FObject f = (FObject) Driver.getFortressLibrary().getRootValue(WellKnownNames.matchFailureException);
            FortressError f_exc = new FortressError(x, e, f);
            throw f_exc;
        }
    }

    /*
     * Only handles 2-ary definitions of chained operators. That should be
     * perfectly wonderful for the moment.
     */
    public FValue forChainExpr(ChainExpr x) {
        Expr first = x.getFirst();
        List<Link> links = x.getLinks();
        FValue idVal = first.accept(this);
        Iterator<Link> i = links.iterator();
        List<FValue> vargs = new ArrayList<FValue>(2);
        if (links.size() == 1) {
            Link link = i.next();
            Fcn fcn = (Fcn) getOp(link.getOp()); // link.getOp().getOriginalName().accept(this);
            FValue exprVal = link.getExpr().accept(this);
            vargs.add(idVal);
            vargs.add(exprVal);
            return fcn.functionInvocation(vargs, x);
        } else {
            FBool boolres = FBool.TRUE;
            vargs.add(idVal);
            vargs.add(idVal);
            while (boolres.getBool() && i.hasNext()) {
                Link link = i.next();
                Fcn fcn = (Fcn) getOp(link.getOp()); // link.getOp().getOriginalName().accept(this);
                FValue exprVal = link.getExpr().accept(this);
                vargs.set(0, idVal);
                vargs.set(1, exprVal);
                FValue invoke = fcn.functionInvocation(vargs, x);
                if (!(invoke instanceof FBool)) {
                    return error(x, errorMsg("Non-boolean result ", invoke, " in chain, args ", vargs));
                }
                boolres = (FBool) invoke;
                idVal = exprVal;
            }
            return boolres;
        }
    }

    private boolean isShadow(Expr expr, String name) {
        return (expr instanceof VarRef && NodeUtil.nameString(((VarRef) expr).getVarId()).equals(name));
    }

    private boolean isShadow(Option<String> var, String name) {
        if (var.isSome()) {
            return var.unwrap().equals(name);
        } else { // var.isNone()
            return false;
        }
    }

    private Option<String> getName(Expr expr) {
        if (expr instanceof VarRef) {
            return Option.some(NodeUtil.nameString(((VarRef) expr).getVarId()));
        } else { // !(expr instanceof VarRef)
            return Option.<String>none();
        }
    }

    public FValue forAPIName(APIName x) {
        String result = "";
        for (Iterator<Id> i = x.getIds().iterator(); i.hasNext();) {
            result = result.concat(i.next().getText());
            if (i.hasNext()) result = result.concat(".");
        }
        return FString.make(result);
    }

    public FValue forExit(Exit x) {
        Option<Id> target = x.getTarget();
        Option<Expr> returnExpr = x.getReturnExpr();
        FValue res;
        LabelException _e;

        if (returnExpr.isSome()) {
            res = returnExpr.unwrap().accept(new Evaluator(this));
        } else {
            res = FVoid.V;
        }

        if (target.isSome()) {
            String t = target.unwrap().getText();
            _e = new NamedLabelException(x, t, res);
        } else {
            _e = new LabelException(x, res);
        }
        throw _e;
    }

    public FValue forExtentRange(ExtentRange x) {
        return NI("forExtentRange");
    }

    @Override
    public FValue forFieldRef(FieldRef x) {
        return forFieldRefCommon(x, x.getField());
    }

    private FValue forFieldRefCommon(FieldRef x, Name fld) throws FortressException, ProgramError {
        String fname = NodeUtil.nameString(fld);
        Expr obj = x.getObj();
        FValue fobj = obj.accept(this);
        FValue gv = fobj.getValue();
        try {
            if (gv instanceof Selectable) {
                FValue res = ((Selectable) gv).select(fname);
                if (!(res instanceof Method)) return res;
                // If it's a method, fall through and make dotted method app.
            } else {
                return error(errorMsg("Non-object ", fobj, " cannot have field ", NodeUtil.nameString(fld)));
            }
            return DottedMethodApplication.make(fobj, fname, fname);
        }
        catch (FortressException ex) {
            throw ex.setContext(x, e);
        }
    }

    public FValue forMethodInvocation(MethodInvocation x) {
        Expr obj = x.getObj();
        FValue self = obj.accept(this);
        Expr arg = x.getArg();
        List<FValue> args = argList(arg.accept(this));

        IdOrOp method = x.getMethod();
        String mname = NodeUtil.nameString(method);
        List<StaticArg> sargs = x.getStaticArgs();
        if (sargs.isEmpty()) {
            return invokeMethod(self, mname, args, x);
        } else {
            return invokeGenericMethod(self, mname, sargs, args, x);
        }
    }

    public FValue forFnExpr(FnExpr x) {
        //Option<Type> return_type = NodeUtil.getReturnType(x);
        //List<Param> params = NodeUtil.getParams(x);
        FunctionClosure cl = new FunctionClosure(e, x); // , return_type, params);
        cl.finishInitializing();
        return cl;
    }

    public FValue forGeneratorClause(GeneratorClause x) {
        return bug(x, errorMsg("Generator clause " + x + " remains after desugaring"));
    }

    public FValue forId(Id x) {
        return FString.make(x.getText());
    }

    public FValue forIf(If x) {
        List<IfClause> clauses = x.getClauses();
        for (IfClause ifclause : clauses) {
            GeneratorClause cond = ifclause.getTestClause();
            Expr testExpr;
            if (cond.getBind().isEmpty()) testExpr = cond.getInit();
            else testExpr = bug(ifclause, "Undesugared generalized if expression.");
            FValue clauseVal = testExpr.accept(this);
            if (clauseVal instanceof FBool) {
                if (((FBool) clauseVal).getBool()) {
                    return ifclause.getBody().accept(this);
                }
            } else {
                if (clauseVal.type() instanceof FTraitOrObject) {
                    if (((FTraitOrObject) clauseVal.type()).getName().equals("Just")) return ifclause.getBody().accept(
                            this);
                } else {
                    return error(ifclause, errorMsg("If clause did not return boolean, " + "but ", clauseVal));
                }
            }
        }
        Option<Block> else_ = x.getElseClause();
        if (else_.isSome()) {
            return else_.unwrap().accept(this);
        } else {
            return FVoid.V;
        }
    }

    public FValue forLValue(LValue x) {
        Id name = x.getName();
        return FString.make(NodeUtil.nameString(name));
    }

    public FValue forLabel(Label x) {
        Id name = x.getName();
        Block body = x.getBody();
        FValue res = FVoid.V;
        try {
            Evaluator ev = new Evaluator(e.extendAt(body));
            res = ev.forBlock(body);
        }
        catch (NamedLabelException e) {
            if (e.match(name.getText())) {
                return e.res();
            } else throw e;
        }
        catch (LabelException e) {
            return e.res();
        }
        return res;
    }

    private FValue juxtApplyStack(Stack<FValue> fns, FValue times, AbstractNode loc) {
        FValue tos = fns.pop();
        while (!fns.empty()) {
            FValue f = fns.pop();

            if (f instanceof Fcn) {
                tos = ((Fcn) f).functionInvocation(argList(tos), loc);
            } else {
                tos = Fcn.functionInvocation(Useful.list(f, tos), times, loc);
            }
        }
        return tos;
    }

    public FValue forJuxt(Juxt x) {
        if (x.isTight()) {
            /** Assumes wrapped FnRefs have ids fields of length 1. */
            return forTightJuxt(x, false);
        } else return forJuxtCommon(x);
    }

    public FValue forJuxtCommon(Juxt x) {
        // This is correct except for one minor detail:
        // We should treat names from another scope as if they were functions.
        // Right now we evaluate them and make the function/non-function
        // distinction based on the resulting getText.
        // We do not handle functional methods, and overlap therewith, at all.
        List<Expr> exprs = x.getExprs();
        FValue times;
        if (exprs.size() == 0) bug(x, "empty juxtaposition");
        try {
            // times = e.getValue("juxtaposition");
            times = e.getValueNull(x.getInfixJuxt());
        }
        catch (FortressException fe) {
            throw fe.setContext(x, e);
        }
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
                r = Fcn.functionInvocation(Useful.list(r, val), times, x);
                stack.push(r);
            }
        }
        return juxtApplyStack(stack, times, x);
    }

    @Override
    public FValue for_RewriteFnApp(_RewriteFnApp that) {
        // This method was written by NEB, so there is plenty of reason to believe
        // that I did it incorrectly.

        Expr fn = that.getFunction();
        Expr arg = that.getArgument();

        List<FValue> evaled = evalExprListParallel(Useful.list(fn, arg));

        FValue fn_ = evaled.get(0);
        FValue arg_ = evaled.get(1);

        return Fcn.functionInvocation(arg_, fn_, that);
    }

    public FValue forArrayElement(ArrayElement x) {
        // MDEs occur only within ArrayElements, and reset
        // row evaluation to an outercontext (in the scope
        // of the element, that is).
        return bug(x, "Singleton paste?  Can't judge dimensionality without type inference.");
        // Evaluator notInPaste = new Evaluator(this);
        // return x.getElement().accept(notInPaste);
    }

    /**
     * Evaluates a pasting, outermost.
     * <p/>
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

    public FValue for_RewriteObjectExprRef(_RewriteObjectExprRef x) {
        String s = x.getGenSymName();
        // FType ft = e.getType(s);
        // System.out.println("for_RewriteObjectExpr "+s);
        FValue v = e.getTopLevel().getRootValue(s);

        if (v instanceof GenericConstructor) {
            GenericConstructor gc = (GenericConstructor) v;
            List<StaticArg> args = x.getStaticArgs();
            Constructor cl = (Constructor) gc.typeApply(args, e, x);
            return cl.applyOEConstructor(x, e);
        } else if (v instanceof Constructor) {
            Constructor cl = (Constructor) v;
            // FTypeObject fto = (FTypeObject) ft;
            // cl.setParams(Collections.<Parameter> emptyList());
            // BuildEnvironments.finishObjectTrait(x.getTraits(), fto, e);
            // cl.finishInitializing();
            return cl.applyOEConstructor(x, e);
        } else {
            return bug(x, e, errorMsg("_RewriteObjectExpr ", s, " has 'constructor' ", v));
        }

        // Option<List<Type>> traits = x.getTraits();
        // List<Decl> defs = x.getDefs();
        // FTypeObject fto = new FTypeObject(genSym(x), e);
        // return BuildEnvironments.anObject(fto, e, traits, defs, x);

    }

    private FValue getOp(FunctionalRef op) {
        try {
            if (op.getLexicalDepth() != Environment.TOP_LEVEL) {
                bug("Expect all oprefs to be top level " + op);
            }
            if (op.getNames().size() != 1) bug(
                    "Should have rewritten ops to length 1, op=" + op + ", list =" + op.getNames());
            IdOrOp name = op.getNames().get(0);
            if (!(name instanceof Op)) bug("The name field of OpRef should be Op.");
            FValue v = e.getValue((Op) name, Environment.TOP_LEVEL);
            return v;
            //return e.getValue(op);
        }
        catch (FortressException ex) {
            throw ex.setContext(op, e);
        }
    }

    //    public FValue forEnclosing(Enclosing x) {
    //        return getOp(x);
    //    }
    //
    //    public FValue forOp(Op op) {
    //        return getOp(op);
    //    }

    private boolean isExponentiation(OpExpr expr) {
        FunctionalRef ref = expr.getOp();

        IdOrOp name = ref.getOriginalName();
        if (!(name instanceof Op)) return false;
        else return (((Op) name).getText().equals("^") || OprUtil.isPostfix((Op) name));
    }


    // This method is not necessary in the long run. These nodes will
    // be removed by the static end. For now, since we are not
    // correctly rebuilding the AST in all cases, some are getting
    // through. Just create an OpExpr that is multifix, since that's
    // what the op fixity was before. Talk to me if you think I am
    // dumb. Nels
    @Override
    public FValue forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
        return this.forOpExpr(ExprFactory.makeOpExpr(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     NodeUtil.getExprType(that),
                                                     that.getMultifix_op(),
                                                     that.getArgs()));
    }

    /**
     * Assumes {@code x.getOps()} is a list of length 1.  At the
     * moment it appears that this is true for every OpExpr node that
     * is ever created.
     */
    public FValue forOpExpr(OpExpr x) {
        FunctionalRef ref = x.getOp();

        IdOrOp op = ref.getOriginalName();
        List<Expr> args = x.getArgs();
        FValue fvalue = getOp(ref); //op.accept(this);
        fvalue = applyToStaticArgs(fvalue, ref.getStaticArgs(), ref);
        // Evaluate actual parameters.
        int s = args.size();
        FValue res = FVoid.V;
        List<FValue> vargs;

        if (op instanceof Op && OprUtil.isPostfix((Op) op) && args.size() == 1) {
            // It is a static error if a function argument is immediately followed
            // by a non-expression element.  For example, f(x)!
            // It is a static error if an exponentiation is immediately followed
            // by a non-expression element.  For example, a^b!
            Expr arg = args.get(0);
            if (arg instanceof OpExpr && isExponentiation((OpExpr) arg)) { // a^b!
                vargs = error(arg,
                              "Syntax Error: an exponentiation should not be " +
                              "immediately followed by a non-expression " + "element.");
            } else if (arg instanceof Juxt && ((Juxt) arg).isTight()) { // f(x)!
                vargs = Useful.list(forTightJuxt((Juxt) arg, true));
            } else if (arg instanceof MathPrimary) { // f(x)^y! y[a](x)!
                vargs = Useful.list(forMathPrimary((MathPrimary) arg, true));
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
            error(x, e, errorMsg("Operator ", op.stringName(), " has a non-function value ", fvalue));
        }
        Fcn fcn = (Fcn) fvalue;
        if (s <= 2 || ((Op) op).isEnclosing()) {
            res = fcn.functionInvocation(vargs, x);
        } else {
            List<FValue> argPair = new ArrayList<FValue>(2);
            argPair.add(vargs.get(0));
            argPair.add(vargs.get(1));
            res = fcn.functionInvocation(argPair, x);
            for (int i = 2; i < s; i++) {
                argPair.set(0, res);
                argPair.set(1, vargs.get(i));
                res = fcn.functionInvocation(argPair, x);
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
        return FString.make(x.getText());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.NodeVisitor#forSubscriptExpr(com.sun.fortress.interpreter.nodes.SubscriptExpr)
     */
    @Override
    public FValue forSubscriptExpr(SubscriptExpr x) {
        Expr obj = x.getObj();
        FValue arr = obj.accept(this);
        List<FValue> subs = evalExprListParallel(x.getSubs());
        Option<Op> op = x.getOp();
        // Should evaluate obj.[](subs, getText)
        String opString = "_[_]";
        if (op.isSome()) {
            opString = NodeUtil.nameString(op.unwrap());
        }
        return invokeMethod(arr, opString, subs, x);
    }

    // Non-static version provides the obvious arguments.
    public FValue invokeMethod(FValue receiver, String mname, List<FValue> args, HasAt site) {
        try {
            return DottedMethodApplication.invokeMethod(receiver, mname, mname, args);
        }
        catch (UnificationError ue) {
            // If we propagate these, they get misinterpreted by enclosing calls,
            // and we lose calling context information.  This leads to really confusing
            // failures!
            // So we need to wrap them instead.
            // See also Fcn.functionInvocation and invokeGenericMethod
            return error(site, errorMsg("Unification error: ", ue.getMessage()), ue);
        }
        catch (FortressException ex) {
            throw ex.setWhere(site);
        }
        catch (StackOverflowError soe) {
            return error(site, errorMsg("Stack overflow on ", site));
        }
    }

    // Version that evaluates arguments first.
    public FValue evalAndInvokeMethod(FValue receiver, String mname, List<Expr> args, HasAt site) {
        return invokeMethod(receiver, mname, evalInvocationArgs(args), site);
    }

    public FValue invokeGenericMethod(FValue receiver,
                                      String mname,
                                      List<StaticArg> sargs,
                                      List<FValue> args,
                                      HasAt site) {
        try {
            DottedMethodApplication app0 = DottedMethodApplication.make(receiver, mname, mname);
            DottedMethodApplication app = app0.typeApply(sargs, e, site);
            return app.functionInvocation(args, site);
        }
        catch (UnificationError ue) {
            // If we propagate these, they get misinterpreted by enclosing calls,
            // and we lose calling context information.  This leads to really confusing
            // failures!
            // So we need to wrap them instead.
            // See also Fcn.functionInvocation and invokeMethod
            return error(site, errorMsg("Unification error: ", ue.getMessage()), ue);
        }
        catch (FortressException ex) {
            throw ex.setWhere(site);
        }
        catch (StackOverflowError soe) {
            return error(site, errorMsg("Stack overflow on ", site));
        }
    }

    private boolean isFunction(FValue val) {
        return (val instanceof Fcn);
    }

    private boolean isExpr(MathItem mi) {
        return (mi instanceof ExprMI);
    }

    private boolean isParenthesisDelimited(MathItem mi) {
        return (mi instanceof ParenthesisDelimitedMI);
    }

    private MathItem dummyExpr() {
        Span span = NodeFactory.interpreterSpan;
        Expr dummyE = ExprFactory.makeVoidLiteralExpr(span);
        return ExprFactory.makeNonParenthesisDelimitedMI(span, dummyE);
    }

    private List<Pair<MathItem, FValue>> stepTwo(List<Pair<MathItem, FValue>> vs, boolean isPostfix) {
        if (vs.size() < 1) return error("Reassociation of MathPrimary failed!");
        else if (vs.size() == 1) return vs;
        else { // vs.size() > 1
            int ftnInd = 0;
            FValue ftn = vs.get(ftnInd).getB();
            FValue arg;
            MathItem argE = vs.get(ftnInd + 1).getA();
            for (Pair<MathItem, FValue> pair : IterUtil.skipFirst(vs)) {
                // 2. If some function element is immediately followed by
                // an expression element then, find the first such function
                // element, and call the next element the argument.
                argE = pair.getA();
                if (isFunction(ftn) && isExpr(argE)) {
                    arg = pair.getB();
                    // It is a static error if either the argument is not
                    // parenthesized, or the argument is immediately followed by
                    // a non-expression element.
                    if (!isParenthesisDelimited(argE)) return error(((ExprMI) argE).getExpr(),
                                                                    "Syntax Error: the " +
                                                                    "argument is not parenthesized.");
                    if (ftnInd + 2 < vs.size() && !isExpr(vs.get(ftnInd + 2).getA()))
                        return error(((ExprMI) argE).getExpr(),
                                     "Syntax Error: the " + "argument should not be immediately followed by a " +
                                     "non-expression element.");
                    // Otherwise, replace the function and argument with a
                    // single element that is the application of the function to
                    // the argument.  This new element is an expression.
                    Pair<MathItem, FValue> app = new Pair<MathItem, FValue>(dummyExpr(), Fcn.functionInvocation(arg,
                                                                                                                ftn,
                                                                                                                argE));
                    vs.set(ftnInd, app);
                    vs.remove(ftnInd + 1);
                    // It is a static error if a function argument is immediately
                    // followed by a postfix operator.  For example, y[a](x)!
                    if (isPostfix && vs.size() == 2 && isFunction(vs.get(0).getB()) && isExpr(vs.get(1).getA()))
                        return error(((ExprMI) vs.get(1).getA()).getExpr(),
                                     "Syntax Error: a " + "function argument should not be immediately " +
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

    private FValue mathItemApplication(NonExprMI opr, FValue front, MathItem loc) {
        if (opr instanceof ExponentiationMI) {
            ExponentiationMI expo = (ExponentiationMI) opr;
            IdOrOp op = expo.getOp().getOriginalName();
            FValue fvalue = getOp(expo.getOp()); //op.accept(this);
            if (!isFunction(fvalue)) return error(op, errorMsg("Operator ",
                                                               op.stringName(),
                                                               " has a non-function value ",
                                                               fvalue));
            Option<Expr> expr = expo.getExpr();
            Fcn fcn = (Fcn) fvalue;
            if (expr.isSome()) { // ^ Exponent
                FValue exponent = expr.unwrap().accept(this);
                return fcn.functionInvocation(Useful.list(front, exponent), op);
            } else { // ExponentOp
                return fcn.functionInvocation(Useful.list(front), op);
            }
        } else { // opr instanceof SubscriptingMI
            SubscriptingMI sub = (SubscriptingMI) opr;
            String opString = NodeUtil.nameString(sub.getOp());
            return evalAndInvokeMethod(front, opString, sub.getExprs(), loc);
        }
    }

    private List<Pair<MathItem, FValue>> stepThree(List<Pair<MathItem, FValue>> vs, boolean isPostfix) {
        if (vs.size() < 1) return error("Reassociation of MathPrimary failed!");
        else if (vs.size() == 1) return vs;
        else { // vs.size() > 1
            int frontInd = 0;
            FValue front = vs.get(frontInd).getB();
            MathItem opr;
            for (Pair<MathItem, FValue> pair : IterUtil.skipFirst(vs)) {
                // 3. If there is any non-expression element (it cannot be the
                // first element)
                opr = pair.getA();
                if (!isExpr(opr)) {
                    // then replace the first such element and the element
                    // immediately preceding it (which must be an expression)
                    // with a single element that does the appropriate operator
                    // application.  This new element is an expression.
                    Pair<MathItem, FValue> app = new Pair<MathItem, FValue>(dummyExpr(),
                                                                            mathItemApplication((NonExprMI) opr,
                                                                                                front,
                                                                                                opr));
                    vs.set(frontInd, app);
                    vs.remove(frontInd + 1);
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

    private List<Pair<MathItem, FValue>> reassociate(List<Pair<MathItem, FValue>> vs, boolean isPostfix) {
        if (vs.size() == 1) return vs;
        return stepThree(stepTwo(vs, isPostfix), isPostfix);
    }

    private FValue tightJuxt(FValue first, FValue second, MathItem loc, MathPrimary x) {
        if (isFunction(first)) return Fcn.functionInvocation(second, first, loc);
        else return Fcn.functionInvocation(Useful.list(first, second), e.getValue(x.getInfixJuxt()), loc);
    }

    private FValue leftAssociate(List<Pair<MathItem, FValue>> vs, MathPrimary x) {
        // vs.size() > 1
        FValue result = vs.get(0).getB();
        for (Pair<MathItem, FValue> i : IterUtil.skipFirst(vs)) {
            result = tightJuxt(result, i.getB(), i.getA(), x);
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
            List<Pair<MathItem, FValue>> vs = Useful.list(new Pair<MathItem, FValue>(null, fval));
            // It is a static error if an exponentiation is immediately followed
            // by a non-expression element.
            boolean isExponentiation = false;
            for (MathItem mi : rest) {
                if (mi instanceof ExprMI) vs.add(new Pair<MathItem, FValue>(mi, ((ExprMI) mi).getExpr().accept(this)));
                else { // mi instanceof NonExprMI
                    if (isExponentiation) return error(x,
                                                       "Syntax Error: an " +
                                                       "exponentiation should not be immediately followed " +
                                                       "by a subscripting or an exponentiation.");
                    vs.add(new Pair<MathItem, FValue>(mi, null));
                    isExponentiation = (mi instanceof ExponentiationMI);
                }
            }
            if (isPostfix && isExponentiation) {
                return error(x,
                             "Syntax Error: an exponentiation should not be " +
                             "immediately followed by a postfix operator.");
            }
            if (vs.size() == 1) fval = vs.get(0).getB();
            else
                // vs.size() > 1
                // 4. Otherwise, left-associate the sequence, which has only
                // expression elements, only the last of which may be a function.
                fval = leftAssociate(reassociate(vs, isPostfix), x);
        }
        return fval;
    }

    Name fldName(FieldRef arf) {
        return arf.getField();
    }

    private FValue forTightJuxt(Juxt x, boolean isPostfix) {
        List<Expr> exprs = x.getExprs();
        if (exprs.size() == 0) bug(x, e, "empty juxtaposition");
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

        } else if (fcnExpr instanceof _RewriteFnRef) {
            // Only come here if there are static args -- must be generic
            // Note that ALL method references have been rewritten into
            // this.that form, so that bare var-ref is a function
            _RewriteFnRef rfr = (_RewriteFnRef) fcnExpr;
            Expr fn = rfr.getFnExpr();
            List<StaticArg> args = rfr.getStaticArgs();
            if (fn instanceof FieldRef) {
                FieldRef arf = (FieldRef) fn;
                FValue fobj = arf.getObj().accept(this);
                return invokeGenericMethod(fobj, NodeUtil.nameString(fldName(arf)), args, evalInvocationArgs(exprs), x);
            } else if (fn instanceof VarRef) {
                // FALL OUT TO REGULAR FUNCTION CASE!
            } else {
                return bug(x, "_RewriteFnRef with unexpected fn " + fn);
            }
        } else if (fcnExpr instanceof FnRef) {
            // TODO this ought to be allowed.
            return bug(fcnExpr, "FnRefs are supposed to be gone from the AST \n" + x.toStringVerbose());
        }

        FValue fnVal = fcnExpr.accept(this);
        if (fnVal instanceof MethodClosure) {
            return bug(x, "Unexpected application of " + fcnExpr);
        } else if (fnVal instanceof Fcn) {
            if (isPostfix) {
                // It is a static error if a function argument is immediately
                // followed by a non-expression element.  For example, f(x)!
                return error(x,
                             "Syntax Error: a function argument " +
                             "should not be immediately followed by a non-expression " + "element.");
            } else {
                return finishFunctionInvocation(exprs, fnVal, x);
            }
        } else {
            // When a tight juxtaposition is not a function application,
            // fake it as a loose juxtaposition.  It's clearly a hack.
            // Please fix it if you know how to do it.  -- Sukyoung
            // Less of a hack now.  -- David
            return forJuxtCommon(x);
        }
    }

    /**
     * Unlike invokeMethod, must handle case where we have a closure-valued field.
     *
     * @param x
     * @param fobj
     * @param fld
     * @param exprs
     * @return
     * @throws ProgramError
     */
    private FValue juxtMemberSelection(Juxt x, FValue fobj, Id fld, List<Expr> exprs) throws ProgramError {
        List<FValue> args = evalInvocationArgs(exprs);
        String mname = NodeUtil.nameString(fld);
        if (fobj instanceof FObject) {
            FObject fobject = (FObject) fobj;
            // TODO Need to distinguish between public/private methods/fields
            Environment se = fobject.getSelfEnv();
            FValue cl = se.getLeafValueNull(mname); // leaf
            if (cl != null && !(cl instanceof Method) && cl instanceof Fcn) {
                // Ordinary closure, assigned to a field.
                return ((Fcn) cl).functionInvocation(args, x);
            }
        }
        return invokeMethod(fobj, mname, args, x);
    }

    /**
     * @param exprs
     * @param fcnExpr
     * @return
     */
    private FValue finishFunctionInvocation(List<Expr> exprs, FValue foo, AbstractNode loc) {
        return Fcn.functionInvocation(evalInvocationArgs(exprs), foo, loc);
    }

    /**
     * Evaluates the tail of the list, EXCLUDING THE FIRST ELEMENT.
     *
     * @param exprs
     * @return
     */
    List<FValue> evalInvocationArgs(List<Expr> exprs) {
        List<FValue> rest = evalExprListParallel(exprs.subList(1, exprs.size()));
        if (rest.size() == 1) {
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

    private FValue handleException(Try x, FObject exc, FortressException original) {
        FType excType = exc.type();
        Option<Catch> _catchClause = x.getCatchClause();
        if (_catchClause.isSome()) {
            Catch _catch = _catchClause.unwrap();
            Id name = _catch.getName();
            List<CatchClause> clauses = _catch.getClauses();
            for (CatchClause clause : clauses) {
                BaseType match = clause.getMatchType();
                Block catchBody = clause.getBody();
                FType foo = EvalType.getFType(match, e);
                if (excType.subtypeOf(foo)) {
                    try {
                        Evaluator evClause = new Evaluator(this, _catch);
                        evClause.e.putValue(name.getText(), exc);
                        return catchBody.accept(evClause);
                    }
                    catch (FortressException err) {
                        throw err.setContext(x, e);
                    }
                }
            }
        }
        for (BaseType forbidType : x.getForbidClause()) {
            if (excType.subtypeOf(EvalType.getFType(forbidType, e))) {
                Environment libE = Driver.getFortressLibrary();
                //FType ftype = libE.getRootTypeNull(WellKnownNames.forbiddenException); // toplevel
                List<FValue> args = new ArrayList<FValue>();
                args.add(exc);
                Constructor c = (Constructor) libE.getRootValue(WellKnownNames.forbiddenException);
                FObject f = (FObject) c.functionInvocation(args, forbidType);
                FortressError f_exc = new FortressError(x, e, f);
                throw f_exc;
            }
        }
        // Nothing has handled or excluded exc; re-throw!
        throw original;
    }

    public FValue forTry(Try x) {
        Block body = x.getBody();
        FValue res = FVoid.V;
        try {
            res = body.accept(this);
            return res;
        }
        catch (LabelException exc) {
            return handleException(x,
                                   (FObject) Driver.getFortressLibrary().getRootValue(WellKnownNames.labelException),
                                   exc);
        }
        catch (FortressError exc) {
            return handleException(x, exc.getException(), exc);
        }
        finally {
            Option<Block> finallyClause = x.getFinallyClause();
            if (finallyClause.isSome()) {
                Block b = finallyClause.unwrap();
                b.accept(this);
            }
        }
    }

    public FValue forTupleExpr(TupleExpr x) {
        if (x.getVarargs().isSome() || x.getKeywords().size() > 0) { // ArgExpr
            List<Expr> exprs = x.getExprs();
            return FTuple.make(evalExprListParallel(exprs));
        } else {
            List<Expr> exprs = x.getExprs();
            return FTuple.make(evalExprListParallel(exprs));
        }
    }

    private List<Id> collectIds(TypeOrPattern t) {
        List<Id> ids = new ArrayList<Id>();
        if (t instanceof Pattern) {
            for (PatternBinding pb : ((Pattern)t).getPatterns().getPatterns()) {
                if (pb instanceof PlainPattern) {
                    PlainPattern that = (PlainPattern)pb;
                    ids.add(that.getName());
                    if (that.getIdType().isSome())
                        ids.addAll(collectIds(that.getIdType().unwrap()));
                } else if (pb instanceof NestedPattern) {
                    ids.addAll(collectIds(((NestedPattern)pb).getPat()));
                }
            }
        }
        return ids;
    }

    private Type getType(TypeOrPattern t) {
        if (t instanceof Type) return (Type)t;
        else {
            Pattern p = (Pattern)t;
            if (p.getName().isSome()) return p.getName().unwrap();
            else {
                List<Type> types = new ArrayList<Type>();
                for (PatternBinding pb : p.getPatterns().getPatterns()) {
                    if (pb instanceof PlainPattern &&
                        ((PlainPattern)pb).getIdType().isSome()) {
                        types.add(getType(((PlainPattern)pb).getIdType().unwrap()));
                    } else if (pb instanceof TypePattern) {
                        types.add(((TypePattern)pb).getTyp());
                    } else return error("typecase match failure!");
                }
                return NodeFactory.makeTupleType(types);
            }
        }
    }

    public FValue forTypecase(Typecase x) {
        Evaluator ev = new Evaluator(this, x);
        Expr expr = x.getBindExpr();
        FValue val = expr.accept(ev);
        FType resTy = val.type();
        FValue result = FVoid.V;

        for (TypecaseClause c : x.getClauses()) {
            TypeOrPattern match = c.getMatchType();
            Type typ = getType(match);
            /* Technically, match and res need not be tuples; they could be
               singletons and the subtype test below ought to be correct. */
            FType matchTy = EvalType.getFType(typ, ev.e);
            List<Id> ids = collectIds(match);
            if (resTy.subtypeOf(matchTy)) {
                if (c.getName().isSome() || ids.size() == 1) {
                    String name;
                    if (c.getName().isSome()) name = c.getName().unwrap().getText();
                    else name = ids.get(0).getText();
                    if (isShadow(expr, name))
                        /* Avoid shadow error when we bind the same var name */
                        ev.e.putValueRaw(name, val);
                    else
                        /* But warn about shadowing in all other cases */
                        ev.e.putValue(name, val);
                } else {
                    List<Pair<FValue, Option<String>>> vals = new ArrayList<Pair<FValue, Option<String>>>();
                    if (val instanceof FTuple) {
                        for (FValue v : ((FTuple) val).getVals()) {
                            vals.add(new Pair<FValue, Option<String>>(v, Option.<String>none()));
                        }
                        int index = 0;
                        for (Id id : ids) {
                            String name = id.getText();
                            Pair<FValue, Option<String>> pair = vals.get(index);
                            FValue v = pair.getA();
                            if (isShadow(pair.getB(), name)) {
                                /* Avoid shadow error when we bind the same var name */
                                ev.e.putValueRaw(name, v);
                            } else {
                                /* But warn about shadowing in all other cases */
                                ev.e.putValue(name, v);
                            }
                            index++;
                        }
                    }
                }
                Block body = c.getBody();
                result = ev.evalExprList(body.getExprs(), body);
                return result;
            }
        }

        Option<Block> el = x.getElseClause();
        if (el.isSome()) {
            Block elseClauses = el.unwrap();
            result = ev.evalExprList(elseClauses.getExprs(), elseClauses);
            return result;
        } else {
            // throw new MatchFailure();
            return error(x, e, errorMsg("typecase match failure given ", resTy));
        }
    }

    public FValue forVarRef(VarRef x) {
        if (NodeUtil.isSingletonObject(x)) {
            Id name = x.getVarId();
            FValue g = forIdOfTLRef(name);
            return applyToActualStaticArgs(g, x.getStaticArgs(), x);
        }

        //        Id id = x.getVar();
        //        String s= id.getText();
        //        if (s.contains("$self")) {
        //            id = x.getVar();
        //        }
        FValue res = BaseEnv.toContainingObjectEnv(e, x.getLexicalDepth()).getValueNull(x);
        if (res == null) {
            res = BaseEnv.toContainingObjectEnv(e, x.getLexicalDepth()).getValueNull(x);
            Iterable<Id> names = NodeUtil.getIds(x.getVarId());
            error(x, e, errorMsg("undefined variable ", names));
        }
        return res;
    }

    public FValue forVoidLiteralExpr(VoidLiteralExpr x) {
        return FVoid.V;
    }

    public FValue forWhile(While x) {
        Expr body = x.getBody();
        GeneratorClause cond = x.getTestExpr();
        Expr test;
        if (cond.getBind().isEmpty()) test = cond.getInit();
        else test = bug(x, "Undesugared generalized while expression.");
        FValue clauseVal = test.accept(this);
        if (clauseVal instanceof FBool) {
            FBool res = (FBool) clauseVal;
            while (res.getBool() != false) {
                body.accept(this);
                res = (FBool) test.accept(this);
            }
            return FVoid.V;
        } else {
            return error(test, errorMsg("While condition does not have type Boolean, " + "but ", clauseVal));
        }
    }

    public FValue forThrow(Throw throw1) {
        Expr ex = throw1.getExpr();
        FObject v = (FObject) ex.accept(this);
        FortressError f_exc = new FortressError(throw1, e, v);
        throw f_exc;
        // neverReached
    }

    public FValue forCharLiteralExpr(CharLiteralExpr x) {
        return FChar.make(x.getCharVal());
    }

    public FValue forFloatLiteralExpr(FloatLiteralExpr x) {
        return new FFloatLiteral(x.getText(), x.getIntPart(), x.getNumerator(), x.getDenomBase(), x.getDenomPower());
    }

    public FValue forIntLiteralExpr(IntLiteralExpr x) {
        return FIntLiteral.make(x.getIntVal());
    }

    public FValue forBooleanLiteralExpr(BooleanLiteralExpr x) {
        return FBool.make(x.getBooleanVal() == 1? true : false);
    }
    
    private FValue forIdOfTLRef(Id x) {
        FValue res = e.getValueNull(x, Environment.TOP_LEVEL);
        if (res == null) {
            error(x, e, errorMsg("undefined variable ", x));
        }
        return res;
    }

    @Override
    public FValue for_RewriteFnRef(_RewriteFnRef x) {
        Expr name = x.getFnExpr();
        FValue g = name.accept(this);
        return applyToStaticArgs(g, x.getStaticArgs(), x);
    }

    public FValue applyToStaticArgs(FValue g, List<StaticArg> args, HasAt x) {
        if (args.size() == 0) return g;
        else return applyToActualStaticArgs(g, args, x);
    }

    public FValue applyToActualStaticArgs(FValue g, List<StaticArg> args, HasAt x) {
        if (g instanceof FGenericFunction) {
            return ((FGenericFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericConstructor) {
            return ((GenericConstructor) g).typeApply(args, e, x);
        } else if (g instanceof OverloadedFunction) {
            return ((OverloadedFunction) g).typeApply(args, e, x);
        } else if (g instanceof GenericSingleton) {
            return ((GenericSingleton) g).typeApply(args, e, x);
        } else {
            return error(x, e, errorMsg("Unexpected _RewriteFnRef value, ", g));
        }
    }

    public FValue for_WrappedFValue(_WrappedFValue w) {
        return w.getFValue();
    }
}
