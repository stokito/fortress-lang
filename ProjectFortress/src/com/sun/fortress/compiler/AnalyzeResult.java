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

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.typechecker.TypeEnv;
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
    
    // Fields that will not exist after every phase of compilation
    private final Option<Map<Pair<Node,Span>, TypeEnv>> _typeEnvAtNode;
    
    public AnalyzeResult(Iterable<? extends StaticError> errors) {
        this(new HashMap<APIName, ApiIndex>(), new HashMap<APIName, ComponentIndex>(), errors);
    }

    public AnalyzeResult(Map<APIName, ApiIndex> apis,
                         Map<APIName, ComponentIndex> components,
                         Iterable<? extends StaticError> errors) {
    	super(errors);
    	_apis = apis;
        _components = components;
        _typeEnvAtNode = Option.none();
    }
    
    public AnalyzeResult(Map<APIName, ApiIndex> apis,
            Map<APIName, ComponentIndex> components,
            Iterable<? extends StaticError> errors, Map<Pair<Node,Span>, TypeEnv> typeEnvAtNode) {
    	super(errors);
    	_apis = apis;
        _components = components;
        _typeEnvAtNode = Option.some(typeEnvAtNode);
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
        _typeEnvAtNode = result._typeEnvAtNode;
    }
    
    public Iterable<Api> apiIterator() { 
    	return new ApiIterable(_apis);
    }

    public Iterable<Component> componentIterator() {
    	return new ComponentIterable(_components);
    }
    
    public Map<APIName, ApiIndex> apis() { return _apis; }
    public Map<APIName, ComponentIndex> components() { return _components; }
    
    /**
     * @return A mapping from Nodes that declare variables to the TypeEnv that was
     * in scope at that AST node. This map is not populated until type-checking and
     * therefore will be None until then.
     */
    public Option<Map<Pair<Node,Span>, TypeEnv>> typeEnvAtNode() { return this._typeEnvAtNode; }
}
