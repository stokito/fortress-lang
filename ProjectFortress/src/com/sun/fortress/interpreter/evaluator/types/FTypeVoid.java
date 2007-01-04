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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.Collections;

public class FTypeVoid extends FTypeTuple {
    public final static FTypeVoid T = new FTypeVoid();

    private FTypeVoid() {
        super(Collections.<FType>emptyList());
    }
    public boolean excludesOther(FType other) {
        if (other instanceof FTypeVoid) {
            return false;
        } else {
            // TODO Eventually this will be a trait
            return true;
        }
    }

}
