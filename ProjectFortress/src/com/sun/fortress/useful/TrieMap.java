/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.*;

/**
 * String maps using BATree-like ternary tries.  Basically, we have
 * a binary tree for each character level in the string.  We only attempt to
 * balance when one side weight is much larger than the other + middle.
 * <p/>
 * A lot of the theory and practice here have been spelled out by
 * Bentley & Sedgewick in "Fast Algorithms for Sorting and Searching
 * Strings": http://www.cs.princeton.edu/~rs/strings/
 * <p/>
 * Note that this map is sorted in the UTF-16 string sense; this is
 * not alphabetic sorting in most settings.
 */
public final class TrieMap<V> extends AbstractMap<String, V>
        implements Iterable<Map.Entry<String, V>>, java.io.Serializable, Cloneable {

    private static final int EMPTY_STRING_TAG = -1;

    private static final int BALANCE_MULTIPLIER = 2;

    Trie<V> trie;

    public TrieMap() {
        this.trie = null;
    }

    public TrieMap(String key, V value) {
        if (key == null) throw new NullPointerException("TrieMap doesn't support null keys");
        this.trie = new TrieLeaf<V>(key, value);
    }

    private TrieMap(TrieMap<V> m) {
        this.trie = m.trie;
    }

    public Object clone() {
        return this.copy();
    }

    public TrieMap<V> copy() {
        return new TrieMap<V>(this);
    }

    public String toString() {
        return debugString();
    }

    public String debugString() {
        if (trie == null) return "Empty TrieMap";
        StringBuilder res = new StringBuilder("TrieMap:\n");
        trie.debugString(res, 2);
        return res.toString();
    }

    public void check() {
        if (trie == null) return;
        if (!trie.check("", 0)) {
            throw new java.lang.Error("Bad trie: " + debugString());
        }
    }

    private static <U> int size(Trie<U> t) {
        if (t == null) return 0;
        if (t instanceof TrieNode) return ((TrieNode<U>) t).size;
        return 1;
    }

    private static int safeCharAt(String s, int i) {
        return (i >= s.length()) ? EMPTY_STRING_TAG : s.charAt(i);
    }

    public int size() {
        return size(trie);
    }

    public boolean isEmpty() {
        return trie == null;
    }

    public boolean containsKey(String k) {
        if (k == null) return false;
        Trie<V> t = trie;
        int i = 0;
        int ch = safeCharAt(k, i);
        while (t != null && t instanceof TrieNode) {
            TrieNode<V> n = (TrieNode<V>) t;
            if (ch == n.ch) {
                i++;
                ch = safeCharAt(k, i);
                t = n.equal;
            } else if (ch < n.ch) {
                t = n.less;
            } else {
                t = n.greater;
            }
        }
        if (t == null) return false;
        if (t instanceof TrieLeaf) return k.equals(((TrieLeaf<V>) t).key);
        throw new Error("TrieMap: non-Trie " + t);
    }

    public V get(String k) {
        if (k == null) return null;
        Trie<V> t = trie;
        if (t == null) return null;
        int i = 0;
        int ch = safeCharAt(k, i);
        while (t != null && t instanceof TrieNode) {
            TrieNode<V> n = (TrieNode<V>) t;
            if (ch == n.ch) {
                i++;
                ch = safeCharAt(k, i);
                t = n.equal;
            } else if (ch < n.ch) {
                t = n.less;
            } else {
                t = n.greater;
            }
        }
        if (t == null) return null;
        if (!(t instanceof TrieLeaf)) throw new Error("TrieMap: non-Trie " + t);
        TrieLeaf<V> l = (TrieLeaf<V>) t;
        if (k.equals(l.key)) return l.value;
        return null;
    }

    public V put(String k, V v) {
        if (k == null) throw new NullPointerException("TrieMap doesn't support null keys.");
        Trie<V> l = new TrieLeaf<V>(k, v);
        TriePath p = seek(k);
        V result = null;
        if (p.matchedLeaf != null) {
            int i = p.position();
            int ch = safeCharAt(k, i);
            int lch = safeCharAt(p.matchedLeaf.key, i);
            while (ch == lch && ch != EMPTY_STRING_TAG) {
                i++;
                ch = safeCharAt(k, i);
                lch = safeCharAt(p.matchedLeaf.key, i);
            }
            if (ch != lch) {
                if (ch < lch) {
                    l = new TrieNode<V>(lch, l, p.matchedLeaf, null);
                } else {
                    l = new TrieNode<V>(ch, p.matchedLeaf, l, null);
                }
                int posn = p.position();
                if (p.inEqPosition()) posn++;
                while (i > posn) {
                    i--;
                    ch = safeCharAt(k, i);
                    l = new TrieNode<V>(ch, null, l, null);
                }
            } else {
                result = p.matchedLeaf.value;
            }
        }
        trie = p.insertOrReplace(l);
        return result;
    }

    public V remove(Object o) {
        if (o == null || !(o instanceof String)) return null;
        String k = (String) o;
        TriePath p = seek(k);
        V result = null;
        if (p.matchedLeaf != null) {
            int i = p.position();
            int ch = safeCharAt(k, i);
            int lch = safeCharAt(p.matchedLeaf.key, i);
            while (ch == lch && ch != EMPTY_STRING_TAG) {
                i++;
                ch = safeCharAt(k, i);
                lch = safeCharAt(p.matchedLeaf.key, i);
            }
            if (ch == lch) {
                result = p.matchedLeaf.value;
                trie = p.delete();
            }
        }
        return result;
    }

    public void clear() {
        trie = null;
    }

    private final class EntrySet extends AbstractSet<Map.Entry<String, V>> {
        final TrieMap<V> m;

        EntrySet(TrieMap<V> m) {
            this.m = m;
        }

        public int size() {
            return m.size();
        }

        public boolean isEmpty() {
            return m.isEmpty();
        }

        public Iterator<Map.Entry<String, V>> iterator() {
            return m.iterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry e = (Map.Entry) o;
            if (!(e.getKey() instanceof String)) return false;
            V v = m.get((String) e.getKey());
            return v.equals(e.getValue());
        }
    }

    public Set<Map.Entry<String, V>> entrySet() {
        return new EntrySet(this);
    }

    private final class TriePath implements Iterator<Map.Entry<String, V>> {
        TrieNode<V>[] nodes;
        int[] positions;
        int nextAvail = 0;
        String key = null;
        TrieLeaf<V> matchedLeaf = null;

        TrieNode<V> trieNodeL(int ch, Trie<V> l, Trie<V> e, Trie<V> g) {
            if (l instanceof TrieNode) {
                TrieNode<V> n = (TrieNode<V>) l;
                if (e instanceof TrieLeaf && g == null && n.greater == null) {
                    return new TrieNode<V>(n.ch, n.less, n.equal, e);
                }
                int lwt = size(n.less) + size(n.equal);
                // int mwt = size(n.greater);
                int gwt = size(e) + size(g);
                if (lwt > 2 * gwt) {
                    return new TrieNode<V>(n.ch, n.less, n.equal, new TrieNode<V>(ch, n.greater, e, g));
                }
            }
            return new TrieNode<V>(ch, l, e, g);
        }

        TrieNode<V> trieNodeG(int ch, Trie<V> l, Trie<V> e, Trie<V> g) {
            if (g instanceof TrieNode) {
                TrieNode<V> n = (TrieNode<V>) g;
                if (e instanceof TrieLeaf && l == null && n.less == null) {
                    return new TrieNode<V>(n.ch, e, n.equal, n.greater);
                }
                int lwt = size(l) + size(e);
                // int mwt = size(n.greater);
                int gwt = size(n.equal) + size(n.greater);
                if (gwt > 2 * lwt) {
                    return new TrieNode<V>(n.ch, new TrieNode<V>(ch, l, e, n.less), n.equal, n.greater);
                }
            }
            return new TrieNode<V>(ch, l, e, g);
        }

        boolean inEqPosition() {
            if (nextAvail == 0) return false;
            TrieNode<V> n = nodes[nextAvail - 1];
            return (n.ch == safeCharAt(key, positions[nextAvail - 1]));
        }

        private TriePath(int n) {
            nodes = (TrieNode<V>[]) new TrieNode[n];
            positions = new int[n];
        }

        private TriePath(String k) {
            this(8 + 3 * k.length());
            key = k;
        }

        private TriePath(Trie<V> t) {
            this(8 + 3 * Integer.highestOneBit(size(t)));
        }

        int position() {
            if (nextAvail == 0) return 0;
            return positions[nextAvail - 1];
        }

        private void enlarge() {
            int newsz = 2 * nodes.length + 1;
            nodes = Arrays.copyOf(nodes, newsz);
            positions = Arrays.copyOf(positions, newsz);
        }

        private void push(TrieNode<V> n, int position) {
            // StringBuilder b = new StringBuilder();
            // indent(b,position*indentBy);
            // b.append("push '");
            // if (n.ch != EMPTY_STRING_TAG) b.append((char)n.ch);
            // b.append("' ["+n.size+"]");
            // System.err.println(b);
            if (nextAvail >= nodes.length) enlarge();
            nodes[nextAvail] = n;
            positions[nextAvail] = position;
            nextAvail++;
        }

        Trie<V> insertOrReplace(Trie<V> t) {
            while (nextAvail > 0) {
                nextAvail--;
                int i = positions[nextAvail];
                TrieNode<V> n = nodes[nextAvail];
                Trie<V> nless = n.less;
                Trie<V> nequal = n.equal;
                Trie<V> ngreater = n.greater;
                // int nch = n.ch;
                int ch = safeCharAt(key, i);
                if (ch == n.ch) {
                    t = new TrieNode<V>(n.ch, nless, t, ngreater);
                } else if (ch < n.ch) {
                    t = trieNodeL(n.ch, t, nequal, ngreater);
                } else {
                    t = trieNodeG(n.ch, nless, nequal, t);
                }
            }
            return t;
        }

        Trie<V> delete() {
            Trie<V> t = null;
            while (t == null) {
                if (--nextAvail < 0) return null;
                int i = positions[nextAvail];
                TrieNode<V> n = nodes[nextAvail];
                int ch = safeCharAt(key, i);
                if (ch != n.ch) {
                    if (ch < n.ch) {
                        t = trieNodeG(n.ch, null, n.equal, n.greater);
                    } else {
                        t = trieNodeL(n.ch, n.less, n.equal, null);
                    }
                    break;
                } else if (n.less == null) {
                    t = n.greater;
                } else if (n.greater == null) {
                    t = n.less;
                } else {
                    t = hoist(n, i);
                }
            }
            return insertOrReplace(t);
        }

        Trie<V> hoist(TrieNode<V> n, int i) {
            if (size(n.less) < size(n.greater)) {
                return hoistMax(n, i);
            } else {
                return hoistMin(n, i);
            }
        }

        Trie<V> hoistMax(TrieNode<V> n, int i) {
            Trie<V> rootGreater = n.greater;
            int rootCh;
            Trie<V> rootEqual;
            Trie<V> t = n.less;
            int mark = nextAvail;
            while (t != null && t instanceof TrieNode) {
                n = (TrieNode<V>) t;
                push(n, i);
                t = n.greater;
            }
            if (t == null) {
                nextAvail--;
                n = nodes[nextAvail];
                rootEqual = n.equal;
                rootCh = n.ch;
                t = n.less;
            } else if (t instanceof TrieLeaf) {
                TrieLeaf<V> l = (TrieLeaf<V>) t;
                rootEqual = l;
                rootCh = safeCharAt(l.key, i);
                t = null;
            } else {
                throw new Error("Non-Trie");
            }
            while (nextAvail > mark) {
                nextAvail--;
                n = nodes[nextAvail];
                t = new TrieNode<V>(n.ch, n.less, n.equal, t);
            }
            return new TrieNode<V>(rootCh, t, rootEqual, rootGreater);
        }


        Trie<V> hoistMin(TrieNode<V> n, int i) {
            Trie<V> rootLess = n.less;
            int rootCh;
            Trie<V> rootEqual;
            Trie<V> t = n.greater;
            int mark = nextAvail;
            while (t != null && t instanceof TrieNode) {
                n = (TrieNode<V>) t;
                push(n, i);
                t = n.less;
            }
            if (t == null) {
                nextAvail--;
                n = nodes[nextAvail];
                rootEqual = n.equal;
                rootCh = n.ch;
                t = n.greater;
            } else if (t instanceof TrieLeaf) {
                TrieLeaf<V> l = (TrieLeaf<V>) t;
                rootEqual = t;
                rootCh = safeCharAt(l.key, i);
                t = null;
            } else {
                throw new Error("Non-Trie");
            }
            while (nextAvail > mark) {
                nextAvail--;
                n = nodes[nextAvail];
                t = new TrieNode<V>(n.ch, t, n.equal, n.greater);
            }
            return new TrieNode<V>(rootCh, rootLess, rootEqual, t);
        }

        public boolean hasNext() {
            return !(matchedLeaf == null);
        }

        public TrieLeaf<V> next() {
            TrieLeaf<V> result = matchedLeaf;
            if (result == null) throw new NoSuchElementException();
            Trie<V> t = null;
            TrieNode<V> n;
            int i = 0;
            String k = result.key;
            while (nextAvail > 0) {
                nextAvail--;
                n = nodes[nextAvail];
                i = positions[nextAvail];
                int ch = safeCharAt(k, i);
                if (ch < n.ch) {
                    nextAvail++;
                    t = n.equal;
                    i++;
                    break;
                } else if (ch == n.ch && n.greater != null) {
                    nextAvail++;
                    t = n.greater;
                    break;
                }
            }
            if (t == null) {
                matchedLeaf = null;
                return result;
            }
            while (t instanceof TrieNode) {
                n = (TrieNode<V>) t;
                push(n, i);
                if (n.less != null) {
                    t = n.less;
                } else {
                    t = n.equal;
                    i++;
                }
            }
            if (t == null) throw new Error("WTF?");
            matchedLeaf = (TrieLeaf<V>) t;
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException("TrieMap iterator does not support remove");
        }

    }

    private TriePath seek(String k) {
        Trie<V> t = trie;
        TriePath p = new TriePath(k);
        int i = 0;
        int ch = safeCharAt(k, i);
        while (t != null && t instanceof TrieNode) {
            TrieNode<V> n = (TrieNode<V>) t;
            p.push(n, i);
            if (ch == n.ch) {
                i++;
                ch = safeCharAt(k, i);
                t = n.equal;
            } else if (ch < n.ch) {
                t = n.less;
            } else {
                t = n.greater;
            }
        }
        // if (t==null) return p;
        p.matchedLeaf = (TrieLeaf<V>) t;
        return p;
    }

    public Iterator<Map.Entry<String, V>> iterator() {
        Trie<V> t = trie;
        if (t == null) return new TriePath(0);
        TriePath p = new TriePath(t);
        int i = 0;
        while (t instanceof TrieNode) {
            TrieNode<V> n = (TrieNode<V>) t;
            p.push(n, i);
            if (n.less == null) {
                i++;
                t = n.equal;
            } else {
                t = n.less;
            }
        }
        p.matchedLeaf = (TrieLeaf<V>) t;
        return p;
    }

    static final String spaces = "                                                                ";

    static final int indentBy = 2;

    private static void indent(StringBuilder b, int i) {
        for (; i > spaces.length(); i -= spaces.length()) {
            b.append(spaces);
        }
        b.append(spaces.substring(spaces.length() - i));
    }

    private static abstract class Trie<V> implements java.io.Serializable {
        abstract void debugString(StringBuilder b, int i);

        abstract boolean check(String prefix, int i);

        public String toString() {
            StringBuilder b = new StringBuilder();
            debugString(b, 0);
            return b.toString();
        }
    }

    private final static class TrieNode<V> extends Trie<V> implements java.io.Serializable {
        final int size;
        final int ch;
        final Trie<V> less;
        final Trie<V> equal;
        final Trie<V> greater;

        TrieNode(int ch, Trie<V> less, Trie<V> equal, Trie<V> greater) {
            this.ch = ch;
            this.less = less;
            this.equal = equal;
            this.greater = greater;
            this.size = size(less) + size(equal) + size(greater);
        }

        private void debugCh(StringBuilder b, int i) {
            indent(b, i);
            if (ch == EMPTY_STRING_TAG) {
                b.append("\"\" ");
            } else {
                b.append('\'');
                b.append((char) ch);
                b.append("\' ");
            }
        }

        void debugString(StringBuilder b, int i) {
            if (less != null) {
                debugCh(b, i);
                b.append("<\n");
                less.debugString(b, i + indentBy);
            }
            debugCh(b, i);
            b.append("[");
            b.append(size);
            b.append("] =\n");
            if (equal == null) {
                b.append("equal IS NULL!!! BAD!!!\n");
            } else {
                equal.debugString(b, i + indentBy);
            }
            if (greater != null) {
                debugCh(b, i);
                b.append(">\n");
                greater.debugString(b, i + indentBy);
            }
        }

        boolean check(String prefix, int i) {
            boolean ok = true;
            if (equal == null) {
                System.err.print("Null equal: ");
                ok = false;
            }
            if (less != null && less instanceof TrieNode && ((TrieNode) less).ch >= ch) {
                System.err.print("Bad less leg: ");
                ok = false;
            }
            if (greater != null && greater instanceof TrieNode && ((TrieNode) greater).ch <= ch) {
                System.err.print("Bad greater leg: ");
                ok = false;
            }
            if (less != null && ch == EMPTY_STRING_TAG) {
                System.err.print("Empty tag, yet less leg: ");
                ok = false;
            }
            if (!ok) System.err.println(this);
            if (less != null) ok = less.check(prefix, i) && ok;
            if (equal != null) {
                String p = (ch == EMPTY_STRING_TAG) ? prefix : (prefix + ((char) ch));
                ok = equal.check(p, i + 1) && ok;
            }
            if (greater != null) ok = greater.check(prefix, i) && ok;
            return ok;
        }
    }

    private final static class TrieLeaf<V> extends Trie<V> implements Map.Entry<String, V>, java.io.Serializable {
        final String key;
        final V value;

        TrieLeaf(String key, V value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V v) {
            throw new UnsupportedOperationException("TrieMap entries are immutable");
        }

        public boolean equals(Object that) {
            if (!(that instanceof TrieLeaf)) {
                return false;
            }
            TrieLeaf<V> l = (TrieLeaf<V>) that;
            return key.equals(l.getKey()) && value.equals(l.getValue());
        }

        /**
         * This hash is specified in java.util.Map.Entry JavaDoc.
         * We assume non-null key.
         */
        public int hashCode() {
            int valueHash = (value == null) ? 0 : value.hashCode();
            int keyHash = key.hashCode();
            return keyHash ^ valueHash;
        }

        void debugString(StringBuilder b, int i) {
            indent(b, i);
            b.append('\"');
            b.append(key);
            b.append("\"=");
            b.append(value);
            b.append("\n");
        }

        boolean check(String prefix, int i) {
            boolean ok = true;
            if (!key.startsWith(prefix)) {
                System.err.println("Key \"" + key + "\" doesn't start with \"" + prefix + "\"");
                ok = false;
            }
            if (key.length() == i && !prefix.equals(key) || key.length() < i) {
                System.err.print("Something is funny wrt check(\"" + prefix + "\"," + i + ") on " + this);
                ok = false;
            }
            return ok;
        }
    }
}
