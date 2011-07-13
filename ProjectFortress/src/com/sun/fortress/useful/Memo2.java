/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.HashMap;

public class Memo2<Index1, Index2, Value> implements Factory2<Index1, Index2, Value> {


    Factory2<Index1, Index2, Value> factory;

    HashMap<Pair<Index1, Index2>, Value> map;

    public Memo2(Factory2<Index1, Index2, Value> factory) {
        this.factory = factory;
        this.map = new HashMap<Pair<Index1, Index2>, Value>();
    }

    // Really need to do something about this synchronization!
    public synchronized Value make(Index1 part1, Index2 part2) {
        Pair<Index1, Index2> probe = new Pair<Index1, Index2>(part1, part2);
        Value result = map.get(probe);
        if (result == null) {
            result = factory.make(part1, part2);
            map.put(probe, result);
        }
        return result;
    }
}
