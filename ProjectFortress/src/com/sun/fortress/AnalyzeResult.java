package com.sun.fortress;

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
