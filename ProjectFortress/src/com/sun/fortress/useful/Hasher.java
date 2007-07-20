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

public abstract class Hasher<T> {
    public abstract long hash(T x);
    public abstract boolean equiv(T x, T y);

    public static Hasher<String> CIHasher = new Hasher<String>() {

        @Override
        public long hash(String x) {
            long h = MagicNumbers.a;

            for (int i = 0; i < x.length(); i++) {
                char cx = Character.toLowerCase (x.charAt(i));
                h = (h + cx) * MagicNumbers.b;
            }
            return h;
        }

        @Override
        public boolean equiv(String x, String y) {
            if (x.length() != y.length())
                return false;
            for (int i = 0; i < x.length(); i++) {
                char cx = Character.toLowerCase (x.charAt(i));
                char cy = Character.toLowerCase (y.charAt(i));
                if (cx != cy)
                    return false;
            }
            return true;
        }

    };
}
