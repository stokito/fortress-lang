/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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
        if (!t.equals(s)) aliases.add(t);
    }
}
