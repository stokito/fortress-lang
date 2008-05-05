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
import java.util.LinkedList;

import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

import com.sun.fortress.useful.Pair;

import junit.framework.TestCase;

public class TemplateVarRewriterJUTest extends TestCase {

	public void testRewriteVars1() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		String r= tvr.rewriteVars(vars, "foo");
		assertTrue(r.equals("foo"));
	}
	
	public void testRewriteVars2() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo");
		String r = tvr.rewriteVars(vars, "foo");
//		System.err.println("R: "+r.getA());
		assertTrue(r.startsWith(TemplateVarRewriter.GAPSYNTAXPREFIX) && 
				   r.endsWith(TemplateVarRewriter.GAPSYNTAXSUFFIX));
	}
	
	public void testRewriteVars3() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo"); vars.add("bar"); vars.add("Baz");
		FreshName.reset();
		String r = tvr.rewriteVars(vars, "a print ( foo Baz ) bar");
		String v1 = TemplateVarRewriter.getGapString("foo");
		String v3 = TemplateVarRewriter.getGapString("Baz");
		String v2 = TemplateVarRewriter.getGapString("bar");
		String res = "a print ( "+v1+" "+v3+" ) "+v2;
//		System.err.println("R: "+r.getA());
//		System.err.println("R: "+res);
		assertTrue(r.equals(res));
	}

	public void testRewriteVars4() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo"); vars.add("bar"); vars.add("Baz");
		FreshName.reset();
		String r = tvr.rewriteVars(vars, "a print ( foo Baz(bar) )");
		String v1 = TemplateVarRewriter.getGapString("foo");
		String v3 = TemplateVarRewriter.getGapString("Baz(bar)");
		
		String res = "a print ( "+v1+" "+v3+" )";
//		System.err.println("R: "+r.getA());
//		System.err.println("R: "+res);
		assertTrue(r.equals(res));
	}
	
	public void testRewriteVars5() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo"); vars.add("bar"); vars.add("Baz");
		FreshName.reset();
		String r = tvr.rewriteVars(vars, "a print foo Baz(bar)");
		String v1 = TemplateVarRewriter.getGapString("foo");
		String v3 = TemplateVarRewriter.getGapString("Baz(bar)");
		String res = "a print "+v1+" "+v3;
//		System.err.println("R: "+r.getA());
//		System.err.println("R: "+res);
		assertTrue(r.equals(res));
	}
	
	public void testRewriteVars6() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo"); vars.add("bar"); vars.add("Baz"); vars.add("Boz");
		FreshName.reset();
		String r = tvr.rewriteVars(vars, "a print foo Baz(bar, Boz) bar");
		String v1 = TemplateVarRewriter.getGapString("foo");
		String v2 = TemplateVarRewriter.getGapString("Baz(bar, Boz)");
		String v3 = TemplateVarRewriter.getGapString("bar");
		String res = "a print "+v1+" "+v2+" "+v3;
//		System.err.println("R: "+r.getA());
//		System.err.println("R: "+res);
		assertTrue(r.equals(res));
	}

	public void testRewriteVars7() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo");
		String r = tvr.rewriteVars(vars, "foo fooo foo");
		String v1 = TemplateVarRewriter.getGapString("foo");
		String v2 = TemplateVarRewriter.getGapString("foo");
		String res = v1+" fooo "+v2;
//		System.err.println("R: "+r.getA());
//		System.err.println("R: "+res);
		assertTrue(r.equals(res));
	}
	
	public void testRewriteVars8() {
		TemplateVarRewriter tvr = new TemplateVarRewriter();
		Collection<String> vars = new LinkedList<String>();
		vars.add("foo");
		vars.add("bar");
		String r = tvr.rewriteVars(vars, "foo(bar");
		assertFalse(r.equals(""));
	}

}
