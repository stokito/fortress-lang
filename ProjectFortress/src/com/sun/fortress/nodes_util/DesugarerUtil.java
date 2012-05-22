/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.*;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.Useful;

public class DesugarerUtil {

    private final static Span span = NodeFactory.internalSpan;

    // Distinct from Types.ANY_NAME because qualified names aren't yet supported
    public final static Id INTERNAL_ANY_NAME =
        NodeFactory.makeId(span, "Any");

    public final static Id LOOP_NAME =
        NodeFactory.makeId(span, WellKnownNames.loop);

    public  static VarRef GENERATE_NAME(Span span) {
        return ExprFactory.makeVarRef(span, WellKnownNames.generate);
    }

    public final static VarRef MAP_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.map);

    public final static VarRef SINGLETON_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.singleton);

    public final static VarRef NEST_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.nest);

    public final static VarRef COND_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.cond);

    public final static VarRef WHILECOND_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.whileCond);

    public final static VarRef BIGOP_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.bigOperator);

    public final static VarRef BIGOP2_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.bigOperator2);

    public final static VarRef FILTER_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.filter);

//    public final static VarRef Q_GENERATE_NAME =
//        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.generate);

    public final static VarRef Q_MAP_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.map);

    public final static VarRef Q_SINGLETON_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.singleton);

    public final static VarRef Q_NEST_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.nest);

    public final static VarRef Q_COND_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.cond);

    public final static VarRef Q_WHILECOND_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.whileCond);

    public final static VarRef Q_BIGOP_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.bigOperator);

    public final static VarRef Q_BIGOP2_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.bigOperator2);

    public final static VarRef Q_FILTER_NAME =
        ExprFactory.makeVarRef(span, WellKnownNames.fortressLibrary(), WellKnownNames.filter);

    /**
     * Used to generate temporary names when rewriting (for example)
     * tuple initializations.
     */
    private static int tempCount = 0;

    public static int genSerNo() {
        return (++tempCount);
    }
    
    public static String gensym(String prefix) {
        return prefix + Naming.GENERATED + (++tempCount);
    }

    public static String gensym() {
        return gensym("t");
    }

    public static String gensymFn(String prefix) {
        return prefix + Naming.GENERATED + "match";
    }

    public static Id gensymId(String prefix) {
        return gensymId(NodeFactory.internalSpan, prefix);
    }

    public static Id gensymId(Span s, String prefix) {
        return NodeFactory.makeId(s, gensym(prefix));
    }

    /**
     *  Given body, binds <- exp
     *  generate (fn binds => body)
     */
    public static Expr bindsAndBody(GeneratorClause g, Expr body) {
        List<Id> binds = g.getBind();
        List<Param> params = new ArrayList<Param>(binds.size());
        for (Id b : binds) params.add(NodeFactory.makeParam(b));
        Expr res = ExprFactory.makeFnExpr(NodeUtil.getSpan(g),params,body);
        return res;
    }

    /** Given a list of generators, return an expression that computes
     *  a composite generator given a reduction and a unit.  Desugars
     *  to fn(r,u) => D where D is as follows:
     *  exp | nothing       =>  __generate(exp,r,u)
     *  body | x <- exp     =>  __generate(exp,r,fn x => u(body))
     *  body | x <- exp, gs =>  __generate(exp,r,fn x => u(body)) | gs
     */
    public static Expr visitGenerators(Span span, List<GeneratorClause> gens, Expr body) {
        Id reduction = gensymId("reduction");
        VarRef redVar = ExprFactory.makeVarRef(reduction);
        Id unitFn = gensymId("unit");
        VarRef unitVar = ExprFactory.makeVarRef(unitFn);
        int i = gens.size();
        if (i==0) {
            /* Single generator as body, with no generator clauses. */
            body = ExprFactory.makeTightJuxt(span,
                                             GENERATE_NAME(span),
                                             ExprFactory.makeTupleExpr(span,body,redVar,unitVar));
        } else {
            List<GeneratorClause> squozenGens =
                new ArrayList<GeneratorClause>(gens.size());
            GeneratorClause prevGen = gens.get(0);
            for (int j = 1; j < i; j++) {
                GeneratorClause gen = gens.get(j);
                GeneratorClause squeeze = filterSqueezeOpportunity(prevGen,gen);
                if (squeeze!=null) {
                    prevGen = squeeze;
                } else {
                    squozenGens.add(prevGen);
                    prevGen=gen;
                }
            }
            squozenGens.add(prevGen);
            gens = squozenGens;

            TupleExpr innerAcc = null;
            /**
             * If the accumulation is like [ BIG OT [ f x | x <- xs] | xs <- gg]
             * nestedGeneratorOpportunity returns tuple (BIG OT, f)
             *
             * by Kento
             */
            innerAcc=nestedGeneratorOpportunity(gens,body);
            /**
             * If nestedGeneratorOpportunity returns the tuple (BIG OT, f),
             * this function returns tuple ((BIG OT, f), gg)
             * (should be refactored)
             *
             * by Kento
             */
            if(gens.size()==1 && innerAcc != null) {
                return ExprFactory.makeTupleExpr(NodeUtil.getSpan(body), innerAcc, gens.get(0).getInit());
            }

            i = gens.size();
            // Wrap the body in parentheses (as a singleton tuple) so that it can be considered
            // as the argument in a function application (denoted by the true argument).
            body = ExprFactory.makeTightJuxt(NodeUtil.getSpan(body), false,
                                             Useful.list(unitVar, body), true);
            for (i--; i>=0; i--) {
                body = oneGenerator(gens.get(i), redVar, body);
            }
        }
        return ExprFactory.makeFnExpr(span,
                       Useful.<Param>list(NodeFactory.makeParam(reduction),
                                          NodeFactory.makeParam(unitFn)),
                                      body);
    }

    /** Given a list of generator clauses and a position i, determine
     * if the i^th generator clause is a predicate that can be fused
     * with the first immediately preceding variable-labeled generator
     * clause.  This requires that the comprehension be of the form:
     * x <- gs, p(x) where p(x) is literally a function call, but where
     * x might actually be a tuple of variables.  That way we can pass
     * the literal function p to filter and make use of its
     * properties.  When we have full type checking, we can simply
     * unconditionally fuse predicates; that will be strictly better
     * than the present approach.
     */
    private static GeneratorClause filterSqueezeOpportunity(GeneratorClause prevGen,
                                                            GeneratorClause gen) {
        // Clause is predicate
        if (gen.getBind().size()!=0) return null;
        // Check shape of clause expression
        Expr predicate = gen.getInit();
        if (!(predicate instanceof Juxt)) return null;
        List<Expr>exprs = ((Juxt)predicate).getExprs();
        if (exprs.size() != 2) return null;
        Expr fn = exprs.get(0);
        if (!(fn instanceof VarRef)) return null;
        // Argument handling.  Handle (a,b,c) <- xs, p(a,b,c) as well.
        Expr arg = exprs.get(1);
        List<Expr> args;
        if (arg instanceof VarRef) {
            args = Collections.<Expr>singletonList(arg);
        } else if (arg instanceof TupleExpr) {
            args = ((TupleExpr) arg).getExprs();
        } else {
            return null;
        }
        // Find all the argument variables, and check that each matches the
        // corresponding variable in the binding of the previous generator.
        List<Id> binds = prevGen.getBind();
        int i = 0;
        if (binds.size() != args.size()) return null;
        for (Expr a : args) {
            if (!(a instanceof VarRef)) return null;
            if (!(binds.get(i).equals(((VarRef)a).getVarId()))) return null;
            i++;
        }
        // FILTER SQUEEZE.  Want to generate a new clause of the form
        // binds <- __filter(init,fn)
        Expr init = prevGen.getInit();
        Span span = NodeUtil.getSpan(gen);
        Expr filtered =
            ExprFactory.makeTightJuxt(span, FILTER_NAME,
                                      ExprFactory.makeTupleExpr(span,init,fn));
        GeneratorClause res =
            ExprFactory.makeGeneratorClause(NodeUtil.getSpan(prevGen), binds, filtered);
        return res;
    }

    /** Given outer generator clauses and inner body expression,
     * determine if there's an opportunity for generator-of-generator.
     * This is true if the generator expression has the form
     *   BIG OP1[gs1, xs <- gOfg] BIG OP2[x <- xs, gs2] body
     * In this case we should desugar into a generator-of-generators
     * which we can naively think of as this:
     *   BIG OP1[gs1] (BIG OP1[xs<-gOfg] BIG OP2 [x<-xs] (BIG OP2[gs2] body))
     * where the middle two are accomplished by a single call to
     * __generate2 with a pair of reductions.
     */
    /**
     * If the given clauses and body are of the form,
     * this function returns tuple (OP2, fn x => body).
     * Otherwise it returns null.
     * Also, if they are like BIG OP2 (BIG <||> [x <- xs, gs2]),
     * this function ignores the BIG <||> and returns (OP2, fn x => body).
     * This is because we want the following nested reduction to be desugared
     *  BIG OP1<| BIG OP2<| body | x <- xs |> | xs <- gg |> .
     *
     * by Kento
     */
    private static TupleExpr nestedGeneratorOpportunity(List<GeneratorClause> gens, Expr body) {
        // Make sure there are outer generators.
        int gs = gens.size();
        if (gs==0) return null;
        Op theOpr = null;
        Accumulator bodyAccum = null;
        List<StaticArg> staticArgs = null;
        /***
         * Check the form of the body.
         * We will accept the following.
         *   BIG OP2 (BIG <||> [ gs2 ])
         *   BIG OP2 [ gs2 ]
         */
        // body is of BIG OP2 (BIG <||> [x <- xs, gs2]) or BIG OP2 xs ?
        if ((body instanceof OpExpr)) {
            OpExpr bodyOpExp = (OpExpr)body;

            FunctionalRef ref = bodyOpExp.getOp();

            IdOrOp name = ref.getNames().get(0);
            // make sure the operator is actually an operator
            if ( ! (name instanceof Op) ) return null;
            theOpr = (Op)name;
            List<Expr> bodyOpExprArgs = bodyOpExp.getArgs();
            if(bodyOpExprArgs.size() != 1) return null;
            String str = theOpr.toString();
            String someBigOperatorName = "BIG";
            String theListEnclosingOperatorName = "BIG <| BIG |>";
            // make sure the body is of application of some big operator
            if (!(str.length() >= someBigOperatorName.length()
                  && str.substring(0, someBigOperatorName.length()).equals(someBigOperatorName))) return null;
            staticArgs = ref.getStaticArgs();
            if ((bodyOpExprArgs.get(0) instanceof Accumulator)) {
                /***
                 * This case is of BIG OP2 (BIG <||> [ gs2 ])
                 * The inner accumulator is bound to bodyAccum.
                 * The BIG OP2 is bound to theOpr above.
                 */
                bodyAccum = (Accumulator)bodyOpExprArgs.get(0);
                // make sure the inner accumulator is (BIG <||> [ gs2 ])
                if(!bodyAccum.getAccOp().toString().equals(theListEnclosingOperatorName)) return null;;
            } else if (bodyOpExprArgs.get(0) instanceof VarRef){
                /***
                 * This case is a direct application of a big operator.
                 * That is, the case  BIG OP1[gs1, xs <- gOfg] BIG OP2 xs.
                 * It returns tuple (BIG OP2, fn x => x) immediately.
                 */
                VarRef vr = (VarRef)bodyOpExprArgs.get(0);
                GeneratorClause outerGen = gens.get(gs-1);
                List<Id> outerVars = outerGen.getBind();
                if (outerVars.size()!=1) return null;
                Id outerVar = outerVars.get(0);
                if(!vr.getVarId().equals(outerVar)) return null;
                Id x = gensymId("x");

                Expr innerBody = ExprFactory.makeFnExpr(NodeUtil.getSpan(body),
                                                       Useful.<Param>list(NodeFactory.makeParam(x)),
                                                       ExprFactory.makeVarRef(x));
                return ExprFactory.makeTupleExpr(NodeUtil.getSpan(body), ExprFactory.makeOpExpr(NodeUtil.getSpan(body),theOpr,staticArgs), innerBody);

            } else {
                return null;
            }
        } else if (body instanceof Accumulator){
            /***
             * The body is an Accumulator,
             * that is BIG OP2 [ gs2 ]
             * The inner accumulator is bound to bodyAccum.
             * The BIG OP2 is bound to theOpr.
             */
            bodyAccum = (Accumulator)body;
            theOpr = bodyAccum.getAccOp();
            staticArgs = bodyAccum.getStaticArgs();
        } else {
            return null;
        }

        // Make sure innermost generator of outer accumulator yields a single variable.
        GeneratorClause outerGen = gens.get(gs-1);
        List<Id> outerVars = outerGen.getBind();

        if (outerVars.size()!=1) return null;
        Id outerVar = outerVars.get(0);
        // Find outermost generator of inner accumulator (might be body)
        List<GeneratorClause> bodyGens = bodyAccum.getGens();
        Expr bodyOuterInit;
        /***
         * Currently, the inner accumulator is limited to [x <- xs] body
         */
        if (bodyGens.size()==0) {  // This cannot occur now
            return null;
        } else if (bodyGens.size()==1) {
            GeneratorClause bodyOuterGen = bodyGens.get(0);
            bodyOuterInit = bodyOuterGen.getInit();
        } else {
            return null;   // Complex case is not accepted yet.
        }
        // Make sure innermost generator is a single variable matching outerVar
        if (!(bodyOuterInit instanceof VarRef)) return null;
        Id innerVar = ((VarRef)bodyOuterInit).getVarId();
        if (!outerVar.equals(innerVar)) return null;

        /**
         * Building a function (fn (x1, ..., xn) => body)
         *  for [ (x1, ..., xn) <- xs] body .
         */
        Expr innerBody = bodyAccum.getBody();
        List<Param> ps = new ArrayList<Param>();
        List<Id> ids = bodyGens.get(0).getBind();
        for(int i = 0; i < ids.size(); i++) {
            ps.add(NodeFactory.makeParam(ids.get(i)));
        }
        innerBody = ExprFactory.makeFnExpr(NodeUtil.getSpan(innerBody),
                                           ps,
                                           innerBody);
        /**
         * Return tuple (OP2, fn x => body)
         */
        return ExprFactory.makeTupleExpr(NodeUtil.getSpan(bodyAccum), ExprFactory.makeOpExpr(NodeUtil.getSpan(theOpr),theOpr,staticArgs), innerBody);
    }

    /**
     *  Given reduction of body | x <- exp, yields
     *  __generate(x,reduction,fn x => body)
     */
    private static Expr oneGenerator(GeneratorClause g, VarRef reduction, Expr body) {
        Expr loopBody = bindsAndBody(g, body);
        Span span = NodeUtil.spanTwo(reduction, loopBody);
        Expr params = ExprFactory.makeTupleExpr(span, g.getInit(), reduction, loopBody);
        return ExprFactory.makeTightJuxt(NodeUtil.getSpan(g), GENERATE_NAME(span), params);
    }
}
