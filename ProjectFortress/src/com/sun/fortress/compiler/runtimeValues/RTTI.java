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
    /*
     * Long term, this will need to be done lazily because
     * of issues generating appropriate bytecodes for methods
     * appearing in some generics -- nominal union types will
     * need to be canonicalized to a form that might not be a
     * union, depending upon subtype relations that might depend
     * on this class itself.
     */
    public RTTI(String javaRep) {
        try {
            this.javaRep = Class.forName(javaRep);
        } catch (ClassNotFoundException ex) {
            throw new Error(ex);
        }
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
