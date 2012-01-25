/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.useful;

public class Triple<T, U, V> {
    private final T a;
    private final U b;
    private final V c;

    public Triple(T a, U b, V c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static <T, U, V> Triple<T, U, V> make(T a, U b, V c) {
        return new Triple<T, U, V>(a, b, c);
    }

    final public T getA() {
        return a;
    }

    final public U getB() {
        return b;
    }
    
    final public V getC() {
        return c;
    }
    
    final public T first() {
        return a;
    }

    final public U  second() {
        return b;
    }
    
    final public Triple<T, U, V> setA(T t) {
        return new Triple(t, b, c);
    }
    
    final public Triple<T, U, V> setB(U u) {
        return new Triple(a, u, c);
    }
    
    public String toString() {
        return "(" + a + "," + b + ", " + c + ")";
    }

    final public boolean equals(Object o) {
        if (o instanceof Triple) {
            Triple p = (Triple) o;
            return p.a.equals(a) && p.b.equals(b) && p.c.equals(c);
        }
        return false;
    }

    final public int hashCode() {
        return (MagicNumbers.Z + a.hashCode()) * (MagicNumbers.Y + b.hashCode()) * (MagicNumbers.Z + c.hashCode());
    }
    
    public static class GetA<TT, UU, VV> implements F<Triple<TT,UU, VV>,TT> {        
        @Override
        public TT apply(Triple<TT, UU, VV> x) {
            return x.getA();
        }
    }
    public static class GetAB<TT, UU, VV> implements F<Triple<TT,UU, VV>,Pair<TT, UU>> {        
        @Override
        public Pair<TT, UU> apply(Triple<TT, UU, VV> x) {
            return new Pair<TT,UU>(x.getA(), x.getB());
        }
    }
    public static class GetB<TT, UU, VV> implements F<Triple<TT,UU, VV>,UU> {        
        @Override
        public UU apply(Triple<TT, UU, VV> x) {
            return x.getB();
        }
    }

    public static class GetC<TT, UU, VV> implements F<Triple<TT,UU, VV>,VV> {        
        @Override
        public VV apply(Triple<TT, UU, VV> x) {
            return x.getC();
        }
    }

}
