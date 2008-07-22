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

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.env.ReferenceCell;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;

import java.lang.reflect.Array;

/**
 * @author mph
 */

public class AtomicArray<T> extends NativeConstructor.FNativeObject {
	// The native constructor, containing type and method information:
    private final NativeConstructor con;
	private ReferenceCell[] array;

	/** Creates a new instance of AtomicArray */
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
        return v==this;
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
	 **/
	public boolean init(int i, T value) {
		if (get(i) == null) {
			array[i].storeValue((FValue) value);
			return true;
		} else return false;
	}

}
