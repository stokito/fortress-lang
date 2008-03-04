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

public class FTypeFloatLiteral extends FBuiltinType {
    public final static FTypeFloatLiteral ONLY = new FTypeFloatLiteral();
    private FTypeFloatLiteral() {
        super("FloatLiteral");
        cannotBeExtended = true;
    }

    protected List<FType> computeTransitiveExtends() {
        return Useful.<FType>list(this, FTypeNumber.ONLY, FTypeFloat.ONLY);
    }

    public boolean subtypeOf(FType other) {
        return (ONLY==other || FTypeFloat.ONLY.subtypeOf(other));
    }
}
