/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes_util.Modifiers;

/**
 * Comprises DeclaredVariable, ParamVariable, and SingletonVariable.
 */
public abstract class Variable extends InferredTypeIndex {

    public abstract Modifiers modifiers();

    public abstract boolean mutable();

    public Variable acceptNodeUpdateVisitor(NodeUpdateVisitor v) {
        return this;
    }
}
