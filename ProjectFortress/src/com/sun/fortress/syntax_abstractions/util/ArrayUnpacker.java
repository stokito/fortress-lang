/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
