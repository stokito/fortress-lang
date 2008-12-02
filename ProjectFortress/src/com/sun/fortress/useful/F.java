/*
 * Created on Dec 2, 2008
 *
 */
package com.sun.fortress.useful;

public abstract class F <T, U>  {
    public abstract U apply(T x);
    public U value(T arg) { return apply(arg); }
}
