
package com.sun.fortress.syntax_abstractions.util;

import java.util.List;
import java.util.LinkedList;

public class ArrayUnpacker {

    public static <T> List<T> unpack(List<Object[]> arrays, int index) {
        List<T> acc = new LinkedList<T>();
        for (Object[] array : arrays) {
            acc.add((T)array[index]);
        }
        return acc;
    }
}
