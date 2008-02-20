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
import com.sun.fortress.compiler.typechecker.*;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.APIName;

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
    
    /** 
     * This field is a temporary switch used for testing. 
     * When typecheck is true, the TypeChecker is called during static checking. 
     * It's false by default to allow the static checker to be used at the command
     * line before the type checker is fully functional.
     * StaticTest sets typecheck to true before running type checking tests.
     */
    public static boolean typecheck = false;
    
    public static class ApiResult extends StaticPhaseResult {
        private Map<APIName, ApiIndex> _apis;
        public ApiResult(Iterable<? extends StaticError> errors, Map<APIName, ApiIndex> apis) { 
            super(errors); 
            _apis = apis;
        }
        public Map<APIName, ApiIndex> apis() { return _apis; }
    }
    
    /**
     * Check the given apis. To support circular references, the apis should appear 
     * in the given environment.
     */
    public static ApiResult checkApis(Map<APIName, ApiIndex> apis,
                                      GlobalEnvironment env) {
        // TODO: implement
        return new ApiResult(IterUtil.<StaticError>empty(), apis);
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
        checkComponents(Map<APIName, ComponentIndex> components,
                        GlobalEnvironment env) 
    {
        HashSet<Component> checkedComponents = new HashSet<Component>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();
        
        for (APIName componentName : components.keySet()) {
            TypeCheckerResult checked = checkComponent(components.get(componentName), env);
            checkedComponents.add((Component)checked.ast());
            errors = IterUtil.compose(checked.errors(), errors);
        }
        return new ComponentResult
            (IndexBuilder.buildComponents(checkedComponents, 
                                          System.currentTimeMillis()).
                 components(),
                                   errors);
    }
    
    public static TypeCheckerResult checkComponent(ComponentIndex component, 
                                                   GlobalEnvironment env) 
    {
        if (typecheck) {
            TypeEnv typeEnv = TypeEnv.make(component);
            
            // Add all top-level function names to the component-level environment.
            //typeEnv.extend(component.functions());
            
            // Iterate over top-level variables, adding each to the component-level environment.
            typeEnv = typeEnv.extend(component.variables());
            
            TypeChecker typeChecker = new TypeChecker(new TraitTable(component, env), 
                                                      StaticParamEnv.make(), typeEnv);
            
//        TypeCheckerResult result = new 
//        // Iterate over top-level functions, checking the body of each.
//        for (Function fn: component.functions()) {
//            typeChecker.check(fn);
//        }
           
            // Iterate over trait and object definitions.
            //for (
            
            return component.ast().accept(typeChecker);
        } else {
            return new TypeCheckerResult(component.ast(), IterUtil.<StaticError>empty());
        }
    }
    
}
