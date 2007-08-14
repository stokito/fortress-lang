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
import java.util.Collections;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.CompilationUnitIndex;

public class TopLevelEnvironment extends Environment {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    
    public TopLevelEnvironment(GlobalEnvironment globalEnv, CompilationUnitIndex current) {
        _globalEnv = globalEnv;
        _current = current;
    }
    
    public boolean hasVar(String name) {
        return _current.variables().containsKey(name);
    }
    
    public boolean hasFn(String name) {
        return _current.functions().containsFirst(name);
    }
    
    public boolean hasMethod(String name) {
        return false;
    }
    
    public boolean hasType(String name) {
        // TODO: implement
        return false;
    }
    
    public Option<String> apiForVar(String name) {
        // TODO: implement
        return Option.none();
    }
    
    public Set<String> apisForFn(String name) {
        // TODO: implement
        return Collections.emptySet();
    }
    
}
    