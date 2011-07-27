/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;
import java.util.Map;

public final class BATreeNode<T, U> implements Map.Entry<T, U>, java.io.Serializable {

    public String toString() {
        return recursiveToStringLines();
    }

    public StringBuffer toStringBuffer(StringBuffer b) {
        b.append(key);
        b.append("=");
        b.append(data);
        return b;
    }

    public String recursiveToString() {
        return recursiveToStringBuffer(new StringBuffer(), false, " ").toString();
    }

    public String recursiveToStringLines() {
        return recursiveToStringBuffer(new StringBuffer(), false, "\n").toString();
    }

    public StringBuffer recursiveToStringBuffer(StringBuffer b, boolean withParens, String sep) {
        if (left != null || right != null) {
            if (withParens) b.append("(");
            if (left != null) left.recursiveToStringBuffer(b, withParens, sep).append(sep);
            toStringBuffer(b);
            if (right != null) right.recursiveToStringBuffer(b.append(sep), withParens, sep);

            if (withParens) b.append(")");
        } else {
            toStringBuffer(b);
        }
        return b;
    }

    public void visit(Visitor2<? super T, ? super U> v) {
        if (left != null) left.visit(v);
        v.visit(key, data);
        if (right != null) right.visit(v);
    }

    public void ok(Comparator<? super T> c) {
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

    final T key;
    final U data;
    final int weight;
    final BATreeNode<T, U> left;
    final BATreeNode<T, U> right;

    int leftWeight() {
        return weight(left);
    }

    int rightWeight() {
        return weight(right);
    }

    public static <T, U> int weight(BATreeNode<T, U> n) {
        return n == null ? 0 : n.weight;
    }

    BATreeNode(T k, U d, BATreeNode<T, U> l, BATreeNode<T, U> r) {
        key = k;
        data = d;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    public BATreeNode(T k, U d) {
        key = k;
        data = d;
        left = null;
        right = null;
        weight = 1;
    }

    BATreeNode(BATreeNode<T, U> n, BATreeNode<T, U> l, BATreeNode<T, U> r) {
        key = n.key;
        data = n.data;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    // Index 0 is leftmost.
    public BATreeNode<T, U> get(int at) {
        BATreeNode<T, U> l = left;
        BATreeNode<T, U> r = right;
        int lw = weight(l);
        if (at < lw) {
            return l.get(at);
        } else if (at > lw) {
            return r.get(at - lw - 1);
        } else return this;
    }

    public BATreeNode<T, U> getObject(T k, Comparator<? super T> comp) {
        BATreeNode<T, U> t = this;
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

    int indexOf(T k, Comparator<? super T> comp) {
        int toTheLeft = 0;

        BATreeNode<T, U> t = this;
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


    BATreeNode<T, U> min() {
        BATreeNode<T, U> t = this;
        while (t.left != null) {
            t = t.left;
        }
        return t;
    }

    BATreeNode<T, U> max() {
        BATreeNode<T, U> t = this;
        while (t.right != null) {
            t = t.right;
        }
        return t;
    }

    public BATreeNode<T, U> add(T k, U d, Comparator<? super T> comp) {
        int c = comp.compare(k, key);
        BATreeNode<T, U> l = left;
        BATreeNode<T, U> r = right;
        if (c < 0) {
            // left
            if (l == null) {
                l = new BATreeNode<T, U>(k, d);
                return new BATreeNode<T, U>(this, l, r);
            } else {
                BATreeNode<T, U> nl = l.add(k, d, comp);
                return leftWeightIncreased(nl, r);
            }

        } else if (c > 0) {
            // right
            if (r == null) {
                r = new BATreeNode<T, U>(k, d);
                return new BATreeNode<T, U>(this, l, r);
            } else {
                BATreeNode<T, U> nr = r.add(k, d, comp);
                return rightWeightIncreased(l, nr);
            }

        } else {
            // Update value.
            return new BATreeNode<T, U>(k, d, l, r);
        }
    }

    // Balance test, factored out so we can be very very clever
    // about null checking and thus avoid making Java think hard
    // about the fact that weight >= 0 is an invariant.
    // Only did this because [l,r]WeightIncreased show up in profiles.
    private boolean outOfBalance(BATreeNode<T, U> heavier, BATreeNode<T, U> lighter) {
        if (heavier == null) return false;
        return (heavier.weight >> 2 > weight(lighter));
    }

    // Heaviness test, factored out so we can be very very clever
    // about null checking and thus avoid making Java think hard
    // about the fact that weight >= 0 is an invariant.
    // Only did this because [l,r]WeightIncreased show up in profiles.
    private boolean atLeastAsHeavy(BATreeNode<T, U> a, BATreeNode<T, U> b) {
        if (b == null) return true;
        if (a == null) return false;
        return (a.weight >= b.weight);
    }

    private BATreeNode<T, U> leftWeightIncreased(BATreeNode<T, U> l, BATreeNode<T, U> r) {
        if (outOfBalance(l, r)) {
            // Must rotate.
            BATreeNode<T, U> lr = l.right;
            if (atLeastAsHeavy(l.left, lr)) {
                // L to root
                return assembleLeft(l.left, l, l.right, this, r);
            } else {
                // LR to root
                return assemble(l.left, l, lr.left, lr, lr.right, this, r);
            }
        }
        return new BATreeNode<T, U>(this, l, r);
    }

    private BATreeNode<T, U> rightWeightIncreased(BATreeNode<T, U> l, BATreeNode<T, U> r) {
        if (outOfBalance(r, l)) {
            // Must rotate.
            BATreeNode<T, U> rl = r.left;
            if (atLeastAsHeavy(r.right, rl)) {
                // R to root
                return assembleRight(l, this, r.left, r, r.right);

            } else {
                // RL to root
                return assemble(l, this, rl.left, rl, rl.right, r, r.right);
            }
        }
        return new BATreeNode<T, U>(this, l, r);
    }

    BATreeNode<T, U> deleteMin(BATreeNode<T, U>[] deleted) {
        BATreeNode<T, U> l = left;
        if (l == null) {
            deleted[0] = this;
            return right;
        } else {
            l = l.deleteMin(deleted);
            BATreeNode<T, U> r = right;
            return rightWeightIncreased(l, r);
        }
    }

    BATreeNode<T, U> deleteMax(BATreeNode<T, U>[] deleted) {
        BATreeNode<T, U> r = right;
        if (r == null) {
            deleted[0] = this;
            return left;
        } else {
            r = r.deleteMax(deleted);
            BATreeNode<T, U> l = left;
            return leftWeightIncreased(l, r);
        }
    }

    public BATreeNode<T, U> delete(T k, Comparator<? super T> comp) {
        int c = comp.compare(k, key);
        BATreeNode<T, U> l = left;
        BATreeNode<T, U> r = right;
        if (c == 0) {
            int lw = weight(left);
            int rw = weight(right);
            BATreeNode<T, U>[] newthis = new BATreeNode[1];
            if (lw > rw) {
                l = l.deleteMax(newthis);
            } else {
                if (rw > 0) r = r.deleteMin(newthis);
                else return null;
            }
            return new BATreeNode<T, U>(newthis[0], l, r);
        } else if (c < 0) {
            if (l == null) return this;
            BATreeNode<T, U> nl = l.delete(k, comp);
            if (nl == l) return this;
            return rightWeightIncreased(nl, r);
        } else {
            if (r == null) return this;
            BATreeNode<T, U> nr = r.delete(k, comp);
            if (nr == r) return this;
            return leftWeightIncreased(l, nr);
        }
    }


    private BATreeNode<T, U> assembleLeft(BATreeNode<T, U> ll,
                                          BATreeNode<T, U> l,
                                          BATreeNode<T, U> lr,
                                          BATreeNode<T, U> old,
                                          BATreeNode<T, U> r) {
        return new BATreeNode<T, U>(l, ll, new BATreeNode<T, U>(old, lr, r));
    }

    private BATreeNode<T, U> assembleRight(BATreeNode<T, U> l,
                                           BATreeNode<T, U> old,
                                           BATreeNode<T, U> rl,
                                           BATreeNode<T, U> r,
                                           BATreeNode<T, U> rr) {
        return new BATreeNode<T, U>(r, new BATreeNode<T, U>(old, l, rl), rr);
    }

    private BATreeNode<T, U> assemble(BATreeNode<T, U> ll,
                                      BATreeNode<T, U> l,
                                      BATreeNode<T, U> lr,
                                      BATreeNode<T, U> top,
                                      BATreeNode<T, U> rl,
                                      BATreeNode<T, U> r,
                                      BATreeNode<T, U> rr) {
        return new BATreeNode<T, U>(top, new BATreeNode<T, U>(l, ll, lr), new BATreeNode<T, U>(r, rl, rr));
    }

    public BATreeNode<T, U> getLeft() {
        return left;
    }

    public BATreeNode<T, U> getRight() {
        return right;
    }

    public T getKey() {
        return key;
    }

    public U getValue() {
        return data;
    }

    public U setValue(U arg0) {
        throw new Error("Applicative data structures cannot be changed!");
    }

    public int getWeight() {
        return weight;
    }

}
