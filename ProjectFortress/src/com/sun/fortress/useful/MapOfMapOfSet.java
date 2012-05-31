/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Collection;
import java.util.HashMap;

public class MapOfMapOfSet<K, L, T> extends HashMap<K, IMultiMap<L, T>> {

    public IMultiMap<L, T> putItem(K k, L l, T t) {
        IMultiMap<L, T> map = get(k);
        if (map == null) {
            map = new MultiMap<L, T>();
            put(k, map);
        }
        map.putItem(l, t);
        return map;
    }

    public IMultiMap<L, T> putItems(K k, L l, Collection<T> s) {
        IMultiMap<L, T> map = get(k);
        if (map == null) {
            map = new MultiMap<L, T>();
            put(k, map);
        }
        map.putItems(l, s);
        return map;
    }


    public IMultiMap<L, T> putItem(K k, L l) {
        IMultiMap<L, T> map = get(k);
        if (map == null) {
            map = new MultiMap<L, T>();
            put(k, map);
        }
        map.putKey(l);
        return map;
    }

}
