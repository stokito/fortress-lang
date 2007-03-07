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

package com.sun.fortress.interpreter.useful;

import java.util.Comparator;
import java.util.Map;

public final class BATreeNode<T, U> implements Map.Entry<T, U> {

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
                if (withParens)
                    b.append("(");
                if (left != null)
                    left.recursiveToStringBuffer(b, withParens).append(" ");
                toStringBuffer(b);
                if (right != null)
                    right.recursiveToStringBuffer(b.append(" "), withParens);

                if (withParens)
                    b.append(")");
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

        public void ok(Comparator<T> c) {
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
        U data;
        int weight;
        BATreeNode<T,U> left;
        BATreeNode<T,U> right;
        int leftWeight() {
            return weight(left);
        }
        int rightWeight() {
            return weight(right);
        }

        public static int weight(BATreeNode n) {
            return n == null ? 0 : n.weight;
        }

        BATreeNode(T k, U d, BATreeNode<T,U> l, BATreeNode<T,U> r) {
            key = k;
            data = d;
            left = l;
            right = r;
            weight = leftWeight() + rightWeight() + 1;
        }

        public BATreeNode(T k, U d) {
            key = k;
            data = d;
            weight = 1;
        }

        BATreeNode(BATreeNode<T,U> n, BATreeNode<T,U> l, BATreeNode<T,U> r) {
            key = n.key;
            data = n.data;
            left = l;
            right = r;
            weight = leftWeight() + rightWeight() + 1;
        }

        // Index 0 is leftmost.
        public BATreeNode<T, U> get(int at) {
            BATreeNode<T,U> l = left;
            BATreeNode<T,U> r = right;
            int lw = weight(l);
            if (at < lw) {
                return l.get(at);
            } else if (at > lw) {
                return r.get(at - lw - 1);
            } else return this;
        }

        public BATreeNode<T, U> getObject(T k, Comparator<T> comp) {
            BATreeNode<T, U> t = this;
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

            BATreeNode<T, U> t = this;
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



        BATreeNode<T, U> min() {
            BATreeNode<T, U> t = this;
            while (t.left != null) t = t.left;
            return t;
        }

        BATreeNode<T, U> max() {
            BATreeNode<T, U> t = this;
            while (t.right != null) t = t.right;
            return t;
        }

        public BATreeNode<T,U> add(T k, U d, Comparator<T> comp) {
            int c = comp.compare(k,key);
            BATreeNode<T,U> l = left;
            BATreeNode<T,U> r = right;
            if (c < 0) {
                // left
                if (l == null) {
                    l = new BATreeNode<T,U>(k,d);

                } else {
                    l = l.add(k,d, comp);
                    // Worst-case balance: 2^(n+1)-1 vs 2^(n-1)
                    if (weight(l) >> 1 > weight(r)) {
                        // Must rotate.
                        if (l.leftWeight() >= l.rightWeight()) {
                            // L to root
                            return assembleLeft(l.left, l, l.right, this, r);
                        } else {
                            // LR to root
                            BATreeNode<T,U> lr = l.right;
                            return assemble(l.left, l, lr.left, lr, lr.right, this, r);
                        }
                    }
                }
                return new BATreeNode<T,U>(this,l,r);
            } else if (c > 0) {
                // right
                if (r == null) {
                    r = new BATreeNode<T,U>(k,d);
                } else {
                    r = r.add(k,d, comp);
//                  Worst-case balance: 2^(n-1) vs 2^(n+1)-1
                    if (weight(r) >> 1 > weight(l)) {
                        // Must rotate.
                        if (r.rightWeight() >= r.leftWeight()) {
                            // R to root
                            return assembleRight(l, this, r.left, r, r.right);

                        } else {
                            // RL to root
                            BATreeNode<T,U> rl = r.left;
                            return assemble(l, this, rl.left, rl, rl.right, r, r.right);
                        }
                    }
                }
                return new BATreeNode<T,U>(this,l,r);
            } else {
                // Update value.
                return new BATreeNode<T,U>(k,d,l,r);
            }
        }
        private BATreeNode<T,U> assembleLeft(BATreeNode<T, U> ll, BATreeNode<T, U> l, BATreeNode<T, U> lr, BATreeNode<T, U> old, BATreeNode<T, U> r) {
            return new BATreeNode<T,U>(l,
                    ll,
                    new BATreeNode<T,U>(old, lr, r));
        }
        private BATreeNode<T,U> assembleRight(BATreeNode<T, U> l, BATreeNode<T, U> old, BATreeNode<T, U> rl, BATreeNode<T, U> r, BATreeNode<T, U> rr) {
                    return new BATreeNode<T,U>(r,
                    new BATreeNode<T,U>(old, l, rl),
                    rr);
        }
        private BATreeNode<T,U> assemble(BATreeNode<T, U> ll, BATreeNode<T, U> l, BATreeNode<T, U> lr,
                              BATreeNode<T, U> top,
                              BATreeNode<T, U> rl, BATreeNode<T, U> r, BATreeNode<T, U> rr) {
            return new BATreeNode<T,U>(top,
                    new BATreeNode<T,U>(l, ll, lr),
                    new BATreeNode<T,U>(r, rl, rr));
        }

        public BATreeNode<T,U> getLeft() {
            return left;
        }

        public BATreeNode<T,U> getRight() {
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
