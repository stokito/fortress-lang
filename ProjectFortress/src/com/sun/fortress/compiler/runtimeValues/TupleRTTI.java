/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.runtimeSystem.Naming;

public abstract class TupleRTTI extends RTTI {

    final RTTI[] elementRTTI;
    
    public TupleRTTI(Class javaRep, RTTI[] elements) {
        super(javaRep);
        elementRTTI = elements;
    }

    /**
     * Tupled covariant extension-of test.
     */
    public boolean runtimeSupertypeOf(RTTI other) {
        if (super.runtimeSupertypeOf(other))
            return true;
        if (! (other instanceof TupleRTTI))
            return false;
        RTTI[] otherRTTI = ((TupleRTTI) other).elementRTTI;
        if (otherRTTI.length != elementRTTI.length)
            return false;
        for (int i = 0; i < elementRTTI.length; i++) {
            if (! elementRTTI[i].runtimeSupertypeOf(otherRTTI[i]))
                return false;
        }
        return true;
    }

    public String className() {
        StringBuilder ret = new StringBuilder(Naming.CONCRETE_TUPLE + Naming.LEFT_OXFORD);
        for (int i = 0; i < this.elementRTTI.length -1; i++)
            ret.append(this.elementRTTI[i].className() + Naming.GENERIC_SEPARATOR);
        ret.append(this.elementRTTI[this.elementRTTI.length-1].className() + Naming.RIGHT_OXFORD);
        return ret.toString();
    }
}
