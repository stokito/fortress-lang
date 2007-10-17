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

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.interpreter.evaluator.values.FValue;

public class NamedLabelException extends LabelException {
    String name;

    public String toString() {
      return "RuhRohRaggy, Should never be called";
    }

    public String getName() {return name;}

    public Boolean match(String n2) {
      return name.equals(n2);
    }

    public NamedLabelException(String n, FValue r) {
        super(r);
        name = n;
    }
}
