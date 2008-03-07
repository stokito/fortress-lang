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

import com.sun.fortress.nodes.SyntaxDef;
import com.sun.fortress.nodes._TerminalDef;

import edu.rice.cs.plt.tuple.Option;

public class GrammarTerminalIndex extends NonterminalIndex<_TerminalDef> {

	public GrammarTerminalIndex(Option<_TerminalDef> ast) {
		super(ast);
	}

	public SyntaxDef getSyntaxDef() {
		if (this.ast().isNone()) {
			throw new RuntimeException("Ast not found.");
		}
		return Option.unwrap(this.ast()).getSyntaxDef();
	}

}
