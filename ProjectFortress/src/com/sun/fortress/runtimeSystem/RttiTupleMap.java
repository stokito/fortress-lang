/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.util.HashMap;

import com.sun.fortress.useful.MagicNumbers;

final public class RttiTupleMap {
    
    public RttiTupleMap() {
        
    }
    
    static class Node {
        final Object a[];
        final int k;
        
        public Node(Object o1) {
            Object[] b = { o1 };
            a = b;
            k = h(b);
        }

        public Node(Object o1, Object o2) {
            Object[] b = { o1, o2 };
            a = b;
            k = h(b);

        }

        public Node(Object o1, Object o2, Object o3) {
            Object[] b = { o1, o2, o3 };
            a = b;
            k = h(b);

        }

        public Node(Object o1, Object o2, Object o3, Object o4) {
            Object[] b = { o1, o2, o3, o4 };
            a = b;
            k = h(b);

        }

        public Node(Object o1, Object o2, Object o3, Object o4, Object o5) {
            Object[] b = { o1, o2, o3, o4, o5 };
            a = b;
            k = h(b);

        }

        public Node(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            Object[] b = { o1, o2, o3, o4, o5, o6 };
            a = b;
            k = h(b);

        }

        static int h(Object[] a) {
            return MagicNumbers.hashArray(a);
        }
        
        public int hashCode() {
            return k;
        }
        
        public boolean equals(Object other) {
            if (other == this)
                return true;
            Object[] oa = ((Node)other).a;
            if (oa.length != a.length)
                return false;
            for (int i = 0; i < a.length; i++)
                if (oa[i] != a[i])
                    return false;
            return true;
        }
    }
    
    final HashMap<Node, Object> hm = new HashMap<Node, Object>();
     
    public Object get(Object o1) {
        Node n = new Node(o1);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public Object putIfNew(Object o1, Object value) {
        Node n = new Node(o1);
        return putIfNewHelper(n, value);
     }
    
    public Object get(Object o1, Object o2) {
        Node n = new Node(o1,o2);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public Object putIfNew(Object o1, Object o2, Object value) {
        Node n = new Node(o1,o2);
        return putIfNewHelper(n, value);
     }

    public Object get(Object o1, Object o2, Object o3) {
        Node n = new Node(o1,o2,o3);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public Object putIfNew(Object o1, Object o2, Object o3, Object value) {
        Node n = new Node(o1,o2,o3);
        return putIfNewHelper(n, value);
     }

    /**
     * @param n
     * @param value
     * @return
     */
    private Object putIfNewHelper(Node n, Object value) {
        synchronized (hm) {
            Object o = hm.get(n);
            if (o == null) {
                hm.put(n, value);
                o = value;
            }
            return o;
        }
    }

    public Object get(Object o1, Object o2, Object o3, Object o4) {
        Node n = new Node(o1,o2,o3, o4);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public Object putIfNew(Object o1, Object o2, Object o3, Object o4, Object value) {
        Node n = new Node(o1,o2,o3,o4);
        return putIfNewHelper(n, value);
     }
  
    public Object get(Object o1, Object o2, Object o3, Object o4, Object o5) {
        Node n = new Node(o1,o2,o3,o4,o5);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public Object putIfNew(Object o1, Object o2, Object o3, Object o4, Object o5, Object value) {
        Node n = new Node(o1,o2,o3,o4,o5);
        return putIfNewHelper(n, value);
     }
  
    public Object get(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
        Node n = new Node(o1,o2,o3,o4,o5,o6);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public Object putIfNew(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object value) {
        Node n = new Node(o1,o2,o3,o4,o5,o6);
        return putIfNewHelper(n, value);
     }
  
}
