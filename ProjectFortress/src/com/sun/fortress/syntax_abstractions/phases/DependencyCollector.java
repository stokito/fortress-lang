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

import java.util.HashSet;
import java.util.Set;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NonterminalSymbol;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;


import edu.rice.cs.plt.tuple.Option;

public class DependencyCollector extends NodeDepthFirstVisitor_void {

	private APIName apiName;
	private Set<QualifiedIdName> result;
	
	public DependencyCollector(APIName apiName) {
		this.apiName = apiName;
		this.result = new HashSet<QualifiedIdName>();
	}

	@Override
	public void forNonterminalSymbolOnly(NonterminalSymbol that) {
		// We know that fortress modules are on the form FortressSyntax.moduleName.nonterminal
		if (ModuleInfo.isFortressModule(that.getNonterminal())) {
			Id id = Option.unwrap(that.getNonterminal().getApi()).getIds().get(1);
			result.add(NodeFactory.makeQualifiedIdName(id));
		}
		else {
			result.add(that.getNonterminal());
		}
	}

	public Set<QualifiedIdName> getResult() {
		return this.result;
	}

}
