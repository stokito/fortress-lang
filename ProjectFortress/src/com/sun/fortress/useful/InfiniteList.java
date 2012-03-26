/*
 * Created on Jan 29, 2012
 *
 */
package com.sun.fortress.useful;

import java.util.AbstractList;

public class InfiniteList<T> extends AbstractList<T> {

    T x;
    
    public InfiniteList(T x) {
        this.x = x;
    }
    @Override
    public T get(int arg0) {
        return x;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return Integer.MAX_VALUE;
    }

}
