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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.Span;

public abstract class Indices extends Node implements Comparable<Indices> {

    public Indices(Span span) {
        super(span);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Indices o) {
        Class tclass = getClass();
        Class oclass = o.getClass();
        if (oclass != tclass) {
            return tclass.getName().compareTo(oclass.getName());
        }
        return subtypeCompareTo(o);
    }

    abstract int subtypeCompareTo(Indices o);

}

// / and indices =
// / [
// / | `FixedDim of extent_range list
// / | `PolyDim
// / ] node
// /
