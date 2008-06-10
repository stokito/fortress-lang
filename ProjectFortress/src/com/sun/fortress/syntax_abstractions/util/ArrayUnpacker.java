
package com.sun.fortress.syntax_abstractions.util;

import java.util.List;
import java.util.LinkedList;
import xtc.util.Pair;

public class ArrayUnpacker {

    public static <T> List<T> unpack(List<Object[]> arrays, int index) {
        List<T> acc = new LinkedList<T>();
        for (Object[] array : arrays) {
            acc.add((T)array[index]);
        }
        return acc;
    }

    public static List<Object[]> convertPackedList(Pair<Object> packed) {
        List<Object[]> acc = new LinkedList<Object[]>();
        for (Object p : packed) {
            if ( p == null ){
                System.out.println( "**Warning** Packed object is null" );
            }
            acc.add((Object[])p);
        }
        return acc;
    }

}
