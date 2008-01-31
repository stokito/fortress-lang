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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NonterminalDecl;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.syntax_abstractions.util.SyntaxAbstractionUtil;

import edu.rice.cs.plt.tuple.Option;

public class GrammarAnalyzer {

	/**
	 * Returns the set of all methods contained in a the grammar represented by the index.
	 * @param name
	 * @param grammarIndex
	 * @return
	 */
	public Collection<ProductionIndex<? extends NonterminalDecl>> getContainedSet(GrammarIndex grammarIndex) {
		Collection<ProductionIndex<? extends NonterminalDecl>> c = getDeclaredSet(grammarIndex);
		c.addAll(getInheritedSet(grammarIndex));
		return c;
	}

	/**
	 * Returns the collection of nonterminal definitions with the same name as
	 * provided as argument.
	 * @param name
	 * @param g
	 * @return
	 */
	public Set<QualifiedIdName> getContained(Id name, GrammarIndex g) {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (ProductionIndex<? extends NonterminalDecl> n: getContainedSet(g)) {
			if (n.getName().getName().equals(name)) {
				rs.add(n.getName());
			}
		}
		return rs;
	}
	
	public Set<ProductionIndex<? extends NonterminalDecl>> getContaindNonterminalIndex(
			Id name, GrammarIndex g) {
		Set<ProductionIndex<? extends NonterminalDecl>> rs = new HashSet<ProductionIndex<? extends NonterminalDecl>>();
		for (ProductionIndex<? extends NonterminalDecl> n: getContainedSet(g)) {
			if (n.getName().getName().equals(name)) {
				rs.add(n);
			}
		}
		return rs;
	}
	
	/**
	 * Returns the collection of inherited nonterminal definitions with the same name as
	 * provided as argument.
	 * @param name
	 * @param g
	 * @return
	 */
	public Set<QualifiedIdName> getInherited(
			Id name, GrammarIndex g) {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (ProductionIndex<? extends NonterminalDecl> n: getInheritedSet(g)) {
			if (n.getName().getName().equals(name)) {
//				if (g.ast().isSome()) {
//					QualifiedIdName gname = Option.unwrap(g.ast()).getName();
//					APIName gApi = Option.unwrap(gname.getApi());
//					rs.addAll(Collections.singleton(SyntaxAbstractionUtil.qualifyProductionName(gApi , gname.getName(), name)));				
//				}
				rs.add(n.getName());
			}
		}
		return rs;
	}

	
	/**
	 * Returns a set of all the nonterminals inherited by the grammar represented 
	 * by the grammar index. 
	 * @param name
	 * @param grammarIndex
	 * @return
	 */
	private Collection<ProductionIndex<? extends NonterminalDecl>> getInheritedSet(GrammarIndex grammarIndex) {
		Collection<ProductionIndex<? extends NonterminalDecl>> nonterminals = new LinkedList<ProductionIndex<? extends NonterminalDecl>>(); 
		for (GrammarIndex gi: grammarIndex.getExtendedGrammars()) {
			for (ProductionIndex<? extends NonterminalDecl> n: getContainedSet(gi)) {
				if (!n.isPrivate()) {
					nonterminals.add(n);
				}
			}
		}
		return nonterminals;
	}

	/**
	 * Returns a set of nonterminals which are declared in the grammar represented
	 * by the grammar index.
	 * @param name
	 * @param grammarIndex
	 * @return
	 */
	private Collection<ProductionIndex<? extends NonterminalDecl>> getDeclaredSet(GrammarIndex grammarIndex) {
		Collection<ProductionIndex<? extends NonterminalDecl>> declaredSet = new LinkedList<ProductionIndex<? extends NonterminalDecl>>();
		declaredSet.addAll(grammarIndex.productions());
		return declaredSet; 
	}

}
