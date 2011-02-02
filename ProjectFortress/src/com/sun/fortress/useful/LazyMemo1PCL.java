/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A lazy memo function with one parameter in addition to the index.
 * Also includes a poor-man's-transaction; a mutating thread excludes others
 * during value creation (which may recursively access lazy memo functions)
 * and uses a private copy of the memo function to shield mutators from
 * half-initialized data.
 *
 * @author chase
 */
public class LazyMemo1PCL<Index, Value, Param> implements Factory1P<Index, Value, Param> {

    LazyFactory1P<Index, Value, Param> factory;

    volatile BATree<Index, Value> map;

    volatile BATree<Index, Value> shadow_map;

    ReentrantLock lock;

    private final static boolean debug = false;

    public LazyMemo1PCL(LazyFactory1P<Index, Value, Param> factory,
                        Comparator<? super Index> comp,
                        ReentrantLock lock) {
        this.factory = factory;
        this.map = new BATree<Index, Value>(comp);
        this.lock = lock;
    }

    public Value make(Index probe, Param param) {
        Value result = null;

        /* If the shadow map is null, we cannot possibly be
         * updating, and we will not become the updater without
         * taking some action of our own.  Therefore, this cheap
         * test detects the not-us case whenever there are no other
         * updaters.
         */
        if (shadow_map == null) {
            result = map.get(probe);

            /*
            * If there is a shadow map, it matters whether it is us,
            * or someone else.  If we hold the lock, we are the uodater
            * and we should use the shadow map.
            */
        } else if (lock.isHeldByCurrentThread()) {
            result = shadow_map.get(probe);
            if (result == null) {
                /*
                 * Insertions (in the factory) go to the shadow_map.
                 */
                result = factory.make(probe, param, shadow_map);
            }
            return result;

        } else {
            /*
             * Not the updater.
             */
            result = map.get(probe);
        }

        if (result == null) {
            /*
             * If we come here, we know that we do not hold the lock,
             * OR the shadow map is null, and that result must be added
             * to the map.  Increment the lock state on the reentrant
             * lock and also create the shadow map for the result
             * creation.
             */
            lock.lock();
            try {
                // double-checked locking
                result = map.get(probe);
                if (result == null) {
                    /*
                     * To create our own world, we must make a copy of the map,
                     * and recur, because it is possible that the item was created
                     * while we were locked.  Recursion is only one deep, since
                     * we hold the lock now.
                     */
                    shadow_map = map.copy();
                    result = make(probe, param);
                    map = shadow_map;
                }
            }
            finally {
                /*
                 * In all cases, reset the lock and shadow_map
                 * to their prior states.
                 */
                shadow_map = null;
                lock.unlock();
            }
        }
        return result;
    }
}
