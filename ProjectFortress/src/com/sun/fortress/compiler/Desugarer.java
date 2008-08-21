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

import java.util.*;
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
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.compiler.typechecker.TraitTable;
import com.sun.fortress.compiler.typechecker.TypeCheckerOutput;
import com.sun.fortress.compiler.typechecker.TypeEnv;

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

    /**
     * These two fields are temporary switches used for testing.
     *
     * When getter_setter_desugar is true, the desugaring for getter and setter
     * is called during static checking.
     *
     * When the objExpr_desugar is true,
     * the closure conversion pass for object expressions is called.
     * The closure conversion comes after the desugaring pass for getter / setter.
     */
    public static boolean getter_setter_desugar = true;
    public static boolean objExpr_desugar = false;

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
                          GlobalEnvironment env,
                          Option<TypeCheckerOutput> typeCheckerOutputOp) {

        HashSet<Component> desugaredComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();

        for (APIName componentName : components.keySet()) {
            Component desugared = desugarComponent(
                    components.get(componentName), env, typeCheckerOutputOp);
            desugaredComponents.add(desugared);
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(desugaredComponents,
                                          System.currentTimeMillis()).
                 components(), errors);
    }

    public static Component desugarComponent(ComponentIndex component,
        GlobalEnvironment env, Option<TypeCheckerOutput> typeCheckerOutputOp) {

        Option<Map<Pair<Id,Id>,FieldRef>> boxedRefMap = 
                                    Option.<Map<Pair<Id,Id>,FieldRef>>none();
     	Component comp = (Component) component.ast();

        if(objExpr_desugar) {
            if(typeCheckerOutputOp.isNone()) {
                throw new DesugarerError("Expected TypeCheckerOutput from " +
                                         "type checking phase is not found.");
            }
            TypeCheckerOutput typeCheckerOutput = typeCheckerOutputOp.unwrap();

        	TraitTable traitTable = new TraitTable(component, env);
        	ObjectExpressionVisitor objExprVisitor =
            		new ObjectExpressionVisitor(traitTable, typeCheckerOutput);
        	comp = (Component) comp.accept(objExprVisitor);
        	boxedRefMap = objExprVisitor.getBoxedRefMap();
        }
        if(getter_setter_desugar) {
            DesugaringVisitor desugaringVisitor = new DesugaringVisitor();
            comp = (Component) comp.accept(desugaringVisitor);
        }
        return comp;
    }

}
