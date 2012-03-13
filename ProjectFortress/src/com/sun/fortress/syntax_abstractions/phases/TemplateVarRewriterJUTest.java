/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.environments.Depth;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TemplateVarRewriterJUTest extends TestCase {
    static Span span = NodeFactory.makeSpan("TemplateVarRewriterJUTest");

    protected static final NTEnv theNTEnv;

    static {
        Id expr = NodeFactory.makeId(span, "Expr");
        Map<Id, BaseType> typemap = new HashMap<Id, BaseType>();
        typemap.put(expr, NodeFactory.makeVarType(span, expr));
        NTEnv ntEnv = EnvFactory.makeTestingNTEnv(typemap);
        theNTEnv = ntEnv;
    }

    protected static final GapEnv emptyGapEnv;

    static {
        Map<Id, Depth> depthmap = new HashMap<Id, Depth>();
        Map<Id, Id> ntmap = new HashMap<Id, Id>();
        Set<Id> stringvars = new HashSet<Id>();
        emptyGapEnv = EnvFactory.makeTestingGapEnv(theNTEnv, depthmap, ntmap, stringvars);
    }

    public void testRewriteVars1() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r = tvr.rewriteVars("foo");
        assertTrue(r.equals("foo"));
    }

    public void testRewriteVars2() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        vars.put(NodeFactory.makeId(span, "foo"), NodeFactory.makeVarType(span, NodeFactory.makeId(span, "Expr")));
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r = tvr.rewriteVars("foo");
        assertTrue(
                r.startsWith(TemplateVarRewriter.GAPSYNTAXPREFIX) && r.endsWith(TemplateVarRewriter.GAPSYNTAXSUFFIX));
    }

    public void testRewriteVars3() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = NodeFactory.makeVarType(span, NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "foo"), expr);
        vars.put(NodeFactory.makeId(span, "bar"), expr);
        vars.put(NodeFactory.makeId(span, "Baz"), expr);
        RatsUtil.resetFreshName();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r = tvr.rewriteVars("a print ( foo Baz ) bar");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz", "Expr");
        String v2 = TemplateVarRewriter.getGapString("bar", "Expr");
        String res = "a print ( " + v1 + " " + v3 + " ) " + v2;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    // FIXME: Gap parameters are disabled for now
    /*
    public void testRewriteVars4() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = NodeFactory.makeVarType(NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "foo"), expr);
        vars.put(NodeFactory.makeId(span, "bar"), expr);
        vars.put(NodeFactory.makeId(span, "Baz"), expr);
        RatsUtil.resetFreshName();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r = tvr.rewriteVars("a print ( foo Baz(bar) )");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz(bar)", "Expr");
        String res = "a print ( "+v1+" "+v3+" )";
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars5() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = NodeFactory.makeVarType(NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "foo"), expr);
        vars.put(NodeFactory.makeId(span, "bar"), expr);
        vars.put(NodeFactory.makeId(span, "Baz"), expr);
        RatsUtil.resetFreshName();
        String r = tvr.rewriteVars("a print foo Baz(bar)");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz(bar)", "Expr");
        String res = "a print "+v1+" "+v3;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars6() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = NodeFactory.makeVarType(NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "foo"), expr);
        vars.put(NodeFactory.makeId(span, "bar"), expr);
        vars.put(NodeFactory.makeId(span, "Baz"), expr);
        vars.put(NodeFactory.makeId(span, "Boz"), expr);
        RatsUtil.resetFreshName();
        String r = tvr.rewriteVars("a print foo Baz(bar, Boz) bar");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v2 = TemplateVarRewriter.getGapString("Baz(bar, Boz)", "Expr");
        String v3 = TemplateVarRewriter.getGapString("bar", "Expr");
        String res = "a print "+v1+" "+v2+" "+v3;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }
    */

    public void testRewriteVars7() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = NodeFactory.makeVarType(span, NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "foo"), expr);
        String r = tvr.rewriteVars("foo fooo foo");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v2 = TemplateVarRewriter.getGapString("foo", "Expr");
        String res = v1 + " fooo " + v2;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars8() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = NodeFactory.makeVarType(span, NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "foo"), expr);
        vars.put(NodeFactory.makeId(span, "bar"), expr);
        String r = tvr.rewriteVars("foo(bar");
        assertFalse(r.equals(""));
    }

    public void testRewriteVars9() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = NodeFactory.makeVarType(span, NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "e"), expr);
        String r = tvr.rewriteVars("(e \" world\"");
        String v1 = TemplateVarRewriter.getGapString("e", "Expr");
        //        System.err.println(r);
        //        System.err.println("("+v1+" \" world\"");
        assertTrue(r.equals("(" + v1 + " \" world\""));
    }

    public void testRewriteVars10() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = NodeFactory.makeVarType(span, NodeFactory.makeId(span, "Expr"));
        vars.put(NodeFactory.makeId(span, "e"), expr);
        String r = tvr.rewriteVars("\"e\"");
        //        System.err.println(r);
        //        System.err.println("\"e\"");
        assertTrue(r.equals("\"e\""));
    }
}
