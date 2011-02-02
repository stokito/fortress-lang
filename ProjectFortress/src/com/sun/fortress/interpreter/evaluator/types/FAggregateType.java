/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

abstract public class FAggregateType extends FType {

    public abstract FType getElementType();

    public FAggregateType(String s) {
        super(s);
    }

}
