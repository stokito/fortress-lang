/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.HashMap;

public class Memo1<Index1, Value> implements Factory1<Index1, Value> {

    Factory1<Index1, Value> factory;

    HashMap<Index1, Value> map;

    public Memo1(Factory1<Index1, Value> factory) {
        this.factory = factory;
        this.map = new HashMap<Index1, Value>();
    }

    // David: Really need to do something about this synchronization!
    // Jan: But we can only skip synchronization if map.get is itself
    // synchronized; otherwise result may contain bogus data.
    public synchronized Value make(Index1 probe) {
        Value result = map.get(probe);
        if (result == null) {
            result = factory.make(probe);
            map.put(probe, result);
        }
        return result;
    }
    
    public boolean known(Index1 probe) {
        return map.containsKey(probe);
    }

    public synchronized Iterable<Value> values() {
        return map.values();
    }
}
