/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A hashmap that can be parameterized by a non-standard hash function.
 */
public class GHashMap<K, V> implements Map<K, V>, Cloneable {
    Hasher<K> hasher;
    HashMap<WrappedKey, V> map;

    public Object clone() throws CloneNotSupportedException {
        super.clone();
        @SuppressWarnings ("unchecked") HashMap<WrappedKey, V> clonedMap = (HashMap<WrappedKey, V>) map.clone();
        GHashMap<K, V> copy = new GHashMap<K, V>(hasher, clonedMap);
        return copy;
    }

    class WrappedKey {
        K value;
        int h;

        public int hashCode() {
            return h;
        }

        WrappedKey(K k) {
            value = k;
            long lh = hasher.hash(value);
            h = (int) (lh) ^ (int) (lh >>> 32);
        }

        public boolean equals(Object o) {
            if (o instanceof GHashMap.WrappedKey) {
                WrappedKey wk = (WrappedKey) o;
                return h == wk.h && hasher.equiv(value, wk.value);
            }
            return false;
        }

        public String toString() {
            return value.toString();
        }
    }

    class FauxEntry implements Map.Entry<K, V> {

        Map.Entry<WrappedKey, V> theRealOne;

        public K getKey() {
            return ((WrappedKey) theRealOne.getKey()).value;
        }

        public V getValue() {
            return theRealOne.getValue();
        }

        public V setValue(V arg0) {
            return theRealOne.setValue(arg0);
        }

        FauxEntry(Map.Entry<WrappedKey, V> e) {
            theRealOne = e;
        }
    }

    private final F<WrappedKey, K> unmapper = new F<WrappedKey, K>() {
        public K apply(WrappedKey e) {
            return e == null ? null : e.value;
        }
    };

    private final F<Map.Entry<WrappedKey, V>, Map.Entry<K, V>> entryUnmapper =
            new F<Map.Entry<WrappedKey, V>, Map.Entry<K, V>>() {
                /* (non-Javadoc)
                * @see com.sun.fortress.interpreter.useful.Fn#apply(T)
                */
                @Override
                public Map.Entry<K, V> apply(Map.Entry<WrappedKey, V> x) {
                    // TODO Auto-generated method stub
                    return new FauxEntry(x);
                }
            };

    public GHashMap(Hasher<K> hasher) {
        this(hasher, new HashMap<WrappedKey, V>());
    }

    private GHashMap(Hasher<K> hasher, HashMap map) {
        this.hasher = hasher;
        this.map = (HashMap<WrappedKey, V>) map;
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        map.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @SuppressWarnings ("unchecked")
    public boolean containsKey(Object arg0) {
        return map.containsKey(new WrappedKey((K) arg0));
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object arg0) {
        return map.containsValue(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set<Map.Entry<K, V>> entrySet() {
        // The elements of the resulting set are all FauxEntries.
        // But it's necessary to return a Set<Map.Entry<K,V>> to match
        // the signature of method entrySet in interface Map.Entry.
        // eric.allen@sun.com 9/21/2006
        Set<Map.Entry<WrappedKey, V>> s = map.entrySet();
        return Useful.applyToAll(s, entryUnmapper);
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */

    public boolean isEmpty() {
        return map.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        return Useful.applyToAll(map.keySet(), unmapper);
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(K, V)
     */
    public V put(K arg0, V arg1) {
        return map.put(new WrappedKey(arg0), arg1);
    }

    public V putIfAbsent(K arg0, V arg1) {
        WrappedKey wk = new WrappedKey((K) arg0);
        V v = map.get(wk);
        if (v != null) {
            return v;
        } else {
            return map.put(wk, (V) arg1);
        }
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> arg0) {
        for (Map.Entry<? extends K, ? extends V> asdf : arg0.entrySet()) {
            Map.Entry<? extends K, ? extends V> e = asdf;
            put(e.getKey(), e.getValue());
        }
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    @SuppressWarnings ("unchecked")
    public V get(Object arg0) {
        // UGLY HACK: We must take an arbitrary Object in order
        // to satisfy the Map<K,V> interface.
        // But we have to perform an unchecked cast arg0 to K in order
        // to wrap it. eric.allen@sun.com 9/21/2006
        // This is a problem because the hash function is undefined
        // for non-K objects.
        return map.get(new WrappedKey((K) arg0));
    }

    /* (non-Javadoc)
    * @see java.util.Map#remove(java.lang.Object)
    */
    @SuppressWarnings ("unchecked")
    public V remove(Object arg0) {
        return map.remove(new WrappedKey((K) arg0));
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        return map.size();
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        return map.values();
    }

    public String toString() {
        return map.toString();
    }

}
