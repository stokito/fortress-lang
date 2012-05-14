/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.HashMap;

public class Memo1P<Index, Value, Param> implements Factory1P<Index, Value, Param> {

    Factory1P<Index, Value, Param> factory;

    HashMap<Index, Value> map;

    public Memo1P(Factory1P<Index, Value, Param> factory) {
        this.factory = factory;
        this.map = new HashMap<Index, Value>();
    }

    // David: Really need to do something about this synchronization!
    // Jan: But we can only skip synchronization if map.get is itself
    // synchronized; otherwise result may contain bogus data.
    public synchronized Value make(Index probe, Param param) {
        Value result = map.get(probe);
        if (result == null) {
            result = factory.make(probe, param);
            map.put(probe, result);
            //            Great place for testing toString
            //            System.err.println(result);
            //            System.err.println();;
        }
        return result;
    }
}
