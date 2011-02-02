/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.*;

/**
 * A subset of all that you might want in a map, but it is embedded in an
 * applicative framework so that small deltas can be obtained at low cost.
 * <p/>
 * Does not yet support item removal; does not yet support all the SortedMap
 * methods that it could in principle support.
 */
public class BATree2<T, U, V> extends AbstractMap<T, Pair<U, V>> implements Map<T, Pair<U, V>> {

    static class EntrySet<T, U, V> extends AbstractSet<Map.Entry<T, Pair<U, V>>>
            implements Set<Map.Entry<T, Pair<U, V>>> {

        BATree2Node<T, U, V> root;
        Comparator<T> comp;

        EntrySet(BATree2Node<T, U, V> r, Comparator<T> c) {
            root = r;
            comp = c;
        }

        /* (non-Javadoc)
         * @see java.util.AbstractCollection#contains(java.lang.Object)
         */
        @SuppressWarnings ("unchecked")
        @Override
        public boolean contains(Object o) {
            if (root == null) return false;

            if (o instanceof Map.Entry<?, ?>) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

                BATree2Node<T, U, V> n = root;
                // TODO Since there's no side effects, we can fake out the generic, I think.
                n = n.getObject((T) e.getKey(), comp);
                if (n == null) return false;

                Object eo = e.getValue();
                if (eo instanceof Pair<?, ?>) {
                    Pair<?, ?> ep = (Pair<?, ?>) eo;
                    return n.data1.equals(ep.getA()) && n.data2.equals(ep.getB());
                }
            }
            return false;
        }

        public boolean isEmpty() {
            return root == null;
        }

        /* (non-Javadoc)
         * @see java.util.AbstractCollection#iterator()
         */
        @Override
        public Iterator<Entry<T, Pair<U, V>>> iterator() {
            return new Iter();
        }

        class Iter implements Iterator<Entry<T, Pair<U, V>>> {
            int i;

            public boolean hasNext() {
                return i < size();
            }

            public java.util.Map.Entry<T, Pair<U, V>> next() {
                if (hasNext()) return root.get(i++);
                return null;
            }

            public void remove() {
                throw new Error("Applicative data structures cannot be changed!");
            }

        }

        /* (non-Javadoc)
         * @see java.util.AbstractCollection#toArray()
         */
        @Override
        public Object[] toArray() {
            // TODO Auto-generated method stub
            return super.toArray();
        }

        @Override
        public int size() {
            if (root == null) return 0;
            else return root.weight;
        }
    }

    BATree2Node<T, U, V> root;
    Comparator<T> comp;

    public BATree2(Comparator<T> c) {
        comp = c;
    }

    protected BATree2(BATree2Node<T, U, V> r, Comparator<T> c) {
        root = r;
        comp = c;
    }

    public int size() {
        if (root == null) return 0;
        else return root.weight;
    }

    @SuppressWarnings ("unchecked")
    public Pair<U, V> get(Object k) {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.getObject((T) k, comp);
        if (f == null) return null;
        return f.asPair();
    }

    public U getA(T k) {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.getObject(k, comp);
        if (f == null) return null;
        return f.data1;
    }

    public V getB(T k) {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.getObject(k, comp);
        if (f == null) return null;
        return f.data2;
    }

    public BATree2Node<T, U, V> getEntry(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k);
    }

    public Pair<U, V> getData(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).asPair();
    }

    public U getDataA(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).data1;
    }

    public V getDataB(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).data2;
    }

    public T getKey(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).key;
    }

    /**
     * Returns the index of key k in the map if k is present,
     * otherwise returns the 1's-complement of the index that k
     * would have if k were inserted into the map.
     */
    public int indexOf(T k) {
        if (root == null) return ~0;
        return root.indexOf(k, comp);
    }

    public Pair<U, V> min() {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.min();
        return f.asPair();
    }

    public Pair<U, V> max() {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.max();
        return f.asPair();
    }

    public U minA() {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.min();
        return f.data1;
    }

    public U maxA() {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.max();
        return f.data1;
    }

    public V minB() {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.min();
        return f.data2;
    }

    public V maxB() {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.max();
        return f.data2;
    }

    public void ok() {
        if (root != null) root.ok(comp);
    }

    public String toString() {
        return root == null ? "()" : root.recursiveToString();
    }

    /**
     * Applicative put;
     * returns a new data structure without affecting any other instances.
     */
    public BATree2<T, U, V> putNew(T k, U d1, V d2) {
        if (root == null) {
            return new BATree2<T, U, V>(new BATree2Node<T, U, V>(k, d1, d2, null, null), comp);
        }

        return new BATree2<T, U, V>(root.add(k, d1, d2, comp), comp);
    }

    public BATree2<T, U, V> copy() {
        return new BATree2<T, U, V>(root, comp);
    }

    /*
     * From the department of ugly patterns; it's hard to call
     * copy from a subclass.
     */
    protected BATree2Node<T, U, V> getRoot() {
        return root;
    }

    public Pair<U, V> put(T k, U d1, V d2) {
        if (root == null) {
            root = new BATree2Node<T, U, V>(k, d1, d2, null, null);
            return null;
        }
        BATree2Node<T, U, V> old = root;
        root = root.add(k, d1, d2, comp);

        // Performance hack; if it wasn't there, then the tree got bigger.
        if (old.weight < root.weight) return null;
        return old.getObject(k, comp).asPair();
    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public Pair<U, V> syncPut(T k, U d1, V d2) {
        BATree2Node<T, U, V> old;
        synchronized (this) {
            if (root == null) {
                root = new BATree2Node<T, U, V>(k, d1, d2, null, null);
                return null;
            }
            old = root;
            root = old.add(k, d1, d2, comp);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (old.weight < root.weight) return null;
        }
        return old.getObject(k, comp).asPair();
    }

    public void clear() {
        root = null;
    }

    @SuppressWarnings ("unchecked")
    public boolean equals(Object o) {
        if (o instanceof BATree2) {
            BATree2 bat = (BATree2) o;
            if (bat.size() != size()) {
                return false;
            }
            BATree2Node a = root;
            BATree2Node b = bat.root;
            for (int i = 0; i < size(); i++) {
                if (!(a.get(i).equals(b.get(i)))) return false;
            }
            return true;
        }
        return super.equals(o);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractMap#entrySet()
     */
    @Override
    public Set<java.util.Map.Entry<T, Pair<U, V>>> entrySet() {
        return new EntrySet<T, U, V>(root, comp);
    }

    /* Slightly more efficient (fewer allocations) versions of get/put */

    public BATree2Node<T, U, V> getNode(T k) {
        if (root == null) return null;
        BATree2Node<T, U, V> f = root.getObject(k, comp);
        if (f == null) return null;
        return f;
    }

    public void putPair(T k, U d1, V d2) {
        if (root == null) {
            root = new BATree2Node<T, U, V>(k, d1, d2, null, null);
            return;
        }
        root = root.add(k, d1, d2, comp);
    }


}
