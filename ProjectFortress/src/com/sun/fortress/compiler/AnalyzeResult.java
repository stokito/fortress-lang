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

package com.sun.fortress.compiler;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;


public final class AnalyzeResult extends StaticPhaseResult {	

    private final Map<APIName, ApiIndex> _apis;
    private final Map<APIName, ComponentIndex> _components;

    public AnalyzeResult(Iterable<? extends StaticError> errors) {
        super(errors);
        _apis = new HashMap<APIName, ApiIndex>();
        _components = new HashMap<APIName, ComponentIndex>();
    }

    public AnalyzeResult(Map<APIName, ApiIndex> apis,
                         Map<APIName, ComponentIndex> components,
                         Iterable<? extends StaticError> errors) {
        super(errors);
        _apis = apis;
        _components = components;
    }
    
    public Iterable<Api> apiIterator() { 
    	return new ApiIterable(_apis);
    }

    public Iterable<Component> componentIterator() {
    	return new ComponentIterable(_components);
    }
    
    public Map<APIName, ApiIndex> apis() { return _apis; }
    public Map<APIName, ComponentIndex> components() { return _components; }
}
