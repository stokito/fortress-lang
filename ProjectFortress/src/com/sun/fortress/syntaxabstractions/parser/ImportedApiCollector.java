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
 * Class for collecting names and imports for a given component.
 * Used in the Fortress com.sun.fortress.compiler.Fortress.
 */

package com.sun.fortress.syntaxabstractions.parser;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.nodes.AliasedDottedName;
import com.sun.fortress.nodes.AliasedName;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;

/**
 * 
 */
public class ImportedApiCollector extends NodeDepthFirstVisitor_void {

	private boolean isTopLevel;
	private GlobalEnvironment env;
	private Map<GrammarDef, Boolean> grammars;
	
	public ImportedApiCollector(GlobalEnvironment env) {
		this.env = env;
		this.isTopLevel = true;
		this.grammars = new HashMap<GrammarDef, Boolean>();
	}
	
	@Override
	public void forImportApiOnly(ImportApi that) {
		for (AliasedDottedName apiAlias : that.getApis()) {
			GrammarCollector grammarCollector = new GrammarCollector();
			env.api(apiAlias.getApi()).ast().accept(grammarCollector);
			for (GrammarDef grammar: grammarCollector.getGrammars()) {
				this.grammars.put(grammar, this.isTopLevel);
			}

			getRecursiveImports(apiAlias.getApi());
		}		
	}

	
	@Override
	public void forImportStarOnly(ImportStar that) {
		GrammarCollector grammarCollector = new GrammarCollector();
		env.api(that.getApi()).ast().accept(grammarCollector);
		for (GrammarDef grammar: grammarCollector.getGrammars()) {
			if (that.getExcept().contains(grammar.getName())) {
				this.grammars.put(grammar, false);
			}
			else {
				this.grammars.put(grammar, this.isTopLevel);
			}
		}

		getRecursiveImports(that.getApi());
	}


	@Override
	public void forImportNamesOnly(ImportNames that) {
		GrammarCollector grammarCollector = new GrammarCollector();
		env.api(that.getApi()).ast().accept(grammarCollector);
		for (GrammarDef grammar: grammarCollector.getGrammars()) {
			boolean found = false;
			for (AliasedName name: that.getAliasedNames()) {
				if (name.getName().toString().equals(grammar.getName().getId().getText())) {
					found  = true;
					break;
				}
			}
			if (found) {
				this.grammars.put(grammar, this.isTopLevel);
			}
			else {
				this.grammars.put(grammar, false);
			}
		}

		getRecursiveImports(that.getApi());
	}

	/**
	 * @param that
	 */
	private void getRecursiveImports(DottedName api) {
		boolean isTopLevel = this.isTopLevel;
		this.isTopLevel = false;
		env.api(api).ast().accept(this);
		this.isTopLevel = isTopLevel;
	}
	
	public Map<GrammarDef, Boolean> getGrammars() {
		return this.grammars;
	}

}
