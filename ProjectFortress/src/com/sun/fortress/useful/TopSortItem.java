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

package com.sun.fortress.useful;

import java.util.Iterator;

public interface TopSortItem<T extends TopSortItem<T>>  {
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
