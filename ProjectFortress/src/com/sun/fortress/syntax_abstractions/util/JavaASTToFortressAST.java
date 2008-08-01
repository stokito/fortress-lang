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

package com.sun.fortress.syntax_abstractions.util;

import java.util.LinkedList;
import java.util.List;

import xtc.util.Pair;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AsIfExpr;
import com.sun.fortress.nodes.CharLiteralExpr;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.MultiFixity;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.InFixity;
import com.sun.fortress.nodes.LooseJuxt;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.Debug;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

/*
 * TODO implement a forCase for each node in the AST.
 */

public class JavaASTToFortressAST extends NodeDepthFirstVisitor<Expr> {

    private Span span;

    public JavaASTToFortressAST(Span span) {
        this.span = span;
    }

    public Expr dispatch(Object value, Option<Type> option) {
        // It is either the result of a optional
        if (option.isSome()) {
            Debug.debug( Debug.Type.SYNTAX, 1,"Dispatch on ", value, " and ", option.unwrap() );
            if (option.unwrap() instanceof TraitType) {
                TraitType it = (TraitType) option.unwrap();
                if (it.getName().equals(NodeFactory.makeId("FortressLibrary","Maybe"))) {
                    return handleOption(value, it);
                }
                // or the result of a repetition
                if ((value instanceof List) &&
                        it.getName().equals(NodeFactory.makeId("List","List"))) {
                    return handleRepetition((List) value, it);
                }

                /*
    if ( value instanceof List ){
        // return ActionRuntime.makeListAST( (List<Expr>) value );
        return handleRepetition((List) value, it );
    }
                 */
            }
        }

        /*
         */

        // It is a piece of AST
        if (value instanceof Node) {
            return handleNode((Node) value);
        }
        // it is one of the primitive types
        if (value instanceof String) {
            return NodeFactory.makeStringLiteralExpr((String)value);
        }
        if (value instanceof Integer) {
            return NodeFactory.makeIntLiteralExpr((Integer)value);
        }
        if (value instanceof Short) {
            return NodeFactory.makeIntLiteralExpr((Short)value);
        }
        if (value instanceof Byte) {
            return NodeFactory.makeIntLiteralExpr((Byte)value);
        }
        if (value instanceof Long) {
            return NodeFactory.makeIntLiteralExpr((Long)value);
        }
        if (value instanceof Character) {
            return NodeFactory.makeCharLiteralExpr((Character)value);
        }

        throw new RuntimeException("Wrong type: "+value.getClass());
    }

    private Expr handleNode(Node value) {
        return value.accept(this);
    }

