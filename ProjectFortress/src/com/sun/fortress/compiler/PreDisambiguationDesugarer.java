/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.HashSet;
import java.util.Map;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.desugarer.PreDisambiguationDesugaringVisitor;
import com.sun.fortress.compiler.desugarer.ChainExprDesugarer;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Run desugaring phases that must occur before disambiguation.
 */
public class PreDisambiguationDesugarer {

    public static class ComponentResult extends StaticPhaseResult {
        private final Map<APIName, ComponentIndex> _components;
        public ComponentResult(Map<APIName, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Map<APIName, ComponentIndex> components() { return _components; }
    }

    public static class ApiResult extends StaticPhaseResult {
        Map<APIName, ApiIndex> _apis;

        public ApiResult(Map<APIName, ApiIndex> apis, Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }
        public Map<APIName, ApiIndex> apis() { return _apis; }
    }

    public static PreDisambiguationDesugarer.ApiResult desugarApis(Map<APIName, ApiIndex> apis,
                                                                   GlobalEnvironment apiEnv) {
        HashSet<Api> desugaredApis = new HashSet<Api>();

        for (ApiIndex apiIndex : apis.values()) {
            Api api = desugarApi(apiIndex,apiEnv);
            desugaredApis.add(api);
        }
        return new ApiResult
            (IndexBuilder.buildApis(desugaredApis,  apiEnv,
                                    System.currentTimeMillis()).apis(),
             IterUtil.<StaticError>empty());
    }

    public static Api desugarApi(ApiIndex apiIndex, GlobalEnvironment env) {
        Api api = (Api)apiIndex.ast();
        api = (Api) api.accept( new PreDisambiguationDesugaringVisitor() );
        return api;
    }

    public static ComponentResult desugarComponents(Map<APIName, ComponentIndex> components,
                                                    GlobalEnvironment apiEnv) {
        HashSet<Component> desugaredComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();

        for (ComponentIndex componentIndex : components.values()) {
            Component desugared = desugarComponent(componentIndex, apiEnv);
            desugaredComponents.add(desugared);
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(desugaredComponents,
                                          System.currentTimeMillis()).
             components(), errors);
    }

    public static Component desugarComponent(ComponentIndex component,
                                             GlobalEnvironment env) {
        Component comp = (Component) component.ast();
        comp = (Component) comp.accept( new PreDisambiguationDesugaringVisitor() );

        // Desugar chain exprs into compound operator expressions.
        if (Shell.getChainExprDesugaring()) {
            ChainExprDesugarer chainExprDesugarer = new ChainExprDesugarer();
            comp = (Component) chainExprDesugarer.walk(comp);
        }

        return comp;
    }
}
