/*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Iterator;

public interface TopSortItem<T extends TopSortItem<T>> {
    /**
     * Returns an iterator over the successors of this node.
     */
    Iterator<T> successors();

    /**
     * Returns this node's current predecessor count.
     */
    int predecessorCount();

    /**
     * Decrements this node's predecessor count and returns the new value.
     */
    int decrementPredecessors();
}
