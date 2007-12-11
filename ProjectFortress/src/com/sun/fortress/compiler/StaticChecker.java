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
    
    public static class SingleComponentResult extends StaticPhaseResult {
        private final ComponentIndex _component;
        public SingleComponentResult(ComponentIndex component, 
                                     Iterable<? extends StaticError> errors) {
            super(errors);
            _component = component;
        }
        public ComponentIndex component() { return _component; }
    }
    
    /** Statically check the given components. */
    public static ComponentResult
        checkComponents(Map<DottedName, ComponentIndex> components,
                        GlobalEnvironment env) 
    {
        Map<DottedName, ComponentIndex> checkedComponents = new HashMap<DottedName, ComponentIndex>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();
        
        for (DottedName componentName : components.keySet()) {
            SingleComponentResult checked = checkComponent(components.get(componentName), env);
            checkedComponents.put(componentName, checked.component());
            errors = IterUtil.compose(checked.errors(), errors);
        }
        return new ComponentResult(checkedComponents, errors);
    }
    
    public static SingleComponentResult checkComponent(ComponentIndex component, GlobalEnvironment env) {
        TypeEnv typeEnv = TypeEnv.make();
        
        // Add all top-level function names to the component-level environment.
        //typeEnv.extend(component.functions());
        
        // Iterate over top-level variables, adding each to the component-level environment.
        typeEnv.extend(component.variables());
        
//        TypeChecker typeChecker = new TypeChecker(env, typeEnv, StaticParamEnv.make());
//        TypeCheckerResult result = new 
//        // Iterate over top-level functions, checking the body of each.
//        for (Function fn: component.functions()) {
//            typeChecker.check(fn);
//        }
        
        // Iterate over trait and object definitions.
        //for (
        //return component.ast().accept(typeChecker);
        return new SingleComponentResult(component, IterUtil.<StaticError>empty());
    }
    
}
