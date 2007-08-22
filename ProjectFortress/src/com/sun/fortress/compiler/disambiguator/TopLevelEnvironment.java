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
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.DottedName;

public class TopLevelEnvironment extends Environment {
    private GlobalEnvironment _globalEnv;
    private CompilationUnitIndex _current;
    
    public TopLevelEnvironment(GlobalEnvironment globalEnv,
                               CompilationUnitIndex current) {
        _globalEnv = globalEnv;
        _current = current;
    }
    
    public boolean hasVar(IdName name) {
        return _current.variables().containsKey(name);
    }
    
    public boolean hasFn(FnName name) {
        return _current.functions().containsFirst(name);
    }
    
    public boolean hasTrait(IdName name) {
        return _current.traits().containsKey(name);
    }
    
    public boolean hasTypeVar(IdName name) {
        return false;
    }
    
    public Option<DottedName> apiForVar(IdName name) {
        // TODO: implement
        return Option.none();
    }
    
    public Set<DottedName> apisForFn(FnName name) {
        // TODO: implement
        return Collections.emptySet();
    }
    
    public Option<DottedName> apiForTrait(IdName name) {
        // TODO: implement
        return Option.none();
    }
    
}
