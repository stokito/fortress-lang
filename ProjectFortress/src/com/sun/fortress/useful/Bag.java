/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Bag<T> extends AbstractSet<T> implements Set<T> {

    static class Counter {
        int i;

        void inc() {
            i++;
        }

        int dec() {
            return i--;
        }
    }

    HashMap<T, Counter> map = new HashMap<T, Counter>();

    /* (non-Javadoc)
     * @see java.util.Set#add(java.lang.Object)
     */
    public boolean add(T arg0) {
        Counter c = map.get(arg0);
        if (c == null) {
            c = new Counter();
            map.put(arg0, c);
        }
        c.inc();
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Set#clear()
     */
    public void clear() {
        map.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Set#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /* (non-Javadoc)
     * @see java.util.Set#isEmpty()
     */
    public boolean isEmpty() {
        return map.size() == 0;
    }

    /* (non-Javadoc)
     * @see java.util.Set#iterator()
     */
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    /* (non-Javadoc)
     * @see java.util.Set#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        Counter c = map.get(o);
        if (c == null) return false;
        if (c.dec() == 0) {
            map.remove(o);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Set#size()
     */
    public int size() {
        return map.size();
    }

    /* (non-Javadoc)
     * @see java.util.Set#toArray()
     */
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    /* (non-Javadoc)
     * @see java.util.Set#toArray(T[])
     */
    public <U> U[] toArray(U[] arg0) {
        return map.keySet().toArray(arg0);
    }


}
