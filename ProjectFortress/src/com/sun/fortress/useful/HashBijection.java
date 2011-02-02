/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

import java.util.*;

public class HashBijection<T, U> implements Bijection<T, U> {

    HashMap<T, U> forward;
    HashMap<U, T> reverse;

    public HashBijection() {
        forward = new HashMap<T, U>();
        reverse = new HashMap<U, T>();
    }

    HashBijection(HashMap<T, U> f, HashMap<U, T> r) {
        forward = (HashMap<T, U>) f.clone();
        reverse = (HashMap<U, T>) r.clone();
    }

    public String toString() {
        return forward.toString();
    }

    public Bijection<U, T> inverse() {
        return new HashBijection<U, T>(reverse, forward);
    }

    public void clear() {
        forward.clear();
        reverse.clear();
    }

    public boolean containsKey(Object key) {
        return forward.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return forward.containsValue(value);
    }

    public Set<java.util.Map.Entry<T, U>> entrySet() {
        return forward.entrySet();
    }

    public U get(Object key) {
        return forward.get(key);
    }

    public T getInverse(Object key) {
        return reverse.get(key);
    }

    public boolean isEmpty() {
        return forward.isEmpty();
    }

    public U put(T key, U value) {
        U v_old = forward.put(key, value);

        // No change
        if (v_old == value) {
            // Be picky about object identity.
            T k_old = reverse.get(value);
            if (k_old != key) {
                reverse.put(value, key);
            }
            return v_old;
        }

        // Some change in v_old/value, so remove and update.
        if (v_old != null) {
            reverse.remove(v_old);
        }

        T k_old = reverse.put(value, key);
        if (k_old != null && k_old != key) {
            if (!k_old.equals(key)) {
                forward.remove(k_old);
            }
        }
        return v_old;
    }

    /**
     * Adds last-iterated bijective view of map to this bijection.
     * That is, if map(x) = map(y) = z and (y,z) appears after (x,z),
     * then (x,z) will not appear in the bijection.
     */
    public void putAll(Map<? extends T, ? extends U> map) {
        for (Map.Entry<? extends T, ? extends U> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }

    }

    public U remove(Object key) {
        U x = forward.remove(key);
        if (x != null) reverse.remove(x);
        return x;
    }

    public T removeInverse(Object key) {
        T x = reverse.remove(key);
        if (x != null) forward.remove(x);
        return x;
    }

    public int size() {
        return forward.size();
    }

    public Set<T> keySet() {
        // If someone needs a modifiable set, let them do the work to implement it.
        return Collections.unmodifiableSet(forward.keySet());
    }

    public Set<U> keySetInverse() {
        // If someone needs a modifiable set, let them do the work to implement it.
        return Collections.unmodifiableSet(reverse.keySet());
    }

    public Collection<U> values() {
        // If someone needs a modifiable collection, let them do the work to implement it.
        return Collections.unmodifiableCollection(forward.values());
    }

    public boolean validate() {
        if (forward.size() != reverse.size()) return false;

        for (T k : keySet()) {
            U v = forward.get(k);
            T k_r = reverse.get(v);
            if (k != k_r) return false;
        }

        for (U v : values()) {
            T k = reverse.get(v);
            U v_r = forward.get(k);
            if (v != v_r) return false;
        }

        return true;
    }

}
