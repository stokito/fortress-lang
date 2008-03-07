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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xtc.parser.ModuleImport;
import xtc.parser.ModuleName;

import com.sun.fortress.compiler.index.GrammarNonterminalIndex;
import com.sun.fortress.compiler.index.GrammarTerminalIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes._TerminalDef;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.syntax_abstractions.intermediate.ContractedNonterminal;
import com.sun.fortress.syntax_abstractions.intermediate.FortressModule;
import com.sun.fortress.syntax_abstractions.intermediate.Module;
import com.sun.fortress.syntax_abstractions.intermediate.UserModule;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

public class ModuleEnvironment {

	private Map<QualifiedIdName, Module> modules;
	private Map<QualifiedIdName, QualifiedIdName> contractedNames;

	public ModuleEnvironment() {
		this.modules = new HashMap<QualifiedIdName, Module>();
		this.contractedNames = new HashMap<QualifiedIdName, QualifiedIdName>();
	}

	/**
	 * Returns the collection of modules in the environment
	 * @return
	 */
	public Collection<Module> getModules() {
		return this.modules.values();
	}

	/**
	 * Given a contracted nonterminal, decide whether to add the alternative to an existing
	 * module or create a new module.
	 * @param member
	 */	
	public void add(ContractedNonterminal cnt) {
		Module module = makeNewModule(cnt);
		Option<Module> om = this.getRelated(module.getName());
		if (om.isSome()) {
			module = this.merge(Option.unwrap(om), module);
		}
		this.modules.put(module.getName(), module);
	}

	/**
	 * Two modules are related if they have the same name.
	 * @param cnt
	 * @return
	 */
	private Option<Module> getRelated(QualifiedIdName name) {
		if (this.modules.keySet().contains(name)) {
			return Option.some(this.modules.get(name));
		}
		return Option.none();
	}

	private Module merge(Module m1, Module m2) {
		if ((m1 instanceof FortressModule) && 
				(m2 instanceof FortressModule)) {
			return merge((FortressModule) m1, (FortressModule) m2);
		}
		else if ((m1 instanceof UserModule) && 
				(m2 instanceof UserModule)) {
			return merge((UserModule) m1, (UserModule) m2);
		}
		return m1;
	}

	private UserModule merge(UserModule m1, UserModule m2) {
		NonterminalIndex<? extends GrammarMemberDecl> member1 = IterUtil.first(m1.getDeclaredNonterminals());
		NonterminalIndex<? extends GrammarMemberDecl> member2 = IterUtil.first(m2.getDeclaredNonterminals());
		
		if ((member1 instanceof GrammarNonterminalIndex) &&
				(member2 instanceof GrammarNonterminalIndex)) {
			GrammarNonterminalIndex<? extends NonterminalDecl> cni1 = (GrammarNonterminalIndex) member1;
			GrammarNonterminalIndex<? extends NonterminalDecl> cni2 = (GrammarNonterminalIndex) member2;
			for (SyntaxDef s: cni2.getSyntaxDefs()) {
				if (!cni1.getSyntaxDefs().contains(s)) {
					cni1.getSyntaxDefs().add(s);
				}
			}
			m1.getDependencies().addAll(m2.getDependencies());
			m1.getParameters().addAll(m2.getParameters());
			return m1;
		}
		return m1;
	}

	private FortressModule merge(FortressModule m1, FortressModule m2) {
		for (NonterminalIndex<? extends GrammarMemberDecl> member2: m2.getDeclaredNonterminals()) {
			boolean found = false;
			for (NonterminalIndex<? extends GrammarMemberDecl> member1: m1.getDeclaredNonterminals()) {
				if (member2.getName().equals(member1.getName())) {
					if ((member1 instanceof GrammarNonterminalIndex) &&
						(member2 instanceof GrammarNonterminalIndex)) {
						GrammarNonterminalIndex<? extends NonterminalDecl> cni1 = (GrammarNonterminalIndex) member1;
						GrammarNonterminalIndex<? extends NonterminalDecl> cni2 = (GrammarNonterminalIndex) member2;
						// We may inherit the same alternative from multiple parents
						// which is not allowed
						for (SyntaxDef s: cni2.getSyntaxDefs()) {
							if (!cni1.getSyntaxDefs().contains(s)) {
								cni1.getSyntaxDefs().add(s);
							}
						}
						m1.getDependencies().addAll(m2.getDependencies());
						m1.getParameters().addAll(m2.getParameters());
						found = true;
					}
				}
			}
			if (!found) {
				m1.addNonterminal(member2);
			}
		}
		return m1;
	}

	private Module makeNewModule(ContractedNonterminal nt) {
		Collection<NonterminalIndex<? extends GrammarMemberDecl>> ls = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
		ls.add(nt.getNonterminal());
		Module m;
		if (ModuleInfo.isFortressModule(nt.getName())) {
			APIName apiName = Option.unwrap(nt.getName().getApi());
			QualifiedIdName newName = NodeFactory.makeQualifiedIdName(apiName.getIds().get(1));
			m = new FortressModule(newName, ls);
		}
		else {
			m = new UserModule(nt.getName(), ls);
			// Add extra depencides
			if (!m.getTokens().isEmpty() || !m.getKeywords().isEmpty()) {
				ModuleName identifier = new ModuleName("Identifier");
				m.getDependencies().add(new ModuleImport(identifier ));
				m.getParameters().add(identifier);
			}
			// Spacing is actually only needed if whitespace is used.
			ModuleName spacing = new ModuleName("Spacing");
			m.getDependencies().add(new ModuleImport(spacing));
			m.getParameters().add(spacing);
		}
		m.getParameters().addAll(makeParameters(nt.getDependencies()));
		m.getDependencies().addAll(makeDependencies(nt.getDependencies()));
		addToNameContractionMap(nt.getContractedNames(), nt.getName());
		return m;
	}

	private Collection<ModuleName> makeParameters(
			Set<QualifiedIdName> dependencies) {
		Collection<ModuleName> dep = new LinkedList<ModuleName>();
		for (QualifiedIdName q: dependencies) {
			dep.add(new ModuleName(q.toString()));
		}
		return dep;
	}

	private Collection<ModuleImport> makeDependencies(
			Set<QualifiedIdName> dependencies) {
		Collection<ModuleImport> dep = new LinkedList<ModuleImport>();
		for (QualifiedIdName q: dependencies) {
			dep.add(new ModuleImport(new ModuleName(q.toString())));
		}
		return dep;
	}

	private void addToNameContractionMap(
			List<QualifiedIdName> contractedNames, QualifiedIdName name) {
		for (QualifiedIdName n: contractedNames) {
			this.contractedNames.put(n, name);
		}
	}
	
	public Option<QualifiedIdName> getContractedName(QualifiedIdName name) {
		QualifiedIdName n = this.contractedNames.get(name);
		if (n != null) {
			return Option.some(n);
		}
		return Option.none();
	}
	
}
