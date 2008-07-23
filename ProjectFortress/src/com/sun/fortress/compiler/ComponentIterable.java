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

import java.util.Iterator;
import java.util.Map;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;

public final class ComponentIterable implements Iterable<Component>  {
	
    private final Map<APIName, ComponentIndex> _components;	
    
    ComponentIterable(Map<APIName, ComponentIndex> components) {
    	_components = components;
    }

	public Iterator<Component> iterator() {
		
		final Iterator<ComponentIndex> componentIndexIterator = _components.values().iterator();
		
		return new Iterator<Component>() {

			public boolean hasNext() {
				return componentIndexIterator.hasNext();
			}

			public Component next() {
				return (Component) componentIndexIterator.next().ast();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
}
