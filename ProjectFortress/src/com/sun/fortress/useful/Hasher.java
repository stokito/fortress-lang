/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public abstract class Hasher<T> {
    public abstract long hash(T x);

    public abstract boolean equiv(T x, T y);

    public static final Hasher<String> CIHasher = new Hasher<String>() {

        @Override
        public long hash(String x) {
            long h = MagicNumbers.a;

            for (int i = 0; i < x.length(); i++) {
                char cx = Character.toLowerCase(x.charAt(i));
                h = (h + cx) * MagicNumbers.b;
            }
            return h;
        }

        @Override
        public boolean equiv(String x, String y) {
            if (x.length() != y.length()) return false;
            for (int i = 0; i < x.length(); i++) {
                char cx = Character.toLowerCase(x.charAt(i));
                char cy = Character.toLowerCase(y.charAt(i));
                if (cx != cy) return false;
            }
            return true;
        }

    };
}
