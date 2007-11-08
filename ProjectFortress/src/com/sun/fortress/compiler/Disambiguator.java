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
import java.util.Set;
import java.util.HashSet;
import edu.rice.cs.plt.iter.IterUtil;

import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.TopLevelEnv;
import com.sun.fortress.compiler.disambiguator.TypeDisambiguator;
import com.sun.fortress.compiler.disambiguator.ExprDisambiguator;

/**
 * Eliminates ambiguities in an AST that can be resolved solely by knowing what
 * kind of entity a name refers to.  This class specifically handles
 * the following:  
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs
 *     and OprExprs may then contain lists of qualified names referring to
 *     multiple APIs).</li>
 * <li>VarRefs referring to functions become FnRefs with placeholders
 *     for implicit static arguments filled in (to be replaced later
 *     during type inference).</li> 
 * <li>VarRefs referring to getters, setters, or methods become FieldRefs.</li>
 * <li>VarRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>FieldRefs referring to methods, and that are juxtaposed with
 *     Exprs, become MethodInvocations.</li>
 * <li>FnRefs referring to methods, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>IdTypes referring to traits become InstantiatedTypes (with 0
 *     arguments)</li> 
 * </ul>
 * 
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.
 */
public class Disambiguator {
    
    /** Result of {@link #disambiguateApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Iterable<Api> _apis;
        
        public ApiResult(Iterable<Api> apis, 
                         Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }
        
        public Iterable<Api> apis() { return _apis; }
    }
    
    /**
     * Disambiguate the given apis. To support circular references,
     * the apis should appear in the given environment.
     */
    public static ApiResult disambiguateApis(Iterable<Api> apis,
                                             GlobalEnvironment globalEnv) {
        List<Api> results = new ArrayList<Api>();
        List<StaticError> errors = new ArrayList<StaticError>();
        for (Api api : apis) {
            ApiIndex index = globalEnv.api(api.getName());
            NameEnv env = new TopLevelEnv(globalEnv, index, errors);
            Set<SimpleName> onDemandImports = new HashSet<SimpleName>();
            List<StaticError> newErrs = new ArrayList<StaticError>();
            TypeDisambiguator td = 
                new TypeDisambiguator(env, onDemandImports, newErrs);
            Api tdResult = (Api) api.accept(td);
            if (newErrs.isEmpty()) {
                ExprDisambiguator ed = 
                    new ExprDisambiguator(env, onDemandImports, newErrs);
                Api edResult = (Api) tdResult.accept(ed);
                if (newErrs.isEmpty()) { results.add(edResult); }
            }
            
            if (!newErrs.isEmpty()) { 
                errors.addAll(newErrs); 
            }
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
                               Map<DottedName, ComponentIndex> indices) {
        List<Component> results = new ArrayList<Component>();
        List<StaticError> errors = new ArrayList<StaticError>();
        for (Component comp : components) {
            ComponentIndex index = indices.get(comp.getName());
            if (index == null) {
                throw new IllegalArgumentException("Missing component index");
            }
            NameEnv env = new TopLevelEnv(globalEnv, index, errors);
            Set<SimpleName> onDemandImports = new HashSet<SimpleName>();
            
            List<StaticError> newErrs = new ArrayList<StaticError>();
            TypeDisambiguator td = 
                new TypeDisambiguator(env, onDemandImports, newErrs);
            Component tdResult = (Component) comp.accept(td);
            if (newErrs.isEmpty()) {
                ExprDisambiguator ed = 
                    new ExprDisambiguator(env, onDemandImports, newErrs);
                Component edResult = (Component) tdResult.accept(ed);
                if (newErrs.isEmpty()) { results.add(edResult); }
            }
            
            if (!newErrs.isEmpty()) { 
                errors.addAll(newErrs); 
            }
        }
        return new ComponentResult(results, errors);
    }
}
