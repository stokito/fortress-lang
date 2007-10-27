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
 * Class which collects the macro declarations from compilation units 
 * containing APIs and provides them to the macro compiler.
 * {@see com.sun.fortress.macro.MacroCompiler}
 * 
 */

package com.sun.fortress.macro;

import java.util.Collection;
import java.util.LinkedList;

import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.MacroDecl;

public class Macro {

	public void execute(Collection<CompilationUnit> apis) {
		Collection<MacroDecl> macroDecls = new LinkedList<MacroDecl>();
		MacroCollector macroCollector = new MacroCollector();
		for (CompilationUnit cu: apis) {
			macroCollector = new MacroCollector();
			cu.accept(macroCollector);
			macroDecls.addAll(macroCollector.getMacroCollection());
		}
		
		MacroCompiler macroCompiler = new FileBasedMacroCompiler(); 
		macroCompiler.compile(macroDecls);
	}
}
