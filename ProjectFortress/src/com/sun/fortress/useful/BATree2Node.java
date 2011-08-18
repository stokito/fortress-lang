/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;
import java.util.Map;

public final class BATree2Node<T, U, V> implements Map.Entry<T, Pair<U, V>> {

    public String toString() {
        return toStringBuffer(new StringBuffer()).toString();
    }

    public StringBuffer toStringBuffer(StringBuffer b) {
        b.append(key);
        b.append("=(");
        b.append(data1);
        b.append(",");
        b.append(data2);
        b.append(")");
        return b;
    }

    public String recursiveToString() {
        return recursiveToStringBuffer(new StringBuffer(), false).toString();
    }

    public StringBuffer recursiveToStringBuffer(StringBuffer b, boolean withParens) {
        if (left != null || right != null) {
            if (withParens) b.append("(");
            if (left != null) left.recursiveToStringBuffer(b, withParens).append(" ");
            toStringBuffer(b);
            if (right != null) right.recursiveToStringBuffer(b.append(" "), withParens);

            if (withParens) b.append(")");
        } else {
            toStringBuffer(b);
        }
        return b;
    }

    public void ok(Comparator<T> c) {
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
        if (lw >> 1 > rw) throw new Error("Left too heavy");
        if (rw >> 1 > lw) throw new Error("Right too heavy");

    }

    T key;

    U data1;

    V data2;

    int weight;

    BATree2Node<T, U, V> left;

    BATree2Node<T, U, V> right;

    int leftWeight() {
        return weight(left);
    }

    int rightWeight() {
        return weight(right);
    }

    public static <T, U, V> int weight(BATree2Node<T, U, V> n) {
        return n == null ? 0 : n.weight;
    }

    BATree2Node(T k, U d1, V d2, BATree2Node<T, U, V> l, BATree2Node<T, U, V> r) {
        key = k;
        data1 = d1;
        data2 = d2;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    public BATree2Node(T k, U d1, V d2) {
        key = k;
        data1 = d1;
        data2 = d2;
        weight = 1;
    }

    BATree2Node(BATree2Node<T, U, V> n, BATree2Node<T, U, V> l, BATree2Node<T, U, V> r) {
        key = n.key;
        data1 = n.data1;
        data2 = n.data2;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    // Index 0 is leftmost.
    public BATree2Node<T, U, V> get(int at) {
        BATree2Node<T, U, V> l = left;
        BATree2Node<T, U, V> r = right;
        int lw = weight(l);
        if (at < lw) {
            return l.get(at);
        } else if (at > lw) {
            return r.get(at - lw - 1);
        } else return this;
    }

    public BATree2Node<T, U, V> getObject(T k, Comparator<T> comp) {
        BATree2Node<T, U, V> t = this;
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

        BATree2Node<T, U, V> t = this;
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

    BATree2Node<T, U, V> min() {
        BATree2Node<T, U, V> t = this;
        while (t.left != null) {
            t = t.left;
        }
        return t;
    }

    BATree2Node<T, U, V> max() {
        BATree2Node<T, U, V> t = this;
        while (t.right != null) {
            t = t.right;
        }
        return t;
    }

    public BATree2Node<T, U, V> add(T k, Pair<U, V> d, Comparator<T> comp) {
        return add(k, d.getA(), d.getB(), comp);
    }

    public BATree2Node<T, U, V> add(T k, U d1, V d2, Comparator<T> comp) {
        int c = comp.compare(k, key);
        BATree2Node<T, U, V> l = left;
        BATree2Node<T, U, V> r = right;
        if (c < 0) {
            // left
            if (l == null) {
                l = new BATree2Node<T, U, V>(k, d1, d2);

            } else {
                l = l.add(k, d1, d2, comp);
                // Worst-case balance: 2^(n+1)-1 vs 2^(n-1)
                if (weight(l) >> 1 > weight(r)) {
                    // Must rotate.
                    if (l.leftWeight() >= l.rightWeight()) {
                        // L to root
                        return assembleLeft(l.left, l, l.right, this, r);
                    } else {
                        // LR to root
                        BATree2Node<T, U, V> lr = l.right;
                        return assemble(l.left, l, lr.left, lr, lr.right, this, r);
                    }
                }
            }
            return new BATree2Node<T, U, V>(this, l, r);
        } else if (c > 0) {
            // right
            if (r == null) {
                r = new BATree2Node<T, U, V>(k, d1, d2);
            } else {
                r = r.add(k, d1, d2, comp);
                // Worst-case balance: 2^(n-1) vs 2^(n+1)-1
                if (weight(r) >> 1 > weight(l)) {
                    // Must rotate.
                    if (r.rightWeight() >= r.leftWeight()) {
                        // R to root
                        return assembleRight(l, this, r.left, r, r.right);

                    } else {
                        // RL to root
                        BATree2Node<T, U, V> rl = r.left;
                        return assemble(l, this, rl.left, rl, rl.right, r, r.right);
                    }
                }
            }
            return new BATree2Node<T, U, V>(this, l, r);
        } else {
            // Update value.
            return new BATree2Node<T, U, V>(k, d1, d2, l, r);
        }
    }

    private BATree2Node<T, U, V> assembleLeft(BATree2Node<T, U, V> ll,
                                              BATree2Node<T, U, V> l,
                                              BATree2Node<T, U, V> lr,
                                              BATree2Node<T, U, V> old,
                                              BATree2Node<T, U, V> r) {
        return new BATree2Node<T, U, V>(l, ll, new BATree2Node<T, U, V>(old, lr, r));
    }

    private BATree2Node<T, U, V> assembleRight(BATree2Node<T, U, V> l,
                                               BATree2Node<T, U, V> old,
                                               BATree2Node<T, U, V> rl,
                                               BATree2Node<T, U, V> r,
                                               BATree2Node<T, U, V> rr) {
        return new BATree2Node<T, U, V>(r, new BATree2Node<T, U, V>(old, l, rl), rr);
    }

    private BATree2Node<T, U, V> assemble(BATree2Node<T, U, V> ll,
                                          BATree2Node<T, U, V> l,
                                          BATree2Node<T, U, V> lr,
                                          BATree2Node<T, U, V> top,
                                          BATree2Node<T, U, V> rl,
                                          BATree2Node<T, U, V> r,
                                          BATree2Node<T, U, V> rr) {
        return new BATree2Node<T, U, V>(top, new BATree2Node<T, U, V>(l, ll, lr), new BATree2Node<T, U, V>(r, rl, rr));
    }

    public BATree2Node<T, U, V> getLeft() {
        return left;
    }

    public BATree2Node<T, U, V> getRight() {
        return right;
    }

    public T getKey() {
        return key;
    }

    public Pair<U, V> getValue() {
        return asPair();
    }

    public Pair<U, V> setValue(Pair<U, V> arg0) {
        throw new Error("Applicative data structures cannot be changed!");
    }

    public int getWeight() {
        return weight;
    }

    public Pair<U, V> asPair() {
        return new Pair<U, V>(data1, data2);
    }

}
