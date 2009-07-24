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

import java.util.HashMap;
import java.util.Map;

public class MapOfMap<K, L, T> extends HashMap<K, Map<L, T>> {

    public Map<L, T> putItem(K k, L l, T t) {
        Map<L, T> map = get(k);
        if (map == null) {
            map = new HashMap<L, T>();
            put(k, map);
        }
        map.put(l, t);
        return map;
    }

    public static <K, L, T> CheapSerializer<Map<K, Map<L, T>>> serializer(CheapSerializer<K> k,
                                                                          CheapSerializer<L> l,
                                                                          CheapSerializer<T> t) {
        return new CheapSerializer.MAP<K, Map<L, T>>(k, new CheapSerializer.MAP<L, T>(l, t));
    }

}
