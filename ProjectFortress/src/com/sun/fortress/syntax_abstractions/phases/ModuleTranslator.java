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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import xtc.parser.ModuleDependency;
import xtc.parser.ModuleImport;
import xtc.parser.ModuleName;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.GrammarEnv;
import com.sun.fortress.syntax_abstractions.intermediate.ContractedNonterminal;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.intermediate.UserModule;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.useful.HasAt;

import edu.rice.cs.plt.tuple.Option;

/*
 * This class transforms the grammar structure into a Rats! module structure
 * without translating the nonterminal declarations.
 */

public class ModuleTranslator {

	/**
	 * Result of the module translation
	 */
	public static class Result extends StaticPhaseResult {
		Collection<Module> modules;

		public Result(Collection<Module> modules, 
				Collection<StaticError> errors) {
			super(errors);
			this.modules = modules;
		}

		public Result(Collection<Module> modules,
				Iterable<? extends StaticError> errors) {
			super(errors);
			this.modules = modules;
		}

		public Collection<Module> modules() { return modules; }
	}

	private static Collection<StaticError> _errors;

	private static void error(String mess, HasAt loc) {
		_errors.add(StaticError.make(mess, loc));
	}

	public static Result translate(Collection<GrammarEnv> environments) {
		_errors = new LinkedList<StaticError>(); 
		ModuleEnvironment menv = new ModuleEnvironment();
		for (GrammarEnv env: environments) {
			for (GrammarIndex g: env.getGrammars()) {
				if (env.isToplevel(g)) {
					NonterminalContractor nc = new NonterminalContractor();
					for (ContractedNonterminal cnt: nc.getContractionList(g)) {
						menv.add(cnt);
					}
				}
			}
		}

		renameModulesToFreshName(menv);
		return new Result(menv.getModules(), _errors);
	}

	private static void renameModulesToFreshName(ModuleEnvironment menv) {
		Map<String, QualifiedIdName> moduleNames = new HashMap<String, QualifiedIdName>();
		for (Module module: menv.getModules()) {
			if (module instanceof UserModule) {
				QualifiedIdName freshName = getFreshName(module.getName().toString(), moduleNames);
				module.setName(freshName);
			}
			Set<ModuleName> ls = new LinkedHashSet<ModuleName>();
			for (ModuleName mn: module.getParameters()) {
				String name = getFreshName(mn.name, menv);
				ls.add(new ModuleName(renameModule(name, moduleNames)));
			}
			module.setParameters(ls);
			Set<ModuleDependency> ms = new LinkedHashSet<ModuleDependency>();
			for (ModuleDependency mp: module.getDependencies()) {
				String name = getFreshName(mp.module.name, menv);
				ms.add(new ModuleImport(new ModuleName(renameModule(name, moduleNames))));
			}
			module.setDependencies(ms);
		}
	}

	private static String getFreshName(String name, ModuleEnvironment menv) {
		APIName apiName = NodeFactory.makeAPIName(name);
		Id id = apiName.getIds().remove(apiName.getIds().size()-1);
		QualifiedIdName qName = NodeFactory.makeQualifiedIdName(apiName, id);
		Option<QualifiedIdName> on = menv.getContractedName(qName);
		String nm = name;
		if (on.isSome()) {
			if (ModuleInfo.isFortressModule(Option.unwrap(on))) {
				return Option.unwrap(Option.unwrap(on).getApi()).getIds().get(1).toString();
			}
			else {
				return Option.unwrap(on).toString();
			}
		}
		return nm;
	}
	
	/**
	 * Renames the given module name to a fresh name, 
	 * if it is not the name of a core Fortress module.
	 * @param nm
	 * @param moduleNames
	 * @return
	 */
	private static String renameModule(String nm,
			Map<String, QualifiedIdName> moduleNames) {
		if (ModuleInfo.getFortressModuleNames().contains(nm)) {
			return nm;
		}
		return getFreshName(nm, moduleNames).toString();
	}

	private static QualifiedIdName getFreshName(String name,
			Map<String, QualifiedIdName> moduleNames) {
		QualifiedIdName freshName;
		if (moduleNames.containsKey(name)) {
			freshName = moduleNames.get(name);
		}
		else {
			String fn = FreshName.getFreshName(name.toString().replace('.', '_'));
			freshName = NodeFactory.makeQualifiedIdName(new Id(fn));
			moduleNames.put(name, freshName);
		}
		return freshName;
	}

}
