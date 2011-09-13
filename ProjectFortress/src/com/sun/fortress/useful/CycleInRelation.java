/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class CycleInRelation extends IllegalArgumentException {

    java.util.List items;
    
    public CycleInRelation(String arg0, java.util.List items) {
        super(arg0);
        this.items = items;
        // TODO Auto-generated constructor stub
    }

    public java.util.List getItems() {
        return items;
    }
    
}
