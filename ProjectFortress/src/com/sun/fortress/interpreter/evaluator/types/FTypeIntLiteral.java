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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;

import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.VarArgs;

public class FTypeIntLiteral extends FBuiltinType {
    public final static FTypeIntLiteral ONLY = new FTypeIntLiteral();
    private FTypeIntLiteral() {
        super("IntLiteral");
        cannotBeExtended=true;
    }

    protected List<FType> computeTransitiveExtends() {
        return Useful.<FType>list(VarArgs.<FType>make(this, FTypeInt.ONLY, FTypeLong.ONLY, FTypeIntegral.ONLY, FTypeFloat.ONLY, FTypeNumber.ONLY));
    }

    public boolean subtypeOf(FType other) {
        return (ONLY==other ||
                FTypeLong.ONLY==other ||
                FTypeFloat.ONLY==other ||
                FTypeInt.ONLY.subtypeOf(other));
    }
}
