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

package com.sun.fortress.interpreter.evaluator.values;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeTuple;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public abstract class FTupleLike extends FValue implements Selectable {

    private final List<FValue> vals;

    public List<FValue> getVals() { return vals;}

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
        if (c != '$')
            error(errorMsg("Tuple selectors (for internal use only) begin with '$': ", s));
        return vals.get(Integer.parseInt(s.substring(1)));
    }
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

}
