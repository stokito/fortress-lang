/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import edu.rice.cs.plt.collect.CollectUtil;

import java.util.Set;

public class LocalGetterSetterEnv extends DelegatingNameEnv {

    private Set<Id> _gettersAndSetters;

    protected LocalGetterSetterEnv(NameEnv parent, Set<Id> gettersAndSetters) {
        super(parent);
        _gettersAndSetters = gettersAndSetters;
    }

    @Override
    public Set<IdOrOp> explicitFunctionNames(IdOrOp name) {
        if (_gettersAndSetters.contains(name)) {
            return CollectUtil.singleton(name);
        } else {
            return super.explicitFunctionNames(name);
        }
    }

    @Override
    public Set<IdOrOp> unambiguousFunctionNames(IdOrOp name) {
        if (_gettersAndSetters.contains(name)) {
            return CollectUtil.singleton(name);
        } else {
            return super.unambiguousFunctionNames(name);
        }
    }

}
