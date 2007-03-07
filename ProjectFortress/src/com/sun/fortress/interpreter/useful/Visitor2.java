/*
 * Created on Feb 28, 2007
 *
 */
package com.sun.fortress.interpreter.useful;

public abstract class Visitor2<T, U> {
    public abstract void visit(T t, U u);
}
