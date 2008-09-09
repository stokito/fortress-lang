/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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