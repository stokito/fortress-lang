/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

public class Memo2<Index1, Index2, Value> implements
        Factory2<Index1, Index2, Value> {



    Factory2<Index1, Index2, Value> factory;

    HashMap<Pair<Index1, Index2>, Value> map;

    public Memo2(Factory2<Index1, Index2, Value> factory) {
        this.factory = factory;
        this.map = new HashMap<Pair<Index1, Index2>, Value>();
    }

    // Really need to do something about this synchronization!
    public synchronized Value make(Index1 part1, Index2 part2) {
        Pair<Index1, Index2> probe = new Pair<Index1,Index2>(part1, part2);
        Value result = map.get(probe);
        if (result == null) {
            result = factory.make(part1, part2);
            map.put(probe, result);
        }
        return result;
    }
}
