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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;


public class FTuple extends FValue {
    List<FValue> vals;

    public String getString() {
        StringBuffer res = new StringBuffer();
        boolean first = true;
        for (Iterator<FValue> i = vals.iterator(); i.hasNext(); ) {
            FValue val = i.next();
            if (first) {
                first = false;
                res.append('(');
            } else {
                res.append(',');
            }
            res = res.append(val.getString());
        }
        return res.append(')').toString();
    }

    public String toString() {
        return (this.getString()+':'+this.type().toString());
    }

    public static FTuple make(List<FValue> elems) {
        if (elems.size() == 0) return FVoid.V;
        else return new FTuple(elems);
    }

    protected FTuple() {
        vals = Collections.<FValue>emptyList();
        setFtype(FTypeTuple.make(Collections.<FType>emptyList()));
    }

    protected FTuple(List<FValue> elems) {
        vals = elems;
        setFtype(FTypeTuple.make(typeListFromValues(elems)));
    }

    public List<FValue> getVals() { return vals;}
}
