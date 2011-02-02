/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import java.util.Collections;
import java.util.List;

public class FTypeVoid extends FTypeTuple {
    public final static FTypeVoid ONLY = new FTypeVoid();

    private FTypeVoid() {
        super(Collections.<FType>emptyList());
    }

    public boolean excludesOther(FType other) {
        if (other instanceof FTypeVoid) return false;
        if (!(other instanceof FTypeTuple)) return true;
        List<FType> otherTypes = ((FTypeTuple) other).l;
        if (otherTypes.size() > 1) return true;
        return (!(otherTypes.get(0) instanceof FTypeRest));
    }

    public String toString() {
        return "()";
    }

}
