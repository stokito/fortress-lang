/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;

public abstract class DelegatingNameEnv extends NameEnv {
    protected NameEnv _parent;

    protected DelegatingNameEnv(NameEnv parent) {
        _parent = parent;
    }

    public Option<APIName> apiName(APIName name) {
        return _parent.apiName(name);
    }

    public Option<StaticParam> hasTypeParam(IdOrOp name) {
        return _parent.hasTypeParam(name);
    }

    public Set<Id> explicitTypeConsNames(Id name) {
        return _parent.explicitTypeConsNames(name);
    }
    public Set<Id> explicitVariableNames(Id name) {
        return _parent.explicitVariableNames(name);
    }
    public List<Id> explicitVariableNames() {
        return _parent.explicitVariableNames();
    }
    public Set<IdOrOp> explicitFunctionNames(IdOrOp name) {
        return _parent.explicitFunctionNames(name);
    }
    public Set<IdOrOp> unambiguousFunctionNames(IdOrOp name) {
        return _parent.unambiguousFunctionNames(name);
    }

    public Set<Id> onDemandTypeConsNames(Id name) {
        return _parent.onDemandTypeConsNames(name);
    }
    public Set<Id> onDemandVariableNames(Id name) {
        return _parent.onDemandVariableNames(name);
    }
    public Set<Id> onDemandFunctionNames(Id name) {
        return _parent.onDemandFunctionNames(name);
    }
    public Set<Op> onDemandFunctionNames(Op name) {
        return _parent.onDemandFunctionNames(name);
    }


    public boolean hasQualifiedTypeCons(Id name) {
        return _parent.hasQualifiedTypeCons(name);
    }
    public boolean hasQualifiedVariable(Id name) {
        return _parent.hasQualifiedVariable(name);
    }
    public boolean hasQualifiedFunction(Id name) {
        return _parent.hasQualifiedFunction(name);
    }

    public TypeConsIndex typeConsIndex(Id name) {
        return _parent.typeConsIndex(name);
    }

	@Override
	public Set<Id> explicitGrammarNames(String name) {
        return Collections.emptySet();
    }

    @Override
	public boolean hasGrammar(String name) {
        return false;
    }

    @Override
	public boolean hasQualifiedGrammar(Id name) {
        return false;
    }

    @Override
        public Set<Id> onDemandGrammarNames(String name) {
        return Collections.emptySet();
    }

    @Override
	public Option<GrammarIndex> grammarIndex(Id name) {
        return Option.none();
    }

}
