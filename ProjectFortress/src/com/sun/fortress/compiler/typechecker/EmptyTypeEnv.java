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

package com.sun.fortress.compiler.typechecker;

import com.sun.fortress.nodes.*;
import java.util.*;

class EmptyTypeEnv extends TypeEnv {
    public static final EmptyTypeEnv NIL = new EmptyTypeEnv();
    
    private EmptyTypeEnv() {}
    
    private RuntimeException error() { throw new RuntimeException("Attempt to lookup in an EmptyTypeEnv."); }
    public Type type(IdName var) { throw error(); }
    public List<Modifier> mods(IdName var) { throw error(); }
    public boolean mutable(IdName var) { throw error(); }
}