/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

/**
 * A balanced, applicative tree, indexed by equivalence classes
 * defined by the (asymmetric) comparator and canonicalizing translator.
 * Comparisons are performed on untranslated keys, insertions cause
 * a canonicalizing translation.
 */
public class BATreeEC<Key, KeyEC, Value> {

    volatile BATreeNodeEC<Key, KeyEC, Value> root;

    EquivalenceClass<Key, KeyEC> equivalenceClass;

    public BATreeEC(EquivalenceClass<Key, KeyEC> e) {
        equivalenceClass = e;
    }

    protected BATreeEC(BATreeNodeEC<Key, KeyEC, Value> r, EquivalenceClass<Key, KeyEC> e) {
        root = r;
        equivalenceClass = e;
    }

    public int size() {
        if (root == null) return 0;
        else return root.weight;
    }

    public Value get(Key k) {
        if (root == null) return null;
        BATreeNodeEC<Key, KeyEC, Value> f = root.getObject(k, equivalenceClass);
        if (f == null) return null;
        return f.data;
    }

    public BATreeNodeEC<Key, KeyEC, Value> getEntry(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k);
    }

    public Value getData(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).data;
    }

    public KeyEC getKey(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).key;
    }

    /**
     * Returns the index of key k in the map if k is present,
     * otherwise returns the 1's-complement of the index that k
     * would have if k were inserted into the map.
     */
    public int indexOf(Key k) {
        if (root == null) return ~0;
        return root.indexOf(k, equivalenceClass);
    }

    public Value min() {
        if (root == null) return null;
        BATreeNodeEC<Key, KeyEC, Value> f = root.min();
        return f.data;
    }

    public Value max() {
        if (root == null) return null;
        BATreeNodeEC<Key, KeyEC, Value> f = root.max();
        return f.data;
    }

    public void ok() {
        if (root != null) root.ok(equivalenceClass);
    }

    public String toString() {
        return root == null ? "()" : root.recursiveToString();
    }

    /**
     * Applicative put;
     * returns a new data structure without affecting any other instances.
     */
    public BATreeEC<Key, KeyEC, Value> putNew(Key k, Value d) {
        if (root == null) {
            return new BATreeEC<Key, KeyEC, Value>(new BATreeNodeEC<Key, KeyEC, Value>(equivalenceClass.translate(k),
                                                                                       d,
                                                                                       null,
                                                                                       null), equivalenceClass);
        }

        return new BATreeEC<Key, KeyEC, Value>(root.add(k, d, equivalenceClass), equivalenceClass);
    }

    public BATreeEC<Key, KeyEC, Value> copy() {
        return new BATreeEC<Key, KeyEC, Value>(root, equivalenceClass);
    }

    /*
     * From the department of ugly patterns; it's hard to call
     * copy from a subclass.
     */
    protected BATreeNodeEC<Key, KeyEC, Value> getRoot() {
        return root;
    }

    public Value put(Key k, Value d) {
        if (root == null) {
            root = new BATreeNodeEC<Key, KeyEC, Value>(equivalenceClass.translate(k), d, null, null);
            return null;
        }
        BATreeNodeEC<Key, KeyEC, Value> old = root;
        root = root.add(k, d, equivalenceClass);

        // Performance hack; if it wasn't there, then the tree got bigger.
        if (old.weight < root.weight) return null;
        return old.getObject(k, equivalenceClass).data;
    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public Value syncPut(Key k, Value d) {
        BATreeNodeEC<Key, KeyEC, Value> old;
        synchronized (this) {
            if (root == null) {
                root = new BATreeNodeEC<Key, KeyEC, Value>(equivalenceClass.translate(k), d, null, null);
                return null;
            }
            old = root;
            root = old.add(k, d, equivalenceClass);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (old.weight < root.weight) return null;
        }
        return old.getObject(k, equivalenceClass).data;
    }

    public void clear() {
        root = null;
    }

    public int hashCode() {
        return super.hashCode();
    }

    @SuppressWarnings ("unchecked")
    public boolean equals(Object o) {
        if (o instanceof BATreeEC) {
            BATreeEC bat = (BATreeEC) o;
            if (bat.size() != size()) {
                return false;
            }
            BATreeNodeEC a = root;
            BATreeNodeEC b = bat.root;
            for (int i = 0; i < size(); i++) {
                if (!(a.get(i).equals(b.get(i)))) return false;
            }
            return true;
        }
        return super.equals(o);
    }

}
