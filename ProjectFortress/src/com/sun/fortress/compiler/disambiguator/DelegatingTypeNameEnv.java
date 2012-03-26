/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.StaticParam;
import edu.rice.cs.plt.tuple.Option;

import java.util.Set;

public abstract class DelegatingTypeNameEnv extends TypeNameEnv {
    private TypeNameEnv _parent;

    protected DelegatingTypeNameEnv(TypeNameEnv parent) {
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

    public Set<Id> onDemandTypeConsNames(Id name) {
        return _parent.onDemandTypeConsNames(name);
    }

    public boolean hasQualifiedTypeCons(Id name) {
        return _parent.hasQualifiedTypeCons(name);
    }

    public TypeConsIndex typeConsIndex(Id name) {
        return _parent.typeConsIndex(name);
    }

    public String toString() {
        return _parent.toString();
    }

}
