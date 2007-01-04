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

import java.util.Comparator;

import com.sun.fortress.interpreter.useful.AnyListComparer;


public abstract class NatRef extends StaticArg {

    public NatRef(Span s2) {
        super(s2);
    }

    public static int compareOptional(Option<NatRef> a, Option<NatRef> b) {
        if (a.isPresent() != b.isPresent()) {
            return a.isPresent() ? 1 : -1;
        }
        if (a.isPresent()) {
            return a.getVal().compareTo(b.getVal());
        }
        return 0;
    }

    // Got in a fight with Java generics -- and lost. This seems to get the job
    // done.
    private static final Comparator<NatRef> nattypeComparer = new Comparator<NatRef>() {
        public int compare(NatRef o1, NatRef o2) {
            return o1.compareTo(o2);
        }
    };

    public static AnyListComparer<NatRef> nattypeListComparer = new AnyListComparer<NatRef>(
            nattypeComparer);

}

// / and nat_type =
// / [
// / | `BaseNatType of int
// / | `IdNatType of id
// / | `SumNatType of nat_type list
// / | `ProductNatType of nat_type list
// / ] node
// /
