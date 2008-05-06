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

/*
 * Class for collecting names and imports for a given component.
 * Used in the Fortress com.sun.fortress.compiler.Fortress.
 */

package com.sun.fortress.syntax_abstractions.parser;

import java.util.Collection;
import java.util.LinkedList;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.AliasedSimpleName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.syntax_abstractions.environments.GlobalGrammarEnv;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

/**
 *
 */
public class ImportedApiCollector extends NodeDepthFirstVisitor_void {

	private boolean isTopLevel;
	private GlobalEnvironment env;
	private Collection<GlobalGrammarEnv> grammars;
	private Iterable<? extends StaticError> errors;

	public class Result extends StaticPhaseResult {
		Collection<GlobalGrammarEnv> grammars;

		public Result(Collection<GlobalGrammarEnv> grammars) {
			super();
			this.grammars = grammars;
		}

		public Result(Collection<GlobalGrammarEnv> grammars,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.grammars = grammars;
		}


		public Collection<GlobalGrammarEnv> grammars() { return grammars; }

		public Result add(Result otherResult) {
			Collection<GlobalGrammarEnv> grammars = new LinkedList<GlobalGrammarEnv>();
			Collection<StaticError> errors = new LinkedList<StaticError>();
			grammars.addAll(this.grammars);
			grammars.addAll(otherResult.grammars);
			for (StaticError e: this.errors()) {
				errors.add(e);
			}
			for (StaticError e: otherResult.errors()) {
				errors.add(e);
			}
			return new Result(grammars, errors);
		}
	}

	public ImportedApiCollector(GlobalEnvironment env) {
		this.env = env;
		this.isTopLevel = true;
		this.grammars = new LinkedList<GlobalGrammarEnv>();
		this.errors = new LinkedList<StaticError>();
	}

	public Result collectApis(CompilationUnit c) {
		c.accept(this);
		return new Result(this.grammars, errors);
	}

	@Override
	public void forImportApiOnly(ImportApi that) {
		for (AliasedAPIName apiAlias : that.getApis()) {
			if (env.definesApi(apiAlias.getApi())) {
				ApiIndex api = env.api(apiAlias.getApi());
				if (!api.grammars().values().isEmpty()) {
					grammars.add(new GlobalGrammarEnv(api .grammars().values(), this.isTopLevel));
					getRecursiveImports(apiAlias.getApi());
				}
			}
			else {
				StaticError.make("Undefined api: "+apiAlias.getApi(), that);
			}
		}
	}

	@Override
	public void forImportStarOnly(ImportStar that) {
		Collection<GrammarIndex> gs = new LinkedList<GrammarIndex>();
		if (env.definesApi(that.getApi())) {
			for (GrammarIndex grammar: env.api(that.getApi()).grammars().values()) {
				if (grammar.ast().isSome()) {
					if (!that.getExcept().contains(Option.unwrap(grammar.ast()).getName())) {
						gs.add(grammar);
					}
				}
			}
			if (!gs.isEmpty()) {
				grammars.add(new GlobalGrammarEnv(gs, this.isTopLevel));
				getRecursiveImports(that.getApi());
			}
		}
		else {
			StaticError.make("Undefined api: "+that.getApi(), that);
		}
	}


	@Override
	public void forImportNamesOnly(ImportNames that) {
		if (env.definesApi(that.getApi())) {
			GlobalGrammarEnv grammarEnv = new GlobalGrammarEnv();
			for (GrammarIndex grammar: env.api(that.getApi()).grammars().values()) {
				boolean found = false;
				for (AliasedSimpleName name: that.getAliasedNames()) {
					String importedNonterminalName = name.getName().toString();
					String nonterminalName = Option.unwrap(grammar.ast()).getName().toString();
					if (importedNonterminalName.equals(nonterminalName )) {
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
			if (!grammarEnv.getGrammars().isEmpty()) {
				grammars.add(grammarEnv);
				getRecursiveImports(that.getApi());
			}
		}
		else {
			StaticError.make("Undefined api: "+that.getApi(), that);
		}
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

	public Collection<GlobalGrammarEnv> getGrammars() {
		return this.grammars;
	}
}
