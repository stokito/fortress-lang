/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.useful.DebugletPrintStream;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import java.util.List;


// Note: implements Comparable, but only for equivalence classes of
// overloads.  Right now, the number of parameters determines,
// the equivalence class.

public class Overload implements Comparable, HasAt {

    /**
     * This overload is slightly tweaked to implement the symmetric test for
     * dotted methods.
     *
     * @author chase
     */
    static public class MethodOverload extends Overload {
        List<FType> params;

        public MethodOverload(MethodClosure mc) {
            super(mc);
            params = Useful.prepend(mc.getDefiner(), super.getParams());
        }

        public int getSelfParameterIndex() {
            return -1;
        }

        public List<FType> getParams() {
            return params;
        }
    }

    public String at() {
        return fn.at();
    }

    public String stringName() {
        return fn.toString();
    }

    public String toString() {
        return fn.toString();
    }

    public Overload(SingleFcn fn) {
        this.fn = fn;
        this.guaranteedOK = false;
    }

    public Overload(SingleFcn fn, OverloadedFunction olf, boolean guaranteedOK) {
        this.fn = fn;
        this.guaranteedOK = guaranteedOK;
    }


    public int getSelfParameterIndex() {
        if (fn instanceof HasSelfParameter) {
            return ((HasSelfParameter) fn).getSelfParameterIndex();
        }
        return -1;
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

    private final SingleFcn fn;

    /**
     * True if this overload cannot possibly conflict -- because it is a
     * functional method of an instantiated generic type.
     * This is a (major) optimization.
     */
    final boolean guaranteedOK;

    DebugletPrintStream ps = null; // never written...

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
