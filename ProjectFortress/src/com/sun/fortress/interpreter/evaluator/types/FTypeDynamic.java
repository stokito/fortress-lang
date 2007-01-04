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

import java.util.TreeSet;
import java.util.Set;

import com.sun.fortress.interpreter.useful.Useful;


public class FTypeDynamic extends FType {
    public final static FTypeDynamic T = new FTypeDynamic();
    public final static Set<FType> SingleT = Useful.<FType>set(T);

    private FTypeDynamic() {
        super("Dynamic");
    }

    public Set<FType> meet(FType t2) {
        // Meet of X with dynamic is X
        return Useful.set(t2);
    }

    public Set<FType> join(FType t2) {
        // Join of X with dynamic is X
        return Useful.set(t2);
    }

    public boolean subtypeOf(FType t) {
        return true;
    }


}
