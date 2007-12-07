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
import com.sun.fortress.nodes.AliasedDottedName;
import com.sun.fortress.nodes.AliasedName;
import com.sun.fortress.nodes.DottedName;
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
import com.sun.fortress.syntax_abstractions.GrammarIndex;

import edu.rice.cs.plt.tuple.Option;

/**
 * 
 */
public class ImportedApiCollector extends NodeDepthFirstVisitor_void {

	private boolean isTopLevel;
	private GlobalEnvironment env;
	private Collection<GrammarIndex> grammars;
	
	public ImportedApiCollector(GlobalEnvironment env) {
		this.env = env;
		this.isTopLevel = true;
		this.grammars = new LinkedList<GrammarIndex>();
	}
	
	@Override
	public void forImportApiOnly(ImportApi that) {
		for (AliasedDottedName apiAlias : that.getApis()) {
			final GrammarCollector grammarCollector = new GrammarCollector();
			env.api(apiAlias.getApi()).ast().accept(grammarCollector);
			for (GrammarDef grammar: grammarCollector.getGrammars()) {
				GrammarIndex grammarIndex = new GrammarIndex(grammar, this.isTopLevel);
				grammarIndex.setApi(env.api(apiAlias.getApi()));
				this.grammars.add(grammarIndex);
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
				GrammarIndex grammarIndex = new GrammarIndex(grammar, false);
				grammarIndex.setApi(env.api(that.getApi()));
				this.grammars.add(grammarIndex);
			}
			else {
				GrammarIndex grammarIndex = new GrammarIndex(grammar, this.isTopLevel);
				grammarIndex.setApi(env.api(that.getApi()));
				this.grammars.add(grammarIndex);
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
				if (name.getName().toString().equals(grammar.getName().getName().getId().getText())) {
					found  = true;
					break;
				}
			}
			if (found) {
				GrammarIndex grammarIndex = new GrammarIndex(grammar, this.isTopLevel);
				grammarIndex.setApi(env.api(that.getApi()));
				this.grammars.add(grammarIndex);
			}
			else {
				GrammarIndex grammarIndex = new GrammarIndex(grammar, false);
				grammarIndex.setApi(env.api(that.getApi()));
				this.grammars.add(grammarIndex);
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
	
//	private GrammarDef disambiguate(GrammarDef that, final Collection<GrammarDef> grammars, final ApiIndex api) {
//		return (GrammarDef) that.accept(new NodeUpdateVisitor() {						
//			@Override
//			public Node forGrammarDefOnly(GrammarDef that, IdName name_result, List<? extends QualifiedName> extends_result, List<ProductionDef> productions_result) {
//				List<QualifiedIdName> ls = new LinkedList<QualifiedIdName>();
//				for (QualifiedName name: extends_result) {
//					// Check if it is locally declared:
//					for (GrammarDef grammar: grammars) {
//						if (name.stringName().equals(grammar.getName().stringName())) {							
//							ls.add(new QualifiedIdName(new IdName(new Id(api.ast().getName().stringName()+"."+name.stringName()))));
//						}
//					}
//					// Check if it is 
//				}
//				return new GrammarDef(that.getSpan(), name_result, extends_result, productions_result);
//			}		
//		});
//	}

	
	public Collection<GrammarIndex> getGrammars() {
		return this.grammars;
	}
}
