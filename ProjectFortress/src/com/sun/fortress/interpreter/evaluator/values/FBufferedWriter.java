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

import java.io.BufferedWriter;
import com.sun.fortress.interpreter.evaluator.types.FBuiltinType;
import com.sun.fortress.interpreter.evaluator.types.FTypeBufferedWriter;
import com.sun.fortress.useful.MagicNumbers;


public class FBufferedWriter extends FBuiltinValue {
    private final BufferedWriter val;
    public FBuiltinType type() {return FTypeBufferedWriter.ONLY;}
    public BufferedWriter getBufferedWriter() {return val;}
    public String toString() {
        return "BufferedWriterFileOpen";
    }
    protected FBufferedWriter(BufferedWriter x) {
        val = x;
    }
    public static FValue make(BufferedWriter x) {
        return new FBufferedWriter(x);
    }
  }
