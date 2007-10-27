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
 * Class... 
 * 
 */

package com.sun.fortress.macro;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sun.fortress.nodes.MacroDecl;

public class RatsMacroTable implements MacroTable {

	private Map<ModuleEnum, Collection<RatsMacroDecl>> table;
	
	public RatsMacroTable() {
		super();
		table = new HashMap<ModuleEnum, Collection<RatsMacroDecl>>();
	}

	public void add(MacroDecl macroDecl) {
		RatsMacroTranslator macroTranslator = new RatsMacroTranslator();
		macroDecl.accept(macroTranslator);
		RatsMacroDecl ratsMacroDecl = macroTranslator.getMacroDecl();
		Collection<RatsMacroDecl> ratsMacroDecls = table.get(ratsMacroDecl.getModule());
		if (ratsMacroDecls == null) {
			ratsMacroDecls = new LinkedList<RatsMacroDecl>();
		}
		ratsMacroDecls.add(ratsMacroDecl);
		table.put(ratsMacroDecl.getModule(), ratsMacroDecls);
	}

	public Collection<ModuleEnum> getModules() {
		return table.keySet();
	}

	public Collection<RatsMacroDecl> getMacroDecls(ModuleEnum e) {
		return this.table.get(e);
	}

}
