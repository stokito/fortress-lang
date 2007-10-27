/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
 * Class traversing a macro declaration and creates a corresponding Rats!
 * macro declaration
 * 
 */

package com.sun.fortress.macro;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.MacroDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.SyntaxDecl;
import com.sun.fortress.nodes.SyntaxDef;
import xtc.parser.*;
import xtc.tree.Attribute;
import xtc.tree.AttributeList;
import xtc.tree.Comment;
import xtc.tree.Printer;

public class RatsMacroTranslator extends NodeDepthFirstVisitor_void {

	private RatsMacroDecl ratsMacroDecl;
	
	public RatsMacroDecl getMacroDecl() {
		return this.ratsMacroDecl;
	}

	@Override
	public void forSyntaxDef(SyntaxDef that) {
		String name = that.getName().stringName();
		
		List<Sequence> seq = new LinkedList<Sequence>();

		List<Element> elms = new LinkedList<Element>();
		NonTerminal nt = new NonTerminal(name);
		elms.add(nt);
		List<Integer> indents = new LinkedList<Integer>();
		indents.add(1);
		indents.add(1);
		indents.add(1);
		Action a = new Action("yyValue = ", indents);
		elms.add(a);	
	
		seq.add(new Sequence(new SequenceName(name.toUpperCase() + "LIT"), elms ));

		
		ratsMacroDecl = new RatsMacroDecl(ModuleEnum.LITERAL, ProductionEnum.LITERAL, seq);
	}


	
}
