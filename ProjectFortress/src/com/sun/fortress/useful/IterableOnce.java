/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.useful;

import java.util.Iterator;

/**
 * This allows the idiom:
 * <p/>
 * for ( Elt e : someObject.someIteratedThing()) {
 * ...
 * }
 * <p/>
 * without double allocation or caching.
 */
public interface IterableOnce<T> extends Iterable<T>, Iterator<T> {

}
