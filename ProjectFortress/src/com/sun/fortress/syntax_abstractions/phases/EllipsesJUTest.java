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
import com.sun.fortress.nodes_util.Span;

import java.util.List;
import java.util.ArrayList;

public class EllipsesJUTest extends TestCase {

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
        return new TemplateGapExpr(new Span(), NodeFactory.makeId( id ), new ArrayList<Id>() );
    }

    public void testBasic1(){
        Node original;
        Node actual;
        Node expected;
        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr(new Span(), mkTemplate( "x" ) ) );
            original = new TightJuxt(new Span(), false, exprs); 
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( "x" ), 1, mkList(new StringLiteralExpr( "hello" )) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new StringLiteralExpr( "hello" ) );
            expected = new TightJuxt( new Span(), false, exprs );
        }

        assertEquals( actual, expected );
    }

    public void testBasic2(){
        Node original;
        Node actual;
        Node expected;
        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr(new Span(), mkTemplate( "x" ) ) );
            original = new TightJuxt(new Span(), false, exprs); 
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( "x" ), 1, mkList(new StringLiteralExpr( "hello" ), new StringLiteralExpr( "goodbye" ) ) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new StringLiteralExpr( "hello" ) );
            exprs.add( new StringLiteralExpr( "goodbye" ) );
            expected = new TightJuxt( new Span(), false, exprs );
        }

        assertEquals( actual, expected );
    }

    public void testComplex1(){
        Node original;
        Node actual;
        Node expected;

        {
            List<Expr> blocks = new ArrayList<Expr>();
            blocks.add( new StringLiteralExpr( "hi" ) );
            blocks.add( mkTemplate( "x" ) );
            Expr extra = new Block( blocks );
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr( new Span(), extra ) );
            original = new TightJuxt(new Span(), false, exprs );
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( "x" ), 1, mkList( new StringLiteralExpr("a"), new StringLiteralExpr( "b" ) ) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new Block( mkExprList( new StringLiteralExpr( "hi" ),
                                              new StringLiteralExpr( "a" ) ) ) );
            exprs.add( new Block( mkExprList( new StringLiteralExpr( "hi" ),
                                              new StringLiteralExpr( "b" ) ) ) );
            expected = new TightJuxt(new Span(), false, exprs );
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
            exprs.add( new StringLiteralExpr( "bar" ) );
            exprs.add( new _EllipsesExpr(new Span(), new TightJuxt(new Span(), false, extra ) ) );
            original = new TightJuxt(new Span(), false, exprs );
            EllipsesEnvironment env = new EllipsesEnvironment();
            env.add( NodeFactory.makeId( "i" ), 0, new StringLiteralExpr( "a" ) );
            env.add( NodeFactory.makeId( "j" ), 1, mkList( new StringLiteralExpr( "1" ),
                                                           new StringLiteralExpr( "2" ),
                                                           new StringLiteralExpr( "3" ) ) );
            actual = original.accept( new EllipsesVisitor( env ) );
        }

        {
            expected = new TightJuxt(new Span(), false,
                                     mkExprList( new StringLiteralExpr( "bar" ),
                                                 new TightJuxt(new Span(), false,
                                                               mkExprList( new StringLiteralExpr("a"),
                                                                           new StringLiteralExpr("1"))),
                                                 new TightJuxt(new Span(), false,
                                                               mkExprList( new StringLiteralExpr("a"),
                                                                           new StringLiteralExpr("2"))),
                                                 new TightJuxt(new Span(), false,
                                                               mkExprList( new StringLiteralExpr("a"),
                                                                           new StringLiteralExpr("3")))));
        }

        assertEquals( actual, expected );
    }
        
}
