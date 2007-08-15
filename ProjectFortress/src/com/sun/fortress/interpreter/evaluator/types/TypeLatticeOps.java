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

import java.util.Set;

import com.sun.fortress.useful.DualLattice;
import com.sun.fortress.useful.LatticeOps;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class TypeLatticeOps implements LatticeOps<FType> {


    public LatticeOps<FType> dual() {
        return dualLattice;
    }

    public FType join(FType x, FType y) {
        // if (x.subtypeOf(y)) return y;
        // if (y.subtypeOf(x)) return x;
        Set<FType> s = x.join(y);
        if (s.size() != 1)
            bug(errorMsg("Join(", x, ", ", y, ") not a singleton: ", s));
        return s.iterator().next();
    }

    public FType meet(FType x, FType y) {
        // if (x.subtypeOf(y)) return x;
        // if (y.subtypeOf(x)) return y;
        Set<FType> s = x.meet(y);
        if (s.size() != 1)
            bug(errorMsg("Meet(", x, ", ", y, ") not a singleton: ", s));
        return s.iterator().next();
    }

    public FType one() {
        return FTypeTop.ONLY;
    }

    public FType zero() {
        return BottomType.ONLY;
    }

    public final static LatticeOps<FType> V = new TypeLatticeOps();
    public final static LatticeOps<FType> dualLattice = new DualLattice<FType>(V);


}
