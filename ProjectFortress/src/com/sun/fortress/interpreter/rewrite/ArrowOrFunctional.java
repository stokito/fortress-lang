/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.rewrite;

/**
 * A simple class for a private enumerated type.
 *
 * @author dr2chase
 */
public class ArrowOrFunctional {
    public String toString() {
        return s;
    }

    ArrowOrFunctional(String s) {
        this.s = s;
    }

    private final String s;
    public final static ArrowOrFunctional FUNCTIONAL = new ArrowOrFunctional("FUNCTIONAL");
    public final static ArrowOrFunctional ARROW = new ArrowOrFunctional("ARROW");
    public final static ArrowOrFunctional NEITHER = new ArrowOrFunctional("NEITHER");
}
