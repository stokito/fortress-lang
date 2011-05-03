/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;


public class BATreeNodeEC<LookupKey, TrueKey, Value> {

    public String toString() {
        return toStringBuffer(new StringBuffer()).toString();
    }

    public StringBuffer toStringBuffer(StringBuffer b) {
        b.append(key);
        b.append("=");
        b.append(data);
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

    public void ok(EquivalenceClass<LookupKey, TrueKey> c) {
        int lw = 0;
        int rw = 0;
        if (left != null) {
            if (c.compareRightKeys(left.key, key) >= 0) throw new Error("Left key too big");
            left.ok(c);
            if (c.compareRightKeys(left.max().key, key) >= 0) throw new Error("Left max key too big");
            lw = left.weight;
        }
        if (right != null) {
            if (c.compareRightKeys(right.key, key) <= 0) throw new Error("Right key too small");
            right.ok(c);
            if (c.compareRightKeys(right.min().key, key) <= 0) throw new Error("Right min key too small");
            rw = right.weight;
        }
        if (weight != 1 + lw + rw) throw new Error("Weight wrong");
        if (lw >> 1 > rw) throw new Error("Left too heavy");
        if (rw >> 1 > lw) throw new Error("Right too heavy");

    }

    TrueKey key;

    Value data;

    int weight;

    BATreeNodeEC<LookupKey, TrueKey, Value> left;

    BATreeNodeEC<LookupKey, TrueKey, Value> right;

    int leftWeight() {
        return weight(left);
    }

    int rightWeight() {
        return weight(right);
    }

    public static <L, T, V> int weight(BATreeNodeEC<L, T, V> n) {
        return n == null ? 0 : n.weight;
    }

    BATreeNodeEC(TrueKey k,
                 Value d,
                 BATreeNodeEC<LookupKey, TrueKey, Value> l,
                 BATreeNodeEC<LookupKey, TrueKey, Value> r) {
        key = k;
        data = d;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    public BATreeNodeEC(TrueKey k, Value d) {
        key = k;
        data = d;
        weight = 1;
    }

    BATreeNodeEC(BATreeNodeEC<LookupKey, TrueKey, Value> n,
                 BATreeNodeEC<LookupKey, TrueKey, Value> l,
                 BATreeNodeEC<LookupKey, TrueKey, Value> r) {
        key = n.key;
        data = n.data;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    // Index 0 is leftmost.
    public BATreeNodeEC<LookupKey, TrueKey, Value> get(int at) {
        BATreeNodeEC<LookupKey, TrueKey, Value> l = left;
        BATreeNodeEC<LookupKey, TrueKey, Value> r = right;
        int lw = weight(l);
        if (at < lw) {
            return l.get(at);
        } else if (at > lw) {
            return r.get(at - lw - 1);
        } else return this;
    }

    public BATreeNodeEC<LookupKey, TrueKey, Value> getObject(LookupKey k, EquivalenceClass<LookupKey, TrueKey> comp) {
        BATreeNodeEC<LookupKey, TrueKey, Value> t = this;
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

    int indexOf(LookupKey k, EquivalenceClass<LookupKey, TrueKey> comp) {
        int toTheLeft = 0;

        BATreeNodeEC<LookupKey, TrueKey, Value> t = this;
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

    BATreeNodeEC<LookupKey, TrueKey, Value> min() {
        BATreeNodeEC<LookupKey, TrueKey, Value> t = this;
        while (t.left != null) {
            t = t.left;
        }
        return t;
    }

    BATreeNodeEC<LookupKey, TrueKey, Value> max() {
        BATreeNodeEC<LookupKey, TrueKey, Value> t = this;
        while (t.right != null) {
            t = t.right;
        }
        return t;
    }

    public BATreeNodeEC<LookupKey, TrueKey, Value> add(LookupKey k,
                                                       Value d,
                                                       EquivalenceClass<LookupKey, TrueKey> comp) {
        int c = comp.compare(k, key);
        BATreeNodeEC<LookupKey, TrueKey, Value> l = left;
        BATreeNodeEC<LookupKey, TrueKey, Value> r = right;
        if (c < 0) {
            // left
            if (l == null) {
                l = new BATreeNodeEC<LookupKey, TrueKey, Value>(comp.translate(k), d);

            } else {
                l = l.add(k, d, comp);
                // Worst-case balance: 2^(n+1)-1 vs 2^(n-1)
                if (weight(l) >> 1 > weight(r)) {
                    // Must rotate.
                    if (l.leftWeight() >= l.rightWeight()) {
                        // L to root
                        return assembleLeft(l.left, l, l.right, this, r);
                    } else {
                        // LR to root
                        BATreeNodeEC<LookupKey, TrueKey, Value> lr = l.right;
                        return assemble(l.left, l, lr.left, lr, lr.right, this, r);
                    }
                }
            }
            return new BATreeNodeEC<LookupKey, TrueKey, Value>(this, l, r);
        } else if (c > 0) {
            // right
            if (r == null) {
                r = new BATreeNodeEC<LookupKey, TrueKey, Value>(comp.translate(k), d);
            } else {
                r = r.add(k, d, comp);
                // Worst-case balance: 2^(n-1) vs 2^(n+1)-1
                if (weight(r) >> 1 > weight(l)) {
                    // Must rotate.
                    if (r.rightWeight() >= r.leftWeight()) {
                        // R to root
                        return assembleRight(l, this, r.left, r, r.right);

                    } else {
                        // RL to root
                        BATreeNodeEC<LookupKey, TrueKey, Value> rl = r.left;
                        return assemble(l, this, rl.left, rl, rl.right, r, r.right);
                    }
                }
            }
            return new BATreeNodeEC<LookupKey, TrueKey, Value>(this, l, r);
        } else {
            // Update value.
            return new BATreeNodeEC<LookupKey, TrueKey, Value>(comp.translate(k), d, l, r);
        }
    }

    private BATreeNodeEC<LookupKey, TrueKey, Value> assembleLeft(BATreeNodeEC<LookupKey, TrueKey, Value> ll,
                                                                 BATreeNodeEC<LookupKey, TrueKey, Value> l,
                                                                 BATreeNodeEC<LookupKey, TrueKey, Value> lr,
                                                                 BATreeNodeEC<LookupKey, TrueKey, Value> old,
                                                                 BATreeNodeEC<LookupKey, TrueKey, Value> r) {
        return new BATreeNodeEC<LookupKey, TrueKey, Value>(l, ll, new BATreeNodeEC<LookupKey, TrueKey, Value>(old,
                                                                                                              lr,
                                                                                                              r));
    }

    private BATreeNodeEC<LookupKey, TrueKey, Value> assembleRight(BATreeNodeEC<LookupKey, TrueKey, Value> l,
                                                                  BATreeNodeEC<LookupKey, TrueKey, Value> old,
                                                                  BATreeNodeEC<LookupKey, TrueKey, Value> rl,
                                                                  BATreeNodeEC<LookupKey, TrueKey, Value> r,
                                                                  BATreeNodeEC<LookupKey, TrueKey, Value> rr) {
        return new BATreeNodeEC<LookupKey, TrueKey, Value>(r,
                                                           new BATreeNodeEC<LookupKey, TrueKey, Value>(old, l, rl),
                                                           rr);
    }

    private BATreeNodeEC<LookupKey, TrueKey, Value> assemble(BATreeNodeEC<LookupKey, TrueKey, Value> ll,
                                                             BATreeNodeEC<LookupKey, TrueKey, Value> l,
                                                             BATreeNodeEC<LookupKey, TrueKey, Value> lr,
                                                             BATreeNodeEC<LookupKey, TrueKey, Value> top,
                                                             BATreeNodeEC<LookupKey, TrueKey, Value> rl,
                                                             BATreeNodeEC<LookupKey, TrueKey, Value> r,
                                                             BATreeNodeEC<LookupKey, TrueKey, Value> rr) {
        return new BATreeNodeEC<LookupKey, TrueKey, Value>(top,
                                                           new BATreeNodeEC<LookupKey, TrueKey, Value>(l, ll, lr),
                                                           new BATreeNodeEC<LookupKey, TrueKey, Value>(r, rl, rr));
    }

    public BATreeNodeEC<LookupKey, TrueKey, Value> getLeft() {
        return left;
    }

    public BATreeNodeEC<LookupKey, TrueKey, Value> getRight() {
        return right;
    }

    public TrueKey getKey() {
        return key;
    }

    public Value getValue() {
        return data;
    }

    public Value setValue(Value arg0) {
        throw new Error("Applicative data structures cannot be changed!");
    }

    public int getWeight() {
        return weight;
    }

}
