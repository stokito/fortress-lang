/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
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
 * Does not yet support item removal; does not yet support all the SortedSet
 * methods that it could in principle support.  (lacks headset/subset/tailset).
 */
public class BASet<T> extends AbstractSet<T> implements Set<T> {

    BASnode<T> root;
    final Comparator<T> comp;

    public BASet(Comparator<T> c) {
        comp = c;
    }

    public BASet(Comparator<T> c, Collection<T> it) {
        comp = c;
        this.addAll(it);
    }

    BASet(BASnode<T> r, Comparator<T> c) {
        root = r;
        comp = c;
    }

    public int size() {
        if (root == null) return 0;
        else return root.weight;
    }


    public BASnode<T> getEntry(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k);
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

    public T min() {
        if (root == null) return null;
        BASnode<T> f = root.min();
        return f.key;
    }

    public T max() {
        if (root == null) return null;
        BASnode<T> f = root.max();
        return f.key;
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
    public BASet<T> putNew(T k) {
        if (root == null) {
            return new BASet<T>(new BASnode<T>(k, null, null), comp);
        }

        return new BASet<T>(root.add(k, comp), comp);
    }

    public BASet<T> copy() {
        return new BASet<T>(root, comp);
    }

    public void addArray(T[] ks) {
        for (T k : ks) {
            add(k);
        }
    }

    public boolean add(T k) {
        return put(k);
    }

    public boolean put(T k) {
        if (root == null) {
            root = new BASnode<T>(k, null, null);
            return true;
        }
        BASnode<T> old = root;
        root = old.add(k, comp);

        return (old.weight < root.weight);

    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public boolean syncPut(T k) {
        BASnode<T> old;
        synchronized (this) {
            if (root == null) {
                root = new BASnode<T>(k, null, null);
                return true;
            }
            old = root;
            root = old.add(k, comp);

            // Performance hack; if it wasn't there, then the tree got bigger.
            return (old.weight < root.weight);
        }
    }

    @SuppressWarnings ("unchecked")
    @Override
    public boolean remove(Object k) {
        if (root == null) return false;

        BASnode<T> old = root;
        root = root.delete((T) k, comp);

        return (root == null || old.weight > root.weight);
    }

    @SuppressWarnings ("unchecked")
    @Override
    public boolean contains(Object k) {
        BASnode<T> r = root;
        if (r == null) return false;

        r = r.getObject((T) k, comp);
        return (r != null);
    }

    public void clear() {
        root = null;
    }

    @SuppressWarnings ("unchecked")
    public boolean addAll(Collection<? extends T> c) {
        // We should actually use covariant types here.
        // Ugh, not obvious how to write this correctly.
        if (!(c instanceof BASet)) {
            return super.addAll(c);
        }
        BASet<T> other = (BASet<T>) c;
        if (comp != other.comp) {
            return super.addAll(c);
        }
        BASnode<T> oldroot = root;
        if (oldroot == null) {
            root = other.root;
            return (other.root != null);
        }
        root = BASnode.union(oldroot, other.root, comp);
        return (root.weight > oldroot.weight);
    }

    @SuppressWarnings ("unchecked")
    public boolean removeAll(Collection<?> c) {
        // We should actually use covariant types here.
        // Ugh, not obvious how to write this correctly.
        if (!(c instanceof BASet)) {
            return super.removeAll(c);
        }
        BASet<T> other = (BASet<T>) c;
        if (comp != other.comp) {
            return super.removeAll(c);
        }
        BASnode<T> oldroot = root;
        if (oldroot == null || other.root == null) {
            return false;
        }
        root = BASnode.difference(oldroot, other.root, comp);
        return (root==null || root.weight < oldroot.weight);
    }

    @SuppressWarnings ("unchecked")
    public boolean retainAll(Collection<?> c) {
        // We should actually use covariant types here.
        // Ugh, not obvious how to write this correctly.
        if (!(c instanceof BASet)) {
            return super.retainAll(c);
        }
        BASet<T> other = (BASet<T>) c;
        if (comp != other.comp) {
            return super.retainAll(c);
        }
        BASnode<T> oldroot = root;
        if (oldroot == null) {
            return false;
        }
        root = BASnode.intersection(oldroot, other.root, comp);
        return (root==null || root.weight < oldroot.weight);
    }

    @SuppressWarnings ("unchecked")
    public boolean equals(Object o) {
        if (this==o) return true;
        if (o instanceof Set) {
            Set bat = (Set) o;
            int sz = bat.size();
            BASnode<T> r = root;
            if (r == null) {
                return (sz == 0);
            }
            if (sz != r.weight) return false;
            Object [] arr = bat.toArray();
            // Double check to guard against concurrent modifications
            if (arr.length != r.weight) return false;
            r.equalsHelp(arr,0);
        }
        return super.equals(o);
    }

    @SuppressWarnings("unchecked")
    public boolean containsAll(Collection<?> c) {
        // Again with the covariance
        if (!(c instanceof BASet)) {
            return super.containsAll(c);
        }
        BASet<T> other = (BASet<T>) c;
        if (comp != other.comp) {
            return super.containsAll(c);
        }
        return BASnode.containsAll(root, other.root, comp);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    class Iter implements Iterator<T> {
        int i;
        T last;

        public boolean hasNext() {
            return i < size();
        }

        public T next() {
            if (hasNext()) {
                last = root.get(i++).key;
                return last;
            }
            return null;
        }

        public void remove() {
            if (last != null) {
                root = root.delete(last, comp);
                i--;
            }
        }

    }

    public Comparator<? super T> comparator() {
        return comp;
    }

    public T first() {
        return min();
    }

    public T last() {
        return max();
    }

    public <U> U[] toArray(U[] example) {
        BASnode<T> r = root;
        if (r==null)
            return Arrays.copyOf(example,0);
        if (example.length > 0)
            example = Arrays.copyOf(example,0);
        U [] result = Arrays.copyOf(example,r.weight);
        r.toArray(result, 0);
        return result;
    }

    public Object[] toArray() {
        BASnode<T> r = root;
        if (r==null)
            return new Object[0];
        Object[] result = new Object[r.weight];
        r.toArray(result,0);
        return result;
    }

    //    public SortedSet<T> headSet(T arg0) {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }
    //
    //    public SortedSet<T> subSet(T arg0, T arg1) {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }
    //
    //    public SortedSet<T> tailSet(T arg0) {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }

}
