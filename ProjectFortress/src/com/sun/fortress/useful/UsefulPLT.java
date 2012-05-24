/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.collect.ImmutableRelation;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;

import java.util.*;

/**
 * It is helpful for Useful to not depend on external libraries.
 * Those useful methods that do, were moved here.
 *
 * @author dr2chase
 */

public class UsefulPLT {
    /**
     * Returns an immutable {@code Relation} with the same contents as the given
     * {@code Map}.
     */
    public static <K, V> Relation<K, V> relation(Map<? extends K, ? extends V> map) {
        IndexedRelation<K, V> result = new IndexedRelation<K, V>();
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            result.add(entry.getKey(), entry.getValue());
        }
        return new ImmutableRelation<K, V>(result);
    }

    /**
     * Does the given ConsList contain the given element?
     */
    public static <T> boolean consListContains(T item, ConsList<? extends T> list) {
        for (T t : list) {
            if (t.equals(item)) return true;
        }
        return false;
    }

    public static <K, V> IMultiMap<K, V> shrinkMultiMap(IMultiMap<K, V> map) {
        if (map.isEmpty()) return emptyMultiMap();
        else if (map.size() == 1) {
            Map.Entry<K, Set<V>> r = IterUtil.first(map.entrySet());
            if (r.getValue().isEmpty()) {
                return singletonMultiMap(r.getKey(), Collections.<V>emptySet());
            } else if (r.getValue().size() == 1) {
                return singletonMultiMap(r.getKey(), IterUtil.first(r.getValue()));
            } else {
                return singletonMultiMap(r.getKey(), r.getValue());
            }
        } else return map;
    }

    public static <K, V> IMultiMap<K, V> emptyMultiMap() {
        return (IMultiMap<K, V>) IMultiMap.EMPTY_MULTIMAP;
    }

    public static <K, V> IMultiMap<K, V> singletonMultiMap(final K key, final V value) {
        return singletonMultiMap(key, Collections.singleton(value));
    }

    public static <K, V> IMultiMap<K, V> singletonMultiMap(final K the_key, final Set<V> the_value) {
        return new IMultiMap<K, V>() {
            private final Map<K, Set<V>> singleton = Collections.singletonMap(the_key, the_value);

            private <T> T error() {
                throw new IllegalStateException("Singleton IMultiMap is immutable.");
            }

            public void clear() {
                singleton.clear();
            }

            public boolean containsKey(Object key) {
                return singleton.containsKey(key);
            }

            public boolean containsValue(Object value) {
                return singleton.containsValue(value);
            }

            public Set<Entry<K, Set<V>>> entrySet() {
                return singleton.entrySet();
            }

            public boolean equals(Object o) {
                return singleton.equals(o);
            }

            public Set<V> get(Object key) {
                return singleton.get(key);
            }

            public int hashCode() {
                return singleton.hashCode();
            }

            public boolean isEmpty() {
                return false;
            }

            public Set<K> keySet() {
                return singleton.keySet();
            }

            public Set<V> put(K key, Set<V> value) {
                return singleton.put(key, value);
            }

            public void putAll(Map<? extends K, ? extends Set<V>> t) {
                singleton.putAll(t);
            }

            public Set<V> remove(Object key) {
                return singleton.remove(key);
            }

            public int size() {
                return 1;
            }

            public Collection<Set<V>> values() {
                return singleton.values();
            }

            public void addInverse(Map<V, K> m) {
                error();
            }

            public Set<V> putItem(K k, V v) {
                return error();
            }

            public Set<V> putItems(K k, Collection<V> vs) {
                return error();
            }

            public Set<V> removeItem(K k, V v) {
                return error();
            }

            public Set<V> putKey(K k) {
                return error();
            }

            @Override
            public Set<V> getEmptyIfMissing(K k) {
                Set<V> s = singleton.get(k);
                
                return s == null ? Collections.<V>emptySet() : s;
            }

            @Override
            public Set<V> removeItemAllowEmpty(K k, V v) {
                return error();
            }
        };
    }


}
