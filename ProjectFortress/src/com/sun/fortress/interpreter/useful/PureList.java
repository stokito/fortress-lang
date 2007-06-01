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

/**
 * This class is an implementation of purely functional lists,
 * unlike those in java.util, which are mutable lists and are
 * therefore unsuitable for certain programming tasks.
 */
public abstract class PureList<T> {
   public static <T> PureList<T> make(T... elts) {
      PureList<T> result = new Empty<T>();
      for (int i = elts.length - 1; i >= 0; i--) {
         result = result.cons(elts[i]);
      }
      return result;
   }

   public static <T> PureList<T> fromJavaList(java.util.List<T> elts) {
     PureList<T> result = new Empty<T>();
     for (T elt : elts) {
       result = result.cons(elt);
     }
     return result.reverse();
   }

   public java.util.List<T> toJavaList() {
     java.util.List<T> result = new java.util.LinkedList<T>();
     PureList<T> remainder = this;

     while (! remainder.isEmpty()) {
       Cons<T> _remainder = (Cons<T>)remainder;
       result.add(_remainder.getFirst());
       remainder = _remainder.getRest();
     }
     return result;
   }

   public Object[] toArray() { return toArray(size()); }

   /* @pre this.size() >= n */
   public Object[] toArray(int n) {
     Object[] result = new Object[n];

     PureList<T> remainder = this;
     for (int i = 0; i < n; i++) {
       Cons<T> _remainder = (Cons<T>)remainder;
       result[i] = _remainder.getFirst();
       remainder = _remainder.getRest();
     }
     return result;
   }

   public abstract boolean isEmpty();
   public abstract int size();
   public abstract <U> PureList<U> map(Fn<T,U> fn);
   public abstract boolean contains(T candidate);
   
   public final PureList<T> cons(T... elts) { 
     PureList<T> result = this;
     for (int i = elts.length - 1; i >= 0; i--) {
       result = new Cons<T>(elts[i], result);
     }
     return result;
   }
   
   public abstract PureList<T> append(PureList<T> that);
   public PureList<T> reverse() { return reverse(new Empty<T>()); }
   public abstract PureList<T> reverse(PureList<T> result);
}
