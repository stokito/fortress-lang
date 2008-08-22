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

package com.sun.fortress.syntax_abstractions.environments;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.syntax_abstractions.util.FortressTypeToJavaType;
import com.sun.fortress.exceptions.MacroError;

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
