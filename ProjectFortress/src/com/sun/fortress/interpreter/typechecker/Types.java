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
package com.sun.fortress.interpreter.typechecker;
import com.sun.fortress.interpreter.evaluator.types.*;

/**
 * This class is responsible for knowing about all types that are intrinsically 
 * part of type checking, and for providing functionality to compare and 
 * manipulate static types.
 */
public class Types {
    public static final Types ONLY = new Types();
    
    public final FType ANY = BottomType.ONLY;
    public final FType BOOLEAN = BottomType.ONLY;
    public final FType BOTTOM = BottomType.ONLY;
    public final FType HEAP_SEQUENCE = BottomType.ONLY;
    public final FType OBJECT = BottomType.ONLY;
    public final FType VOID = FTypeVoid.ONLY;
        
    private Types() {}
    
    /** Test whether s is a subtype of t */
    public boolean isSubtype(FType s, FType t) {
        return true;  // TODO: implement
    }
    
    public FType union(FType... ts) {
        return BOTTOM; // TODO: implement
    }
    
    public FType union(Iterable<FType> ts) {
        return BOTTOM; // TODO: implement
    }
}
