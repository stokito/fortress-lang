/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.runtimeSystem.Naming;

public abstract class RTTI {
    
    private static long snCount = 0;
    
    final Class javaRep;
    
    private final long serialNumber;
    
    public boolean runtimeSupertypeOf(RTTI other) {
        return this.javaRep.isAssignableFrom( other.javaRep );
    }
    
    public RTTI(Class javaRep) {
        this.javaRep = javaRep;
        this.serialNumber = snCount++;
    }
    
    public int hashCode() { return javaRep.hashCode(); }
    
    public String className() {
        String className = javaRep.getCanonicalName();
        String deMangle = Naming.demangleFortressIdentifier(className);
        String noDots = Naming.dotToSep(deMangle);
        return noDots;
        
    }
    
    public long getSN() {
        return this.serialNumber;
    }
    
    public String toString() {
        return serialNumber + ":" + javaRep.getSimpleName();
    }
    
}
