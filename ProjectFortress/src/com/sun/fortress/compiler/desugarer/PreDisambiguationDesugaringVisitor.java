/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.Shell;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.DesugarerUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.typechecker.TypeNormalizer;
import com.sun.fortress.compiler.typechecker.SelfTypeBoundsInserter;
import com.sun.fortress.useful.Iter;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.nodes_util.DesugarerUtil.*;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

/** Run desugaring phases that must occur before disambiguation.
 *  1) Rewrite trait, object, and object expressions to explicitly extend Object.
 *  2) Remove conditional operators, replacing their operands with thunks.
 *  Desugar conditional operators into operators that take thunks.
 *  This desugaring is described in section 22.8 of the specification.
 *  We find {@code e_1 AND: e_2}, for example, and change it into
 *  {@code e_1 AND (fn () => e_2)}, for which an overloading must exist.
 *  This desugaring must go before disambiguation, and is therefore called
 *  by {@code PreDisambiguationDesugarer}.
 *  3) Rewrite reductions to explicit invocations of big operators.
 *  4) Desugar compound assignments (this requires identifying subexpressions of LHS)
 *  5) Desugar subscripting expressions to method calls.
 *  6) Desugar subscripting assignments
 */
public class PreDisambiguationDesugaringVisitor extends NodeUpdateVisitor {

    private final Id anyTypeId = NodeFactory.makeId(NodeFactory.makeSpan("singleton"), WellKnownNames.anyTypeName);

    /** If the extends clause of a trait declaration, an object declaration, or
     *  an object expression is empty, then replace the empty extends clause
     *  with {Object}.
     */
    private List<TraitTypeWhere> rewriteExtendsClause(Node whence,
                                                      List<TraitTypeWhere> extendsClause) {
        if (extendsClause.size() > 0) return extendsClause;
        if ( ! ( whence instanceof ASTNode ) )
            bug(whence, "Only ASTNodes are supported.");
        Id objectId = NodeFactory.makeId(NodeUtil.getSpan((ASTNode)whence),
                                         WellKnownNames.objectTypeName);
        TraitType typeObject = NodeFactory.makeTraitType(objectId);
        TraitTypeWhere extendsObject = NodeFactory.makeTraitTypeWhere(typeObject);
        return Collections.singletonList(extendsObject);
    }

    @Override
        public Node forObjectExprOnly(ObjectExpr that,
                                      ExprInfo info,
                                      TraitTypeHeader header,
                                      Option<SelfType> selfType) {
    	SelfTypeBoundsInserter bi = new SelfTypeBoundsInserter();
    	that = (ObjectExpr)that.accept(bi);
    	header = (TraitTypeHeader)header.accept(bi);
        Span span = NodeUtil.getSpan(that);
        List<TraitTypeWhere> extendsClause = rewriteExtendsClause(that, header.getExtendsClause());
        header = NodeFactory.makeTraitTypeHeader(NodeFactory.makeId(span,"_"),
                                                 extendsClause,
                                                 header.getDecls());
        return super.forObjectExprOnly(that, info, header, selfType);
    }

    @Override
        public Node forTraitDecl(TraitDecl that) {
    	that = (TraitDecl)that.accept(new SelfTypeBoundsInserter());
        TraitTypeHeader header_result = (TraitTypeHeader) recur(that.getHeader());
        List<BaseType> excludesClause_result = recurOnListOfBaseType(that.getExcludesClause());
        Option<List<NamedType>> comprisesClause_result = recurOnOptionOfListOfNamedType(that.getComprisesClause());

        if (!NodeUtil.getName(that).equals(anyTypeId)) {
            header_result = NodeFactory.makeTraitTypeHeader(header_result,
                                                            rewriteExtendsClause(that, header_result.getExtendsClause()));
        }

        return super.forTraitDeclOnly(that, that.getInfo(), header_result,
                                      that.getSelfType(),
                                      excludesClause_result,
                                      comprisesClause_result);
    }

