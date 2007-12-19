/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.compiler.index.TypeConsIndex;

public abstract class DelegatingNameEnv extends NameEnv {
    private NameEnv _parent;

    protected DelegatingNameEnv(NameEnv parent) {
        _parent = parent;
    }

    public Option<APIName> apiName(APIName name) {
        return _parent.apiName(name);
    }

    public boolean hasTypeParam(Id name) {
        return _parent.hasTypeParam(name);
    }

    public Set<QualifiedIdName> explicitTypeConsNames(Id name) {
        return _parent.explicitTypeConsNames(name);
    }
    public Set<QualifiedIdName> explicitVariableNames(Id name) {
        return _parent.explicitVariableNames(name);
    }
    public Set<QualifiedIdName> explicitFunctionNames(Id name) {
        return _parent.explicitFunctionNames(name);
    }
    public Set<QualifiedOpName> explicitFunctionNames(OpName name) {
        return _parent.explicitFunctionNames(name);
    }

    public Set<QualifiedIdName> onDemandTypeConsNames(Id name) {
        return _parent.onDemandTypeConsNames(name);
    }
    public Set<QualifiedIdName> onDemandVariableNames(Id name) {
        return _parent.onDemandVariableNames(name);
    }
    public Set<QualifiedIdName> onDemandFunctionNames(Id name) {
        return _parent.onDemandFunctionNames(name);
    }
    public Set<QualifiedOpName> onDemandFunctionNames(OpName name) {
        return _parent.onDemandFunctionNames(name);
    }


    public boolean hasQualifiedTypeCons(QualifiedIdName name) {
        return _parent.hasQualifiedTypeCons(name);
    }
    public boolean hasQualifiedVariable(QualifiedIdName name) {
        return _parent.hasQualifiedVariable(name);
    }
    public boolean hasQualifiedFunction(QualifiedIdName name) {
        return _parent.hasQualifiedFunction(name);
    }

    public TypeConsIndex typeConsIndex(QualifiedIdName name) {
        return _parent.typeConsIndex(name);
    }

}
