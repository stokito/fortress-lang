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

import com.sun.fortress.interpreter.useful.PureList;

public final class TypeCheckerResult {
    public static final TypeCheckerResult VALID = new TypeCheckerResult();
    
    private final PureList<TypeError> errors;

    public static TypeCheckerResult combine(PureList<TypeCheckerResult> results) {
        TypeCheckerResult result = VALID;
        for (TypeCheckerResult next : results) {
            result = result.combine(next);
        }
        return result;
    }
        
    public TypeCheckerResult(TypeError... errors) {
        this.errors = PureList.make(errors);
    }
    public TypeCheckerResult(PureList<TypeError> errors) {
        this.errors = errors;
    }
    public boolean isValid() { return errors.isEmpty(); }
    public boolean hasErrors() { return ! isValid(); }
    public PureList<TypeError> getErrors() { return errors; }
    public int errorCount() { return errors.size(); }
    public TypeCheckerResult combine(TypeCheckerResult that) { 
        return new TypeCheckerResult(errors.append(that.getErrors()));
    }
}
    