    @Override
        public Node forObjectDecl(ObjectDecl that) {
    	that = (ObjectDecl)that.accept(new SelfTypeBoundsInserter());
        TraitTypeHeader header_result = (TraitTypeHeader) recur(that.getHeader());
        Option<List<Param>> params_result = recurOnOptionOfListOfParam(NodeUtil.getParams(that));
        header_result = NodeFactory.makeTraitTypeHeader(header_result,
                                                        rewriteExtendsClause(that, header_result.getExtendsClause()),
                                                        params_result);
        return super.forObjectDeclOnly(that, that.getInfo(),
                                       header_result, that.getSelfType());
    }

    @Override
    public Node forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
        // If there is a colon at all, the operator is no longer ambiguous:
        // It must be infix.
        IdOrOp name = that.getInfix_op().getNames().get(0);
        if ( ! (name instanceof Op) )
            return bug(name, "The name field of OpRef should be Op.");
        Op op_name = (Op)name;
        boolean prefix = OprUtil.hasPrefixColon(op_name);
        boolean suffix = OprUtil.hasSuffixColon(op_name);

        if( prefix || suffix ) {
            OpExpr new_op = ExprFactory.makeOpExpr(NodeUtil.getSpan(that),
                                                   NodeUtil.isParenthesized(that),
                                                   NodeUtil.getExprType(that),
                                                   that.getInfix_op(),
                                                   that.getArgs());
            return recur(new_op);
        }
        else {
            return super.forAmbiguousMultifixOpExpr(that);
        }
    }

    @Override
    public Node forOpExpr(OpExpr that) {
        FunctionalRef op_result = (FunctionalRef) recur(that.getOp());

        /***
         * For  BIG OP <| BIG OT <| f y | y <- ys |> | ys <- gg |>
         * Is this case, BIG <||> is being removed.
         * The nested reduction is replaced with
         *   __bigOperator2(BIG OP, BIG OT, gg)
         *
         * by Kento
         */
        String str = op_result.toString();
        String theListEnclosingOperatorName = "BIG <| BIG |>";
        String someBigOperatorName = "BIG";
        //make sure the body is of application of some big operator
        if ((str.length() >= someBigOperatorName.length()
             && str.substring(0, someBigOperatorName.length()).equals(someBigOperatorName))) {
            // make sure that BIG OP (Accumulator (BIG <||>, gs))
            if(that.getArgs().size()==1 && that.getArgs().get(0) instanceof Accumulator && ((Accumulator)that.getArgs().get(0)).getAccOp().toString().equals(theListEnclosingOperatorName)) {

                Accumulator acc = (Accumulator)that.getArgs().get(0);
                Expr body = visitGenerators(NodeUtil.getSpan(acc), acc.getGens(), acc.getBody());
                /***
                 * If the accumulation is a nested reduction like <| BIG OT <| f y | y <- ys |> | ys <- gg |> ,
                 * visitGenerators returns a tuple of ((BIG OT, f), gg)
                 *  (this should be refactored, though)
                 * In this case, the nested reduction is replaced with __bigOperator2
                 */
                if(body instanceof TupleExpr) {
                    // a tuple of the inner Accumulator (op, body) and the gg
                    TupleExpr tuple = (TupleExpr)body;
                    TupleExpr innerAccumTuple = (TupleExpr)tuple.getExprs().get(0);
                    Expr opexpI = (Expr)innerAccumTuple.getExprs().get(0);
                    Expr innerBody = (Expr)innerAccumTuple.getExprs().get(1);
                    FunctionalRef ref = (FunctionalRef) op_result;
                    IdOrOp name = ref.getNames().get(0);
                    // make sure the operator is actually an operator
                    if ( ! (name instanceof Op) ) return null;
                    Expr opexpO = ExprFactory.makeOpExpr(NodeUtil.getSpan(that),(Op)name,ref.getStaticArgs());
                    Expr gg = tuple.getExprs().get(1);
                    Expr res =
                        ExprFactory.make_RewriteFnApp(NodeUtil.getSpan(that),
                            BIGOP2_NAME,
                            ExprFactory.makeTupleExpr(NodeUtil.getSpan(body),
                                                      opexpO, opexpI, gg, innerBody));
                    return (Expr)recur(res);
                }
            }
        }

        List<Expr> args_result = recurOnListOfExpr(that.getArgs());

        OpExpr new_op;
        if( op_result == that.getOp() && args_result == that.getArgs() ) {
            new_op = that;
        }
        else {
            new_op = ExprFactory.makeOpExpr(NodeUtil.getSpan(that),
                                            NodeUtil.isParenthesized(that),
                                            NodeUtil.getExprType(that),
                                            op_result,
                                            args_result);
        }
        return cleanupOpExpr(new_op);
    }

    private static Expr thunk(Expr e) {
        return ExprFactory.makeFnExpr(NodeUtil.getSpan(e),
                                      Collections.<Param>emptyList(), e);
    }

    private Expr cleanupOpExpr(OpExpr opExp) {
        FunctionalRef ref = opExp.getOp();

        List<Expr> args = opExp.getArgs();

        if (args.size() <= 1) return opExp;
        IdOrOp name = ref.getNames().get(0);
        if ( ! (name instanceof Op) )
            return bug(name, "The name field of OpRef should be Op.");
        Op qop = (Op)name;

        if (OprUtil.isEnclosing(qop)) return opExp;
        if (OprUtil.isUnknownFixity(qop))
            return bug(opExp, "The operator fixity is unknown: " +
                       ((Op)qop).getText());
        boolean prefix = OprUtil.hasPrefixColon(qop);
        boolean suffix = OprUtil.hasSuffixColon(qop);
        if (!prefix && !suffix) return opExp;
        qop = OprUtil.noColon(qop);
        Iterator<Expr> i = args.iterator();
        Expr res = i.next();
        Span sp = NodeUtil.getSpan(opExp);
        for (Expr arg: Iter.iter(i)) {
            if (prefix) {
                res = thunk(res);
            }
            if (suffix) {
                arg = thunk(arg);
            }
            res = ExprFactory.makeOpExpr(sp, NodeUtil.isParenthesized(opExp), qop, res, arg);
        }
        return res;
    }

    /**
     * Given generalized if expression, desugar into __cond calls (binding)
     * where required.
     */
    @Override
    public Node forIf(If i) {
        List<IfClause> clauses = i.getClauses();
        int n = clauses.size();
        if (n <= 0) bug(i, "if with no clauses!");
        for (IfClause c : clauses) {
            if (c.getTestClause().getBind().size() == 0) continue;
            // If we get here we have a generalized if.
            // Desugar it into nested ifs and calls.
            // Then return the desugared result.
            Expr result = null;
            if (i.getElseClause().isSome()) {
                result = i.getElseClause().unwrap();
            }
            // Traverse each clause and desugar it into an if or a __cond as appropriate.
            for (--n; n >= 0; --n) {
                result = addIfClause(clauses.get(n), result);
            }
            return result;
        }
        // If we get here, it's not a generalized if.  Just recur.
        return super.forIf(i);
    }

    /**
     * Add an if clause to a (potentially) pre-existing else clause.
     * The else clase can be null, or can be an if expression.
     */
    private Expr addIfClause(IfClause c, Expr elsePart) {
        GeneratorClause g = c.getTestClause();
        if (g.getBind().size() > 0) {
            // if binds <- expr then body else elsePart end desugars to
            // __cond(expr, fn (binds) => body, elsePart)
            ArrayList<Expr> args = new ArrayList<Expr>(3);
            args.add(g.getInit());
            args.add(bindsAndBody(g, c.getBody()));
            if (elsePart != null) args.add(thunk(elsePart));
            return (Expr)recur(ExprFactory.make_RewriteFnApp(
                                   NodeUtil.getSpan(c),
                                   COND_NAME,
                                   ExprFactory.makeTupleExpr(NodeUtil.getSpan(c), args)));
        }
        // if expr then body else elsePart end is preserved
        // (but we replace elif chains by nesting).
        if (elsePart == null) {
            return (Expr)super.forIf(ExprFactory.makeIf(NodeUtil.getSpan(c), c));
        } else {
            return (Expr)super.forIf(ExprFactory.makeIf(NodeUtil.getSpan(c), c,
                                                        ExprFactory.makeBlock(elsePart)));
        }
    }

    /**
     * Desugar a generalized While clause.
     */
    @Override
    public Node forWhile(While w) {
        GeneratorClause g = w.getTestExpr();
        if (g.getBind().size() > 0) {
            // while binds <- expr  do body end
            // desugars to
            // while __whileCond(expr, fn (binds) => body) do end
            ArrayList<Expr> args = new ArrayList<Expr>(2);
            args.add(g.getInit());
            args.add(bindsAndBody(g, w.getBody()));
            Expr cond =
                ExprFactory.make_RewriteFnApp(NodeUtil.getSpan(g),
                    WHILECOND_NAME,
                    ExprFactory.makeTupleExpr(NodeUtil.getSpan(w), args));
            w = ExprFactory.makeWhile(NodeUtil.getSpan(w), cond);
        }
        return (Expr)super.forWhile(w);
    }

    @Override
    public Node forFor(For f) {
        Block df = f.getBody();
        Do doBlock = ExprFactory.makeDo(NodeUtil.getSpan(df), Useful.list(df));
        return visitLoop(NodeUtil.getSpan(f), f.getGens(), doBlock);
    }

    /**
     * @param loc  Containing context
     * @param gens Generators in generator list
     * @return single generator equivalent to the generator list
     *         Desugars as follows:
     *         body, empty  =>  body
     *         body, x <- exp, gs  => exp.loop(fn x => body, gs)
     */
    Expr visitLoop(Span span, List<GeneratorClause> gens, Expr body) {
        for (int i = gens.size() - 1; i >= 0; i--) {
            GeneratorClause g = gens.get(i);
            Expr loopBody = bindsAndBody(g, body);
            body = ExprFactory.makeMethodInvocation(NodeUtil.getSpan(g),
                                                    g.getInit(),
                                                    LOOP_NAME,
                                                    loopBody);
        }
        // System.out.println("Desugared to "+body.toStringVerbose());
        return (Expr)recur(body);
    }

    @Override
    public Node forAccumulator(Accumulator that) {
        return visitAccumulator(NodeUtil.getSpan(that), that.getGens(),
                                that.getAccOp(), that.getBody(),
                                that.getStaticArgs(),
                                that.getInfo().isParenthesized());
    }

    private Expr visitAccumulator(Span span, List<GeneratorClause> gens,
                                  Op op, Expr body,
                                  List<StaticArg> staticArgs, boolean isParen) {
        body = visitGenerators(span, gens, body);
        /***
         * If the accumulation is a nested reduction like BIG OP [ys <- gg] BIG OT <| f y | y <- ys |> ,
         * visitGenerators returns a tuple of ((BIG OT, f), gg)
         *  (this should be refactored, though)
         */
        Expr res;
        if (body instanceof FnExpr) {
            Expr opexp = ExprFactory.makeOpExpr(span,op,staticArgs);
            res = ExprFactory.make_RewriteFnApp(span,
                      BIGOP_NAME,
                      ExprFactory.makeTupleExpr(span,opexp,body));
        } else if (body instanceof TupleExpr){
            /***
             * For  BIG OP [ys <- gg] BIG OT <| f y | y <- ys |>
             * The nested reduction is replaced with
             *   __bigOperator2(BIG OP, BIG OT, gg)
             *
             * This is similar to forOpExpr(OpExpr that) .
             *
             * by Kento
             */
            // a tuple of the inner Accumulator (op, body) and the gg
            TupleExpr tuple = (TupleExpr)body;
            TupleExpr innerAccumTuple = (TupleExpr)tuple.getExprs().get(0);
            Expr opexpI = (Expr)innerAccumTuple.getExprs().get(0);
            Expr innerBody = (Expr)innerAccumTuple.getExprs().get(1);
            Expr opexpO = ExprFactory.makeOpExpr(span,op,staticArgs);
            Expr gg = tuple.getExprs().get(1);

            res = ExprFactory.make_RewriteFnApp(span,
                      BIGOP2_NAME,
                      ExprFactory.makeTupleExpr(span,opexpO,opexpI,gg, innerBody));
        } else
            res = bug(body, "Function expressions or tuple expressions are expected.");
        if ( isParen ) res = ExprFactory.makeInParentheses(res);
        return (Expr)recur(res);
    }

    @Override
    public Node forSubscriptExpr(SubscriptExpr that) {
	if (!Shell.getAssignmentPreDesugaring()) {
	    return super.forSubscriptExpr(that);
	} else {
	    // Rewrite a subscript expression into a method call (pretty straightforward)
	    Expr obj = that.getObj();
	    List<Expr> subs = that.getSubs();
	    Option<Op> op = that.getOp();
	    List<StaticArg> staticArgs =that.getStaticArgs();
	    if (!op.isSome()) bug(that, "Subscript operator expected");
	    Op knownOp = op.unwrap();
	    Expr result = ExprFactory.makeMethodInvocation(that,
							   obj,
							   knownOp,
							   staticArgs,
							   ExprFactory.makeTupleExpr(NodeUtil.getSpan(knownOp), subs));					   
	    return (Expr)recur(result);
	}
    }
    
    @Override
    public Node forAssignment(Assignment that) {
	// Here there are three sorts of rewrite to consider:
	// (a) If this is a compound assignment, rewrite to use ordinary assignment.
	// (b) If the lhs is a tuple, rewrite into a set of individual assignments.
	// (c) If the lhs is a subscript expression, rewrite to a method call.
	List<Lhs> lhs = that.getLhs();
	Option<FunctionalRef> assignOp = that.getAssignOp();
	Expr rhs = that.getRhs();
	List<CompoundAssignmentInfo> assignmentInfos = that.getAssignmentInfos();
	if (!Shell.getAssignmentPreDesugaring()) {
	    return super.forAssignment(that);
	}
        else if (assignOp.isSome() || lhs.size() > 1) {
	    // Compound and/or tuple assignment
	    // The basic idea is to transform `(a, b.field, c[sub1,sub2]) := e` into
	    // `do (ta, tb, tc, tsub1, tsub2, (t1, t2, t3)) = (a, b, c, sub1, sub2, e)
	    //     a := t1; tb.field := t2; tc[tsub1, tsub2] := t3 end`
	    // (TODO) Unfortunately, currently we don't handle nested binding tuples.
	    // For now, we'll just transform it into
	    // `do (ta, tb, tc, tsub1, tsub2) = (a, b, c, sub1, sub2)
	    //     (t1, t2, t3) = e
	    //     a := t1; tb.field := t2; tc[tsub1, tsub2] := t3 end`
	    // which merely loses a bit of potential parallelism.
	    // We omit the first tuple binding if the tuple is empty.
	    // If it is a compound assignment `(a, b.field, c[sub1,sub2]) OP= e`, it becomes
	    // `do (ta, tb, tc, tsub1, tsub2) = (a, b, c, sub1, sub2)
	    //     (t1, t2, t3) = (ta, tb, tc[tsub1, tsub2]) OP e
	    //     a := t1; tb.field := t2; tc[tsub1, tsub2] := t3 end`
	    List<LValue> exprLValues = Useful.list();
	    List<LValue> otherLValues = Useful.list();
	    List<Expr> otherExprs = Useful.list();
	    List<Expr> assignments = Useful.list();
	    boolean isCompound = assignOp.isSome();
	    List<Expr> accesses = Useful.list();
	    Span thatSpan = NodeUtil.getSpan(that);
	    for (Lhs lh : lhs) {
		Span lhSpan = NodeUtil.getSpan((Expr)lh);
		Id tempId = DesugarerUtil.gensymId(lhSpan, "e");
		VarRef tempVar = ExprFactory.makeVarRef(lhSpan, tempId);
		exprLValues = Useful.snoc(exprLValues, NodeFactory.makeLValue(lhSpan ,tempId));
		if (lh instanceof SubscriptExpr) {
		    SubscriptExpr lhsub = (SubscriptExpr)lh;
		    Expr obj = lhsub.getObj();
		    Span objSpan = NodeUtil.getSpan(obj);
		    List<Expr> subs = lhsub.getSubs();
		    Id baseTempId = DesugarerUtil.gensymId(objSpan, "b");
		    VarRef baseTempVar = ExprFactory.makeVarRef(objSpan, baseTempId);
		    otherLValues = Useful.snoc(otherLValues, NodeFactory.makeLValue(objSpan, baseTempId));
		    otherExprs = Useful.snoc(otherExprs, obj);
		    List<Expr> subTempVars = Useful.list();
		    for (Expr sub : lhsub.getSubs()) {
			Span subSpan = NodeUtil.getSpan(sub);
			Id subTempId = DesugarerUtil.gensymId(subSpan, "s");
			subTempVars = Useful.snoc(subTempVars, ExprFactory.makeVarRef(subSpan, subTempId));
			otherLValues = Useful.snoc(otherLValues, NodeFactory.makeLValue(subSpan, subTempId));
		    }
		    otherExprs = Useful.concat(otherExprs, subs);
		    SubscriptExpr newLhs = ExprFactory.makeSubscriptExpr(NodeUtil.getSpan(lhsub),
									 baseTempVar, subTempVars,
									 lhsub.getOp(), lhsub.getStaticArgs());
		    if (isCompound) accesses = Useful.snoc(accesses, newLhs);
		    assignments = Useful.snoc(assignments, ExprFactory.makeAssignment(thatSpan, newLhs, tempVar));
		} else if (lh instanceof FieldRef) {
		    FieldRef lhref = (FieldRef)lh;
		    Expr obj = lhref.getObj();
		    Span objSpan = NodeUtil.getSpan(obj);
		    Id objTempId = DesugarerUtil.gensymId(objSpan, "o");
		    VarRef objTempVar = ExprFactory.makeVarRef(objSpan, objTempId);
		    otherLValues = Useful.snoc(otherLValues, NodeFactory.makeLValue(objSpan, objTempId));
		    otherExprs = Useful.snoc(otherExprs, obj);
		    FieldRef newLhs = ExprFactory.makeFieldRef(NodeUtil.getSpan(lhref), objTempVar, lhref.getField());
		    if (isCompound) accesses = Useful.snoc(accesses, newLhs);
		    assignments = Useful.snoc(assignments, ExprFactory.makeAssignment(thatSpan, newLhs, tempVar));
		} else if (lh instanceof VarRef) {
		    VarRef lhvar = (VarRef)lh;
		    Span varSpan = NodeUtil.getSpan(lhvar);
		    Id varTempId = DesugarerUtil.gensymId(varSpan, "v");
		    VarRef varTempVar = ExprFactory.makeVarRef(varSpan, varTempId);
		    otherLValues = Useful.snoc(otherLValues, NodeFactory.makeLValue(varSpan, varTempId));
		    otherExprs = Useful.snoc(otherExprs, lhvar);
		    if (isCompound) accesses = Useful.snoc(accesses, varTempVar);
		    assignments = Useful.snoc(assignments, ExprFactory.makeAssignment(thatSpan, lhvar, tempVar));
		} else {
		    bug(that, "Malformed assignment LHS");
		}
	    }
	    Expr result = ExprFactory.makeBlock(thatSpan, assignments);
	    if (otherExprs.size() > 0) {
		Expr otherRhs = ExprFactory.makeMaybeTupleExpr(thatSpan, otherExprs);
		result = ExprFactory.makeLocalVarDecl(thatSpan, otherLValues, otherRhs,  result);
	    }
	    Expr newRhs = isCompound ?
		ExprFactory.makeOpExpr(NodeUtil.spanTwo(assignOp.unwrap(), rhs),
				       assignOp.unwrap(),
				       ExprFactory.makeMaybeTupleExpr(thatSpan, accesses),
				       rhs) :
		rhs;
	    result = ExprFactory.makeLocalVarDecl(thatSpan, exprLValues, newRhs, result);
	    return (Expr)recur(result);
	} else if (lhs.get(0) instanceof SubscriptExpr) {
	    // Subscripted assignment
	    SubscriptExpr lhExpr = (SubscriptExpr)lhs.get(0);
	    Expr obj = lhExpr.getObj();
	    List<Expr> subs = lhExpr.getSubs();
	    Option<Op> op = lhExpr.getOp();
	    List<StaticArg> staticArgs = lhExpr.getStaticArgs();
	    if (!op.isSome()) bug(lhExpr, "Subscript operator expected");
	    Op knownOp = op.unwrap();
	    Expr result = ExprFactory.makeMethodInvocation(that,
							   obj,
							   NodeFactory.makeOp(knownOp, knownOp.getText() + ":="),
							   staticArgs,
							   ExprFactory.makeTupleExpr(NodeUtil.spanTwo(knownOp, rhs),
										     Useful.cons(rhs, subs)));
	    return (Expr)recur(result);
	} else {
	    return super.forAssignment(that);
	}
    }


}
