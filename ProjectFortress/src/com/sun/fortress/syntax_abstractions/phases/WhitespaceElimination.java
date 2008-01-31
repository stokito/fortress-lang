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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.NoWhitespaceSymbol;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes.SyntaxSymbol;
import com.sun.fortress.nodes.WhitespaceSymbol;

public class WhitespaceElimination extends NodeUpdateVisitor {

	@Override
	public Node forSyntaxDef(SyntaxDef that) {
		List<SyntaxSymbol> ls = new LinkedList<SyntaxSymbol>();
		Iterator<SyntaxSymbol> it = that.getSyntaxSymbols().iterator();
		boolean ignoreWhitespace = false;
		while (it.hasNext()) {
			SyntaxSymbol symbol = it.next();
			if (!ignoreWhitespace || !(symbol instanceof WhitespaceSymbol)) {			
				if (symbol instanceof NoWhitespaceSymbol) {
					symbol = ((NoWhitespaceSymbol) symbol).getSymbol();
					ignoreWhitespace = true;
				}
				else {
					ignoreWhitespace = false;
				}
				ls.add(symbol);
			}
		}
		return new SyntaxDef(that.getSpan(),ls, that.getTransformationExpression());
	}

}

