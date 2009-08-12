/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

import java.util.*;

/**
 * A subset of all that you might want in a map, but it is embedded in an
 * applicative framework so that small deltas can be obtained at low cost.
 * <p/>
 * Does not yet support item removal; does not yet support all the SortedSet
 * methods that it could in principle support.  (lacks headset/subset/tailset).
 */
public class BASet<T> extends AbstractSet<T> implements Set<T> {

    static final class BASnode<T> {

        final T key;
        final int weight;
        final BASnode<T> left;
        final BASnode<T> right;

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
                if (left != null) left.recursiveToStringBuffer(b).append(" ");
                toStringBuffer(b);
                if (right != null) right.recursiveToStringBuffer(b.append(" "));

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
                if (c.compare(left.key, key) >= 0) throw new Error("Left key too big");
                left.ok(c);
                if (c.compare(left.max().key, key) >= 0) throw new Error("Left max key too big");
                lw = left.weight;
            }
            if (right != null) {
                if (c.compare(right.key, key) <= 0) throw new Error("Right key too small");
                right.ok(c);
                if (c.compare(right.min().key, key) <= 0) throw new Error("Right min key too small");
                rw = right.weight;
            }
            if (weight != 1 + lw + rw) throw new Error("Weight wrong");
            if (lw >> 2 > rw) throw new Error("Left too heavy");
            if (rw >> 2 > lw) throw new Error("Right too heavy");

        }

        int leftWeight() {
            return weight(left);
        }

        int rightWeight() {
            return weight(right);
        }

        public static <T> int weight(BASnode<T> n) {
            return n == null ? 0 : n.weight;
        }

        // Special BASnode to create results of split
        // (doesn't enforce structural constraints)
        BASnode(T k, int w, BASnode<T> l, BASnode<T> r) {
            key = k;
            left = l;
            right = r;
            weight = w;
        }

        BASnode(T k, BASnode<T> l, BASnode<T> r) {
            key = k;
            left = l;
            right = r;
            weight = combine(l, r);
        }

        BASnode(T k) {
            key = k;
            left = null;
            right = null;
            weight = 1;
        }

        BASnode(BASnode<T> n, BASnode<T> l, BASnode<T> r) {
            key = n.key;
            left = l;
            right = r;
            weight = combine(l, r);
        }

        private Error combFail(String how, BASnode<T> l, BASnode<T> r) {
            String ls = (l==null?"null":l.recursiveToString());
            String rs = (r==null?"null":r.recursiveToString());
            return new Error(how + weight(l) + " " + weight(r) +
                             "\nkey = " + key +
                             "\nl = " + ls + "\nr = " + rs);
        }

        private int combine(BASnode<T> l, BASnode<T> r) {
            int lw = weight(l);
            int rw = weight(r);
            if (lw >> 2 > rw) throw combFail("Left too heavy ",l,r);
            if (rw >> 2 > lw) throw combFail("Right too heavy ",l,r);
            return lw + rw + 1;
        }

        // Index 0 is leftmost.
        BASnode<T> get(int at) {
            BASnode<T> self = this;
            while (self != null) {
                BASnode<T> l = self.left;
                int lw = weight(l);
                if (at < lw) {
                    self = l;
                } else if (at > lw) {
                    at -= lw+1;
                    self = self.right;
                } else {
                    break;
                }
            }
            return self;
        }

        BASnode<T> getObject(T k, Comparator<T> comp) {
            BASnode<T> t = this;
            while (t != null) {
                int c = comp.compare(k, t.key);
                if (c == 0) break;
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
                if (c == 0) return toTheLeft + weight(t.left);
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
            while (t.left != null) {
                t = t.left;
            }
            return t;
        }

        BASnode<T> max() {
            BASnode<T> t = this;
            while (t.right != null) {
                t = t.right;
            }
            return t;
        }

        BASnode<T> add(T k, Comparator<T> comp) {
            int c = comp.compare(k, key);
            BASnode<T> l = left;
            BASnode<T> r = right;
            if (c < 0) {
                // left
                if (l == null) {
                    l = new BASnode<T>(k);
                    return new BASnode<T>(this, l, r);
                } else {
                    l = l.add(k, comp);
                    return leftWeightIncreased(l, r);
                }
            } else if (c > 0) {
                // right
                if (r == null) {
                    r = new BASnode<T>(k);
                    return new BASnode<T>(this, l, r);
                } else {
                    r = r.add(k, comp);
                    return rightWeightIncreased(l, r);
                }
            } else {
                // Update value.
                return new BASnode<T>(k, l, r);
            }
        }

        private BASnode<T> leftWeightIncreased(BASnode<T> l, BASnode<T> r) {
            // Worst-case balance: 2^(n+1)-1 vs 2^(n-1)
            int rw = weight(r);
            int lw = weight(l);
            if (lw >> 1 > rw) {
                // Must rotate.
                int lrw = l.rightWeight();
                int llw = l.leftWeight();
                BASnode<T> lr = l.right;
                if (llw >= lrw) {
                    // L to root
                    return assembleLeft(l.left, l, l.right, this, r);
                } else {
                    // LR to root
                    return assemble(l.left, l, lr.left, lr, lr.right, this, r);
                }
            }
            return new BASnode<T>(this, l, r);
        }

        private BASnode<T> leftHeavy(BASnode<T> l, BASnode<T> r) {
            // Worst-case balance is nothing left, everything r!
            int rw = weight(r);
            int lw = weight(l);
            if (lw >> 2 > rw) {
                // Way out of balance; put root in r and join.
                return join(l, join(new BASnode<T>(key), r));
            } else {
                return leftWeightIncreased(l,r);
            }
        }

        private BASnode<T> rightWeightIncreased(BASnode<T> l, BASnode<T> r) {
            // Worst-case balance: 2^(n-1) vs 2^(n+1)-1
            int rw = weight(r);
            int lw = weight(l);
            if (rw >> 1 > lw) {
                // Must rotate.
                int rrw = r.rightWeight();
                int rlw = r.leftWeight();
                BASnode<T> rl = r.left;
                if (rrw >= rlw) {
                    // R to root
                    return assembleRight(l, this, r.left, r, r.right);

                } else {
                    // RL to root

                    return assemble(l, this, rl.left, rl, rl.right, r, r.right);
                }
            } else {
                return new BASnode<T>(this, l, r);
            }
        }

        private BASnode<T> rightHeavy(BASnode<T> l, BASnode<T> r) {
            // Worst-case balance is nothing left, everything r!
            int rw = weight(r);
            int lw = weight(l);
            if (rw >> 2 > lw) {
                // Way out of balance; put root in l and join.
                return join(join(l, new BASnode<T>(key)), r);
            } else {
                return rightWeightIncreased(l,r);
            }
        }

        BASnode<T> deleteMin(BASnode<T>[] deleted) {
            BASnode<T> l = left;
            if (l == null) {
                deleted[0] = this;
                return right;
            } else {
                l = l.deleteMin(deleted);
                BASnode<T> r = right;
                return rightWeightIncreased(l, r);
            }
        }

        BASnode<T> deleteMax(BASnode<T>[] deleted) {
            BASnode<T> r = right;
            if (r == null) {
                deleted[0] = this;
                return left;
            } else {
                r = r.deleteMax(deleted);
                BASnode<T> l = left;
                return leftWeightIncreased(l, r);
            }
        }

        @SuppressWarnings ("unchecked")
        BASnode<T> delete(T k, Comparator<T> comp) {
            int c = comp.compare(k, key);
            BASnode<T> l = left;
            BASnode<T> r = right;
            if (c == 0) {
                int lw = weight(left);
                int rw = weight(right);
                BASnode<T>[] newthis = new BASnode[1];
                if (lw > rw) {
                    l = l.deleteMax(newthis);
                } else {
                    if (rw > 0) r = r.deleteMin(newthis);
                    else return null;
                }
                return new BASnode<T>(newthis[0], l, r);
            } else if (c < 0) {
                if (l == null) return this;
                BASnode<T> nl = l.delete(k, comp);
                if (nl == l) return this;
                return rightWeightIncreased(nl, r);
            } else {
                if (r == null) return this;
                BASnode<T> nr = r.delete(k, comp);
                if (nr == r) return this;
                return leftWeightIncreased(l, nr);
            }
        }

        // Hoist key k to root, ignoring structural constraints on
        // root but enforcing them on left and right children.  If k
        // is absent, conceptually insert dummy with null key in the
        // place where k belonged and hoist that node.  So result.key
        // is null if k is absent and k otherwise; result.left and result.right
        // are the nodes below and above k respecively.
        public BASnode<T> split(T k, Comparator<T> comp) {
            int c = comp.compare(k, key);
            BASnode<T> l = left;
            BASnode<T> r = right;
            if (c < 0) {
                BASnode<T> sl = split(l, k, comp);
                return new BASnode<T>(sl.key, weight,
                                      sl.left, rightHeavy(sl.right,r));
            } else if (c > 0) {
                BASnode<T> sr = split(r, k, comp);
                return new BASnode<T>(sr.key, weight,
                                      leftHeavy(l, sr.left), sr.right);
            } else {
                return this;
            }
        }

        public static <T> BASnode<T> split(BASnode<T> t, T k, Comparator<T> comp) {
            if (t==null) return new BASnode<T>(null,0,null,null);
            return t.split(k,comp);
        }

        // Ordered join; all keys in l < all keys in r
        public static <T> BASnode<T> join(BASnode<T> l, BASnode<T> r) {
            if (l==null) return r;
            if (r==null) return l;
            if (l.weight < r.weight) {
                return r.leftWeightIncreased(join(l,r.left), r.right);
            } else {
                return l.rightWeightIncreased(l.left, join(l.right, r));
            }
        }

        public static <T> BASnode<T> union(BASnode<T> a, BASnode<T> b, Comparator<T> comp) {
            if (a==null) return b;
            if (b==null) return a;
            if (a.weight < b.weight) {
                BASnode<T> t=a; a=b; b=t;
            }
            b = b.split(a.key, comp);
            BASnode<T> lu = union(a.left,  b.left,  comp);
            BASnode<T> ru = union(a.right, b.right, comp);
            if (weight(lu) < weight(ru)) {
                return a.rightWeightIncreased(lu,ru);
            } else {
                return a.leftWeightIncreased(lu,ru);
            }
        }

        public static <T> BASnode<T> intersection(BASnode<T> a, BASnode<T> b, Comparator<T> comp) {
            if (a==null || b==null) return null;
            if (a.weight < b.weight) {
                BASnode<T> t=a; a=b; b=t;
            }
            b = b.split(a.key, comp);
            BASnode<T> lu = intersection(a.left,  b.left,  comp);
            BASnode<T> ru = intersection(a.right, b.right, comp);
            if (b.key == null) {
                return join(lu,ru);
            } else if (weight(lu) < weight(ru)) {
                return a.rightWeightIncreased(lu,ru);
            } else {
                return a.leftWeightIncreased(lu,ru);
            }
        }

        public static <T> BASnode<T> difference(BASnode<T> a, BASnode<T> b, Comparator<T> comp) {
            if (a==null || b==null) return a;
            if (a.weight < b.weight) {
                a = a.split(b.key, comp);
            } else {
                b = b.split(a.key, comp);
            }
            BASnode<T> lu = difference(a.left,  b.left,  comp);
            BASnode<T> ru = difference(a.right, b.right, comp);
            if (a.key==null || b.key != null) {
                return join(lu,ru);
            } else if (weight(lu) < weight(ru)) {
                return a.rightWeightIncreased(lu,ru);
            } else {
                return a.leftWeightIncreased(lu,ru);
            }
        }

        private BASnode<T> assembleLeft(BASnode<T> ll, BASnode<T> l, BASnode<T> lr, BASnode<T> old, BASnode<T> r) {
            return new BASnode<T>(l, ll, new BASnode<T>(old, lr, r));
        }

        private BASnode<T> assembleRight(BASnode<T> l, BASnode<T> old, BASnode<T> rl, BASnode<T> r, BASnode<T> rr) {
            return new BASnode<T>(r, new BASnode<T>(old, l, rl), rr);
        }

        private BASnode<T> assemble(BASnode<T> ll,
                                    BASnode<T> l,
                                    BASnode<T> lr,
                                    BASnode<T> top,
                                    BASnode<T> rl,
                                    BASnode<T> r,
                                    BASnode<T> rr) {
            return new BASnode<T>(top, new BASnode<T>(l, ll, lr), new BASnode<T>(r, rl, rr));
        }

        public <U> void toArray(U [] result, int leftMost) {
            if (left!=null) {
                left.toArray(result, leftMost);
                leftMost += left.weight;
            }
            result[leftMost] = (U)key;
            if (right!=null) {
                right.toArray(result, leftMost+1);
            }
        }

        public static <T> boolean equals(BASnode<T> self, BASnode<T> other, Comparator<T> comp) {
            while (self != other) {
                if (self==null || other==null) return false;
                if (self.weight != other.weight) return false;
                other = other.split(self.key, comp);
                if (other.key == null) return false;
                if (!equals(self.left, other.left, comp)) return false;
                self = self.right;
                other = other.right;
            }
            return true;
        }

        public boolean equalsHelp(Object [] to, int leftMost) {
            BASnode<T> self = this;
            while (self != null) {
                BASnode<T> l = self.left;
                if (l!=null) {
                    if (!l.equalsHelp(to,leftMost)) return false;
                    leftMost += l.weight;
                }
                if (!self.key.equals(to[leftMost++])) return false;
                self = self.right;
            }
            return true;
        }

        public static <T> boolean containsAll(BASnode<T> a, BASnode<T> b, Comparator<T> comp) {
            while (a!=b) {
                if (a==null) return false;
                if (b==null) return true;
                if (b.weight > a.weight) return false;
                b = b.split(a.key, comp);
                if (!containsAll(a.left, b.left, comp)) return false;
                a = a.right;
                b = b.right;
            }
            return true;
        }

        public T getKey() {
            return key;
        }

    }


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
