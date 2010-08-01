/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.useful;

public class Pair<T, U> {
    private T a;
    private U b;

    public Pair(T a, U b) {
        this.a = a;
        this.b = b;
    }

    public static <T, U> Pair<T, U> make(T a, U b) {
        return new Pair<T, U>(a, b);
    }

    final public T getA() {
        return a;
    }

    final public U getB() {
        return b;
    }
    
    final public T first() {
        return a;
    }

    final public U  second() {
        return b;
    }
    
    final public Pair<T, U> setA(T t) {
        return new Pair(t, b);
    }
    
    final public Pair<T, U> setB(U u) {
        return new Pair(a, u);
    }
    
    public String toString() {
        return "(" + a + "," + b + ")";
    }

    final public boolean equals(Object o) {
        if (o instanceof Pair) {
            Pair p = (Pair) o;
            return p.a.equals(a) && p.b.equals(b);
        }
        return false;
    }

    final public int hashCode() {
        return (MagicNumbers.Z + a.hashCode()) * (MagicNumbers.Y + b.hashCode());
    }
    
    public static class GetA<TT, UU> implements F<Pair<TT,UU>,TT> {        
        @Override
        public TT apply(Pair<TT, UU> x) {
            return x.getA();
        }
    }
    public static class GetB<TT, UU> implements F<Pair<TT,UU>,UU> {        
        @Override
        public UU apply(Pair<TT, UU> x) {
            return x.getB();
        }
    }

}
