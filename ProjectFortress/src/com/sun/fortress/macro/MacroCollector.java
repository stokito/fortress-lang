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
 * Class collecting all macro declarations from the compilation unit
 * it is applied to.
 * 
 */

package com.sun.fortress.macro;

import java.util.Collection;
import java.util.LinkedList;

import com.sun.fortress.nodes.MacroDecl;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;

public class MacroCollector extends NodeDepthFirstVisitor_void {

	private Collection<MacroDecl> macroDecls;

	public MacroCollector() {
		super();
		macroDecls = new LinkedList<MacroDecl>();
	}

	@Override
	public void forMacroDecl(MacroDecl that) {
		this.macroDecls.add(that);
	}

	public Collection<MacroDecl> getMacroCollection() {
		return this.macroDecls;
	}

}
