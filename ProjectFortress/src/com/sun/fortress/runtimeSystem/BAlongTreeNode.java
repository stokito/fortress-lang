/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.runtimeSystem;

public final class BAlongTreeNode {

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

    public void ok() {
        int lw = 0;
        int rw = 0;
        if (left != null) {
            if (left.key >= key ) throw new Error("Left key too big");
            left.ok();
            if (left.max().key >= key) throw new Error("Left max key too big");
            lw = left.weight;
        }
        if (right != null) {
            if (right.key <= key) throw new Error("Right key too small");
            right.ok();
            if (right.min().key <= key) throw new Error("Right min key too small");
            rw = right.weight;
        }
        if (weight != 1 + lw + rw) throw new Error("Weight wrong");
        if (lw >> 2 > rw) throw new Error("Left too heavy");
        if (rw >> 2 > lw) throw new Error("Right too heavy");

    }

    final long key;
    final Object data;
    final int weight;
    final BAlongTreeNode left;
    final BAlongTreeNode right;

    int leftWeight() {
        return weight(left);
    }

    int rightWeight() {
        return weight(right);
    }

    public static  int weight(BAlongTreeNode n) {
        return n == null ? 0 : n.weight;
    }

    BAlongTreeNode(long k, Object d, BAlongTreeNode l, BAlongTreeNode r) {
        key = k;
        data = d;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    public BAlongTreeNode(long k, Object d) {
        key = k;
        data = d;
        left = null;
        right = null;
        weight = 1;
    }

    BAlongTreeNode(BAlongTreeNode n, BAlongTreeNode l, BAlongTreeNode r) {
        key = n.key;
        data = n.data;
        left = l;
        right = r;
        weight = leftWeight() + rightWeight() + 1;
    }

    // Index 0 is leftmost.
    public BAlongTreeNode get(int at) {
        BAlongTreeNode l = left;
        BAlongTreeNode r = right;
        int lw = weight(l);
        if (at < lw) {
            return l.get(at);
        } else if (at > lw) {
            return r.get(at - lw - 1);
        } else return this;
    }

    public BAlongTreeNode getObject(long k) {
        BAlongTreeNode t = this;
        while (t != null) {
            if (k == t.key) break;
            if (k < t.key) {
                t = t.left;
            } else {
                t = t.right;
            }
        }
        return t;
    }

    int indexOf(long k) {
        int toTheLeft = 0;

        BAlongTreeNode t = this;
        while (t != null) {
            if (k == t.key) return toTheLeft + weight(t.left);
            if (k < t.key) {
                t = t.left;
            } else {
                toTheLeft += 1 + weight(t.left);
                t = t.right;
            }
        }
        return ~toTheLeft;
    }


    BAlongTreeNode min() {
        BAlongTreeNode t = this;
        while (t.left != null) {
            t = t.left;
        }
        return t;
    }

    BAlongTreeNode max() {
        BAlongTreeNode t = this;
        while (t.right != null) {
            t = t.right;
        }
        return t;
    }

    public BAlongTreeNode add(long k, Object d) {
        BAlongTreeNode l = left;
        BAlongTreeNode r = right;
        if (k < key) {
            // left
            if (l == null) {
                l = new BAlongTreeNode(k, d);
                return new BAlongTreeNode(this, l, r);
            } else {
                BAlongTreeNode nl = l.add(k, d);
                return leftWeightIncreased(nl, r);
            }

        } else if (k > key) {
            // right
            if (r == null) {
                r = new BAlongTreeNode(k, d);
                return new BAlongTreeNode(this, l, r);
            } else {
                BAlongTreeNode nr = r.add(k, d);
                return rightWeightIncreased(l, nr);
            }

        } else {
            // Update value.
            return new BAlongTreeNode(k, d, l, r);
        }
    }

    // Balance test, factored out so we can be very very clever
    // about null checking and thus avoid making Java think hard
    // about the fact that weight >= 0 is an invariant.
    // Only did this because [l,r]WeightIncreased show up in profiles.
    private boolean outOfBalance(BAlongTreeNode heavier, BAlongTreeNode lighter) {
        if (heavier == null) return false;
        return (heavier.weight >> 2 > weight(lighter));
    }

    // Heaviness test, factored out so we can be very very clever
    // about null checking and thus avoid making Java think hard
    // about the fact that weight >= 0 is an invariant.
    // Only did this because [l,r]WeightIncreased show up in profiles.
    private boolean atLeastAsHeavy(BAlongTreeNode a, BAlongTreeNode b) {
        if (b == null) return true;
        if (a == null) return false;
        return (a.weight >= b.weight);
    }

    private BAlongTreeNode leftWeightIncreased(BAlongTreeNode l, BAlongTreeNode r) {
        if (outOfBalance(l, r)) {
            // Must rotate.
            BAlongTreeNode lr = l.right;
            if (atLeastAsHeavy(l.left, lr)) {
                // L to root
                return assembleLeft(l.left, l, l.right, this, r);
            } else {
                // LR to root
                return assemble(l.left, l, lr.left, lr, lr.right, this, r);
            }
        }
        return new BAlongTreeNode(this, l, r);
    }

    private BAlongTreeNode rightWeightIncreased(BAlongTreeNode l, BAlongTreeNode r) {
        if (outOfBalance(r, l)) {
            // Must rotate.
            BAlongTreeNode rl = r.left;
            if (atLeastAsHeavy(r.right, rl)) {
                // R to root
                return assembleRight(l, this, r.left, r, r.right);

            } else {
                // RL to root
                return assemble(l, this, rl.left, rl, rl.right, r, r.right);
            }
        }
        return new BAlongTreeNode(this, l, r);
    }

    BAlongTreeNode deleteMin(BAlongTreeNode[] deleted) {
        BAlongTreeNode l = left;
        if (l == null) {
            deleted[0] = this;
            return right;
        } else {
            l = l.deleteMin(deleted);
            BAlongTreeNode r = right;
            return rightWeightIncreased(l, r);
        }
    }

    BAlongTreeNode deleteMax(BAlongTreeNode[] deleted) {
        BAlongTreeNode r = right;
        if (r == null) {
            deleted[0] = this;
            return left;
        } else {
            r = r.deleteMax(deleted);
            BAlongTreeNode l = left;
            return leftWeightIncreased(l, r);
        }
    }

    public BAlongTreeNode delete(long k) {
        BAlongTreeNode l = left;
        BAlongTreeNode r = right;
        if (k == key) {
            int lw = weight(left);
            int rw = weight(right);
            BAlongTreeNode[] newthis = new BAlongTreeNode[1];
            if (lw > rw) {
                l = l.deleteMax(newthis);
            } else {
                if (rw > 0) r = r.deleteMin(newthis);
                else return null;
            }
            return new BAlongTreeNode(newthis[0], l, r);
        } else if (k < key) {
            if (l == null) return this;
            BAlongTreeNode nl = l.delete(k);
            if (nl == l) return this;
            return rightWeightIncreased(nl, r);
        } else {
            if (r == null) return this;
            BAlongTreeNode nr = r.delete(k);
            if (nr == r) return this;
            return leftWeightIncreased(l, nr);
        }
    }


    private BAlongTreeNode assembleLeft(BAlongTreeNode ll,
                                          BAlongTreeNode l,
                                          BAlongTreeNode lr,
                                          BAlongTreeNode old,
                                          BAlongTreeNode r) {
        return new BAlongTreeNode(l, ll, new BAlongTreeNode(old, lr, r));
    }

    private BAlongTreeNode assembleRight(BAlongTreeNode l,
                                           BAlongTreeNode old,
                                           BAlongTreeNode rl,
                                           BAlongTreeNode r,
                                           BAlongTreeNode rr) {
        return new BAlongTreeNode(r, new BAlongTreeNode(old, l, rl), rr);
    }

    private BAlongTreeNode assemble(BAlongTreeNode ll,
                                      BAlongTreeNode l,
                                      BAlongTreeNode lr,
                                      BAlongTreeNode top,
                                      BAlongTreeNode rl,
                                      BAlongTreeNode r,
                                      BAlongTreeNode rr) {
        return new BAlongTreeNode(top, new BAlongTreeNode(l, ll, lr), new BAlongTreeNode(r, rl, rr));
    }

    public BAlongTreeNode getLeft() {
        return left;
    }

    public BAlongTreeNode getRight() {
        return right;
    }

    public long getKey() {
        return key;
    }

    public Object getValue() {
        return data;
    }

    public Object setValue(Object arg0) {
        throw new Error("Applicative data structures cannot be changed!");
    }

    public int getWeight() {
        return weight;
    }

}
