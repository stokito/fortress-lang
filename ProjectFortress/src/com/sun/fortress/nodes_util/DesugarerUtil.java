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

package com.sun.fortress.nodes_util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.*;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.useful.Useful;

public class DesugarerUtil {

    // Distinct from Types.ANY_NAME because qualified names aren't yet supported
    public final static Id INTERNAL_ANY_NAME =
        NodeFactory.makeId("Any");

    public final static Id LOOP_NAME =
        NodeFactory.makeId(WellKnownNames.loop);

    public final static VarRef GENERATE_NAME =
        ExprFactory.makeVarRef(WellKnownNames.generate);

    public final static VarRef MAP_NAME =
        ExprFactory.makeVarRef(WellKnownNames.map);

    public final static VarRef SINGLETON_NAME =
        ExprFactory.makeVarRef(WellKnownNames.singleton);

    public final static VarRef NEST_NAME =
        ExprFactory.makeVarRef(WellKnownNames.nest);

    public final static VarRef COND_NAME =
        ExprFactory.makeVarRef(WellKnownNames.cond);

    public final static VarRef WHILECOND_NAME =
        ExprFactory.makeVarRef(WellKnownNames.whileCond);

    public final static VarRef BIGOP_NAME =
        ExprFactory.makeVarRef(WellKnownNames.bigOperator);

    public final static VarRef BIGOP2_NAME =
        ExprFactory.makeVarRef(WellKnownNames.bigOperator2);

    public final static VarRef FILTER_NAME =
        ExprFactory.makeVarRef(WellKnownNames.filter);

    public final static VarRef Q_GENERATE_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.generate);

    public final static VarRef Q_MAP_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.map);

    public final static VarRef Q_SINGLETON_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.singleton);

    public final static VarRef Q_NEST_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.nest);

    public final static VarRef Q_COND_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.cond);

    public final static VarRef Q_WHILECOND_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.whileCond);

    public final static VarRef Q_BIGOP_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.bigOperator);

    public final static VarRef Q_BIGOP2_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.bigOperator2);

    public final static VarRef Q_FILTER_NAME =
        ExprFactory.makeVarRef(WellKnownNames.fortressLibrary, WellKnownNames.filter);

    /**
     * Used to generate temporary names when rewriting (for example)
     * tuple initializations.
     */
    private static int tempCount = 0;

    public static String gensym(String prefix) {
        return prefix + "$" + (++tempCount);
    }

    public static String gensym() {
        return gensym("t");
    }

    public static Id gensymId(String prefix) {
        return NodeFactory.makeId(gensym(prefix));
    }

    /**
     *  Given body, binds <- exp
     *  generate (fn binds => body)
     */
    public static Expr bindsAndBody(GeneratorClause g, Expr body) {
        List<Id> binds = g.getBind();
        List<Param> params = new ArrayList<Param>(binds.size());
        for (Id b : binds) params.add(NodeFactory.makeParam(b));
        Expr res = ExprFactory.makeFnExpr(g.getSpan(),params,body);
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
            body = new TightJuxt(span, false,
                             Useful.list(GENERATE_NAME,
                                         ExprFactory.makeTuple(body,redVar,unitVar)));
        } else {
            List<GeneratorClause> squozenGens =
                new ArrayList<GeneratorClause>(gens.size());
            GeneratorClause prevGen = gens.get(0);
            for (int j = 1; j < i; j++) {
                GeneratorClause gen = gens.get(j);
                GeneratorClause squeeze = filterSqueezeOpportunity(prevGen,gen);
                if (squeeze!=null) {
                    // System.out.println(gen+": Filter squeeze opportunity\n"+
                    //                    squeeze.toStringVerbose());
                    prevGen = squeeze;
                } else {
                    squozenGens.add(prevGen);
                    prevGen=gen;
                }
            }
            squozenGens.add(prevGen);
            gens = squozenGens;
            i = gens.size();
            if (nestedGeneratorOpportunity(gens,body)) {
                System.out.println(span+" and\n"+body.getSpan()+
                                   ": Generator of generator opportunity?");
            }
            // Wrap the body in parentheses (as a singleton tuple) so that it can be considered
            // as the argument in a function application (denoted by the true argument).
            body = new TightJuxt(body.getSpan(),
                                 false,
                                 Useful.list(unitVar, body),
                                 true);
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
            if (!(binds.get(i).equals(((VarRef)a).getVar()))) return null;
            i++;
        }
        // FILTER SQUEEZE.  Want to generate a new clause of the form
        // binds <- __filter(init,fn)
        Expr init = prevGen.getInit();
        Expr filtered =
            ExprFactory.makeTightJuxt(gen.getSpan(), FILTER_NAME,
                                      ExprFactory.makeTuple(init,fn));
        GeneratorClause res =
            ExprFactory.makeGeneratorClause(prevGen.getSpan(), binds, filtered);
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
    private static boolean nestedGeneratorOpportunity(List<GeneratorClause> gens, Expr body) {
        // Make sure there are outer generators.
        int gs = gens.size();
        if (gs==0) return false;
        // Make sure body is an Accumulator expression.
        if (!(body instanceof Accumulator)) return false;
        Accumulator bodyAccum = (Accumulator)body;
        // Make sure innermost generator of outer accumulator yields a single variable.
        GeneratorClause outerGen = gens.get(gs-1);
        List<Id> outerVars = outerGen.getBind();
        if (outerVars.size()!=1) return false;
        Id outerVar = outerVars.get(0);
        // Find outermost generator of inner accumulator (might be body)
        List<GeneratorClause> bodyGens = bodyAccum.getGens();
        Expr bodyOuterInit;
        if (bodyGens.size()==0) {
            bodyOuterInit = bodyAccum.getBody();
        } else {
            GeneratorClause bodyOuterGen = bodyGens.get(0);
            bodyOuterInit = bodyOuterGen.getInit();
        }
        // Make sure innermost generator is a single variable matching outerVar
        if (!(bodyOuterInit instanceof VarRef)) return false;
        Id innerVar = ((VarRef)bodyOuterInit).getVar();
        return (outerVar.equals(innerVar));
    }

    /**
     *  Given reduction of body | x <- exp, yields
     *  __generate(x,reduction,fn x => body)
     */
    private static Expr oneGenerator(GeneratorClause g, VarRef reduction, Expr body) {
        Expr loopBody = bindsAndBody(g, body);
        Expr params = ExprFactory.makeTuple(g.getInit(), reduction, loopBody);
        return new TightJuxt(g.getSpan(), false,
                             Useful.list(GENERATE_NAME,params));
    }
}
