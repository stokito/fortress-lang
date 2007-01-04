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

import java.util.List;

import com.sun.fortress.interpreter.useful.Useful;

public class FTypeIntLiteral extends FType {
    public final static FTypeIntLiteral T = new FTypeIntLiteral();
    private FTypeIntLiteral() {
        super("IntLiteral");
        cannotBeExtended=true;
    }

    protected List<FType> computeTransitiveExtends() {
        return Useful.<FType>list(this, FTypeInt.T, FTypeLong.T, FTypeIntegral.T, FTypeFloat.T, FTypeNumber.T);
    }

    public boolean subtypeOf(FType other) {
        return (T==other ||
                FTypeLong.T==other ||
                FTypeFloat.T==other ||
                FTypeInt.T.subtypeOf(other));
    }
}
