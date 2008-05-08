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

package com.sun.fortress.syntax_abstractions.environments;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.index.GrammarIndex;

public class GlobalGrammarEnv {

	private Map<GrammarIndex, Boolean> grammars;

	public GlobalGrammarEnv() {
		this.grammars = new HashMap<GrammarIndex, Boolean>();
	}

	public GlobalGrammarEnv(Collection<GrammarIndex> gs, boolean isTopLevel) {
		this();
		for (GrammarIndex g: gs) {
			this.addGrammar(g, isTopLevel);
		}
	}

	public void addGrammar(GrammarIndex g, boolean isTopLevel) {
		this.grammars.put(g, isTopLevel);
	}

	public Collection<GrammarIndex> getGrammars() {
		return this.grammars.keySet();
	}

	public boolean isToplevel(GrammarIndex g) {
		if (this.grammars.containsKey(g)) {
			return this.grammars.get(g);
		}
		return false;
	}
	
}
