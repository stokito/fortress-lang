/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.StaticParam;

import java.util.List;

/**
 * Represents a possibly-parameterized type declaration.  Comprises
 * {@link TraitIndex}, {@link TypeAliasIndex}, {@link Dimension}, {@link Unit}
 */
public abstract class TypeConsIndex {

    public abstract Node ast();

    public abstract List<StaticParam> staticParameters();

}
