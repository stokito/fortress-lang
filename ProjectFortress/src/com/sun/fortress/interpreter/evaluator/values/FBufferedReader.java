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

package com.sun.fortress.interpreter.evaluator.values;

import java.io.BufferedReader;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeBufferedReader;
import com.sun.fortress.interpreter.useful.MagicNumbers;


public class FBufferedReader extends FValue {
    private final BufferedReader val;
    public FType type() {return FTypeBufferedReader.T;}
    public BufferedReader getBufferedReader() {return val;}
    public String toString() {
        return "BufferedReader";
    }
    protected FBufferedReader(BufferedReader x) {
        val = x;
    }
    public static FValue make(BufferedReader x) {
        return new FBufferedReader(x);
    }
  }
