/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public abstract class TupleRTTI extends RTTI {

    final RTTI[] elementRTTI;
    
    public TupleRTTI(Class javaRep, RTTI[] elements) {
        super(javaRep);
        elementRTTI = elements;
    }

    /**
     * Tupled covariant extension-of test.
     */
    public boolean argExtendsThis(RTTI other) {
        if (super.argExtendsThis(other))
            return true;
        if (! (other instanceof TupleRTTI))
            return false;
        RTTI[] otherRTTI = ((TupleRTTI) other).elementRTTI;
        if (otherRTTI.length != elementRTTI.length)
            return false;
        for (int i = 0; i < elementRTTI.length; i++) {
            if (! elementRTTI[i].argExtendsThis(otherRTTI[i]))
                return false;
        }
        return true;
    }

    
}
