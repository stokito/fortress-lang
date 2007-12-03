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

package com.sun.fortress.syntaxabstractions.phases;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.ItemSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.ProductionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntaxabstractions.GrammarEnvironment;
import com.sun.fortress.syntaxabstractions.GrammarIndex;
import com.sun.fortress.syntaxabstractions.intermediate.Module;
import com.sun.fortress.syntaxabstractions.rats.util.ModuleInfo;

import edu.rice.cs.plt.tuple.Option;

/*
 * If a grammar contains a production which extends a production defined 
 * in the core fortress grammar either directly or indirectly then it should be
 * moved to a special extending module which imports the original module from the
 * original grammar. TODO: Optimization, if the grammar contains only one such 
 * production there is no need to separate the two.
 */

public class ModuleResolver {

	public class Result extends StaticPhaseResult {
		Collection<Module> modules;

		public Result(Collection<Module> modules) {
			super();
			this.modules = modules;
		}

		public Result(Collection<Module> modules,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.modules = modules;
		}


		public Collection<Module> modules() { return modules; }
	}

	// private static Map<IdName, Module> modules;

	public static Result resolve(GrammarEnvironment env) {
		Map<GrammarIndex, Module> grammarToModules = new HashMap<GrammarIndex, Module>();

		for (GrammarIndex grammar: env.getGrammars()) {
			// If the grammar has been seen in an import then get 
			// it from the map. Else create a new one.
			Module m = new Module();
			if (grammarToModules.containsKey(grammar)) {
				m = grammarToModules.get(grammar);
			}
			else {
				grammarToModules.put(grammar, m);
			}
			final Module module = m;

			// Set the basic things
			module.setName(grammar.getName().getId().getText());
			module.isTopLevel(grammar.isTopLevel());

			// Add all productions
			module.addProductions(grammar.getProductions());

			// Add all extended modules
			for (GrammarIndex g: grammar.getExtendedGrammar()) {
				if (!grammarToModules.keySet().contains(g)) {
					grammarToModules.put(g, new Module());
				}
				module.addExtendedModule(grammarToModules.get(g));
			}
		}

		// Move extending toplevel productions to their own module
		Map<Module, Set<ProductionDef>> result = new HashMap<Module, Set<ProductionDef>>();
		for (Module module: grammarToModules.values()) {
			if (module.isTopLevel()) {
				result.putAll(module.getExtendedCoreModules());
			}
		}

		List<Module> modules = new LinkedList<Module>();
		modules.addAll(grammarToModules.values()); 
		for (Module coreModule: result.keySet()) {
			Module module = new Module();
			modules.add(module);
			module.setName(ModuleInfo.getExtendedModuleName(coreModule.getName()));
			module.setModify(coreModule);
			module.addProductions(result.get(coreModule));
			module.setImports(coreModule.getImports());
		}

		// Disambiguate productions names
		for (final Module m: grammarToModules.values()) {
			Collection<ProductionDef> productions = new LinkedList<ProductionDef>();
			for (ProductionDef production: m.getDefinedProductions()) {
				productions.add(renameProduction(production, m.getQualifiedName(production.getName().toString())));
			}
			m.setProductions(productions);
		}
		return new ModuleResolver().new Result(modules);
	}

	public static ProductionDef renameProduction(final ProductionDef production, final String newName) {
		return (ProductionDef) production.accept(new NodeUpdateVisitor() {						
			@Override
			public Node forProductionDefOnly(ProductionDef that, QualifiedName name_result, IdName type_result, Option<? extends QualifiedName> extends_result, List<SyntaxDef> syntaxDefs_result) {
				name_result = new QualifiedIdName(new IdName(new Id(newName)));
				return new ProductionDef(that.getSpan(), name_result, type_result, extends_result, syntaxDefs_result);
			}		
		});
	}

}
