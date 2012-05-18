/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.*;
import com.sun.fortress.Shell;
import com.sun.fortress.compiler.desugarer.AssignmentAndSubscriptDesugarer;
import com.sun.fortress.compiler.desugarer.TypecaseExprDesugarer;
import com.sun.fortress.compiler.desugarer.AbstractDesugarer;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.scala_src.typechecker.TraitTable;
import com.sun.fortress.compiler.desugarer.PreTypeCheckDesugaringVisitor;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Pair;

/**
 * Performs desugarings of Fortress programs that can be done before type checking.
 * This is the right place to put a desugaring if the result should go through
 * the type checker.
 *
 * Assumes all names referring to APIs are fully-qualified,
 * and that the other transformations handled by the {@link com.sun.fortress.compiler.Disambiguator} have
 * been performed.
 */
public class PreTypeCheckDesugarer {

    public static class ApiResult extends StaticPhaseResult {
        Map<APIName, ApiIndex> _apis;

        public ApiResult(Map<APIName, ApiIndex> apis, Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }
        public Map<APIName, ApiIndex> apis() { return _apis; }
    }

    /**
     * Check the given apis. To support circular references, the apis should appear
     * in the given environment.
     */
    public static ApiResult desugarApis(Map<APIName, ApiIndex> apis,
                                        GlobalEnvironment env) {
        HashSet<Api> desugaredApis = new HashSet<Api>();

        for (ApiIndex apiIndex : apis.values()) {
            Api api = desugarApi(apiIndex,env);
            desugaredApis.add(api);
        }
        return new ApiResult
            (IndexBuilder.buildApis(desugaredApis,
                                    env,
                                    System.currentTimeMillis()).apis(),
             IterUtil.<StaticError>empty());
    }

    public static Api desugarApi(ApiIndex apiIndex, GlobalEnvironment env) {
        Api api = (Api)apiIndex.ast();
        return api;
    }

    public static class ComponentResult extends StaticPhaseResult {
        private final Map<APIName, ComponentIndex> _components;
        public ComponentResult(Map<APIName, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Map<APIName, ComponentIndex> components() { return _components; }
    }

    /** Desugar the given components. */
    public static ComponentResult
        desugarComponents(Map<APIName, ComponentIndex> components,
                          GlobalEnvironment env)
    {
        HashSet<Component> desugaredComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();

        for (Map.Entry<APIName, ComponentIndex> component : components.entrySet()) {
            desugaredComponents.add(desugarComponent(component.getValue(), env));
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(desugaredComponents,
                                          System.currentTimeMillis()).
                 components(), errors);
    }

    public static Component desugarComponent(ComponentIndex component,
                                             GlobalEnvironment env) {
        Component comp = (Component) component.ast();

        // Desugar compound/tuple assignments and all uses of subscripts.
        if (Shell.getAssignmentDesugaring()) {
            AssignmentAndSubscriptDesugarer assnDesugarer = new AssignmentAndSubscriptDesugarer();
            comp = (Component) assnDesugarer.walk(comp);
        }

        // Desugar typecase expressions
        if (Shell.getCompiledExprDesugaring()) {
	    TypecaseExprDesugarer typecaseExprDesugarer = new TypecaseExprDesugarer();
	    comp = (Component) comp.accept(typecaseExprDesugarer);
        }
        
        // Mark bodyless FnDecl as abstract
        if (Shell.getCompiledExprDesugaring()) {
	    AbstractDesugarer abstractDesugarer = new AbstractDesugarer();
	    comp = (Component) comp.accept(abstractDesugarer);
        }
        
	comp = (Component) comp.accept( new PreTypeCheckDesugaringVisitor() );

        return comp;
    }
}
