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

import java.util.List;

import com.sun.fortress.interpreter.evaluator.InterpreterBug;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.useful.DebugletPrintStream;
import com.sun.fortress.useful.HasAt;


// Note: implements Comparable, but only for equivalence classes of
// overloads.  Right now, the number of parameters determines,
// the equivalence class.

public class Overload implements Comparable, HasAt {
    public String at() {
        return fn.at();
    }

    public String stringName() {
        return fn.toString();
    }

    public String toString() {
        return fn.toString();
    }

    public Overload(SingleFcn fn, OverloadedFunction olf) {
        this.fn = fn;
    }

    public Overload(SingleFcn fn, OverloadedFunction olf, boolean guaranteedOK) {
        this.fn = fn;
        this.guaranteedOK = guaranteedOK;
    }



    /**
     * @param fn The fn to set.
     */
    public void setFn(SingleFcn fn) {
        this.fn = fn;
    }

    /**
     * @return Returns the fn.
     */
    public SingleFcn getFn() {
        return fn;
    }

    /**
     * @return Returns the params.
     */
    public List<FType> getParams() {
        return fn.getNormalizedDomain();
    }



    private SingleFcn fn;
    /**
     * True if this overload cannot possibly conflict -- because it is a
     * functional method of an instantiated generic type.
     * This is a (major) optimization.
     */
    boolean guaranteedOK;
    
    DebugletPrintStream ps;

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof Overload) {
            Overload that = (Overload) arg0;
            return this.fn.equals(that.fn);
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.fn.hashCode();
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof Overload) {
            Overload that = (Overload) arg0;
            int this_size = this.getParams().size();
            int that_size = that.getParams().size();
            if (this_size < that_size) return -1;
            if (this_size > that_size) return 1;
            return 0;
        }
        throw new InterpreterBug("Overload cannot be compared to " + arg0.getClass());
    }
}
