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
