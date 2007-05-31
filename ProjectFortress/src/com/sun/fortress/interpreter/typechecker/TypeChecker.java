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

package com.sun.fortress.interpreter.typechecker;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.nodes.*;


public class TypeChecker extends NodeVisitor<FType> {

    public static void check(CompilationUnit p) throws TypeError {
        p.accept(new TypeChecker());
    }

    private final BetterEnv e;

    public TypeChecker() {
        this.e = BetterEnv.primitive();
    }

    public TypeChecker(TypeChecker tc) {
        this.e = tc.e;
    }

    public TypeChecker(BetterEnv _e) {
        this.e = _e;
    }

    public FType NI(com.sun.fortress.interpreter.useful.HasAt x, String s) {
        return BottomType.ONLY;
    }

    public FType forVarRefExpr(VarRefExpr v) {
        String s = v.getVar().getName();
        FType t = e.getVarType(s);
        if (t == null)
            throw new Error(s + " Not present in environment");
        return t;
    }

}
