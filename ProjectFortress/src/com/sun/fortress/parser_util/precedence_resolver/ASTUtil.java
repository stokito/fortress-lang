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

import static com.sun.fortress.exceptions.ProgramError.error;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.nodes.AmbiguousMultifixOpExpr;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Link;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.parser_util.precedence_opexpr.RealExpr;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.PureList;

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
        return ExprFactory.makeOpExpr(span, NodeFactory.makeOpInfix(op),
                                       left, right);
    }   
    
    // let prefix (span : span) (op : op) (arg : expr) : expr =
    //     opr span (node op.node_span (`Opr op)) [arg]
    static Expr prefix(Op op, Expr arg) {
        return ExprFactory.makeOpExpr(arg.getSpan(),
                                       NodeFactory.makeOpPrefix(op), arg);
    }

    // let postfix (span : span) (arg : expr) (op : op) : expr =
    //   opr span (node op.node_span (`Postfix op)) [arg]
    public static Expr postfix(Span span, Expr arg, Op op) {
        return ExprFactory.makeOpExpr(span, NodeFactory.makeOpPostfix(op), arg);
    }

    // let multifix (span : span) (op : op) (args : expr list) : expr =
    //   opr span (node op.node_span (`Opr op)) args
    static Expr multifix(Span span, Op op, List<Expr> args) {
        Op infix_op_ = NodeFactory.makeOpInfix(op);
        OpRef infix_op = new OpRef(op.getSpan(), infix_op_,  Collections.<OpName>singletonList(infix_op_)); 
        
        Op multifix_op_ = NodeFactory.makeOpMultifix(op);
        OpRef multifix_op = new OpRef(op.getSpan(), multifix_op_,  Collections.<OpName>singletonList(multifix_op_));
        
        if (args.size() > 2) {
        	return new AmbiguousMultifixOpExpr(span, false, infix_op, multifix_op, args);
        } 
        else if (args.size() == 2) {
        	return new OpExpr(span, false, infix_op, args);
              
        }
        else {
        	return error(op, "Operator fixity is invalid in its application.");
        }
    }

    // let enclosing (span : span) (left : op) (args : expr list) (right : op) : expr =
    //     opr span (node (span_two left right) (`Enclosing (left,right))) args
    public static Expr enclosing(Span span, Op left, List<Expr> args, Op right) {
        return enclosing(span, left, Collections.<StaticArg>emptyList(),
                         args, right);
    }

    public static Expr enclosing(Span span, Op left, List<StaticArg> sargs,
                                 List<Expr> args, Op right) {
        if (PrecedenceMap.ONLY.matchedBrackets(left.getText(), right.getText())) {
            Span s = FortressUtil.spanTwo(left, right);
            Enclosing en = new Enclosing(s, left, right);
            OpRef ref = new OpRef(s,
                    en,
                                  Collections.<OpName>singletonList(en),
                                  sargs);
            return new OpExpr(span, false, ref, args);
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
    static Expr chain(Span span, Expr first, List<Link> links) {
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
