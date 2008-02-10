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

package com.sun.fortress.compiler.disambiguator;

import java.util.Set;
import java.util.Collections;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

public class LocalVarEnv extends DelegatingNameEnv {
    private Set<Id> _vars;

    public LocalVarEnv(NameEnv parent, Set<Id> vars) {
        super(parent);
        _vars = vars;
    }

    @Override public Set<QualifiedIdName> explicitVariableNames(Id name) {
        if (_vars.contains(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return super.explicitVariableNames(name); }
    }

	@Override
	public Set<QualifiedIdName> explicitGrammarNames(QualifiedIdName name) {
		return Collections.emptySet();
	}

	@Override
	public boolean hasGrammar(QualifiedIdName name) {
		return false;
	}

	@Override
	public boolean hasQualifiedGrammar(QualifiedIdName name) {
		return false;
	}

	@Override
	public Set<QualifiedIdName> onDemandGrammarNames(Id name) {
		return Collections.emptySet();
	}

	@Override
	public Option<GrammarIndex> grammarIndex(QualifiedIdName name) {
		return Option.none();
	}

}
