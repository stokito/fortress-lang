/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.interpreter.env.ReferenceCell;
import com.sun.fortress.interpreter.evaluator.values.FNativeObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;

/**
 * @author mph
 */

public class AtomicArray<T> extends FNativeObject {
    // The native constructor, containing type and method information:
    private final NativeConstructor con;
    private ReferenceCell[] array;

    /**
     * Creates a new instance of AtomicArray
     */
    public AtomicArray(NativeConstructor con, int capacity) {
        super(con);
        this.con = con;
        array = new ReferenceCell[capacity];
        for (int i = 0; i < capacity; i++) {
            array[i] = new ReferenceCell();
        }
    }

    public NativeConstructor getConstructor() {
        return this.con;
    }

    public boolean seqv(FValue v) {
        return v == this;
    }

    public T get(int i) {
        return (T) array[i].getValue();
    }

    public void set(int i, T value) {
        array[i].assignValue((FValue) value);
    }

    /**
     * Init is equivalent to set, but fails (returns false) for non-null
     * contents.
     */
    public boolean init(int i, T value) {
        if (get(i) == null) {
            array[i].storeValue((FValue) value);
            return true;
        } else return false;
    }

}
