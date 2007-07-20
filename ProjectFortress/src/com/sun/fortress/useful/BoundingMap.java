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

import java.util.Map;

public interface BoundingMap<T, U, L extends LatticeOps<U>> extends Map<T,U>
{
    /** Creates an aliased lattice-dual of this map */
    BoundingMap<T, U, L> dual();
    
    /** Creates an unaliased copy of this map */
    BoundingMap<T, U, L> copy();
    
    /** puts min/intersection of v and old */
    U meetPut(T k, U v);
    /** puts max/union of v and old */
    U joinPut(T k, U v);
    
    /** Used for backtracking during unification */
    void assign(BoundingMap<T,U,L> replacement);
}
