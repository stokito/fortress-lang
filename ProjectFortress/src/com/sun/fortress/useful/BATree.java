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
 * Does not yet support all the SortedMap methods that it could in principle support.
 */
public class BATree<T, U> extends AbstractMap<T, U> implements Map<T, U>, java.io.Serializable, Cloneable {

    static class EntrySet<T, U> extends AbstractSet<Map.Entry<T, U>> implements Set<Map.Entry<T, U>> {

        final Comparator<? super T> comp;
        BATreeNode<T, U> root;

        EntrySet(BATreeNode<T, U> r, Comparator<? super T> c) {
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
                BATreeNode<T, U> n = root;
                // TODO Since there's no side effects, we can fake out the generic, I think.
                n = n.getObject((T) e.getKey(), comp);
                if (n == null) return false;
                return n.data.equals(e.getValue());
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
        public Iterator<Entry<T, U>> iterator() {
            return new Iter();
        }

        class Iter implements Iterator<Entry<T, U>> {
            int i;

            public boolean hasNext() {
                return i < size();
            }

            public java.util.Map.Entry<T, U> next() {
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

    volatile BATreeNode<T, U> root;
    final Comparator<? super T> comp;

    public BATree(Comparator<? super T> c) {
        comp = c;
    }

    protected BATree(BATreeNode<T, U> r, Comparator<? super T> c) {
        root = r;
        comp = c;
    }

    public BATree(BATree<T, U> t) {
        root = t.root;
        comp = t.comp;
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int size() {
        if (root == null) return 0;
        else return root.weight;
    }

    @SuppressWarnings ("unchecked")
    public U get(Object k) {
        if (root == null) return null;
        BATreeNode<T, U> f = root.getObject((T) k, comp);
        if (f == null) return null;
        return f.data;
    }

    public BATreeNode<T, U> getEntry(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k);
    }

    public U getData(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).data;
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

    public U min() {
        if (root == null) return null;
        BATreeNode<T, U> f = root.min();
        return f.data;
    }

    public U max() {
        if (root == null) return null;
        BATreeNode<T, U> f = root.max();
        return f.data;
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
    public BATree<T, U> putNew(T k, U d) {
        if (root == null) {
            return new BATree<T, U>(new BATreeNode<T, U>(k, d, null, null), comp);
        }

        return new BATree<T, U>(root.add(k, d, comp), comp);
    }

    public BATree<T, U> copy() {
        return new BATree<T, U>(root, comp);
    }

    @SuppressWarnings ("unchecked")
    @Override
    public U remove(Object arg0) {
        T k = (T) arg0;
        if (root == null) return null;
        BATreeNode<T, U> old = root;
        root = root.delete(k, comp);
        if (root == null || root.weight < old.weight) {
            return old.getObject(k, comp).data;
        }
        return null;
    }

    /*
     * From the department of ugly patterns; it's hard to call
     * copy from a subclass.
     */
    protected BATreeNode<T, U> getRoot() {
        return root;
    }

    public U put(T k, U d) {
        if (root == null) {
            root = new BATreeNode<T, U>(k, d, null, null);
            return null;
        }
        BATreeNode<T, U> old = root;
        root = root.add(k, d, comp);

        // Performance hack; if it wasn't there, then the tree got bigger.
        if (old.weight < root.weight) return null;
        return old.getObject(k, comp).data;
    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public U syncPut(T k, U d) {
        BATreeNode<T, U> old;
        synchronized (this) {
            if (root == null) {
                root = new BATreeNode<T, U>(k, d, null, null);
                return null;
            }
            old = root;
            root = old.add(k, d, comp);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (old.weight < root.weight) return null;
        }
        return old.getObject(k, comp).data;
    }

    /**
     * Puts a value only if there is no data present.
     * Returns the data that is actually in the tree.
     */
    public U syncPutIfMissing(T k, U d) {
        BATreeNode<T, U> next;
        synchronized (this) {
            if (root == null) {
                root = new BATreeNode<T, U>(k, d, null, null);
                return d;
            }

            next = root.add(k, d, comp);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (next.weight == root.weight) return root.getObject(k, comp).getValue();

            root = next;
            return d;
        }

    }

    public void clear() {
        root = null;
    }

    @SuppressWarnings ("unchecked")
    public boolean equals(Object o) {
        if (o instanceof BATree) {
            BATree bat = (BATree) o;
            if (bat.size() != size()) {
                return false;
            }
            BATreeNode a = root;
            BATreeNode b = bat.root;
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
    public Set<java.util.Map.Entry<T, U>> entrySet() {
        return new EntrySet<T, U>(root, comp);
    }

}
