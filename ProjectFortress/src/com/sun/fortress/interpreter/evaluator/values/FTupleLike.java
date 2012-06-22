/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class FTupleLike extends FValue implements Selectable {

    private final List<FValue> vals;

    public List<FValue> getVals() {
        return vals;
    }

    public FType type() {
        return FTypeTuple.make(typeListFromValues(getVals()));
    }

    protected FTupleLike() {
        vals = Collections.<FValue>emptyList();
    }

    protected FTupleLike(List<FValue> elems) {
        vals = elems;
    }

    /* (non-Javadoc)
    * @see com.sun.fortress.interpreter.evaluator.values.Selectable#select(java.lang.String)
    */
    public FValue select(String s) {
        char c = s.charAt(0);
        if (c != '$') error(errorMsg("Tuple selectors (for internal use only) begin with '$': ", s));
        return vals.get(Integer.parseInt(s.substring(1)));
    }

    public String getString() {
        StringBuilder res = new StringBuilder();
        boolean first = true;
        for (FValue val : vals) {
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
        return (this.getString() + ": " + this.type().toString());
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FTupleLike)) return false;
        FTupleLike t = (FTupleLike) v;
        if (getVals().size() != t.getVals().size()) return false;
        Iterator<FValue> titer = t.getVals().iterator();
        for (FValue i : getVals()) {
            FValue ti = titer.next();
            if (i != ti && !i.seqv(ti)) return false;
        }
        return true;
    }
}
