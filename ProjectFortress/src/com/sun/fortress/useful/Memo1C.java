/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;

public class Memo1C<Index1, Value> implements Factory1<Index1, Value> {

    Factory1<Index1, Value> factory;

    BATree<Index1, Value> map;

    public Memo1C(Factory1<Index1, Value> factory, Comparator<? super Index1> comp) {
        this.factory = factory;
        this.map = new BATree<Index1, Value>(comp);
    }

    // David: Really need to do something about this synchronization!
    // Jan: But we can only skip synchronization if map.get is itself
    // synchronized; otherwise result may contain bogus data.
    // Applicativeness is no help here (in Fortress it would be
    // sufficient, and this trick would work).
    public synchronized Value make(Index1 probe) {
        Value result = map.get(probe);
        if (result == null) {
            result = factory.make(probe);
            map.put(probe, result);
        }
        return result;
    }
}