    private Expr handleOption(Object value, TraitType it) {
        if (value == null) {
            return SyntaxAbstractionUtil.makeNoParamObjectInstantiation(this.span, SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.NOTHING, it.getArgs());
        }
        List<Expr> args = new LinkedList<Expr>();
        args.add(dispatch(value, Option.<Type>none()));
        return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, SyntaxAbstractionUtil.FORTRESSLIBRARY, SyntaxAbstractionUtil.JUST, args, it.getArgs());
    }

    private Expr handleRepetition(List value, TraitType type) {
        if (value.isEmpty()) {
            return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, "List", "emptyList", new LinkedList<Expr>(), type.getArgs());
        }
        List<OpName> ops = new LinkedList<OpName>();
        OpName op = NodeFactory.makeListOpName(this.span);
        ops.add(op);
        List<Expr> args = new LinkedList<Expr>();
        boolean first = true;
        for (Object o: value) {
            Node n = (Node) o;
            Debug.debug( Debug.Type.SYNTAX, 1, "Node: ", n.getClass());
            JavaASTToFortressAST jaTofss = new JavaASTToFortressAST(n.getSpan());
            Expr e = n.accept(jaTofss);
            if (first) {
                StaticArg t = IterUtil.first(type.getArgs());
                e = new AsIfExpr(e, ((TypeArg) t).getType());
                first = false;
            }
            args.add(e);
        }
        return new OpExpr(new OpRef(op, ops), args);
    }

    /*
 private Expr handleRepetition(Pair value, TraitType type) {
  if (value.list().isEmpty()) {
   return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, "List", "emptyList", new LinkedList<Expr>(), type.getArgs());
  }
  List<OpName> ops = new LinkedList<OpName>();
  ops.add(NodeFactory.makeListOpName(this.span));
  List<Expr> args = new LinkedList<Expr>();
  boolean first = true;
  for (Object o: value.list()) {
   Node n = (Node) o;
   Debug.debug( 1, "Node: "+n.getClass());
   JavaASTToFortressAST jaTofss = new JavaASTToFortressAST(n.getSpan());
   Expr e = n.accept(jaTofss);
   if (first) {
    StaticArg t = IterUtil.first(type.getArgs());
    e = new AsIfExpr(e, ((TypeArg) t).getType());
    first = false;
   }
   args.add(e);
  }
  return new OpExpr(new OpRef(ops), args);
 }
     */

    @Override
    public Expr forStringLiteralExpr(StringLiteralExpr that) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(that);
        return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, "FortressAst", "StringLiteralExpr", args, new LinkedList<StaticArg>());
    }

    @Override
    public Expr forIntLiteralExpr(IntLiteralExpr that) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(that);
        return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, "FortressAst", "IntLiteralExpr", args, new LinkedList<StaticArg>());
    }

    @Override
    public Expr forAPINameOnly(APIName that, List<Expr> ids_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeList(this.span, ids_result, "Id"));
        return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, "FortressAst", "APIName", args, new LinkedList<StaticArg>());
    }

    @Override
    public Expr forCharLiteralExprOnly(CharLiteralExpr that, Option<Expr> exprType_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(that);
        return SyntaxAbstractionUtil.makeObjectInstantiation(this.span, "FortressAst", "CharLiteralExpr", args, new LinkedList<StaticArg>());
    }

    @Override
    public Expr forEnclosingFixityOnly(EnclosingFixity that) {
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "EnclosingFixity", new LinkedList<Expr>());
    }

    @Override
    public Expr forEnclosingOnly(Enclosing that, Option<Expr> api_result, Expr open_result,
            Expr close_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeMaybe(this.span, api_result, "ApiName"));
        args.add(open_result);
        args.add(close_result);
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "Enclosing", args );
    }

    @Override
    public Expr forFnRefOnly(FnRef that, Option<Expr> exprType_result,
                             Expr originalName_result,
                             List<Expr> fns_result,
                             List<Expr> staticArgs_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeList(this.span, fns_result, "Id"));
        args.add(SyntaxAbstractionUtil.makeList(this.span, staticArgs_result, "StaticArg"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "FnRef", args);
    }

    @Override
    public Expr forId(Id that) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(new StringLiteralExpr(that.getText()));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "Id", args);
    }

    @Override
    public Expr forLooseJuxtOnly(LooseJuxt that, Option<Expr> exprType_result, Expr multiJuxt_result,
            Expr infixJuxt_result,
            List<Expr> exprs_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeList(this.span, exprs_result, "Expr"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "LooseJuxt", args);
    }

    @Override
    public Expr forMultiFixityOnly(MultiFixity that) {
        List<Expr> args = new LinkedList<Expr>();
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "MultiFixity", args);
    }

    @Override
    public Expr forInFixity(InFixity that) {
        List<Expr> args = new LinkedList<Expr>();
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "InFixity", args);
    }

    @Override
    public Expr forOpOnly(Op that, Option<Expr> api_result, Option<Expr> fixity_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeMaybe(this.span, api_result, "ApiName"));
        args.add(SyntaxAbstractionUtil.makeMaybe(this.span, fixity_result, "Fixity"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "Op", args);
    }

    @Override
    public Expr forOpRefOnly(OpRef that, Option<Expr> exprType_result,
                             Expr originalName_result,
                             List<Expr> ops_result,
            List<Expr> staticArgs_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeList(this.span, ops_result, "OpName"));
        args.add(SyntaxAbstractionUtil.makeList(this.span, staticArgs_result, "StaticArg"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "OpRef", args);
    }

    @Override
    public Expr forOpExprOnly(OpExpr that, Option<Expr> exprType_result, Expr op_result,
            List<Expr> args_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(op_result);
        args.add(SyntaxAbstractionUtil.makeList(this.span, args_result, "Expr"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "OpExpr", args);
    }

    @Override
    public Expr forIdOnly(Id that,
            Option<Expr> api_result) {
        return handleId("Id", api_result);
    }

    private Expr handleId(String objectName, Option<Expr> api_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeMaybe(this.span, api_result, "Expr"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", objectName, args);
    }

    @Override
    public Expr forTightJuxtOnly(TightJuxt that, Option<Expr> exprType_result, Expr multiJuxt_result,
                                 Expr infixJuxt_result,
                                 List<Expr> exprs_result) {
        List<Expr> args = new LinkedList<Expr>();
        args.add(SyntaxAbstractionUtil.makeList(this.span, exprs_result, "Expr"));
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "TightJuxt", args);
    }

    @Override
    public Expr forVoidLiteralExpr(VoidLiteralExpr that) {
        return SyntaxAbstractionUtil.makeVoidObjectInstantiation(this.span, "FortressAst", "VoidLiteralExpr", new LinkedList<Expr>());
    }
}
