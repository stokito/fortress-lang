/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Map;

public interface BoundingMap<T, U, L extends LatticeOps<U>> extends Map<T, U> {
    /**
     * Indicate whether we have a forward map or its dual.
     */
    boolean isForward();

    /**
     * Creates an aliased lattice-dual of this map
     */
    BoundingMap<T, U, L> dual();

    /**
     * Creates an unaliased copy of this map
     */
    BoundingMap<T, U, L> copy();

    /**
     * puts min/intersection of v and old
     */
    U meetPut(T k, U v);

    /**
     * puts max/union of v and old
     */
    U joinPut(T k, U v);

    /**
     * Used for backtracking during unification
     */
    void assign(BoundingMap<T, U, L> replacement);
}
