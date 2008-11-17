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

package com.sun.fortress.compiler.desugarer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.DesugarerUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.interpreter.glue.WellKnownNames;
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
 *  3) Remwrite reductions to explicit invocations of big operators.
 */
public class PreDisambiguationDesugaringVisitor extends NodeUpdateVisitor {

    private final Id anyTypeId = new Id(NodeFactory.makeSpan("singleton"), WellKnownNames.anyTypeName);

    /** If the extends clause of a trait declaration, an object declaration, or
     *  an object expression is empty, then replace the empty extends clause
     *  with {Object}.
     */
    private List<TraitTypeWhere> rewriteExtendsClause(Node whence,
                                                      List<TraitTypeWhere> extendsClause) {
        if (extendsClause.size() > 0) return extendsClause;
        Id objectId = NodeFactory.makeId(whence.getSpan(),
                                         WellKnownNames.objectTypeName);
        TraitType typeObject = NodeFactory.makeTraitType(objectId);
        TraitTypeWhere extendsObject = NodeFactory.makeTraitTypeWhere(typeObject);
        return Collections.singletonList(extendsObject);
    }

    @Override
        public Node forObjectExprOnly(ObjectExpr that,
                                      Option<Type> exprType_result,
                                      List<TraitTypeWhere> extendsClause,
                                      List<Decl> decls) {
        extendsClause = rewriteExtendsClause(that, extendsClause);
        return super.forObjectExprOnly(that, exprType_result, extendsClause, decls);
    }

    @Override
        public Node forAbsTraitDeclOnly(AbsTraitDecl that,
                                        List<Modifier> mods,
                                        Id name,
                                        List<StaticParam> staticParams,
                                        List<TraitTypeWhere> extendsClause,
                                        Option<WhereClause> where,
                                        List<BaseType> excludes,
                                        Option<List<BaseType>> comprises,
                                        List<AbsDecl> decls) {
        if (!that.getName().equals(anyTypeId)) {
            extendsClause = rewriteExtendsClause(that, extendsClause);
        }
        return super.forAbsTraitDeclOnly(that, mods, name, staticParams, extendsClause,
                                         where, excludes, comprises, decls);
    }

    @Override
        public Node forTraitDeclOnly(TraitDecl that,
                                     List<Modifier> mods,
                                     Id name,
                                     List<StaticParam> staticParams,
                                     List<TraitTypeWhere> extendsClause,
                                     Option<WhereClause> where,
                                     List<BaseType> excludes,
                                     Option<List<BaseType>> comprises,
                                     List<Decl> decls) {
        if (!that.getName().equals(anyTypeId)) {
            extendsClause = rewriteExtendsClause(that, extendsClause);
        }
        return super.forTraitDeclOnly(that, mods, name, staticParams, extendsClause,
                                      where, excludes, comprises, decls);
    }

    @Override
        public Node forAbsObjectDeclOnly(AbsObjectDecl that,
                                         List<Modifier> mods,
                                         Id name,
                                         List<StaticParam> staticParams,
                                         List<TraitTypeWhere> extendsClause,
                                         Option<WhereClause> where,
                                         Option<List<Param>> params,
                                         Option<List<BaseType>> throwsClause,
                                         Option<Contract> contract,
                                         List<AbsDecl> decls) {
        extendsClause = rewriteExtendsClause(that, extendsClause);
        return super.forAbsObjectDeclOnly(that, mods, name, staticParams, extendsClause,
                                          where, params, throwsClause, contract, decls);
    }

    @Override
        public Node forObjectDeclOnly(ObjectDecl that,
                                      List<Modifier> mods,
                                      Id name,
                                      List<StaticParam> staticParams,
                                      List<TraitTypeWhere> extendsClause,
                                      Option<WhereClause> where,
                                      Option<List<Param>> params,
                                      Option<List<BaseType>> throwsClause,
                                      Option<Contract> contract,
                                      List<Decl> decls) {
        extendsClause = rewriteExtendsClause(that, extendsClause);
        return super.forObjectDeclOnly(that, mods, name, staticParams, extendsClause,
                                       where, params, throwsClause, contract, decls);
    }

    @Override
	public Node forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
        // If there is a colon at all, the operator is no longer ambiguous:
        // It must be infix.
        OpName op_name = IterUtil.first(that.getInfix_op().getOps());
        boolean prefix = OprUtil.hasPrefixColon(op_name);
        boolean suffix = OprUtil.hasSuffixColon(op_name);

        if( prefix || suffix ) {
            OpExpr new_op = new OpExpr(that.getSpan(), that.isParenthesized(), that.getInfix_op(), that.getArgs());
            return recur(new_op);
        }
        else {
            return super.forAmbiguousMultifixOpExpr(that);
        }
    }

    @Override
	public Node forOpExpr(OpExpr that) {
        OpRef op_result = (OpRef) recur(that.getOp());
        List<Expr> args_result = recurOnListOfExpr(that.getArgs());

        OpExpr new_op;
        if( op_result == that.getOp() && args_result == that.getArgs() ) {
            new_op = that;
        }
        else {
            new_op = new OpExpr(that.getSpan(), that.isParenthesized(), op_result, args_result);
        }
        return cleanupOpExpr(new_op);
    }

    private static Expr thunk(Expr e) {
        return ExprFactory.makeFnExpr(e.getSpan(),
                                      Collections.<Param>emptyList(), e);
    }

    private Expr cleanupOpExpr(OpExpr opExp) {
        OpRef ref = opExp.getOp();

        List<Expr> args = opExp.getArgs();

        if (args.size() <= 1) return opExp;
        OpName qop = IterUtil.first(ref.getOps());

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
        Span sp = opExp.getSpan();
        while (i.hasNext()) {
            Expr arg = (Expr)i.next();
            if (prefix) {
                res = thunk(res);
            }
            if (suffix) {
                arg = thunk(arg);
            }
            res = ExprFactory.makeOpExpr(sp,qop,res,arg);
        }
        return res;
    }

    @Override
	public Node forAccumulator(Accumulator that) {
        return visitAccumulator(that.getSpan(), that.getGens(),
                                that.getOpr(), that.getBody(),
                                that.getStaticArgs());
    }

    private Expr visitAccumulator(Span span, List<GeneratorClause> gens,
                                  OpName op, Expr body,
                                  List<StaticArg> staticArgs) {
        body = visitGenerators(span, gens, body);
        Expr opexp = ExprFactory.makeOpExpr(span,op,staticArgs);
        Expr res = new TightJuxt(span, false,
                                 Useful.list(BIGOP_NAME,
                                             ExprFactory.makeTuple(opexp,body)));
        return (Expr)recur(res);
    }
}
