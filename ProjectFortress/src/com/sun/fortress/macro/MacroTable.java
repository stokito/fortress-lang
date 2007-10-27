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
 * Interface for macro tables. A Macro table contains a set of...
 * 
 */

package com.sun.fortress.macro;

import java.util.Collection;

import com.sun.fortress.nodes.MacroDecl;

public interface MacroTable {
	
	public void add(MacroDecl macroDecl);

	public Collection<ModuleEnum> getModules();

	public Collection<RatsMacroDecl> getMacroDecls(ModuleEnum e);
}
