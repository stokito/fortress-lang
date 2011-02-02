/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.environments;

import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;

import java.util.HashMap;
import java.util.Map;

public class NTEnv {

    protected Map<Id, BaseType> ntToType;

    protected NTEnv() {
        this(new HashMap<Id, BaseType>());
    }

    protected NTEnv(Map<Id, BaseType> ntToType) {
        this.ntToType = ntToType;
    }

    public BaseType getType(Id nt) {
        if (!ntToType.containsKey(nt)) {
            throw new MacroError(nt, "no type entry for " + nt);
        } else {
            return ntToType.get(nt);
        }
    }

    public String getJavaType(Id nt) {
        return FortressTypeToJavaType.analyze(getType(nt));
    }
}
