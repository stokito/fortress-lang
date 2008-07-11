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

import java.io.File;
import junit.framework.TestCase;

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.TemplateGapExpr;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes._EllipsesExpr;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import com.sun.fortress.nodes_util.Printer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.BufferedWriter;

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

    public void testBasic1(){
        Node original;
        Node actual;
        Node expected;
        {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add( new _EllipsesExpr( new TemplateGapExpr( NodeFactory.makeId( "x" ), new ArrayList<Id>() ) ) );
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
            exprs.add( new _EllipsesExpr( new TemplateGapExpr( NodeFactory.makeId( "x" ), new ArrayList<Id>() ) ) );
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
        
}
