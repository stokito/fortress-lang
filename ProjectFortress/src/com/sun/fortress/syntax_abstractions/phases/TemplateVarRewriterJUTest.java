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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

import com.sun.fortress.syntax_abstractions.environments.GapEnv;
import com.sun.fortress.syntax_abstractions.environments.NTEnv;
import com.sun.fortress.syntax_abstractions.environments.EnvFactory;
import com.sun.fortress.syntax_abstractions.environments.Depth;

import junit.framework.TestCase;

public class TemplateVarRewriterJUTest extends TestCase {

    protected static NTEnv theNTEnv;
    static {
        Id expr = NodeFactory.makeId("Expr");
        Map<Id,BaseType> typemap = new HashMap<Id,BaseType>();
        typemap.put(expr, new VarType(expr));
        NTEnv ntEnv = EnvFactory.makeTestingNTEnv(typemap);
        theNTEnv = ntEnv;
    }

    protected static GapEnv emptyGapEnv;
    static {
        Map<Id,Depth> depthmap = new HashMap<Id,Depth>();
        Map<Id,Id> ntmap = new HashMap<Id,Id>();
        Set<Id> stringvars = new HashSet<Id>();
        emptyGapEnv = EnvFactory.makeTestingGapEnv(theNTEnv, depthmap, ntmap, stringvars);
    }

    public void testRewriteVars1() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r= tvr.rewriteVars("foo");
        assertTrue(r.equals("foo"));
    }

    public void testRewriteVars2() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        vars.put(NodeFactory.makeId("foo"), new VarType(NodeFactory.makeId("Expr")));
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r = tvr.rewriteVars("foo");
        assertTrue(r.startsWith(TemplateVarRewriter.GAPSYNTAXPREFIX) && 
                   r.endsWith(TemplateVarRewriter.GAPSYNTAXSUFFIX));
    }

    public void testRewriteVars3() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); 
        vars.put(NodeFactory.makeId("bar"), expr); 
        vars.put(NodeFactory.makeId("Baz"), expr);
        FreshName.reset();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        String r = tvr.rewriteVars("a print ( foo Baz ) bar");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v3 = TemplateVarRewriter.getGapString("Baz", "Expr");
        String v2 = TemplateVarRewriter.getGapString("bar", "Expr");
        String res = "a print ( "+v1+" "+v3+" ) "+v2;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars4() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); 
        vars.put(NodeFactory.makeId("bar"), expr); 
        vars.put(NodeFactory.makeId("Baz"), expr);
        FreshName.reset();
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
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); 
        vars.put(NodeFactory.makeId("bar"), expr); 
        vars.put(NodeFactory.makeId("Baz"), expr);
        FreshName.reset();
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
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr); 
        vars.put(NodeFactory.makeId("bar"), expr); 
        vars.put(NodeFactory.makeId("Baz"), expr); 
        vars.put(NodeFactory.makeId("Boz"), expr);
        FreshName.reset();
        String r = tvr.rewriteVars("a print foo Baz(bar, Boz) bar");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v2 = TemplateVarRewriter.getGapString("Baz(bar, Boz)", "Expr");
        String v3 = TemplateVarRewriter.getGapString("bar", "Expr");
        String res = "a print "+v1+" "+v2+" "+v3;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars7() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr);
        String r = tvr.rewriteVars("foo fooo foo");
        String v1 = TemplateVarRewriter.getGapString("foo", "Expr");
        String v2 = TemplateVarRewriter.getGapString("foo", "Expr");
        String res = v1+" fooo "+v2;
        //		System.err.println("R: "+r.getA());
        //		System.err.println("R: "+res);
        assertTrue(r.equals(res));
    }

    public void testRewriteVars8() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("foo"), expr);
        vars.put(NodeFactory.makeId("bar"), expr);
        String r = tvr.rewriteVars("foo(bar");
        assertFalse(r.equals(""));
    }

    public void testRewriteVars9() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("e"), expr);
        String r = tvr.rewriteVars("(e \" world\"");
        String v1 = TemplateVarRewriter.getGapString("e", "Expr");
//        System.err.println(r);
//        System.err.println("("+v1+" \" world\"");
        assertTrue(r.equals("("+v1+" \" world\""));
    }

    public void testRewriteVars10() {
        Map<Id, BaseType> vars = new HashMap<Id, BaseType>();
        TemplateVarRewriter tvr = new TemplateVarRewriter(emptyGapEnv, vars);
        VarType expr = new VarType(NodeFactory.makeId("Expr"));
        vars.put(NodeFactory.makeId("e"), expr);
        String r = tvr.rewriteVars("\"e\"");
//        System.err.println(r);
//        System.err.println("\"e\"");
        assertTrue(r.equals("\"e\""));
    }
}
