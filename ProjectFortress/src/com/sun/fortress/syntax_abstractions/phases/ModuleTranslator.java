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

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
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
import com.sun.fortress.syntax_abstractions.GrammarEnv;
import com.sun.fortress.syntax_abstractions.intermediate.FortressModule;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.intermediate.UserModule;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;

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
					  Map<String, String> modulesReplacingFortressModules) {
			super();
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

	public static Result translate(Collection<GrammarEnv> environments) {
		Map<GrammarIndex, Module> grammarToModules = new HashMap<GrammarIndex, Module>();
		Map<String, String> modulesReplacingFortressModules = new HashMap<String, String>(); 
		
		for (GrammarEnv env: environments) {
			for (GrammarIndex g: env.getGrammars()) {			
				Module m = newModule(g, grammarToModules);
				
				initializeNewModule(m, g.productions(), new LinkedList<Module>());
				if (!g.getExtendedGrammars().isEmpty()) {
					Module modify = newModule(IterUtil.first(g.getExtendedGrammars()),
								              grammarToModules);					
					m.setModify(modify);
					if (modify instanceof FortressModule) { // TODO: Revise this when implementing multiple extension
						modulesReplacingFortressModules.put(m.getName(), modify.getName());
					}
				}

				if (env.isToplevel(g)) {
					m.isTopLevel(true);
				}

				if (!grammarToModules.containsKey(g)) {
					grammarToModules.put(g, m);
				}
			}
		}

		return new ModuleTranslator().new Result(grammarToModules.values(), modulesReplacingFortressModules);
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
			m = new FortressModule(g.getName().getName().toString(), g.productions());
		}
		else {
			m = new UserModule(g.getName().getName().toString(), g.productions());
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
