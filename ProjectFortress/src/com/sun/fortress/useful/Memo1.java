/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
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
    
    public synchronized Iterable<Value> values() {
        return map.values();
    }
}
