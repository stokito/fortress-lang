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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.Iterator;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class FRangeIterator implements Iterator {
    FRange range;
    int index;

    public boolean hasNext() {
	return (index < (range.getBase() + range.getSize()));
    }

    public Object next() {
	return FInt.make(index++);
    }

    public boolean hasAtMostOne() {
	return index >= range.getBase() + range.getSize() - 1;
    }

    public void remove() {
        bug("FRangeIterator remove operation not implemented");
    }

    public FRangeIterator(FRange r) {
	range = r;
	index = r.getBase();
    }
}
