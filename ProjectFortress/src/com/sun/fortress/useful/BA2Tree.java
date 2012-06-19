/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;


/**
 * A subset of all that you might want in a map, but it is embedded in an
 * applicative framework so that small deltas can be obtained at low cost.
 * <p/>
 * Does not yet support item removal; does not yet support all the SortedMap
 * methods that it could in principle support.
 */
public class BA2Tree<T1, T2, U> {

    static class Node<T1, T2, U> {

        public String toString() {
            return toStringBuffer(new StringBuffer()).toString();
        }

        public StringBuffer toStringBuffer(StringBuffer b) {
            b.append(key1);
            b.append(",");
            b.append(key2);
            b.append("=");
            b.append(data);
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

        void ok(BA2Tree<T1, T2, U> c) {
            int lw = 0;
            int rw = 0;
            if (left != null) {
                if (c.compare(left, this) >= 0) throw new Error("Left key too big");
                left.ok(c);
                if (c.compare(left.max(), this) >= 0) throw new Error("Left max key too big");
                lw = left.weight;
            }
            if (right != null) {
                if (c.compare(right, this) <= 0) throw new Error("Right key too small");
                right.ok(c);
                if (c.compare(right.min(), this) <= 0) throw new Error("Right min key too small");
                rw = right.weight;
            }
            if (weight != 1 + lw + rw) throw new Error("Weight wrong");
            if (lw >> 1 > rw) throw new Error("Left too heavy");
            if (rw >> 1 > lw) throw new Error("Right too heavy");

        }

        T1 key1;
        T2 key2;
        U data;
        int weight;
        Node<T1, T2, U> left;
        Node<T1, T2, U> right;

        int leftWeight() {
            return weight(left);
        }

        int rightWeight() {
            return weight(right);
        }

        public static <T1, T2, U> int weight(Node<T1, T2, U> n) {
            return n == null ? 0 : n.weight;
        }

        Node(T1 k1, T2 k2, U d, Node<T1, T2, U> l, Node<T1, T2, U> r) {
            key1 = k1;
            key2 = k2;
            data = d;
            left = l;
            right = r;
            weight = leftWeight() + rightWeight() + 1;
        }

        Node(T1 k1, T2 k2, U d) {
            key1 = k1;
            key2 = k2;
            data = d;
            weight = 1;
        }

        Node(Node<T1, T2, U> n, Node<T1, T2, U> l, Node<T1, T2, U> r) {
            key1 = n.key1;
            key2 = n.key2;
            data = n.data;
            left = l;
            right = r;
            weight = leftWeight() + rightWeight() + 1;
        }

        // Index 0 is leftmost.
        Node<T1, T2, U> get(int at) {
            Node<T1, T2, U> l = left;
            Node<T1, T2, U> r = right;
            int lw = weight(l);
            if (at < lw) {
                return l.get(at);
            } else if (at > lw) {
                return r.get(at - lw - 1);
            } else return this;
        }

        Node<T1, T2, U> getObject(T1 k1, T2 k2, BA2Tree<T1, T2, U> comp) {
            Node<T1, T2, U> t = this;
            while (t != null) {
                int c = comp.compare(k1, k2, t);
                if (c == 0) break;
                if (c < 0) {
                    t = t.left;
                } else {
                    t = t.right;
                }
            }
            return t;
        }

        int indexOf(T1 k1, T2 k2, BA2Tree<T1, T2, U> comp) {
            int toTheLeft = 0;

            Node<T1, T2, U> t = this;
            while (t != null) {
                int c = comp.compare(k1, k2, t);
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


        Node<T1, T2, U> min() {
            Node<T1, T2, U> t = this;
            while (t.left != null) {
                t = t.left;
            }
            return t;
        }

        Node<T1, T2, U> max() {
            Node<T1, T2, U> t = this;
            while (t.right != null) {
                t = t.right;
            }
            return t;
        }

        Node<T1, T2, U> add(T1 k1, T2 k2, U d, BA2Tree<T1, T2, U> comp) {
            int c = comp.compare(k1, k2, this);
            Node<T1, T2, U> l = left;
            Node<T1, T2, U> r = right;
            if (c < 0) {
                // left
                if (l == null) {
                    l = new Node<T1, T2, U>(k1, k2, d);

                } else {
                    l = l.add(k1, k2, d, comp);
                    // Worst-case balance: 2^(n+1)-1 vs 2^(n-1)
                    if (weight(l) >> 1 > weight(r)) {
                        // Must rotate.
                        if (l.leftWeight() >= l.rightWeight()) {
                            // L to root
                            return assembleLeft(l.left, l, l.right, this, r);
                        } else {
                            // LR to root
                            Node<T1, T2, U> lr = l.right;
                            return assemble(l.left, l, lr.left, lr, lr.right, this, r);
                        }
                    }
                }
                return new Node<T1, T2, U>(this, l, r);
            } else if (c > 0) {
                // right
                if (r == null) {
                    r = new Node<T1, T2, U>(k1, k2, d);
                } else {
                    r = r.add(k1, k2, d, comp);
                    //                  Worst-case balance: 2^(n-1) vs 2^(n+1)-1
                    if (weight(r) >> 1 > weight(l)) {
                        // Must rotate.
                        if (r.rightWeight() >= r.leftWeight()) {
                            // R to root
                            return assembleRight(l, this, r.left, r, r.right);

                        } else {
                            // RL to root
                            Node<T1, T2, U> rl = r.left;
                            return assemble(l, this, rl.left, rl, rl.right, r, r.right);
                        }
                    }
                }
                return new Node<T1, T2, U>(this, l, r);
            } else {
                // Update value.
                return new Node<T1, T2, U>(k1, k2, d, l, r);
            }
        }

        private Node<T1, T2, U> assembleLeft(Node<T1, T2, U> ll,
                                             Node<T1, T2, U> l,
                                             Node<T1, T2, U> lr,
                                             Node<T1, T2, U> old,
                                             Node<T1, T2, U> r) {
            return new Node<T1, T2, U>(l, ll, new Node<T1, T2, U>(old, lr, r));
        }

        private Node<T1, T2, U> assembleRight(Node<T1, T2, U> l,
                                              Node<T1, T2, U> old,
                                              Node<T1, T2, U> rl,
                                              Node<T1, T2, U> r,
                                              Node<T1, T2, U> rr) {
            return new Node<T1, T2, U>(r, new Node<T1, T2, U>(old, l, rl), rr);
        }

        private Node<T1, T2, U> assemble(Node<T1, T2, U> ll,
                                         Node<T1, T2, U> l,
                                         Node<T1, T2, U> lr,
                                         Node<T1, T2, U> top,
                                         Node<T1, T2, U> rl,
                                         Node<T1, T2, U> r,
                                         Node<T1, T2, U> rr) {
            return new Node<T1, T2, U>(top, new Node<T1, T2, U>(l, ll, lr), new Node<T1, T2, U>(r, rl, rr));
        }

        public T1 getKey1() {
            return key1;
        }

        public T2 getKey2() {
            return key2;
        }

        public U getValue() {
            return data;
        }

        public U setValue(U arg0) {
            throw new Error("Applicative data structures cannot be changed!");
        }

    }

    Node<T1, T2, U> root;
    Comparator<T1> comp1;
    Comparator<T2> comp2;

    int compare(T1 a1, T2 a2, T1 b1, T2 b2) {
        int a = comp1.compare(a1, b1);
        if (a != 0) return a;
        return comp2.compare(a2, b2);
    }

    int compare(Node<T1, T2, U> a, T1 b1, T2 b2) {
        return compare(a.key1, a.key2, b1, b2);
    }

    int compare(T1 a1, T2 a2, Node<T1, T2, U> b) {
        return compare(a1, a2, b.key1, b.key2);
    }

    int compare(Node<T1, T2, U> a, Node<T1, T2, U> b) {
        return compare(a.key1, a.key2, b.key1, b.key2);
    }

    public BA2Tree(Comparator<T1> c1, Comparator<T2> c2) {
        comp1 = c1;
        comp2 = c2;
    }

    BA2Tree(BA2Tree.Node<T1, T2, U> r, Comparator<T1> c1, Comparator<T2> c2) {
        this(c1, c2);
        root = r;

    }

    public int size() {
        if (root == null) return 0;
        else return root.weight;
    }

    public U get(T1 k1, T2 k2) {
        if (root == null) return null;
        Node<T1, T2, U> f = root.getObject(k1, k2, this);
        if (f == null) return null;
        return f.data;
    }

    public Node<T1, T2, U> getEntry(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k);
    }

    public U getData(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        return root.get(k).data;
    }

    public Pair<T1, T2> getKey(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("" + k + " not between 0 and " + size());
        Node<T1, T2, U> n = root.get(k);
        return new Pair<T1, T2>(n.key1, n.key2);

    }

    /**
     * Returns the index of key k in the map if k is present,
     * otherwise returns the 1's-complement of the index that k
     * would have if k were inserted into the map.
     */
    public int indexOf(T1 k1, T2 k2) {
        if (root == null) return ~0;
        return root.indexOf(k1, k2, this);
    }

    public U min() {
        if (root == null) return null;
        Node<T1, T2, U> f = root.min();
        return f.data;
    }

    public U max() {
        if (root == null) return null;
        Node<T1, T2, U> f = root.max();
        return f.data;
    }

    public void ok() {
        if (root != null) root.ok(this);
    }

    public String toString() {
        return root == null ? "()" : root.recursiveToString();
    }

    /**
     * Applicative put;
     * returns a new data structure without affecting any other instances.
     */
    public BA2Tree<T1, T2, U> putNew(T1 k1, T2 k2, U d) {
        if (root == null) {
            return new BA2Tree<T1, T2, U>(new Node<T1, T2, U>(k1, k2, d, null, null), comp1, comp2);
        }

        return new BA2Tree<T1, T2, U>(root.add(k1, k2, d, this), comp1, comp2);
    }

    public BA2Tree<T1, T2, U> copy() {
        return new BA2Tree<T1, T2, U>(root, comp1, comp2);
    }

    public U put(T1 k1, T2 k2, U d) {
        if (root == null) {
            root = new Node<T1, T2, U>(k1, k2, d, null, null);
            return null;
        }
        Node<T1, T2, U> old = root;
        root = root.add(k1, k2, d, this);

        // Performance hack; if it wasn't there, then the tree got bigger.
        if (old.weight < root.weight) return null;
        return old.getObject(k1, k2, this).data;
    }

    /**
     * Because the underlying tree is applicative, synchronization
     * is only necessary for additions to the tree.
     */
    public U syncPut(T1 k1, T2 k2, U d) {
        Node<T1, T2, U> old;
        synchronized (this) {
            if (root == null) {
                root = new Node<T1, T2, U>(k1, k2, d, null, null);
                return null;
            }
            old = root;
            root = old.add(k1, k2, d, this);

            // Performance hack; if it wasn't there, then the tree got bigger.
            if (old.weight < root.weight) return null;
        }
        return old.getObject(k1, k2, this).data;
    }

    public void clear() {
        root = null;
    }

    public int hashCode() {
        return super.hashCode();
    }

    @SuppressWarnings ("unchecked")
    public boolean equals(Object o) {
        if (o instanceof BA2Tree) {
            BA2Tree bat = (BA2Tree) o;
            if (bat.size() != size()) {
                return false;
            }
            Node a = root;
            Node b = bat.root;
            for (int i = 0; i < size(); i++) {
                if (!(a.get(i).equals(b.get(i)))) return false;
            }
            return true;
        }
        return super.equals(o);
    }


}
