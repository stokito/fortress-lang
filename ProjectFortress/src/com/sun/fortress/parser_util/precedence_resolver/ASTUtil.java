/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Utility functions for the Fortress com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util.precedence_resolver;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.precedence_opexpr.RealExpr;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.PureList;

import java.io.BufferedWriter;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

// From Fortress/interpreter/ast/ast_utils.ml
public class ASTUtil {

    // let nofix (span : span) (op : op) : expr =
    //   opr span (node op.node_span (`Opr op)) []
    public static Expr nofix(Span span, Op op) {
        return ExprFactory.makeOpExpr(span, NodeFactory.makeOpNofix(op));
    }

    // let infix (span : span) (left : expr) (op : op) (right : expr) : expr =
    //   opr span (node op.node_span (`Opr op)) [left; right]
    public static Expr infix(Span span, Expr left, Op op, Expr right) {
        return ExprFactory.makeOpExpr(span, NodeFactory.makeOpInfix(op), left, right);
    }

    // let prefix (span : span) (op : op) (arg : expr) : expr =
    //     opr span (node op.node_span (`Opr op)) [arg]
    static Expr prefix(Op op, Expr arg) {
        return ExprFactory.makeOpExpr(NodeUtil.getSpan(arg), NodeFactory.makeOpPrefix(op), arg);
    }

    // let postfix (span : span) (arg : expr) (op : op) : expr =
    //   opr span (node op.node_span (`Postfix op)) [arg]
    public static Expr postfix(Span span, Expr arg, Op op) {
        return ExprFactory.makeOpExpr(span, NodeFactory.makeOpPostfix(op), arg);
    }

    // let multifix (span : span) (op : op) (args : expr list) : expr =
    //   opr span (node op.node_span (`Opr op)) args
    static Expr multifix(BufferedWriter writer, Span span, Op op, List<Expr> args) {
        Op infix_op_ = NodeFactory.makeOpInfix(op);
        FunctionalRef infix_op = ExprFactory.makeOpRef(infix_op_);

        Op multifix_op_ = NodeFactory.makeOpMultifix(op);
        FunctionalRef multifix_op = ExprFactory.makeOpRef(multifix_op_);

        if (args.size() > 2) {
            return ExprFactory.makeAmbiguousMultifixOpExpr(span, infix_op, multifix_op, args);
        } else if (args.size() == 2) {
            return ExprFactory.makeOpExpr(span, infix_op, args);
        } else {
            NodeUtil.log(writer, span, "Operator fixity is invalid in its application.");
            return ExprFactory.makeVoidLiteralExpr(span);
        }
    }

    // let enclosing (span : span) (left : op) (args : expr list) (right : op) : expr =
    //     opr span (node (span_two left right) (`Enclosing (left,right))) args
    public static Expr enclosing(BufferedWriter writer, Span span, Op left, List<Expr> args, Op right) {
        return enclosing(writer, span, left, Collections.<StaticArg>emptyList(), args, right);
    }

    public static Expr enclosing(BufferedWriter writer,
                                 Span span,
                                 Op left,
                                 List<StaticArg> sargs,
                                 List<Expr> args,
                                 Op right) {
        if (PrecedenceMap.ONLY.matchedBrackets(left.getText(), right.getText())) {
            Span s = NodeUtil.spanTwo(left, right);
            Op en = NodeFactory.makeEnclosing(s, left.getText(), right.getText());
            FunctionalRef ref = ExprFactory.makeOpRef(en, sargs);
            return ExprFactory.makeOpExpr(span, ref, args);
        } else {
            NodeUtil.log(writer, span, "Mismatched Enclosers: " + left.getText() + " and " + right.getText());
            return ExprFactory.makeVoidLiteralExpr(span);
        }
    }

    // let chain (span : span) (first : expr) (links : (op * expr) list) : expr =
    //   node span
    //     (`ChainExpr
    //        (node span
    //           { chain_expr_first = first;
    //             chain_expr_links = links; }))
    static Expr chain(Span span, Expr first, List<Link> links) {
        return ExprFactory.makeChainExpr(span, first, links);
    }

    // let loose (exprs : expr list) : expr =
    //     node (span_all exprs) (`LooseJuxt exprs)
    static Expr loose(PureList<RealExpr> exprs) {
        PureList<Expr> _exprs = exprs.map(new Fn<RealExpr, Expr>() {
            public Expr apply(RealExpr e) {
                return e.getExpr();
            }
        });
        if (exprs.isEmpty()) return bug("Cannot make a loose juxtaposition from an empty list of expression.");
        else return ExprFactory.makeLooseJuxt(spanAll(exprs), _exprs.toJavaList());
    }

    static Span spanAll(PureList<RealExpr> exprs) {
        int size = exprs.size();
        if (size == 0) return bug("Cannot create a span from an empty list of nodes.");
        else { // size != 0
            Object[] _exprs = exprs.toArray();
            return new Span(NodeUtil.getSpan(((RealExpr) Array.get(_exprs, 0)).getExpr()).getBegin(),
                            NodeUtil.getSpan(((RealExpr) Array.get(_exprs, size - 1)).getExpr()).getEnd());
        }
    }
}
