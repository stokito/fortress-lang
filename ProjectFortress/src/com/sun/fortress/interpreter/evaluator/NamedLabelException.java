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

import com.sun.fortress.useful.HasAt;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class NamedLabelException extends LabelException {
    final String name;

    public String toString() {
        return (loc.at()+": exit from nonexistent label block "+name);
    }

    public String getName() {return name;}

    public Boolean match(String n2) {
      return name.equals(n2);
    }

    public NamedLabelException(HasAt loc, String n, FValue r) {
        super(loc,r);
        name = n;
    }
}
