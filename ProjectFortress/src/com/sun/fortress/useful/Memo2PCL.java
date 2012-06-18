/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

public class Memo2PCL<Index, Part2, Value, Param> implements Factory2P<Index, Part2, Value, Param> {

    Factory2P<Index, Part2, Value, Param> factory;

    volatile BATree<Index, Value> map;

    volatile BATree<Index, Value> shadow_map;

    ReentrantLock lock;

    private final static boolean debug = false;

    public Memo2PCL(Factory2P<Index, Part2, Value, Param> factory, Comparator<? super Index> comp, ReentrantLock lock) {
        this.factory = factory;
        this.map = new BATree<Index, Value>(comp);
        this.lock = lock;
    }

    // David: Really need to do something about this synchronization!
    // Jan: But we can only skip synchronization if map.get is itself
    // synchronized; otherwise result may contain bogus data.
    public Value make(Index probe, Part2 part2, Param param) {
        Value result = null;
        // Cheapest possible
        if (shadow_map == null) {
            result = map.get(probe);
        } else if (lock.isHeldByCurrentThread()) {
            result = shadow_map.get(probe);
            if (result == null) {
                result = factory.make(probe, part2, param);
                shadow_map.put(probe, result);
            }
            return result;
        } else {
            result = map.get(probe);
        }

        if (result == null) {
            // We do not hold lock, we need to add the item to the map.
            lock.lock();
            try {
                DebugletPrintStream ps = null;
                if (debug) {
                    ps = DebugletPrintStream.make("OVERLOADS");
                    System.err.println("M1PCL " + ps + " " + probe);
                    ps.backtrace().flush();
                }
                shadow_map = map.copy();
                result = make(probe, part2, param);
                map = shadow_map;
                if (debug) {
                    System.err.println("M1PCL " + ps + " close");
                    ps.close();
                }
            }
            catch (IOException e) {}
            finally {
                shadow_map = null;
                lock.unlock();
            }
        }
        return result;
    }
}
