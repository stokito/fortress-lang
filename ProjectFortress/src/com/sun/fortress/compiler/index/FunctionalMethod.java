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

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.FnAbsDeclOrDecl;
import com.sun.fortress.nodes.Id;

/**
 * Note that this is a {@link Function}, not a {@link Method}, despite the name
 * (methods have distinct receivers).
 */
public class FunctionalMethod extends Function {
    
    private final FnAbsDeclOrDecl _ast;
    private final Id _declaringTrait;
    
    public FunctionalMethod(FnAbsDeclOrDecl ast, Id declaringTrait) {
        _ast = ast;
        _declaringTrait = declaringTrait;
    }
    
    public FnAbsDeclOrDecl ast() { return _ast; }
}
