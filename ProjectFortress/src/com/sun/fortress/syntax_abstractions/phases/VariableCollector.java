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
import java.util.LinkedList;

import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.PrefixedSymbol;
import com.sun.fortress.nodes.GroupSymbol;
import com.sun.fortress.nodes.SyntaxSymbol;

public class VariableCollector extends NodeDepthFirstVisitor<Collection<PrefixedSymbol>> {

	@Override
	public Collection<PrefixedSymbol> defaultCase(com.sun.fortress.nodes.Node that) {
		return new LinkedList<PrefixedSymbol>();
	}	
	
	@Override
	public Collection<PrefixedSymbol> forPrefixedSymbol(PrefixedSymbol that) {
		Collection<PrefixedSymbol> c = super.forPrefixedSymbol(that);
		if (that.getId().isSome()) {
			c.add(that);
		}
		return c;
	}

	@Override
	public Collection<PrefixedSymbol> forGroupSymbol(GroupSymbol that) {
		Collection<PrefixedSymbol> c = super.forGroupSymbol(that);
		for ( SyntaxSymbol symbol : that.getSymbols() ){
			c.addAll( symbol.accept(this) );
		}
		System.out.println( "Bound symbols for group: " + c );
		return c;
	}
}
