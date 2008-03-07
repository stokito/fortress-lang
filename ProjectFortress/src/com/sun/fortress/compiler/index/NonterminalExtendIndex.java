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

package com.sun.fortress.compiler.index;

import java.util.Collection;
import java.util.LinkedList;

import com.sun.fortress.nodes.GrammarMemberDecl;
import com.sun.fortress.nodes.NonterminalExtensionDef;

import edu.rice.cs.plt.tuple.Option;

public class NonterminalExtendIndex extends GrammarNonterminalIndex<NonterminalExtensionDef> {

	private Collection<NonterminalIndex<? extends GrammarMemberDecl>> extend;
	
	public NonterminalExtendIndex(Option<NonterminalExtensionDef> ast) {
		super(ast);
		this.extend = new LinkedList<NonterminalIndex<? extends GrammarMemberDecl>>();
	}

	public Collection<NonterminalIndex<? extends GrammarMemberDecl>> getExtends() {
		return this.extend;
	}

	public void addExtendedNonterminal(NonterminalIndex<? extends GrammarMemberDecl> ext) {
		this.extend.add(ext);
	}
	
	public void addExtendedNonterminals(Collection<NonterminalIndex<? extends GrammarMemberDecl>> ext) {
		this.extend.addAll(ext);
	}
}
