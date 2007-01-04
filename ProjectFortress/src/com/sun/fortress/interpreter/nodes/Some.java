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

package com.sun.fortress.interpreter.nodes;

import java.util.List;

public class Some<T> extends Option<T> {
    T val;

    public Some(T v) {
        val = v;
    }

    public static <T> Option<T> make(T v) {
        return new Some<T>(v);
    }

    public static <T> Option<List<T>> makeSomeList(List<T> v) {
        return new Some<List<T>>(v);
    }

    public static <T> Option<List<T>> makeSomeListOrNone(List<T> v) {
        if (v.size() == 0) {
            return new None<List<T>>();
        }
        return new Some<List<T>>(v);
    }

    /**
     * @return Returns the val.
     */
    @Override
    public T getVal() {
        return val;
    }

    @Override
    public final boolean isPresent() {
        return true;
    }

    @Override
    public String toString() {
        return val.toString();
    }

    @Override
    public int hashCode() {
        return val.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Some) {
            Some s = (Some) o;
            return val.equals(s.getVal());
        }
        return false;
    }
}
