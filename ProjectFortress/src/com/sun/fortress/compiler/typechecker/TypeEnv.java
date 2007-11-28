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

import static com.sun.fortress.nodes_util.NodeFactory.makeIdName;


public abstract class TypeEnv {
    public abstract Type type(IdName var);
    public Type type(String var) { return type(makeIdName(var)); }
    public abstract List<Modifier> mods(IdName var);
    public abstract boolean mutable(IdName var);
    
    public TypeEnv extend(LValueBind... _entries) {
        return new NonEmptyTypeEnv(_entries, this);
    }
}
