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
import java.util.Set;

import com.sun.fortress.compiler.index.ProductionIndex;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.QualifiedIdName;

public class GrammarAnalyzer<T extends Analyzable<T>> {

	/**
	 * Returns the set of all methods contained in a the grammar represented by the index.
	 * @param name
	 * @param a
	 * @return
	 */
	public Collection<ProductionIndex<? extends GrammarMemberDecl>> getContainedSet(Analyzable<T> a) {
		Collection<ProductionIndex<? extends GrammarMemberDecl>> c = getDeclaredSet(a);
		c.addAll(this.getInheritedSet(a));
		return c;
	}

	/**
	 * Returns the collection of nonterminal definitions with the same name as
	 * provided as argument.
	 * @param name
	 * @param a
	 * @return
	 */
	public Set<QualifiedIdName> getContained(Id name, Analyzable<T> a) {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (ProductionIndex<? extends GrammarMemberDecl> n: getContainedSet(a)) {
			if (n.getName().getName().equals(name)) {
				rs.add(n.getName());
			}
		}
		return rs;
	}
	
	public Set<ProductionIndex<? extends GrammarMemberDecl>> getOverridingNonterminalIndex(
			Id name, Analyzable<T> a) {
		Set<ProductionIndex<? extends GrammarMemberDecl>> rs = new HashSet<ProductionIndex<? extends GrammarMemberDecl>>();
		for (ProductionIndex<? extends GrammarMemberDecl> n: getPotentiallyInheritedSet(a)) {
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
	 * @param a
	 * @return
	 */
	public Set<QualifiedIdName> getInherited(Id name, Analyzable<T> a) {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (ProductionIndex<? extends GrammarMemberDecl> n: getInheritedSet(a)) {
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
	 * Returns the collection of declared nonterminal definitions with the same name as
	 * provided as argument.
	 * @param name
	 * @param a
	 * @return
	 */
	private Set<QualifiedIdName> getDeclared(Id name, Analyzable<T> a) {
		Set<QualifiedIdName> rs = new HashSet<QualifiedIdName>();
		for (ProductionIndex<? extends GrammarMemberDecl> n: getDeclaredSet(a)) {
			if (n.getName().getName().equals(name)) {
				rs.add(n.getName());
			}
		}
		return rs;
	}
	
	/**
	 * Returns a set of all the nonterminals inherited by the grammar represented 
	 * by the grammar index. 
	 * @param name
	 * @param a
	 * @return
	 */
	private Collection<ProductionIndex<? extends GrammarMemberDecl>> getInheritedSet(Analyzable<T> a) {
		Collection<ProductionIndex<? extends GrammarMemberDecl>> nonterminals = new LinkedList<ProductionIndex<? extends GrammarMemberDecl>>(); 
		for (T gi: a.getExtended()) {
			for (ProductionIndex<? extends GrammarMemberDecl> n: this.getContainedSet(gi)) {
				if (!n.isPrivate()) {
					if (this.getDeclared(n.getName().getName(), a).isEmpty()) {
						nonterminals.add(n);
					}
				}
			}
		}
		return nonterminals;
	}

	/**
	 * Returns a set of all the nonterminals <b>potentially</b> inherited by 
	 * the grammar represented by the grammar index. 
	 * @param name
	 * @param a
	 * @return
	 */
	private Collection<ProductionIndex<? extends GrammarMemberDecl>> getPotentiallyInheritedSet(Analyzable<T> a) {
		Collection<ProductionIndex<? extends GrammarMemberDecl>> nonterminals = new LinkedList<ProductionIndex<? extends GrammarMemberDecl>>(); 
		for (T gi: a.getExtended()) {
			for (ProductionIndex<? extends GrammarMemberDecl> n: this.getContainedSet(gi)) {
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
	 * @param a
	 * @return
	 */
	private Collection<ProductionIndex<? extends GrammarMemberDecl>> getDeclaredSet(Analyzable<T> a) {
		Collection<ProductionIndex<? extends GrammarMemberDecl>> declaredSet = new LinkedList<ProductionIndex<? extends GrammarMemberDecl>>();
		declaredSet.addAll(a.getDeclaredNonterminals());
		return declaredSet; 
	}

}
