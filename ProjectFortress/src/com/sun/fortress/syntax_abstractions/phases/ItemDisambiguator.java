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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.ItemSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.GrammarEnv;
import com.sun.fortress.syntax_abstractions.GrammarIndex;
import com.sun.fortress.syntax_abstractions.intermediate.Module;


public class ItemDisambiguator {

	public static class Result extends StaticPhaseResult {
		private Collection<Module> modules;

		public Result(Collection<Module> modules,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.modules = modules;
		}

		public Collection<Module> modules() { return modules; }
	}
	
	public static Result disambiguateEnv(Collection<Module> modules) {
		for (final Module module: modules) {
			List<ProductionDef> productions = new LinkedList<ProductionDef>();
			for (final ProductionDef production: module.getDefinedProductions()) {
				final Set<TokenSymbol> tokens = new HashSet<TokenSymbol>();
				productions.add((ProductionDef) production.accept(new NodeUpdateVisitor() {						
					@Override
					public Node forItemSymbolOnly(ItemSymbol that) {
						Node n = nameResolution(module, production, that);
						if (n != null) {
							return n;
						}
						TokenSymbol token = new TokenSymbol(that.getSpan(), that.getItem());
						tokens.add(token);
						return token;
					}

					private Node nameResolution(final Module module, final ProductionDef production, ItemSymbol that) {
						if (module.containsProduction(production, that.getItem().replace(".", ""))) {
							return makeNonterminal(that, that.getItem().replace(".", "")); // TODO: don't rewrite production names
						}
//						else if (module.containsProduction(production, module.getQualifiedName(that.getItem().replace(".", "")))) {
//							return makeNonterminal(that, module.getQualifiedName(that.getItem().replace(".", ""))); // TODO: same here
//						}
						else {
							Node n = null;
							for (Module m: module.getImports()) {
								n =  nameResolution(m, production, that);
								if (n != null) {
									return n;
								}
							}							
						}
						return null;
					}

					private Node makeNonterminal(ItemSymbol that, String name) {
						return new NonterminalSymbol(that.getSpan(), new QualifiedIdName(new IdName(that.getSpan(), new Id(that.getSpan(), name))));
					}		
				}));
				module.addTokens(production.getName(), tokens);
			}
			module.setProductions(productions);
		}
		return new Result(modules, new LinkedList<StaticError>());
	}

}
