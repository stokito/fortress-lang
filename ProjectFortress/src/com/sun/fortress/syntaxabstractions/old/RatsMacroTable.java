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
 * Class for storing macros compiled to Rats! which allows retrieval of all
 * macro declarations related to a specified Rats! module.  
 * 
 */

package com.sun.fortress.syntaxabstractions.old;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sun.fortress.syntaxabstractions.rats.util.ModuleEnum;

public class RatsMacroTable {

	/*
	 * A mapping from module to the macro declarations which
	 * should be defined in the extending module. 
	 */
	private Map<ModuleEnum, Collection<RatsMacroDecl>> table;

	/*
	 * Constructor which initializes the mapping
	 */
	public RatsMacroTable() {
		table = new HashMap<ModuleEnum, Collection<RatsMacroDecl>>();
	}

	/**
	 * Add a Rats! macro declaration to the table
	 * @param ratsMacroDecl
	 */
	public void add(RatsMacroDecl ratsMacroDecl) {
		Collection<RatsMacroDecl> ratsMacroDecls = table.get(ratsMacroDecl.getModule());
		if (ratsMacroDecls == null) {
			ratsMacroDecls = new LinkedList<RatsMacroDecl>();
		}
		ratsMacroDecls.add(ratsMacroDecl);
		table.put(ratsMacroDecl.getModule(), ratsMacroDecls);
	}

	/**
	 * Returns all the modules which should be extended
	 * based on the macro declarations in this table.
	 * @return Collection of modules
	 */
	public Collection<ModuleEnum> getModules() {
		return table.keySet();
	}

	/**
	 * Returns a collection of Rats! macro declarations which extend 
	 * the module given as argument.
	 * @param e - Module
	 * @return Collection of Rats! macro declarations
	 */
	public Collection<RatsMacroDecl> getMacroDecls(ModuleEnum e) {
		return this.table.get(e);
	}

	public Collection<RatsMacroDecl> getAllMacroDecls() {
		Collection<RatsMacroDecl> ls = new LinkedList<RatsMacroDecl>();
		for (Collection<RatsMacroDecl> ratsMacroDecls: this.table.values()) {
			ls.addAll(ratsMacroDecls);
		}
		return ls;
	}

}
