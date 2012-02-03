/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.*;

/**
 * A MultiMap is implemented as a Map from keys to sets of
 * values, and in fact the regular Map methods act on sets,
 * not items.
 */
public class MultiMap<K, V> extends HashMap<K, Set<V>> implements IMultiMap<K, V> {

    /**
     *
     */
    private static final long serialVersionUID = 3275403475085923977L;

    public MultiMap() {
        super();
    }


    public MultiMap(Map<? extends K, ? extends Set<V>> m) {
        super(m);
    }

    // TODO can we get the Java generics right on this?
    public void addInverse(Map<V, K> m) {
        for (Map.Entry<V, K> e : m.entrySet()) {
            putItem((K) e.getValue(), (V) e.getKey());
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

    /**
     * Ensures that k is in the map, perhaps mapping to an empty set.
     *
     * @param k
     * @return
     */
    public Set<V> putKey(K k) {
        Set<V> s = get(k);
        if (s == null) {
            s = new HashSet<V>();
            put(k, s);
        }
        return s;
    }

    public Set<V> putItems(K k, Collection<V> vs) {
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

    public Set<V> getEmptyIfMissing(K k) {
        Set<V> s = get(k);
        if (s == null) {
            s = Collections.emptySet();
        }
        return s;
    }

    public Set<V> removeItemAllowEmpty(K k, V v) {
        Set<V> s = get(k);
        if (s != null) {
            s.remove(v);
        }
        return s;
    }

}
