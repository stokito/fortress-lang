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

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionExtendIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.GrammarEnv;
import com.sun.fortress.syntax_abstractions.intermediate.FortressModule;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.intermediate.UserModule;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

/*
 * This class transforms the grammar structure into a Rats! module structure
 * without translating the nonterminal declarations.
 */

public class ModuleTranslator {

	/**
	 * Result of the module translation
	 */
	public class Result extends StaticPhaseResult {
		Collection<Module> modules;
		private Map<String, String> modulesReplacingFortressModules;

		public Result(Collection<Module> modules, 
				Map<String, String> modulesReplacingFortressModules,
				Collection<StaticError> errors) {
			super(errors);
			this.modules = modules;
			this.modulesReplacingFortressModules = modulesReplacingFortressModules;
		}

		public Result(Collection<Module> modules,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.modules = modules;
		}

		public Collection<Module> modules() { return modules; }
		public Map<String, String> modulesReplacingFortressModules() { return this.modulesReplacingFortressModules; }
	}

	private static Collection<StaticError> _errors;
	private static Map<String, String> modulesReplacingFortressModules;

	private static void error(String mess, HasAt loc) {
		_errors.add(StaticError.make(mess, loc));
	}

	public static Result translate(Collection<GrammarEnv> environments) {
		Map<GrammarIndex, Module> grammarToModules = new HashMap<GrammarIndex, Module>(); 
		_errors = new LinkedList<StaticError>();

		for (GrammarEnv env: environments) {
			for (GrammarIndex g: env.getGrammars()) {
				Module m = newModule(g, grammarToModules);

				initializeNewModule(m, g.getDeclaredNonterminals(), new LinkedList<Module>());

				m.setExtended(initializeExtend(grammarToModules, g));

				if (env.isToplevel(g)) {
					m.isTopLevel(true);
				}					

				if (!grammarToModules.containsKey(g)) {
					grammarToModules.put(g, m);
				}
			}
		}

		resolveModification(grammarToModules);
		return new ModuleTranslator().new Result(grammarToModules.values(), modulesReplacingFortressModules, _errors);
	}

	private static Option<GrammarIndex> findGrammarIndex(Collection<GrammarIndex> gs, QualifiedIdName n) {
		for (GrammarIndex g: gs) {
			if (n.equals(g.getName())) {
				return Option.some(g);
			}
		}
		return Option.none();
	}

	private static void resolveModification(Map<GrammarIndex, Module> grammarToModules) {
		modulesReplacingFortressModules = new HashMap<String, String>();
		
		Set<APIName> modifiedNames = new HashSet<APIName>();
		Set<Module> modifiedModules = new HashSet<Module>();

		for (Module m: grammarToModules.values()) {
			if (m.isTopLevel()) {
				// Intentional raw type javac 1.5 bug on Solaris 
				for (ProductionIndex/*<? extends NonterminalDecl>*/ n: m.getDeclaredNonterminals()) {				
					if (n instanceof ProductionExtendIndex) {
						ProductionExtendIndex nei = (ProductionExtendIndex) n;

						for (ProductionIndex<? extends NonterminalDecl> e: nei.getExtends()) {
							if (e.getName().getApi().isSome()) {
								APIName apiName = Option.unwrap(e.getName().getApi());
								if (modifiedNames.contains(apiName )) {
									error("Multiple modifications of productions in the same grammar "+e.getName(), apiName);	
								}
								modifiedNames.add(apiName);
								QualifiedIdName grammarName = getGrammarName(apiName);
								Option<GrammarIndex> g = findGrammarIndex(grammarToModules.keySet(), grammarName);
								if (g.isSome()) {
									modifiedModules.add(grammarToModules.get(Option.unwrap(g)));
								}
								else {
									throw new RuntimeException("Grammar not found: "+grammarName);
								}
							}
							else {
								throw new RuntimeException("Found non disambiguated name in production definition: "+e.getName());
							}
						}
					}
				}
				if (modifiedNames.size() == 1) {
					Module modify = IterUtil.first(modifiedModules); 
					if (modify instanceof FortressModule) {
						modulesReplacingFortressModules.put(m.getName(), modify.getName());
					}
					m.setModify(modify);
				}
				else if (modifiedNames.isEmpty()) {
					error("No modifications found, you must modify at least one existing Fortress grammar to be useful. ", IterUtil.first(m.getDeclaredNonterminals()).getName());
				}
				else {
					error("NYI: Modifications to multiple grammars", IterUtil.first(m.getDeclaredNonterminals()).getName());
				}
			}
		}
	}

	private static QualifiedIdName getGrammarName(APIName apiName) {
		List<Id> apiIds = new LinkedList<Id>();
		apiIds.addAll(apiName.getIds());
		Id gId = apiIds.remove(apiIds.size()-1);
		return NodeFactory.makeQualifiedIdName(apiIds, gId);
	}

	/**
	 * @param grammarToModules
	 * @param g
	 */
	private static Collection<Module> initializeExtend(Map<GrammarIndex, Module> grammarToModules, GrammarIndex g) {
		Collection<Module> extended = new LinkedList<Module>();
		if (!g.getExtended().isEmpty()) {
			for (GrammarIndex otherGrammar: g.getExtended()) { 
				Module extend = newModule(otherGrammar, grammarToModules);					
				extended.add(extend);
			}
		}
		return extended;
	}

	/**
	 * Create a new module m corresponding to the grammar g, if a corresponding module
	 * has not already been created (look in grammarToModules).
	 * If the grammar is part of the Fortress grammars then a FortressModule is
	 * created and if it is a user defined grammar, a UserModule is created.  
	 */
	private static Module newModule(GrammarIndex g, Map<GrammarIndex, Module> grammarToModules) {
		Module m = null;
		if (grammarToModules.containsKey(g)) {
			m = grammarToModules.get(g);
		}
		if (m != null) {
			return m;
		}
		if (ModuleInfo.isFortressModule(g.getName().toString())) {
			m = new FortressModule(g.getName().getName().toString(), g.getDeclaredNonterminals());
		}
		else {
			m = new UserModule(g.getName().getName().toString(), g.getDeclaredNonterminals());
		}
		grammarToModules.put(g, m);
		return m;		
	}

	/**
	 * If the module is a user module, we must create a unique name to avoid conflicts with
	 * existing Fortress grammar modules, and set the correct imports.
	 * @param m
	 * @param productions
	 * @param imports
	 * @return
	 */
	private static Module initializeNewModule(Module m, 
			Collection<? extends ProductionIndex<? extends NonterminalDecl>> productions,
					Collection<Module> imports) {
		if (!(m instanceof FortressModule)) { 
			m.setName(FreshName.getFreshName(m.getName()));
		}
		m.setImports(imports);
		return m;
	}
}
