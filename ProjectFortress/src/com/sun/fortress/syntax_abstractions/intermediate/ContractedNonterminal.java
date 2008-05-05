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

package com.sun.fortress.syntax_abstractions.intermediate;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarNonterminalIndex;
import com.sun.fortress.compiler.index.GrammarTerminalIndex;
import com.sun.fortress.compiler.index.NonterminalDefIndex;
import com.sun.fortress.compiler.index.NonterminalIndex;
import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.NonterminalDef;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.tuple.Option;


public class ContractedNonterminal {

	/* The nonterminals are kept in a list corresponding to the ordering in which
	 * their alternatives should be merged.
	 */
	private List<NonterminalIndex<? extends GrammarMemberDecl>> members;
	private Set<Id> dependencies;
	private List<Id> contractedNames;

	public ContractedNonterminal(List<NonterminalIndex<? extends GrammarMemberDecl>> ls,
								 Set<Id> dependencies) {
		this.members = ls;
		this.dependencies = dependencies;
		this.contractedNames = new LinkedList<Id>();
	}

	public ContractedNonterminal(NonterminalIndex<? extends GrammarMemberDecl> n,
								 Set<Id> dependencies) {
		this.members = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
		this.members.add(n);
		this.dependencies = dependencies;
		this.contractedNames = new LinkedList<Id>();
	}

	/**
	 * Contract all the nonterminals to one new nonterminal preserving the order of
	 * alternatives according the position in the list.
	 * We assume the list is non-empty.
	 * @return
	 */
	public NonterminalIndex<? extends GrammarMemberDecl> getNonterminal() {
		List<SyntaxDef> syntaxDefs = new LinkedList<SyntaxDef>();
		Id name = this.getName();
		for (NonterminalIndex/*<? extends GrammarMemberDecl>*/ gnt: this.members) {
			contractedNames.add(gnt.getName());
			if (gnt instanceof GrammarNonterminalIndex) {
				syntaxDefs.addAll(((GrammarNonterminalIndex) gnt).getSyntaxDefs());
			}
			if (gnt instanceof GrammarTerminalIndex) {
				syntaxDefs.add(((GrammarTerminalIndex) gnt).getSyntaxDef());
			}
		}
		Span span = this.members.get(0).getAst().getSpan();
		Option<TraitType> type = this.members.get(0).getAst().getType();

		List<Pair<Id, Type>> params = this.members.get(0).getAst().getParams();
		NonterminalDef nonterminal = new NonterminalDef(span, name, type, Option.<Modifier>none(), params, syntaxDefs);
		Option<NonterminalDef> nonterminalDef = Option.some(nonterminal);
		return new NonterminalDefIndex(nonterminalDef);
	}

	/**
	 * Returns the canonical name of the contracted nonterminal which is the name of
	 * the nonterminal definition
	 * @return
	 */
	public Id getName() {
		for (NonterminalIndex/*<? extends GrammarMemberDecl>*/ member: this.members) {
			if (member instanceof NonterminalDefIndex) {
				return member.getName();
			}
		}
		return this.members.get(0).getName();
	}

	public Set<Id> getDependencies() {
		return this.dependencies;
	}

	/**
	 * Returns a list of nonterminal names which are contracted to the
	 * nonterminal represented by this object.
	 * @return
	 */
	public List<Id> getContractedNames() {
		return this.contractedNames;
	}
}
