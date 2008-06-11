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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.index.NonterminalDefIndex;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalHeader;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.environments.GrammarEnv;
import com.sun.fortress.syntax_abstractions.environments.MemberEnv;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;

import junit.framework.TestCase;

public class TemplateVarRewriterJUTest extends TestCase {

    
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Id name = NodeFactory.makeId("Expr");
        List<Pair<Id, Id>> params = new LinkedList<Pair<Id,Id>>();
        Option<Type> type = Option.<Type>wrap(new VarType(name));
        NonterminalHeader header = new NonterminalHeader(name, params, type);
        Option<BaseType> astType1 = Option.<BaseType>wrap(new VarType(name));
        NonterminalDef nd = new NonterminalDef(header, astType1, new LinkedList<SyntaxDef>());
        GrammarEnv.add(NodeFactory.makeId("Expr"), new MemberEnv(new NonterminalDefIndex(Option.wrap(nd))));
    }

    public void testRewriteVars1() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        String r= tvr.rewriteVars(vars, "foo");
        assertTrue(r.equals("foo"));
    }

    public void testRewriteVars2() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        vars.put(NodeFactory.makeId("foo"), new VarType(NodeFactory.makeId("Expr")));
        String r = tvr.rewriteVars(vars, "foo");
        assertTrue(r.startsWith(TemplateVarRewriter.GAPSYNTAXPREFIX) && 
                r.endsWith(TemplateVarRewriter.GAPSYNTAXSUFFIX));
    }

    public void testRewriteVars3() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); vars.put(NodeFactory.makeId("bar"), expr); vars.put(NodeFactory.makeId("Baz"), expr);
        FreshName.reset();
        String r = tvr.rewriteVars(vars, "a print ( foo Baz ) bar");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz", "Expr");
        String v2 = TemplateVarRewriter.getGapString("bar", "Expr");
        String res = "a print ( "+v1+" "+v3+" ) "+v2;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars4() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); vars.put(NodeFactory.makeId("bar"), expr); vars.put(NodeFactory.makeId("Baz"), expr);
        FreshName.reset();
        String r = tvr.rewriteVars(vars, "a print ( foo Baz(bar) )");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz(bar)", "Expr");

        String res = "a print ( "+v1+" "+v3+" )";
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars5() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); vars.put(NodeFactory.makeId("bar"), expr); vars.put(NodeFactory.makeId("Baz"), expr);
        FreshName.reset();
        String r = tvr.rewriteVars(vars, "a print foo Baz(bar)");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz(bar)", "Expr");
        String res = "a print "+v1+" "+v3;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars6() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); vars.put(NodeFactory.makeId("bar"), expr); vars.put(NodeFactory.makeId("Baz"), expr); vars.put(NodeFactory.makeId("Boz"), expr);
        FreshName.reset();
        String r = tvr.rewriteVars(vars, "a print foo Baz(bar, Boz) bar");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v2 = TemplateVarRewriter.getGapString("Baz(bar, Boz)", "Expr");
        String v3 = TemplateVarRewriter.getGapString("bar", "Expr");
        String res = "a print "+v1+" "+v2+" "+v3;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars7() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr);
        String r = tvr.rewriteVars(vars, "foo fooo foo");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v2 = TemplateVarRewriter.getGapString("foo", "Expr");
        String res = v1+" fooo "+v2;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars8() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr);
        vars.put(NodeFactory.makeId("bar"), expr);
        String r = tvr.rewriteVars(vars, "foo(bar");
        assertFalse(r.equals(""));
    }

    public void testRewriteVars9() {
        TemplateVarRewriter tvr = new TemplateVarRewriter();
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("e"), expr);
        String r = tvr.rewriteVars(vars, "(e \" world\"");
        String v1 = TemplateVarRewriter.getGapString("e", "Expr");
//        System.err.println(r);
//        System.err.println("("+v1+" \" world\"");
        assertTrue(r.equals("("+v1+" \" world\""));
    }

//    public void testRewriteVars10() {
//        TemplateVarRewriter tvr = new TemplateVarRewriter();
//        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
//        VarType expr = new VarType(NodeFactory.makeId("Expr"));
//        vars.put(NodeFactory.makeId("e"), expr);
//        String r = tvr.rewriteVars(vars, "\"e\"");
//        System.err.println(r);
//        System.err.println("\"e\"");
//        assertTrue(r.equals("\"e\""));
//    }
}
