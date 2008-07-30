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

import java.util.HashSet;
import java.util.Map;

import com.sun.fortress.compiler.desugarer.ConditionalOpDesugarer;
import com.sun.fortress.compiler.desugarer.ExtendsObjectVisitor;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Run desugaring phases that must occur before disambiguation.
 */
public class PreDisambiguationDesugarer {

	/** Remove conditional operators, replacing their operands with thunks. */
	public static final boolean conditional_op_desugar = true;
	
	/** Rewrite trait, object, and object expressions to explicitly extend Object. */
    public static final boolean extends_object_desugar = true;	
	
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

	public static PreDisambiguationDesugarer.ApiResult desugarApis(
			Map<APIName, ApiIndex> apis, GlobalEnvironment apiEnv) {
        HashSet<Api> desugaredApis = new HashSet<Api>();

        for (ApiIndex apiIndex : apis.values()) {
            Api api = desugarApi(apiIndex,apiEnv);
            desugaredApis.add(api);
        }
        return new ApiResult
            (IndexBuilder.buildApis(desugaredApis,
                                    System.currentTimeMillis()).apis(),
             IterUtil.<StaticError>empty());
	}

    public static Api desugarApi(ApiIndex apiIndex, GlobalEnvironment env) {
        Api api = (Api)apiIndex.ast();
        if(extends_object_desugar) {
            ExtendsObjectVisitor extendsObjectVisitor = new ExtendsObjectVisitor();
            api = (Api) api.accept(extendsObjectVisitor);
        }        
        return api;
    }
	
	public static ComponentResult desugarComponents(
			Map<APIName, ComponentIndex> components, GlobalEnvironment apiEnv) {
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
		
		if(conditional_op_desugar) {
			ConditionalOpDesugarer condOpDesugarer = new ConditionalOpDesugarer();
			comp = (Component) comp.accept(condOpDesugarer);
		}
		if(extends_object_desugar) {
            ExtendsObjectVisitor extendsObjectVisitor = new ExtendsObjectVisitor();
            comp = (Component) comp.accept(extendsObjectVisitor);		    
		}
		return comp;
	}
}
