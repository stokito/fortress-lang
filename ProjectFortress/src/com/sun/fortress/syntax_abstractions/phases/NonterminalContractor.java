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
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.GrammarNonterminalIndex;
import com.sun.fortress.compiler.index.NonterminalDefIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeVisitor;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.NonterminalExtensionDef;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.syntax_abstractions.intermediate.ContractedNonterminal;

import edu.rice.cs.plt.tuple.Option;

public class NonterminalContractor {

	private Collection<GrammarIndex> visitedGrammars;

	/**
	 * Given a grammar index g, return a collection of contained terminals and nonterminals where 
	 * each overriding nonterminal has been contracted so it contains all the alternatives 
	 * of the nonterminals it overrides.
	 * @param g
	 * @return A collection of grammar member declarations
	 */
	public Collection<ContractedNonterminal> getContractionList(GrammarIndex g) {
		Collection<ContractedNonterminal> result = new LinkedList<ContractedNonterminal>();		
		GrammarAnalyzer<GrammarIndex> analyzer = new GrammarAnalyzer<GrammarIndex>();

		for (NonterminalIndex<? extends GrammarMemberDecl> n: analyzer.getContainedSet(g)) {
			DependencyCollector dc = new DependencyCollector(Option.unwrap(n.getName().getApi()));
			n.getAst().accept(dc);
			Set<QualifiedIdName> dependencies = dc.getResult();
			if (n.getAst() instanceof NonterminalExtensionDef) {
				visitedGrammars = new LinkedList<GrammarIndex>();

				List<NonterminalIndex<? extends GrammarMemberDecl>> ls = getCollapsedNonterminal(n.getName().getName(), g);			
				dependencies.addAll(getDependencies(ls));
				
				result.add(new ContractedNonterminal(ls, dependencies));
			}
			else {
				result.add(new ContractedNonterminal(n, dependencies));
			}
		}
		return result;
	}

	private Set<QualifiedIdName> getDependencies(
			List<NonterminalIndex<? extends GrammarMemberDecl>> ls) {
		Set<QualifiedIdName> s = new HashSet<QualifiedIdName>();
		for (NonterminalIndex<? extends GrammarMemberDecl> m: ls) {
			DependencyCollector dc = new DependencyCollector(Option.unwrap(m.getName().getApi()));
			m.getAst().accept(dc);
			s.addAll(dc.getResult());
		}
		return s ;
	}

	/**
	 * Returns a set of all the members which the member with the given name  
	 * overrides.
	 * @param name
	 * @param a
	 * @return
	 */
	private List<NonterminalIndex<? extends GrammarMemberDecl>> getCollapsedNonterminal(Id name, GrammarIndex g) {
		visitedGrammars.add(g);		
		List<NonterminalIndex<? extends GrammarMemberDecl>> ls = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>(); 
		for (GrammarIndex gi: g.getExtended()) {
			if (!visitedGrammars.contains(gi)) {
				ls.addAll(getCollapsedNonterminal(name, gi));
			}
		}
		Option<GrammarNonterminalIndex<? extends NonterminalDecl>> cnd = g.getNonterminalDecl(name);
		if (cnd.isSome()) {
			ls.add(Option.unwrap(cnd));
		}
		return ls;
	}

}
