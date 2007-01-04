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

public abstract class StaticParam extends Tree implements
        Comparable<StaticParam> {
    StaticParam(Span span) {
        super(span);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(StaticParam o) {
        Class tclass = getClass();
        Class oclass = o.getClass();
        if (oclass != tclass) {
            return tclass.getName().compareTo(oclass.getName());
        }
        return subtypeCompareTo(o);
    }

    abstract int subtypeCompareTo(StaticParam o);

    public abstract String getName();

    public static com.sun.fortress.interpreter.useful.ListComparer<StaticParam> listComparer = new com.sun.fortress.interpreter.useful.ListComparer<StaticParam>();
}

// / and type_param =
// / [
// / | `SimpleTypeParam of simple_type_param
// / | `NatParam of id
// / | `DimensionParam of id
// / | `OperatorParam of op
// / ] node
// /
