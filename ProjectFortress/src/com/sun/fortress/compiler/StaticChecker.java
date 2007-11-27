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

package com.sun.fortress.compiler;

import java.util.*;
import com.sun.fortress.compiler.typechecker.*;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.DottedName;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Verifies all static properties of a valid Fortress program that require
 * interpreting types.  Assumes all names referring to APIs are fully-qualified,
 * and that the other transformations handled by the {@link Disambiguator} have
 * been performed.  In addition to checking the program, performs the following
 * transformations:
 * <ul>
 * <li>All unknown placeholder types are provided explicit (inferred) values.</li>
 * <li>Explicit coercions are added where needed.</li>
 * <li>Juxtapositions are given a binary structure.</li>
 * <li>FieldRefs that refer to methods and that are followed by an argument expression
 *     become MethodInvocations.</li>
 * </li>
 */
public class StaticChecker {
    
    public static class ApiResult extends StaticPhaseResult {
        public ApiResult(Iterable<? extends StaticError> errors) { super(errors); }
    }
    
    /**
     * Check the given apis. To support circular references, the apis should appear 
     * in the given environment.
     */
    public static ApiResult checkApis(Map<DottedName, ApiIndex> apis,
                                      GlobalEnvironment env) {
        // TODO: implement
        return new ApiResult(IterUtil.<StaticError>empty());
    }
    
    
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<DottedName, ComponentIndex> _components;
        public ComponentResult(Map<DottedName, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Map<DottedName, ComponentIndex> components() { return _components; }
    }
    
    /** Statically check the given components. */
    public static ComponentResult
        checkComponents(Map<DottedName, ComponentIndex> components,
                        GlobalEnvironment env) 
    {
        Map<DottedName, ComponentIndex> checkedComponents = new HashMap<DottedName, ComponentIndex>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();
        
//        for (DottedName componentName : components.keySet()) {
//            ComponentResult checkedComponent = checkComponent(components.get(componentName), env);
//            checkedComponents.put(componentName, checkedComponent.components().get(componentName));
//            errors = IterUtil.compose(checkedComponent.errors(), errors);
//        }
        return new ComponentResult(components, errors);
        //return new ComponentResult(checkedComponents, errors);
    }
    
    public static ComponentResult checkComponent(ComponentIndex component, GlobalEnvironment env) {
        //TypeEnv typeEnv = 
        // Iterate over top-level variables, adding each to the component-level environment.
        //Iterable<Variable> vars = component.variables();
        
        // Add all top-level function names to the component-level environment.
        // Iterate over top-level functions, checking the body of each.
        // Iterate over trait and object definitions.
        //TypeChecker typeChecker = new TypeChecker(env);
        //return component.ast().accept(typeChecker);
        return null;
    }
    
}
