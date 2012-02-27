/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.runtimeSystem;

import java.util.*;


/**
 * A subset of all that you might want in a map, but it is embedded in an
 * applicative framework so that small deltas can be obtained at low cost.
 * <p/>
 * Does not yet support all the SortedMap methods that it could in principle support.
 */
public class BAlongTree {

    volatile BAlongTreeNode root;

    public BAlongTree() {
    }

    protected BAlongTree(BAlongTreeNode r) {
        root = r;
    }

    public BAlongTree(BAlongTree t) {
        root = t.root;
    }

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int size() {
        if (root == null) return 0;
        else return root.weight;
    }

    @SuppressWarnings ("unchecked")
    public Object get(long k) {
        BAlongTreeNode r = root;
        if (r == null) return null;
        BAlongTreeNode f = r.getObject( k);
        if (f == null) return null;
        return f.data;
    }

    public BAlongTreeNode getEntry(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k);
    }

    public Object getData(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).data;
    }

    public long getKey(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).key;
    }

    /**
     * Returns the index of key k in the map if k is present,
     * otherwise returns the 1's-complement of the index that k
     * would have if k were inserted into the map.
     */
    public int indexOf(long k) {
        BAlongTreeNode r = root;
        if (r == null) return ~0;
        return r.indexOf(k);
    }

    public Object min() {
        BAlongTreeNode r = root;
        if (r == null) return null;
        BAlongTreeNode f = r.min();
        return f.data;
    }

    public Object max() {
        BAlongTreeNode r = root;
        if (r == null) return null;
        BAlongTreeNode f = r.max();
        return f.data;
    }

    public void ok() {
        BAlongTreeNode r = root;
        if (r != null) r.ok();
    }

    public String toString() {
        BAlongTreeNode r = root;
        return r == null ? "()" : r.recursiveToString();
    }

    /**
     * Applicative put;
     * returns a new data structure without affecting any other instances.
     */
    public BAlongTree putNew(long k, Object d) {
        BAlongTreeNode r = root;
       if (r == null) {
            return new BAlongTree(new BAlongTreeNode(k, d, null, null));
        }

        return new BAlongTree(r.add(k, d));
    }

    public BAlongTree copy() {
        return new BAlongTree(root);
    }

    @SuppressWarnings ("unchecked")
    public Object remove(long arg0) {
        BAlongTreeNode r = root;
       long k =  arg0;
        if (r == null) return null;
        BAlongTreeNode old = r;
        r = r.delete(k);
        root = r;
        if (r == null || r.weight < old.weight) {
            return old.getObject(k).data;
        }
        return null;
    }

    /*
     * From the department of ugly patterns; it's hard to call
     * copy from a subclass.
     */
    protected BAlongTreeNode getRoot() {
        return root;
    }

    public Object put(long k, Object d) {
        BAlongTreeNode r = root;
        if (r == null) {
            root = new BAlongTreeNode(k, d, null, null);
            return null;
        }
        BAlongTreeNode old = r;
        r = r.add(k, d);
        root = r;
        // Performance hack; if it wasn't there, then the tree got bigger.
        if (old.weight < r.weight) return null;
        return old.getObject(k).data;
    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public Object syncPut(long k, Object d) {
        BAlongTreeNode r = root;
        BAlongTreeNode old;
        synchronized (this) {
            if (r == null) {
                root = new BAlongTreeNode(k, d, null, null);
                return null;
            }
            old = r;
            r = old.add(k, d);
            root = r;
            
            // Performance hack; if it wasn't there, then the tree got bigger.
            if (old.weight < r.weight) return null;
        }
        return old.getObject(k).data;
    }

    /**
     * Puts a value only if there is no data present.
     * Returns the data that is actually in the tree.
     */
    public Object syncPutIfMissing(long k, Object d) {
        BAlongTreeNode r = root;
        BAlongTreeNode next;
        synchronized (this) {
            if (r == null) {
                root = new BAlongTreeNode(k, d, null, null);
                return d;
            }

            next = r.add(k, d);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (next.weight == r.weight) return r.getObject(k).getValue();

            root = next;
            return d;
        }

    }

    public void clear() {
        root = null;
    }

    public int hashCode() {
        return super.hashCode();
    }

    @SuppressWarnings ("unchecked")
    public boolean equals(Object o) {
        if (o instanceof BAlongTree) {
            BAlongTree bat = (BAlongTree) o;
            if (bat.size() != size()) {
                return false;
            }
            BAlongTreeNode a = root;
            BAlongTreeNode b = bat.root;
            for (int i = 0; i < size(); i++) {
                if (!(a.get(i).equals(b.get(i)))) return false;
            }
            return true;
        }
        return super.equals(o);
    }

  
}
