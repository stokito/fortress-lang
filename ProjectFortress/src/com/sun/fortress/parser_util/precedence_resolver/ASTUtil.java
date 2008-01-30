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

/*
 * Utility functions for the Fortress com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util.precedence_resolver;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Collections;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.parser_util.precedence_opexpr.RealExpr;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.PureList;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

// From Fortress/interpreter/ast/ast_utils.ml
public class ASTUtil {

    // let nofix (span : span) (op : op) : expr =
    //   opr span (node op.node_span (`Opr op)) []
    public static Expr nofix(Span span, Op op) {
        return ExprFactory.makeOprExpr(span, NodeFactory.makeOpNofix(op));
    }

    // let infix (span : span) (left : expr) (op : op) (right : expr) : expr =
    //   opr span (node op.node_span (`Opr op)) [left; right]
    public static Expr infix(Span span, Expr left, Op op, Expr right) {
        return ExprFactory.makeOprExpr(span, NodeFactory.makeOpInfix(op),
                                       left, right);
    }

    // let prefix (span : span) (op : op) (arg : expr) : expr =
    //     opr span (node op.node_span (`Opr op)) [arg]
    static Expr prefix(Op op, Expr arg) {
        return ExprFactory.makeOprExpr(arg.getSpan(),
                                       NodeFactory.makeOpPrefix(op), arg);
    }

    // let postfix (span : span) (arg : expr) (op : op) : expr =
    //   opr span (node op.node_span (`Postfix op)) [arg]
    public static Expr postfix(Span span, Expr arg, Op op) {
        return ExprFactory.makeOprExpr(span, NodeFactory.makeOpPostfix(op), arg);
    }

    // let multifix (span : span) (op : op) (args : expr list) : expr =
    //   opr span (node op.node_span (`Opr op)) args
    static Expr multifix(Span span, Op op, List<Expr> args) {
        Op opr;
        if (args.size() > 2)
            opr = NodeFactory.makeOpMultifix(op);
        else if (args.size() == 2)
            opr = NodeFactory.makeOpInfix(op);
        else
            opr = error(op, "Operator fixity is invalid in its application.");
        QualifiedOpName qName =
            new QualifiedOpName(op.getSpan(), Option.<APIName>none(), opr);
        OpRef ref = new OpRef(op.getSpan(),
                              Collections.<QualifiedOpName>singletonList(qName));
        return new OprExpr(span, false, ref, args);
    }

    // let enclosing (span : span) (left : op) (args : expr list) (right : op) : expr =
    //     opr span (node (span_two left right) (`Enclosing (left,right))) args
    public static Expr enclosing(Span span, Op left, List<Expr> args, Op right) {
        if (PrecedenceMap.ONLY.matchedBrackets(left.getText(), right.getText())) {
            Span s = FortressUtil.spanTwo(left, right);
            Enclosing en = new Enclosing(s, left, right);
            QualifiedOpName qName = new QualifiedOpName(s, Option.<APIName>none(),
                                                        new Enclosing(s, left, right));
            OpRef ref = new OpRef(s,
                                  Collections.<QualifiedOpName>singletonList(qName));
            return new OprExpr(span, false, ref, args);
        } else {
            return error(right, "Mismatched Enclosers: " +
                         left.getText() + " and " + right.getText());
        }
    }

    // let chain (span : span) (first : expr) (links : (op * expr) list) : expr =
    //   node span
    //     (`ChainExpr
    //        (node span
    //           { chain_expr_first = first;
    //             chain_expr_links = links; }))
    static Expr chain(Span span, Expr first, List<Pair<Op, Expr>> links) {
        return new ChainExpr(span, false, first, links);
    }

    // let loose (exprs : expr list) : expr =
    //     node (span_all exprs) (`LooseJuxt exprs)
    static Expr loose(PureList<RealExpr> exprs) {
        PureList<Expr> _exprs =
            exprs.map(new Fn<RealExpr, Expr>() {
                public Expr apply(RealExpr e) {
                    return e.getExpr();
                }
            });
        return new LooseJuxt(spanAll(exprs), false, _exprs.toJavaList());
    }

    static Span spanAll(PureList<RealExpr> exprs) {
        int size = exprs.size();
        if (size == 0) return new Span();
        else { // size != 0
            Object[] _exprs = exprs.toArray();
            return new Span(((RealExpr)Array.get(_exprs,0)).getExpr().
                                             getSpan().getBegin(),
                            ((RealExpr)Array.get(_exprs,size-1)).getExpr().
                                             getSpan().getEnd());
        }
    }
}
