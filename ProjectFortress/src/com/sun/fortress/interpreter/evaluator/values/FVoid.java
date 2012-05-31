/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;


public class FVoid extends FTuple {
    // At the user level, FVoids should look like "()". EricAllen 3/13/2008
    public String getString() {
        return "()";
    }

    public static final FVoid V = new FVoid();

    private FVoid() {
        super();
    }
}
