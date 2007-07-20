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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A MultiMap is implemented as a Map from keys to sets of
 * values, and in fact the regular Map methods act on sets,
 * not items.
 */
public class MultiMap<K, V> extends HashMap<K, Set<V>> implements Map<K, Set<V>> {

    /**
     *
     */
    private static final long serialVersionUID = 3275403475085923977L;
    public MultiMap() {
        super();
    }

    // TODO can we get the Java generics right on this?
    public void addInverse(Map<V, K> m) {
        for (Map.Entry<V, K> e : m.entrySet()) {
            putItem((K)e.getValue(), (V)e.getKey());
        }

    }

    public Set<V> putItem(K k, V v) {
        Set<V> s = get(k);
        if (s == null) {
            s = new HashSet<V>();
            s.add(v);
            put(k, s);
        } else {
            s.add(v);
        }
        return s;
    }
    public Set<V> putItems(K k, Set<V> vs) {
        Set<V> s = get(k);
        if (s == null) {
            s = new HashSet<V>(vs);
            put(k, s);
        } else {
            s.addAll(vs);
        }
        return s;
    }
    public Set<V> removeItem(K k, V v) {
        Set<V> s = get(k);
        if (s != null) {
            s.remove(v);
            if (s.isEmpty()) {
                remove(k);
                s = null;
            }
        }
        return s;
    }

}
