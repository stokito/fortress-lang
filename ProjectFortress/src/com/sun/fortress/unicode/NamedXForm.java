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

package com.sun.fortress.unicode;

import java.util.HashSet;

public class NamedXForm {
    NamedXForm(String s) {
        name = s;
    }

    String name;
    public String toString() {
        return name;
    }

    HashSet<String> aliases = new HashSet<String>();

    public void addName(String t, String s) {
        // Intrinsic collisions are not the fault of this transform
        if (! t.equals(s))
                aliases.add(t);
    }
}
