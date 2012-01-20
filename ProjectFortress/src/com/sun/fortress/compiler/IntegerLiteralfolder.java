/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.util.*;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.desugarer.DesugaringVisitor;
import com.sun.fortress.compiler.desugarer.IntegerLiteralFoldingVisitor;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
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

public class IntegerLiteralfolder {
	
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<APIName, ComponentIndex> _components;
        public ComponentResult(Map<APIName, ComponentIndex> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Map<APIName, ComponentIndex> components() { return _components; }
    }
    
    public static ComponentResult
    foldComponents(Map<APIName, ComponentIndex> components) {

    HashSet<Component> foldedComponents = new HashSet<Component>();
    Iterable<? extends StaticError> errors = new HashSet<StaticError>();

    for (Map.Entry<APIName, ComponentIndex> component : components.entrySet()) {
        foldedComponents.add(foldComponent(component.getValue()));
    }
    return new ComponentResult
        (IndexBuilder.buildComponents(foldedComponents,
                                      System.currentTimeMillis()).
             components(), errors);
}
    
    public static Component
    foldComponent(ComponentIndex component) {

    Component comp = (Component) component.ast();

    IntegerLiteralFoldingVisitor visitor = new IntegerLiteralFoldingVisitor( );
    comp = (Component) comp.accept(visitor);
    
    return comp;
}
    
}