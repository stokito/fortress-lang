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

import com.sun.fortress.nodes.FnName;

public class LocalFnEnvironment extends DelegatingEnvironment {
    private Set<FnName> _fns;
    
    public LocalFnEnvironment(Environment parent, Set<FnName> fns) {
        super(parent);
        _fns = fns;
    }
    
    @Override public boolean hasFn(FnName name) {
        return _fns.contains(name) || super.hasFn(name);
    }
    
}
