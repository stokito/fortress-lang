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

    @Override
    public String getMessage() {
        // TODO Auto-generated method stub
        String items_string;
        try {
            items_string = items.toString();
        } catch (Throwable kablooie) {
            items_string = "[exception " + kablooie.getMessage() +
                            "from toString() of items]";
        }
        return super.getMessage() + items_string;
    }
    
}
