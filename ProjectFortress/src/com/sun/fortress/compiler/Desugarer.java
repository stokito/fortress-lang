/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.*;

import com.sun.fortress.scala_src.typechecker.STypeChecker;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.desugarer.*;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.DesugarerError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.scala_src.typechecker.IndexBuilder;
import com.sun.fortress.scala_src.typechecker.TraitTable;

import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

/**
 * Performs desugaring of Fortress programs after type checking.
 * Specifically, the following desugarings are performed:
 * <ul>
 * <li>Object expressions are desugared into top-level object declarations.
 * <li>All field declarations in traits are transformed to abstract getter declarations</li>
 * <li>All field references are transformed into getter invocations</li>
 * <li>All field names in objects are rewritten so as not to clash with their getter names</li>
 * <li>Getters and setters are added for all fields declared in an object definition (as appropriate)</li>
 * </ul>
 * Assumes all names referring to APIs are fully-qualified,
 * and that the other transformations handled by the {@link com.sun.fortress.compiler.Disambiguator} have
 * been performed.
 */
public class Desugarer {

    public static class ApiResult extends StaticPhaseResult {
        Map<APIName, ApiIndex> _apis;

        public ApiResult(Map<APIName, ApiIndex> apis,
                         Iterable<? extends StaticError> errors) {
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

    /** Statically check the given components. */
    public static ComponentResult
        desugarComponents(Map<APIName, ComponentIndex> components,
                          GlobalEnvironment env, Map<APIName,STypeChecker> typeCheckers) {

        HashSet<Component> desugaredComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();

        for (Map.Entry<APIName, ComponentIndex> component : components.entrySet()) {
            desugaredComponents.add(desugarComponent(component.getValue(), env, typeCheckers.get(component.getKey())));
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(desugaredComponents,
                                          System.currentTimeMillis()).
                 components(), errors);
    }

    public static Component
        desugarComponent(ComponentIndex component,
                         GlobalEnvironment env, STypeChecker typeChecker) {

        Option<Map<Pair<Id,Id>,FieldRef>> boxedRefMap =
            Option.<Map<Pair<Id,Id>,FieldRef>>none();
	Component comp = (Component) component.ast();
        TraitTable traitTable = new TraitTable(component, env);

        // Desugar case expressions
        if (Shell.getCompiledExprDesugaring()) {
        	CaseExprDesugarer caseExprDesugarer = new CaseExprDesugarer( typeChecker );
        	comp = (Component) comp.accept(caseExprDesugarer);
        }
        
        // Desugar coercion invocation nodes into function applications.
        if (Shell.getCoercionDesugaring()) {
            CoercionDesugarer coercionDesugarer = new CoercionDesugarer();
            comp = (Component) coercionDesugarer.walk(comp);
        }

        // Desugar chain exprs into compound operator expressions.
        if (Shell.getChainExprDesugaring()) {
            ChainExprDesugarer chainExprDesugarer = new ChainExprDesugarer();
            comp = (Component) chainExprDesugarer.walk(comp);
        }

        if (Shell.getGetterSetterDesugaring()) {
            DesugaringVisitor desugaringVisitor = new DesugaringVisitor( boxedRefMap );
            comp = (Component) comp.accept(desugaringVisitor);
        }

        // Desugar type ascription
        if (Shell.getCompiledExprDesugaring()) {
        	TypeAscriptionDesugarer typeAscriptionDesugarer = new TypeAscriptionDesugarer();
        	comp = (Component) comp.accept(typeAscriptionDesugarer);
        }
        
        return comp;
    }

}
