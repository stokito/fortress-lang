/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
