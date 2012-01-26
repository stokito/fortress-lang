/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.util;

import xtc.util.Pair;

import java.util.LinkedList;
import java.util.List;

/* cheesy way of passing multiple values around by shoving them into an array
 * and getting them back out of the array
 */
public class ArrayUnpacker {

    @SuppressWarnings ("unchecked")
    public static <T> List<T> unpack(List<Object[]> arrays, int index) {
        List<T> acc = new LinkedList<T>();
        for (Object[] array : arrays) {
            acc.add((T) array[index]);
        }
        return acc;
    }

    public static List<Object[]> convertPackedList(Pair<Object> packed) {
        List<Object[]> acc = new LinkedList<Object[]>();
        for (Object p : packed) {
            if (p == null) {
                System.out.println("**Warning** Packed object is null");
            }
            acc.add((Object[]) p);
        }
        return acc;
    }

}
