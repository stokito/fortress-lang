/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

public class SymbolicNat extends FTypeNat {

    public SymbolicNat(String name) {
        super(name);
        isSymbolic = true;
    }

}
