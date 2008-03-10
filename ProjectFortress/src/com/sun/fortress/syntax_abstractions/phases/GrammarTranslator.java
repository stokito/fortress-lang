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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleList;
import xtc.parser.ModuleName;
import xtc.parser.Production;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.syntax_abstractions.intermediate.FortressModule;
import com.sun.fortress.syntax_abstractions.intermediate.UserModule;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;

public class GrammarTranslator {
	private Collection<Module> ratsModules;
	private Iterable<? extends StaticError> errors;
	
	public class Result extends StaticPhaseResult {

		public Result(Iterable<? extends StaticError> errors) {
			super(errors);
		}

		public Collection<Module> modules() { return ratsModules; }
	}
	
	public GrammarTranslator() {
		ratsModules = new LinkedList<Module>();
		errors = new LinkedList<StaticError>();
	}

	public static Result translate(Collection<com.sun.fortress.syntax_abstractions.intermediate.Module> modules,
			GlobalEnvironment env) {
		return (new GrammarTranslator()).doTranslation(modules, env);
	}

	private Result doTranslation(
			Collection<com.sun.fortress.syntax_abstractions.intermediate.Module> modules,
			GlobalEnvironment env) {	
		
		NonterminalTypeDictionary.addAll(modules);		
		
		for (com.sun.fortress.syntax_abstractions.intermediate.Module module: modules) {
			if (module instanceof FortressModule) {
				ratsModules.add(makeFortressModule((FortressModule) module));
			}
			if (module instanceof UserModule) {
				ratsModules.add(makeUserModule((UserModule) module));
			}
		}
		ratsModules.add(loadFortressGrammarModule(modules));
		ratsModules.add(loadKeywordGrammarModule(modules));
		return new Result(errors);
	}

	/**
	 * Load the corresponding Fortress grammar module and add the 
	 * translated syntax definitions to the relevant productions.
	 * @param module
	 * @return
	 */
	private Module makeFortressModule(FortressModule module) {
		Module m = RatsUtil.getRatsModule(RatsUtil.getParserPath()+module.getName().toString()+".rats");
		
		List<ModuleName> ls = new LinkedList<ModuleName>();
		ls.addAll(m.parameters.names);
		ls.addAll(module.getParameters());
		m.parameters = new ModuleList(ls);
		
		List<ModuleDependency> mds = new LinkedList<ModuleDependency>();
		mds.addAll(m.dependencies);
		mds.addAll(module.getDependencies());
		m.dependencies = mds;
		
		for (Production p: m.productions) {
			for (NonterminalIndex<? extends GrammarMemberDecl> member: module.getDeclaredNonterminals()) {
				if (member.getName().getName().toString().equals(p.name.name)) {
					SyntaxDefTranslator.Result ptr = SyntaxDefTranslator.translate(member);				
					p.choice.alternatives.addAll(ptr.alternatives());
				}
			}
		}
		
		m.documentation.text.addAll(RatsUtil.getComment().text);
		return m;
	}

	/**
	 * Make a new module with the members defined in the module. 
	 * @param module
	 * @return
	 */
	private Module makeUserModule(UserModule module) {
		Module m = RatsUtil.makeExtendingRatsModule(module);
		MemberTranslator.Result ptr = MemberTranslator.translate(module.getDeclaredNonterminals());
		m.productions = ptr.productions();
		return m;
	}

	/**
	 * Load Fortress.rats and modify the instantiations so the correct number of
	 * parameters are passed
	 * @param modules
	 * @return
	 */
	private Module loadFortressGrammarModule(
			Collection<com.sun.fortress.syntax_abstractions.intermediate.Module> modules) {
		Module m = RatsUtil.getRatsModule(RatsUtil.getParserPath()+"Fortress.rats");
		for (com.sun.fortress.syntax_abstractions.intermediate.Module module: modules) { 
			if (module instanceof FortressModule) {
				for (ModuleDependency md: m.dependencies) {
					if (md instanceof ModuleInstantiation) {
						if (md.module.name.equals(RatsUtil.getModuleNamePrefix()+module.getName().toString())) {
							List<ModuleName> deps = new LinkedList<ModuleName>();
							ModuleInstantiation mi = (ModuleInstantiation) md;
							deps.addAll(mi.arguments.names);
							deps.addAll(module.getParameters());
							mi.arguments = new ModuleList(deps);
						}
					}
				}
			}
			if (module instanceof UserModule) {
				ModuleName name = new ModuleName(RatsUtil.getModuleNamePrefix()+module.getName().toString());
				List<ModuleName> params = new LinkedList<ModuleName>();
				params.addAll(module.getParameters());
				ModuleList parameters = new ModuleList(params);
				m.dependencies.add(new ModuleInstantiation(name, parameters, new ModuleName(module.getName().toString())));
			}
		}
		return m;
	}

	private Module loadKeywordGrammarModule(
			Collection<com.sun.fortress.syntax_abstractions.intermediate.Module> modules) {
		Module m = RatsUtil.getRatsModule(RatsUtil.getParserPath()+"Keyword.rats");
		
		Set<String> keywords = new HashSet<String>();
		for (com.sun.fortress.syntax_abstractions.intermediate.Module module: modules) {
			keywords.addAll(module.getKeywords());
		}
		
		// Adding keyword to the set of FORTRESS_KEYWORDS
		List<String> code = m.body.code;
		String insertString = "";
		int inx = 0;
		// Locate insertion point
		for (inx=0; inx<code.size(); inx++) {
			if (code.get(inx).contains("}")) {
				insertString = code.get(inx);
				break;
			}
		}
		for (String keyword: keywords) {
			int insertPoint = insertString.indexOf("}");
			if (insertPoint < 0) {
				throw new RuntimeException("Expected to add keyword, but found no insert point "+insertString);
			}
			else {
				if (!com.sun.fortress.parser.Fortress.FORTRESS_KEYWORDS.contains(keyword)) {
					insertString = insertString.substring(0, insertPoint) + ", \""+keyword+"\""+insertString.substring(insertPoint);
				}
			}
		}
		code.remove(inx);
		code.add(inx, insertString);
		
		return m;
	}
}
