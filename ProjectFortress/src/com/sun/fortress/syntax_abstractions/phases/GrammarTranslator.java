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

package com.sun.fortress.syntax_abstractions.phases;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.parser.AlternativeAddition;
import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleImport;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleList;
import xtc.parser.ModuleModification;
import xtc.parser.ModuleName;
import xtc.parser.NonTerminal;
import xtc.parser.OrderedChoice;
import xtc.parser.Production;
import xtc.parser.SequenceName;
import xtc.tree.Node;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.TokenSymbol;
import com.sun.fortress.syntax_abstractions.intermediate.UserModule;
import com.sun.fortress.syntax_abstractions.old.RatsMacroDecl;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleEnum;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.syntax_abstractions.rats.util.ProductionEnum;

import edu.rice.cs.plt.tuple.Option;

public class GrammarTranslator {

	public class Result extends StaticPhaseResult {
		private Collection<Module> modules;
		private Set<String> keywords;
		private Collection<Module> keywordModules;

		public Result(Collection<Module> modules, Set<String> keywords,
				Collection<Module> keywordModules, Iterable<? extends StaticError> errors) {
			super(errors);
			this.modules = modules;
			this.keywords = keywords;
			this.keywordModules = keywordModules;
		}

		public Collection<Module> modules() { return modules; }

		public Set<String> keywords() { return keywords; }
		
		public Collection<Module> keywordModules() { return this.keywordModules; }
	}

	public static Result translate(Collection<com.sun.fortress.syntax_abstractions.intermediate.Module> modules,
			GlobalEnvironment env) {
		GrammarTranslator grammarTranslator = new GrammarTranslator();
		Collection<Module> ratsModules = new LinkedList<Module>();
		Set<String> keywords = new HashSet<String>();
		Set<String> tokens = new HashSet<String>();
		List<Module> keywordModules = new LinkedList<Module>();
		List<Module> tokenModules = new LinkedList<Module>();
		
		Iterable<? extends StaticError> errors = new LinkedList<StaticError>();

		for (com.sun.fortress.syntax_abstractions.intermediate.Module module: modules) {
			if (module instanceof UserModule) {
				Module m = RatsUtil.makeExtendingRatsModule(module);

				GrammarAnalyzer<com.sun.fortress.syntax_abstractions.intermediate.Module> ga = new GrammarAnalyzer<com.sun.fortress.syntax_abstractions.intermediate.Module>();
//				System.err.println("\nM: "+module.getName());
//				for (ProductionIndex<? extends NonterminalDecl> n: ga.getContainedSet(module)) {
//					System.err.println("N: "+n.getName());	
//				}
				ProductionTranslator.Result ptr = ProductionTranslator.translate(ga.getContainedSet(module), env);
				if (!ptr.isSuccessful()) { return grammarTranslator.new Result(ratsModules, keywords, keywordModules, ptr.errors()); }
				
				m.productions = ptr.productions();
				ratsModules.add(m);

				Collection<? extends String> kws = module.getKeywords();
				if (!kws.isEmpty()) {
					List<ModuleName> parameters = m.parameters.names;
					
					ModuleName keyword = new ModuleName("Keyword");
					if (!parameters.contains(keyword)) {
						parameters.add(keyword );
						m.dependencies.add(new ModuleImport(keyword));
						keywordModules.add(m);
					}
					keywords.addAll(kws);
				}
			}
		}

		return grammarTranslator.new Result(ratsModules, keywords, keywordModules, errors);
	}

	/**
	 * @param modules
	 * @param e
	 * @param ratsMacroDecls
	 */
	private Module createRatsModule(ModuleEnum e, Collection<RatsMacroDecl> ratsMacroDecls) {
		Module m = null; //RatsUtil.makeEmptyExtendingRatsModule(e);

		// Get the parameter which the Fortress grammar Rats! files use
		List<ModuleName> parameters = ModuleInfo.getParameters(e);
		List<ModuleDependency> dependencies = ModuleInfo.getModuleModification(e);


		Map<ProductionEnum,AlternativeAddition> prods = new HashMap<ProductionEnum,AlternativeAddition>();
		for (RatsMacroDecl ratsMacroDecl: ratsMacroDecls) {

			// Add any additional parameters and dependencies to the module:
			parameters.addAll(ratsMacroDecl.getParameters());
			m.parameters = new ModuleList(removeDuplicates(parameters));

			dependencies.addAll(ratsMacroDecl.getDependencies());
			m.dependencies = removeDuplicates(dependencies);

			/* If we haven't seen this production before we need to
			 * create it
			 */ 
			if (!prods.keySet().contains(ratsMacroDecl.getProduction())) {
				prods.put(ratsMacroDecl.getProduction(), 
						new AlternativeAddition(ModuleInfo.getProductionReturnType(ratsMacroDecl.getProduction()),
								new NonTerminal(ModuleInfo.getProductionName(ratsMacroDecl.getProduction())),
								new OrderedChoice(ratsMacroDecl.getSequence()), 
								new SequenceName(ModuleInfo.getExtensionPoint(ratsMacroDecl.getProduction().name())),false));
			}
			else {
				/*
				 * If we already have seen this production, then just add another alternative
				 */
				// TODO
//				AlternativeAddition oldChoice = prods.get(ratsMacroDecl.getProduction()).choice;
//				OrderedChoice choice = new AlternativeAddition(oldChoice.getName(),
//				oldChoice.name,
//				oldChoice.choice
//				oldChoice.sequence);
//				choice.add(ratsMacroDecl.getSequence());
//				prods.get(ratsMacroDecl.getProduction()).choice = choice ;
			}
		}
		List<Production> productions = new LinkedList<Production>();
		productions.addAll(prods.values());
		m.productions = productions;

		return m;
	}

	private <T> List<T> removeDuplicates(List<T> cs) {
		List<T> ls = new LinkedList<T>();
		for (int inx = 0; inx<cs.size(); inx++) {
			boolean found = false;
			for (int iny = inx+1; iny<cs.size(); iny++) {
				if (cs.get(inx).equals(cs.get(iny))) {
					found = true;
				}
			}
			if (!found) {
				ls.add(cs.get(inx));
			}
		}
		return ls;
	}

}
