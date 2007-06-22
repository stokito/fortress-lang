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

public abstract class Option<T> {
    Option() {
    }

    public T getVal() {
        throw new Error("Missing value");
    }

    public boolean isPresent() {
        return false;
    }
    
    /**
     * Returns the value if there is one, otherwise returns the default value ifMissing.
     * 
     * @param ifMissing  Default value to return
     */
    public T getVal(T ifMissing) {
        return ifMissing;
    }
    
    static <T> int compare(Option<T> a, Option<T> b, Comparator<T> c) {
        if (a.isPresent()) {
            if (b.isPresent()) {
                return c.compare (a.getVal(),b.getVal());
            } else {
                return 1;
            }
        } else if (b.isPresent()) {
            return -1;
        } else {
            return 0;
        }
    } 
}
