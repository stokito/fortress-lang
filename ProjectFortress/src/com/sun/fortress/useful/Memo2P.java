/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.HashMap;

public class Memo2P<Index1, Index2, Value, Param> implements Factory2P<Index1, Index2, Value, Param> {

    Factory2P<Index1, Index2, Value, Param> factory;

    HashMap<Pair<Index1, Index2>, Value> map;

    public Memo2P(Factory2P<Index1, Index2, Value, Param> factory) {
        this.factory = factory;
        this.map = new HashMap<Pair<Index1, Index2>, Value>();
    }

    // David: Really need to do something about this synchronization!
    // Jan: But we can only skip synchronization if map.get is itself
    // synchronized; otherwise result may contain bogus data.
    public synchronized Value make(Index1 part1, Index2 part2, Param param) {
        Pair<Index1, Index2> probe = new Pair<Index1, Index2>(part1, part2);
        Value result = map.get(probe);
        if (result == null) {
            result = factory.make(part1, part2, param);
            map.put(probe, result);
        }
        return result;
    }
}
