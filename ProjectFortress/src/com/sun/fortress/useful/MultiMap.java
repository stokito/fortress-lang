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
public class MultiMap<K, V> extends AbstractMap<K, Set<V>> implements IMultiMap<K, V> {

    
    private static interface SetMaker<V>
    extends FnOfVoid<Set<V>>, F<Collection<V>, Set<V>> {}

    Map<K, Set<V>> delegate;
    SetMaker<V> setMaker;
    
    private static final long serialVersionUID = 3275403475085923977L;
    
    public MultiMap() {
        delegate = new HashMap<K, Set<V>>();
        setMaker = hashSetMaker();
    }

    public MultiMap(Comparator<K> comparator) {
        delegate = new BATree<K, Set<V>>(comparator);
        setMaker = hashSetMaker();
    }

    public MultiMap(Comparator<K> k_comp, Comparator<V> v_comp) {
        delegate = k_comp == null ? new HashMap<K, Set<V>>() : new BATree<K, Set<V>>(k_comp);
        setMaker = baSetMaker(v_comp);
    }

    public MultiMap(Map<? extends K, ? extends Set<V>> m) {
        if (m instanceof MultiMap) {
            MultiMap<K,V> mmm = ((MultiMap<K,V>)m);
            setMaker = mmm.setMaker;
            m = mmm;
        } else {
            setMaker = hashSetMaker();
        }
        if (m instanceof BATree) {
            // Combo of signature generic params and instanceof BATree says ok.
            delegate = new BATree<K,Set<V>>(((BATree<K,Set<V>>) m).comp);
        } else {
            delegate = new HashMap<K, Set<V>>();
        }
        for (Map.Entry<? extends K, ? extends Set<V>> entry : m.entrySet())
            putItems(entry.getKey(), entry.getValue());
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
            s = setMaker.apply();
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
            s = setMaker.apply();
            put(k, s);
        }
        return s;
    }

    public Set<V> putItems(K k, Collection<V> vs) {
        Set<V> s = get(k);
        if (s == null) {
            s = setMaker.apply(vs);
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

    /**
     * @return
     */
    private SetMaker<V> hashSetMaker() {
        return new SetMaker<V>() {
            public Set<V> apply() {
                return new HashSet<V>();
            }
            public Set<V> apply(Collection<V> old) {
                return new HashSet<V>(old);
            }
        };
    }

    /**
     * @return
     */
    private SetMaker<V> baSetMaker(final Comparator<V> comp) {
        return new SetMaker<V>() {
            public Set<V> apply() {
                return new BASet<V>(comp);
            }
            public Set<V> apply(Collection<V> old) {
                BASet<V> s = new BASet<V>(comp);
                s.addAll(old);
                return s;
            }
        };
    }


    public void clear() {
        delegate.clear();
    }


    public boolean containsKey(Object arg0) {
        return delegate.containsKey(arg0);
    }


    public boolean containsValue(Object arg0) {
        return delegate.containsValue(arg0);
    }


    public Set<java.util.Map.Entry<K, Set<V>>> entrySet() {
        return delegate.entrySet();
    }


    public boolean equals(Object arg0) {
        return delegate.equals(arg0);
    }


    public Set<V> get(Object arg0) {
        return delegate.get(arg0);
    }


    public int hashCode() {
        return delegate.hashCode();
    }


    public boolean isEmpty() {
        return delegate.isEmpty();
    }


    public Set<K> keySet() {
        return delegate.keySet();
    }


    public Set<V> put(K arg0, Set<V> arg1) {
        return delegate.put(arg0, arg1);
    }


    public void putAll(Map<? extends K, ? extends Set<V>> arg0) {
        delegate.putAll(arg0);
    }


    public Set<V> remove(Object arg0) {
        return delegate.remove(arg0);
    }


    public int size() {
        return delegate.size();
    }


    public Collection<Set<V>> values() {
        return delegate.values();
    }

}
