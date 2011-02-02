/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.useful.Useful;

public class FTypeRange extends FType {
    public final static FTypeRange ONLY = new FTypeRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

    public FTypeRange(int x, int y) {
        super("Range");
        this.x = x;
        this.y = y;
        cannotBeExtended = true;
    }

    final int x, y;
    String lazyName;

    public String getName() {
        if (lazyName == null) lazyName = "Range" + Useful.inOxfords(String.valueOf(x), String.valueOf(x));
        return lazyName;
    }

}
