/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LocalStaticParamEnv extends DelegatingTypeNameEnv {
    private List<StaticParam> _staticParams;

    public LocalStaticParamEnv(TypeNameEnv parent, List<StaticParam> staticParams) {
        super(parent);
        _staticParams = staticParams;
    }

    public Option<StaticParam> hasTypeParam(IdOrOp name) {
        for (StaticParam typeVar : _staticParams) {
            if (typeVar.getName().equals(name)) {
                return Option.some(typeVar);
            }
        }
        return super.hasTypeParam(name);
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

    @Override
    public String toString() {
        return (Useful.listInOxfords(_staticParams) + "  " + super.toString());
    }
}
