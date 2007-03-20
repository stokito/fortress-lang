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

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeRange;


public class FRange extends FValue {
    final int base;

    final int size;

    final boolean sequential;

    private FRange(int b, int s, boolean seq) {
        base = b;
        size = s;
        sequential = seq;
    }

    public FRange(int b, int s) {
        base = b;
        size = s;
        sequential = false;
    }

    static List<FRange> make(IndexedShape i) {
        ArrayList<FRange> a = new ArrayList<FRange>(i.dim());
        for (int j = 0; j < i.dim(); j++) {
            a.add(new FRange(0, i.size(j)));
        }
        return a;
    }

    public FType type() {
        return new FTypeRange(base,size);
    }

    public boolean contains(FValue x) {
        int val = x.getInt();
        return val >= base && val < base + size;
    }

    public FRange firstHalf() {
        return new FRange(base, Integer.highestOneBit(size-1), sequential);
    }

    public FRange secondHalf() {
        int bot = Integer.highestOneBit(size-1);
        return new FRange(base+bot, size-bot, sequential);
    }

    public FRange sequential() {
        if (sequential) {
            return this;
        } else {
            return new FRange(base,size,true);
        }
    }

    public boolean isSequential() {
        return sequential;
    }

    public int getSize() {
        return size;
    }

    public int getBase() {
        return base;
    }
}
