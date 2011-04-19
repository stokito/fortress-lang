/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository.graph;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import edu.rice.cs.plt.tuple.Option;

import java.io.File;
import java.io.IOException;

public class ComponentGraphNode extends GraphNode {

    private Option<ComponentIndex> component;
    private final String k;

    public ComponentGraphNode(APIName name, String canonicalSourceName, long sourceDate) {
        super(name, canonicalSourceName, sourceDate);
        k = key(name);
        this.component = Option.none();
    }

    public ComponentGraphNode(APIName name, File source_file) throws IOException {
        super(name, source_file);
        k = key(name);
        this.component = Option.none();
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof ComponentGraphNode) {
            ComponentGraphNode a = (ComponentGraphNode) o;
            return a.getName().equals(getName());
        }
        return false;
    }

    public Option<ComponentIndex> getComponent() {
        return component;
    }

    public void setComponent(ComponentIndex c, long cacheDate) {
        this.component = Option.wrap(c);
        this.cacheDate = cacheDate;
    }


    public String toString() {
        return "Component " + getName().toString();
    }

    public <T, F extends Throwable> T accept(GraphVisitor<T, F> g) throws F {
        return g.visit(this);
    }

    public String key() {
        return k;
    }

    public static String key(APIName k) {
        return "component " + k.getText();
    }
}
