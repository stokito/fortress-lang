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

import java.util.List;
import java.util.Collections;
import java.util.Set;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

public class LocalStaticParamEnv extends DelegatingTypeNameEnv {
    private List<StaticParam> _staticParams;

    public LocalStaticParamEnv(TypeNameEnv parent, List<StaticParam> staticParams) {
        super(parent);
        _staticParams = staticParams;
    }

    @Override public boolean hasTypeParam(Id name) {
        for (StaticParam typeVar : _staticParams) {
            if (typeVar instanceof IdStaticParam &&
                ((IdStaticParam)typeVar).getName().equals(name)) {
                return true;
            }
        }
        return super.hasTypeParam(name);
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
