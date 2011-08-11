/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.util.HashMap;

import com.sun.fortress.compiler.runtimeValues.RTTI;
import com.sun.fortress.useful.MagicNumbers;

/**
 * 
 * @author drc, kbn
 * maps tuples of RTTIs representing instantiations of type parameters
 * for generic classes, tuples, or functions to their actual RTTI values
 * (in other words, it is not used just for tuples)	
 *
 */

final public class RttiTupleMap {
    
    public RttiTupleMap() {
        
    }
    
    static class Node {
        final RTTI a[];
        final int k;
        
        public Node(RTTI o1) {
            RTTI[] b = { o1 };
            a = b;
            k = h(b);
            
        }

        public Node(RTTI o1, RTTI o2) {
            RTTI[] b = { o1, o2 };
            a = b;
            k = h(b);

        }

        public Node(RTTI o1, RTTI o2, RTTI o3) {
            RTTI[] b = { o1, o2, o3 };
            a = b;
            k = h(b);

        }

        public Node(RTTI o1, RTTI o2, RTTI o3, RTTI o4) {
            RTTI[] b = { o1, o2, o3, o4 };
            a = b;
            k = h(b);

        }

        public Node(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI o5) {
            RTTI[] b = { o1, o2, o3, o4, o5 };
            a = b;
            k = h(b);

        }

        public Node(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI o5, RTTI o6) {
            RTTI[] b = { o1, o2, o3, o4, o5, o6 };
            a = b;
            k = h(b);

        }
        
        public Node(RTTI[] args) {
        	a = args;
        	k = h(args);
        }

        static int h(RTTI[] a) {
            return MagicNumbers.hashArray(a);
        }
        
        public int hashCode() {
            return k;
        }
        
        public boolean equals(Object other) {
            if (other == this)
                return true;
            RTTI[] oa = ((Node)other).a;
            if (oa.length != a.length)
                return false;
            for (int i = 0; i < a.length; i++)
                if (oa[i] != a[i])
                    return false;
            return true;
        }
    }
    
    final HashMap<Node, RTTI> hm = new HashMap<Node, RTTI>();
     
    public RTTI get(RTTI o1) {
        Node n = new Node(o1);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI o1, RTTI value) {
        Node n = new Node(o1);
        return putIfNewHelper(n, value);
     }
    
    public RTTI get(RTTI o1, RTTI o2) {
        Node n = new Node(o1,o2);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI o1, RTTI o2, RTTI value) {
        Node n = new Node(o1,o2);
        return putIfNewHelper(n, value);
     }

    public RTTI get(RTTI o1, RTTI o2, RTTI o3) {
        Node n = new Node(o1,o2,o3);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI o1, RTTI o2, RTTI o3, RTTI value) {
        Node n = new Node(o1,o2,o3);
        return putIfNewHelper(n, value);
     }

    /**
     * @param n
     * @param value
     * @return
     */
    private RTTI putIfNewHelper(Node n, RTTI value) {
        synchronized (hm) {
            RTTI o = hm.get(n);
            if (o == null) {
                hm.put(n, value);
                o = value;
            }
            return o;
        }
    }

    public RTTI get(RTTI o1, RTTI o2, RTTI o3, RTTI o4) {
        Node n = new Node(o1,o2,o3, o4);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI value) {
        Node n = new Node(o1,o2,o3,o4);
        return putIfNewHelper(n, value);
     }
  
    public RTTI get(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI o5) {
        Node n = new Node(o1,o2,o3,o4,o5);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI o5, RTTI value) {
        Node n = new Node(o1,o2,o3,o4,o5);
        return putIfNewHelper(n, value);
     }
  
    public RTTI get(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI o5, RTTI o6) {
        Node n = new Node(o1,o2,o3,o4,o5,o6);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI o1, RTTI o2, RTTI o3, RTTI o4, RTTI o5, RTTI o6, RTTI value) {
        Node n = new Node(o1,o2,o3,o4,o5,o6);
        return putIfNewHelper(n, value);
    }
    
    public RTTI get(RTTI[] o) {
        Node n = new Node(o);
        synchronized (hm) {
           return hm.get(n);
        }
    }
    
    public RTTI putIfNew(RTTI[] o, RTTI value) {
        Node n = new Node(o);
        return putIfNewHelper(n, value);
    }
  
}
