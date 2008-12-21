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

package com.sun.fortress.syntax_abstractions.phases;

import junit.framework.TestCase;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Span;

import java.util.List;
import java.util.ArrayList;

public class EllipsesJUTest extends TestCase {

    static Span span = NodeFactory.makeSpan("EllipsesJUTest");

    private <T> List<T> mkList( T... e ){
        List<T> list = new ArrayList<T>();
        for ( T t : e ){
            list.add( t );
        }
        return list;
    }

    private List<Expr> mkExprList( Expr... e ){
        return mkList(e);
    }

    private TemplateGapExpr mkTemplate( String id ){
        return new TemplateGapExpr(NodeFactory.makeExprInfo(span), NodeFactory.makeId( span, id ), new ArrayList<Id>() );
    }

    public void testBasic1(){
        Node original;
        Node actual;
        Node expected;
        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr(NodeFactory.makeExprInfo(span), mkTemplate( "x" ) ) );
            original = ExprFactory.makeTightJuxt(span, exprs);
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( span, "x" ), 1, mkList(ExprFactory.makeStringLiteralExpr(span,  "hello" )) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( ExprFactory.makeStringLiteralExpr(span,  "hello" ) );
            expected = ExprFactory.makeTightJuxt( span, exprs );
        }

        assertEquals( actual, expected );
    }

    public void testBasic2(){
        Node original;
        Node actual;
        Node expected;
        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr(NodeFactory.makeExprInfo(span), mkTemplate( "x" ) ) );
            original = ExprFactory.makeTightJuxt(span, exprs);
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( span, "x" ), 1, mkList(ExprFactory.makeStringLiteralExpr(span, "hello" ), ExprFactory.makeStringLiteralExpr(span, "goodbye" ) ) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            expected = ExprFactory.makeTightJuxt( span,
                                                  ExprFactory.makeStringLiteralExpr(span, "hello" ),
                                                  ExprFactory.makeStringLiteralExpr(span, "goodbye" ) );
        }

        assertEquals( actual, expected );
    }

    public void testComplex1(){
        Node original;
        Node actual;
        Node expected;

        {
            List<Expr> blocks = new ArrayList<Expr>();
            blocks.add( ExprFactory.makeStringLiteralExpr(span, "hi" ) );
            blocks.add( mkTemplate( "x" ) );
            Expr extra = ExprFactory.makeBlock(span,  blocks );
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr( NodeFactory.makeExprInfo(span), extra ) );
            original = ExprFactory.makeTightJuxt(span, exprs );
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( span, "x" ), 1, mkList( ExprFactory.makeStringLiteralExpr(span, "a"), ExprFactory.makeStringLiteralExpr(span, "b" ) ) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( ExprFactory.makeBlock(span, mkExprList( ExprFactory.makeStringLiteralExpr(span, "hi" ),
                                              ExprFactory.makeStringLiteralExpr(span, "a" ) ) ) );
            exprs.add( ExprFactory.makeBlock(span,  mkExprList( ExprFactory.makeStringLiteralExpr(span, "hi" ),
                                              ExprFactory.makeStringLiteralExpr(span, "b" ) ) ) );
            expected = ExprFactory.makeTightJuxt(span, exprs );
        }

        assertEquals( actual, expected );
    }

    public void testComplex2(){
        Node original;
        Node actual;
        Node expected;

        {
            List<Expr> extra = mkExprList(mkTemplate("i"), mkTemplate("j"));
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( ExprFactory.makeStringLiteralExpr(span, "bar" ) );
            exprs.add( new _EllipsesExpr(NodeFactory.makeExprInfo(span), ExprFactory.makeTightJuxt(span, extra ) ) );
            original = ExprFactory.makeTightJuxt(span, exprs );
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( span, "i" ), 0, ExprFactory.makeStringLiteralExpr(span, "a" ) );
            env.add( NodeFactory.makeId( span, "j" ), 1, mkList( ExprFactory.makeStringLiteralExpr(span, "1" ),
                                                           ExprFactory.makeStringLiteralExpr(span, "2" ),
                                                           ExprFactory.makeStringLiteralExpr(span, "3" ) ) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            expected = ExprFactory.makeTightJuxt(span,
                                     mkExprList( ExprFactory.makeStringLiteralExpr(span, "bar" ),
                                                 ExprFactory.makeTightJuxt(span,
                                                                           ExprFactory.makeStringLiteralExpr(span, "a"),
                                                                           ExprFactory.makeStringLiteralExpr(span, "1")),
                                                 ExprFactory.makeTightJuxt(span,
                                                                           ExprFactory.makeStringLiteralExpr(span, "a"),
                                                                           ExprFactory.makeStringLiteralExpr(span, "2")),
                                                 ExprFactory.makeTightJuxt(span,
                                                                           ExprFactory.makeStringLiteralExpr(span, "a"),
                                                                           ExprFactory.makeStringLiteralExpr(span, "3"))));
        }

        assertEquals( actual, expected );
    }

}
