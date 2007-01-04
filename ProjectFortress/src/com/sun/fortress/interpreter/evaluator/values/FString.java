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

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeString;
import com.sun.fortress.interpreter.nodes.Unprinter;


public class FString extends FValue {
    String val;
    public String getString() {return val;}
    public String toString() {
        return "\"" + Unprinter.enQuote(val) + "\"";
    }
    protected FString(String x, FType t) {
        val = x;
        setFtype(t);
    }
    public FString(String x) {
      this(x, FTypeString.T);
    }
    public static FValue make(String s) {
        return new FString(s);
    }
  }
