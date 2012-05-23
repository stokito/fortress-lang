/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import com.sun.fortress.nodes.Id;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LocalVarEnv extends DelegatingNameEnv {
    private Set<Id> _vars;

    public LocalVarEnv(NameEnv parent, Set<Id> vars) {
        super(parent);
        _vars = vars;
    }

    @Override
    public Set<Id> explicitVariableNames(Id name) {
        for (Id var : _vars) {
            if (var.getText().equals(name.getText())) {
                return Collections.singleton(var);
            }
        }
        return super.explicitVariableNames(name);
    }

    @Override
    public List<Id> explicitVariableNames() {
        List<Id> result = new LinkedList<Id>();
        result.addAll(_vars);
        result.addAll(_parent.explicitVariableNames());
        return result;
    }

}
