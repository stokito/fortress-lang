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

package com.sun.fortress.syntax_abstractions.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.GrammarEnv;

import edu.rice.cs.plt.tuple.Option;

/**
 * 
 */
public class ImportedApiCollector extends NodeDepthFirstVisitor_void {

	private boolean isTopLevel;
	private GlobalEnvironment env;
	private Collection<GrammarEnv> grammars;

	public ImportedApiCollector(GlobalEnvironment env) {
		this.env = env;
		this.isTopLevel = true;
		this.grammars = new LinkedList<GrammarEnv>();
	}

	@Override
	public void forImportApiOnly(ImportApi that) {
		for (AliasedAPIName apiAlias : that.getApis()) {
			grammars.add(new GrammarEnv(env.api(apiAlias.getApi()).grammars().values(), this.isTopLevel));
			getRecursiveImports(apiAlias.getApi());
		}		
	}

	@Override
	public void forImportStarOnly(ImportStar that) {
		Collection<GrammarIndex> gs = new LinkedList<GrammarIndex>();
		for (GrammarIndex grammar: env.api(that.getApi()).grammars().values()) {
			if (grammar.ast().isSome()) {
				if (!that.getExcept().contains(Option.unwrap(grammar.ast()).getName())) {
					gs.add(grammar);
				}
			}
		}
		grammars.add(new GrammarEnv(gs, this.isTopLevel));
		getRecursiveImports(that.getApi());
	}


	@Override
	public void forImportNamesOnly(ImportNames that) {
		GrammarEnv grammarEnv = new GrammarEnv();
		for (GrammarIndex grammar: env.api(that.getApi()).grammars().values()) {
			boolean found = false;
			for (AliasedSimpleName name: that.getAliasedNames()) {
				if (name.getName().toString().equals(Option.unwrap(grammar.ast()).getName().getName().getId().getText())) {
					found  = true;
					break;
				}
			}
			if (found) {
				grammarEnv.addGrammar(grammar, this.isTopLevel);
			}
			else {
				grammarEnv.addGrammar(grammar, false);
			}
		}
		grammars.add(grammarEnv);
		getRecursiveImports(that.getApi());
	}

	/**
	 * @param that
	 */
	private void getRecursiveImports(APIName api) {
		boolean isTopLevel = this.isTopLevel;
		this.isTopLevel = false;
		env.api(api).ast().accept(this);
		this.isTopLevel = isTopLevel;
	}

	public Collection<GrammarEnv> getGrammars() {
		return this.grammars;
	}
}
