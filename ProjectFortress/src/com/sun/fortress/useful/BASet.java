/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

/**
 * A subset of all that you might want in a map, but it is embedded in an
 * applicative framework so that small deltas can be obtained at low cost.
 *
 * Does not yet support item removal; does not yet support all the SortedSet
 * methods that it could in principle support.
 */
public class BASet<T> extends AbstractSet<T> implements Set<T> {

    static class BASnode<T> {

        public String toString() {
            return toStringBuffer(new StringBuffer()).toString();
        }

        public StringBuffer toStringBuffer(StringBuffer b) {
            b.append(key);
            return b;
        }

        public String recursiveToString() {
            return recursiveToStringBuffer(new StringBuffer()).toString();
        }

        public StringBuffer recursiveToStringBuffer(StringBuffer b) {
            if (left != null || right != null) {
                b.append("(");
                if (left != null)
                    left.recursiveToStringBuffer(b).append(" ");
                toStringBuffer(b);
                if (right != null)
                    right.recursiveToStringBuffer(b.append(" "));

                b.append(")");
            } else {
                toStringBuffer(b);
            }
            return b;
        }

        void ok(Comparator<T> c) {
            int lw = 0;
            int rw = 0;
            if (left != null) {
                if (c.compare(left.key,key) >= 0)
                    throw new Error("Left key too big");
                left.ok(c);
                if (c.compare(left.max().key,key) >= 0)
                    throw new Error("Left max key too big");
                lw = left.weight;
            }
            if (right != null) {
                if (c.compare(right.key,key) <= 0)
                    throw new Error("Right key too small");
                right.ok(c);
                if (c.compare(right.min().key,key) <= 0)
                    throw new Error("Right min key too small");
                rw =  right.weight;
            }
            if (weight != 1 + lw + rw)
                throw new Error("Weight wrong");
            if (lw >> 1 > rw)
                throw new Error("Left too heavy");
            if (rw >> 1 > lw)
                throw new Error("Right too heavy");

        }
        T key;
        int weight;
        BASnode<T> left;
        BASnode<T> right;
        int leftWeight() {
            return weight(left);
        }
        int rightWeight() {
            return weight(right);
        }

        public static int weight(BASnode n) {
            return n == null ? 0 : n.weight;
        }

        BASnode(T k, BASnode<T> l, BASnode<T> r) {
            key = k;
            left = l;
            right = r;
            weight = leftWeight() + rightWeight() + 1;
        }

        BASnode(T k) {
            key = k;
            weight = 1;
        }

        BASnode(BASnode<T> n, BASnode<T> l, BASnode<T> r) {
            key = n.key;
            left = l;
            right = r;
            weight = leftWeight() + rightWeight() + 1;
        }

        // Index 0 is leftmost.
        BASnode<T> get(int at) {
            BASnode<T> l = left;
            BASnode<T> r = right;
            int lw = weight(l);
            if (at < lw) {
                return l.get(at);
            } else if (at > lw) {
                return r.get(at - lw - 1);
            } else return this;
        }

        BASnode<T> getObject(T k, Comparator<T> comp) {
            BASnode<T> t = this;
            while (t != null) {
                int c = comp.compare(k,t.key);
                if (c == 0)
                    break;
                if (c < 0) {
                    t = t.left;
                } else {
                    t = t.right;
                }
            }
            return t;
        }

        int indexOf(T k, Comparator<T> comp) {
            int toTheLeft = 0;

            BASnode<T> t = this;
            while (t != null) {
                int c = comp.compare(k, t.key);
                if (c == 0)
                    return toTheLeft + weight(t.left);
                if (c < 0) {
                    t = t.left;
                } else {
                    toTheLeft += 1 + weight(t.left);
                    t = t.right;
                }
            }
            return ~toTheLeft;
        }



        BASnode<T> min() {
            BASnode<T> t = this;
            while (t.left != null) t = t.left;
            return t;
        }

        BASnode<T> max() {
            BASnode<T> t = this;
            while (t.right != null) t = t.right;
            return t;
        }

