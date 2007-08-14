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

package com.sun.fortress.useful;

import java.util.ArrayList;
import java.util.List;
import edu.rice.cs.plt.lambda.Lambda2;

public abstract class Fn2<T, U, V> implements Lambda2<T, U, V> {
    public abstract V apply(T t, U u);
    public V value(T t, U u) { return apply(t, u); }
    
    public final static Fn2<String, String, String> stringAppender = new Fn2<String, String, String> () {
        @Override
        public String apply(String t, String u) {
            return t + u;
        }
    };

    public final static <W> Fn2<List<W>, List<W>, List<W>> listAppender() {
        return new Fn2<List<W>, List<W>, List<W>> () {
            @Override
            public List<W> apply(List<W> t, List<W> u) {
                ArrayList<W> a = new ArrayList<W>(t);
                a.addAll(u);
                return a;
            }
        };
    }

}
