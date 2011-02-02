/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.useful.HasAt;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class NamedLabelException extends LabelException {
    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -3703662379352447236L;

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
