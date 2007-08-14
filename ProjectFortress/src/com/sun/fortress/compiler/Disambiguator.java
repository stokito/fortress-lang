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
import java.util.List;
import java.util.ArrayList;
import edu.rice.cs.plt.iter.IterUtil;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.disambiguator.Environment;
import com.sun.fortress.compiler.disambiguator.TopLevelEnvironment;
import com.sun.fortress.compiler.disambiguator.DisambiguationVisitor;

/**
 * Eliminates ambiguities in an AST that can be resolved solely by knowing what kind
 * of entity a name refers to.  This class specifically handles the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs and OprExprs may then
 *     contain lists of qualified names referring to multiple APIs).
 * <li>VarRefs referring to functions become FnRefs with placeholders for implicit static
 *     arguments filled in (to be replaced later during type inference).</li>
 * <li>VarRefs referring to getters, setters, or methods become FieldRefs.</li>
 * <li>VarRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>FieldRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>FnRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>IdTypes referring to traits become InstantiatedTypes (with 0 arguments)</li>
 * </ul>
 * 
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.
 */
public class Disambiguator {
    
    /** Result of {@link #disambiguateApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Iterable<Api> _apis;
            
        public ApiResult(Iterable<Api> apis, Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }
        
        public Iterable<Api> apis() { return _apis; }
    }
    
    /**
     * Disambiguate the given apis. To support circular references, the apis should
     * appear in the given environment.
     */
    public static ApiResult disambiguateApis(Iterable<Api> apis,
                                             GlobalEnvironment globalEnv) {
        List<Api> results = new ArrayList<Api>();
        Iterable<StaticError> errors = IterUtil.empty();
        for (Api api : apis) {
            ApiIndex index = globalEnv.api(NodeUtil.getName(api.getDottedId()));
            Environment env = new TopLevelEnvironment(globalEnv, index);
            List<StaticError> newErrs = new ArrayList<StaticError>();
            DisambiguationVisitor v = new DisambiguationVisitor(env, globalEnv, newErrs);
            Api result = (Api) api.accept(v);
            
            if (newErrs.isEmpty()) { results.add(result); }
            else { errors = IterUtil.compose(errors, newErrs); }
        }
        return new ApiResult(results, errors);
    }
    
    
    /** Result of {@link #disambiguateComponents}. */
    public static class ComponentResult extends StaticPhaseResult {
        private final Iterable<Component> _components;
        public ComponentResult(Iterable<Component> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Iterable<Component> components() { return _components; }
    }
    
    /** Disambiguate the given components. */
    public static ComponentResult
        disambiguateComponents(Iterable<Component> components,
                               GlobalEnvironment globalEnv,
                               Map<String, ComponentIndex> indices) {
        List<Component> results = new ArrayList<Component>();
        Iterable<StaticError> errors = IterUtil.empty();
        for (Component comp : components) {
            ComponentIndex index = indices.get(NodeUtil.getName(comp.getDottedId()));
            if (index == null) {
                throw new IllegalArgumentException("Missing component index");
            }
            Environment env = new TopLevelEnvironment(globalEnv, index);
            List<StaticError> newErrs = new ArrayList<StaticError>();
            DisambiguationVisitor v = new DisambiguationVisitor(env, globalEnv,newErrs);
            Component result = (Component) comp.accept(v);
            
            if (newErrs.isEmpty()) { results.add(result); }
            else { errors = IterUtil.compose(errors, newErrs); }
        }
        return new ComponentResult(results, errors);
    }
    
}
