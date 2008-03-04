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

package com.sun.fortress.interpreter.evaluator.values;
import com.sun.fortress.interpreter.evaluator.types.FBuiltinType;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloat;

public class FFloat extends FBuiltinValue {
    private final double val;
    public FBuiltinType type() {return FTypeFloat.ONLY;}
    public double getFloat() {return val;}
    public String getString() {return Double.toString(val);}
    public String toString() {
        return (val+":RR64");
    }
    private FFloat(double x) {
        val = x;
    }
    static final FFloat Zero = new FFloat(0.0);
    static public FFloat make(double x) {
        return new FFloat(x);
    }
}
