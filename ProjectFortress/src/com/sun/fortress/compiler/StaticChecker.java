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

import java.util.Map;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Verifies all static properties of a valid Fortress program that require
 * interpreting types.  Assumes all names referring to APIs are fully-qualified,
 * and that the other transformations handled by the {@link Disambiguator} have
 * been performed.  In addition to checking the program, transforms components so 
 * that all unknown types are provided explicit values, introduces
 * explicit coercions, and restructures juxtapositions into a binary form.
 * <li>Assignments to GetterInvocations referring to setters become
 *     SetterInvocations.</li>
 * <li>GetterInvocations referring to methods juxtaposed with Exprs become
 *     MethodInvocations.</li>
 * <li>FunctionRefs juxtaposed with Exprs become MethodInvocations.</li>
 */
public class StaticChecker {
    
    public static class ApiResult extends StaticPhaseResult {
        public ApiResult(Iterable<? extends StaticError> errors) { super(errors); }
    }
    
    /**
     * Check the given apis. To support circular references, the apis should appear 
     * in the given environment.
     */
    public static ApiResult checkApis(Map<String, ApiIndex> apis,
                                      GlobalEnvironment env) {
        // TODO: implement
        return new ApiResult(IterUtil.<StaticError>empty());
    }
    
    
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<String, ComponentIndex> _components;
        public ComponentResult(Map<String, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Map<String, ComponentIndex> components() { return _components; }
    }
    
    /** Disambiguate the given components. */
    public static ComponentResult
        checkComponents(Map<String, ComponentIndex> components,
                        GlobalEnvironment env) {
        // TODO: implement
        return new ComponentResult(components, IterUtil.<StaticError>empty());
    }
    
}
