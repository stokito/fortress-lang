/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.scala_src.typechecker.STypeChecker;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;


public final class AnalyzeResult extends StaticPhaseResult {

    private final Map<APIName, ApiIndex> _apis;
    private final Map<APIName, ComponentIndex> _components;
    private final Map<APIName, STypeChecker> _typeCheckers;
    
    public AnalyzeResult(Iterable<? extends StaticError> errors) {
        this(new HashMap<APIName, ApiIndex>(), new HashMap<APIName, ComponentIndex>(),
                errors);
    }

    public AnalyzeResult(Map<APIName, ApiIndex> apis,
            Map<APIName, ComponentIndex> components,
            Iterable<? extends StaticError> errors) {
        super(errors);
        _apis = apis;
        _components = components;
        _typeCheckers = null;
    }

    // Use this constructor if you want to pass the type checkers to the next phase.
    // Use with caution!
    public AnalyzeResult(Map<APIName, ApiIndex> apis,
            Map<APIName, ComponentIndex> components,
            Iterable<? extends StaticError> errors, 
            Map<APIName, STypeChecker> typeCheckers) {
        super(errors);
        _apis = apis;
        _components = components;
        _typeCheckers = typeCheckers;
    }    
    
    /**
     * A copying constructor, where every other field of the given result will be
     * copied, except apis, components, and errors, which will be replaced by the
     * given arguments. Use this constructor to implicitly copy fields that earlier
     * passes populated that your pass wants to ignore. Using this constructor means
     * that if new fields are added to this class in the future, your code will not
     * need to call a different constructor.
     */
    public AnalyzeResult(AnalyzeResult result, Map<APIName, ApiIndex> apis,
            Map<APIName, ComponentIndex> components,
            Iterable<? extends StaticError> errors) {
    	super(errors);
    	_apis = apis;
        _components = components;
        _typeCheckers = null;
    }
    
    public Iterable<Api> apiIterator() { 
    	return new ApiIterable(_apis);
    }

    public Iterable<Component> componentIterator() {
    	return new ComponentIterable(_components);
    }
    
    public Map<APIName, ApiIndex> apis() { return _apis; }
    public Map<APIName, ComponentIndex> components() { return _components; }
    public Map<APIName, STypeChecker> typeCheckers() { 
    	if (_typeCheckers == null) {
    		throw new Error("A compiler pass is trying to get access to the type chekcers when it should not. Please report.");
    	}
    	return _typeCheckers; 
    }
}
