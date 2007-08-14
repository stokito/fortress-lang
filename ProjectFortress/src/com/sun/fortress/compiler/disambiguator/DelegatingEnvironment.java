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

public abstract class DelegatingEnvironment extends Environment {
    private Environment _parent;
    
    public DelegatingEnvironment(Environment parent) {
        _parent = parent;
    }
    
    public boolean hasVar(String name) { return _parent.hasVar(name); }
    public boolean hasFn(String name) { return _parent.hasFn(name); }
    public boolean hasMethod(String name) { return _parent.hasMethod(name); }
    public boolean hasTrait(String name) { return _parent.hasTrait(name); }
    public boolean hasTypeVar(String name) { return _parent.hasTypeVar(name); }
    
    public Option<String> apiForVar(String name) { return _parent.apiForVar(name); }
    public Set<String> apisForFn(String name) { return _parent.apisForFn(name); }
    public Option<String> apiForTrait(String name) { return _parent.apiForTrait(name); }
}
