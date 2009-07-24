/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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
