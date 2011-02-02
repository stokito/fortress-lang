/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.useful.HasAt;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class LabelException extends FortressException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -8845405020352925483L;

    final FValue res;
    final HasAt loc;

    public String toString() {
        return (loc.at()+": exit without enclosing label block");
    }

    public String getMessage() {
        return toString();
    }

    public LabelException(HasAt loc, FValue r) {
        super();
        this.loc = loc;
        res = r;
    }
    public FValue res() { return res; }
}
