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

public class StaticVarEnvironment extends DelegatingEnvironment {
    private Set<String> _typeParams;
    private Set<String> _valParams;
    
    public StaticVarEnvironment(Environment parent, Set<String> typeParams,
                                Set<String> valParams) {
        super(parent);
        _typeParams = typeParams;
        _valParams = valParams;
    }
    
    @Override public boolean hasVar(String name) {
        return _valParams.contains(name) || super.hasVar(name);
    }
    
    @Override public boolean hasTypeVar(String name) {
        return _typeParams.contains(name) || super.hasTypeVar(name);
    }
    
}
