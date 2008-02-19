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
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Performs desugaring of Fortress programs. Specifically, the following transformations are performed:
 * <ul>
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
     * This field is a temporary switch used for testing.
     * When typecheck is true, the TypeChecker is called during static checking.
     * It's false by default to allow the static checker to be used at the command
     * line before the type checker is fully functional.
     * CompilerTopLevelJUTests sets typecheck to true before running its tests.
     */
    public static boolean desugar = false;

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
        // TODO: implement
        return new ApiResult(apis, IterUtil.<StaticError>empty());
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
                        GlobalEnvironment env)
    {
        HashSet<Component> desugaredComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();

        for (APIName componentName : components.keySet()) {
            Component desugared = desugarComponent(components.get(componentName), env);
            desugaredComponents.add(desugared);
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(desugaredComponents,
                                          System.currentTimeMillis()).
                 components(),
                                   errors);
    }

    public static Component desugarComponent(ComponentIndex component,
                                                     GlobalEnvironment env)
    {
        if (desugar) {
            DesugaringVisitor desugaringVisitor = new DesugaringVisitor();
            return (Component)component.ast().accept(desugaringVisitor);
        } else {
            return (Component)component.ast();
        }
    }

}