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

package com.sun.fortress.compiler;

import java.util.Set;

public class StaticVarEnvironment extends DelegatingEnvironment {
    private Set<String> _types;
    private Set<String> _vars;
    
    public StaticVarEnvironment(Environment parent, Set<String> types, Set<String> vars) {
        super(parent);
        _types = types;
        _vars = vars;
    }
    
    public boolean hasVar(String name) {
        return _vars.contains(name) || super.hasVar(name);
    }
    
    public boolean hasType(String name) {
        return _types.contains(name) || super.hasType(name);
    }
    
}
