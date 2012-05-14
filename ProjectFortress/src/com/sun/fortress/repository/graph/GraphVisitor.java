/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository.graph;

public interface GraphVisitor<T, Failure extends Throwable> {
    public T visit(ApiGraphNode node) throws Failure;

    public T visit(ComponentGraphNode node) throws Failure;
}