        BASnode<T> add(T k, Comparator<T> comp) {
            int c = comp.compare(k,key);
            BASnode<T> l = left;
            BASnode<T> r = right;
            if (c < 0) {
                // left
                if (l == null) {
                    l = new BASnode<T>(k);

                } else {
                    l = l.add(k, comp);
                    // Worst-case balance: 2^(n+1)-1 vs 2^(n-1)
                    if (weight(l) >> 1 > weight(r)) {
                        // Must rotate.
                        if (l.leftWeight() >= l.rightWeight()) {
                            // L to root
                            return assembleLeft(l.left, l, l.right, this, r);
                        } else {
                            // LR to root
                            BASnode<T> lr = l.right;
                            return assemble(l.left, l, lr.left, lr, lr.right, this, r);
                        }
                    }
                }
                return new BASnode<T>(this,l,r);
            } else if (c > 0) {
                // right
                if (r == null) {
                    r = new BASnode<T>(k);
                } else {
                    r = r.add(k, comp);
//                  Worst-case balance: 2^(n-1) vs 2^(n+1)-1
                    if (weight(r) >> 1 > weight(l)) {
                        // Must rotate.
                        if (r.rightWeight() >= r.leftWeight()) {
                            // R to root
                            return assembleRight(l, this, r.left, r, r.right);

                        } else {
                            // RL to root
                            BASnode<T> rl = r.left;
                            return assemble(l, this, rl.left, rl, rl.right, r, r.right);
                        }
                    }
                }
                return new BASnode<T>(this,l,r);
            } else {
                // Update value.
                return new BASnode<T>(k,l,r);
            }
        }
        private BASnode<T> assembleLeft(BASnode<T> ll, BASnode<T> l, BASnode<T> lr, BASnode<T> old, BASnode<T> r) {
            return new BASnode<T>(l,
                    ll,
                    new BASnode<T>(old, lr, r));
        }
        private BASnode<T> assembleRight(BASnode<T> l, BASnode<T> old, BASnode<T> rl, BASnode<T> r, BASnode<T> rr) {
                    return new BASnode<T>(r,
                    new BASnode<T>(old, l, rl),
                    rr);
        }
        private BASnode<T> assemble(BASnode<T> ll, BASnode<T> l, BASnode<T> lr,
                              BASnode<T> top,
                              BASnode<T> rl, BASnode<T> r, BASnode<T> rr) {
            return new BASnode<T>(top,
                    new BASnode<T>(l, ll, lr),
                    new BASnode<T>(r, rl, rr));
        }

        public T getKey() {
            return key;
        }

    }



    BASnode<T> root;
    Comparator<T> comp;

    public BASet(Comparator<T> c) {
        comp = c;
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
        if (k < 0 || k >= size())
            throw new IndexOutOfBoundsException("" + k + " not between 0 and "  + size());
        return  root.get(k);
    }



    public T getKey(int k) {
        if (k < 0 || k >= size())
            throw new IndexOutOfBoundsException("" + k + " not between 0 and "  + size());
        return  root.get(k).key;
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
            return new BASet<T>(new BASnode<T>(k,null, null), comp);
        }

        return new BASet<T>(root.add(k, comp), comp);
    }
    public BASet<T> copy() {
         return new BASet<T>(root, comp);
    }

    public boolean add(T k) {
        return put(k);
    }

    public boolean put(T k) {
        if (root == null) {
            root = new BASnode<T>(k,null, null);
            return true;
        }
        BASnode<T> old = root;
        root = root.add(k, comp);

        if (old.weight < root.weight)
            return true;

        return false;

    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public boolean syncPut(T k) {
        BASnode<T> old;
        synchronized (this) {
            if (root == null) {
                root = new BASnode<T>(k,  null, null);
                return true;
            }
            old = root;
            root = old.add(k,  comp);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (old.weight < root.weight)
                return true;
        }
        return false;
    }

    public void clear() {
        root = null;
    }

    public boolean equals(Object o) {
        if (o instanceof BASet) {
            BASet bat = (BASet) o;
            if (bat.size() != size()) {
                return false;
            }
            BASnode a = root;
            BASnode b = bat.root;
            for (int i = 0; i < size(); i++) {
                if (!(a.get(i).equals(b.get(i))))
                    return false;
            }
            return true;
        }
        return super.equals(o);
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

        public boolean hasNext() {
            return i < size();
        }

        public T next() {
            if (hasNext())
                return root.get(i++).key;
            return null;
        }

        public void remove() {
            throw new Error("Applicative data structures cannot be changed!");
        }

    }

}
