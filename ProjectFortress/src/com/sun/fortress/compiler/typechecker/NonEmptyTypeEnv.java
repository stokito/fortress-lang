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
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

class NonEmptyTypeEnv extends TypeEnv {
    private LValueBind[] entries;
    private TypeEnv parent;
    
    NonEmptyTypeEnv(LValueBind[] _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }
    
    public Type type(IdName var) { 
        for (LValueBind entry : entries) {
            if (var.equals(entry.getName())) { 
                return Option.unwrap(entry.getType()); 
            }
        }
        return parent.type(var);
    }

    public List<Modifier> mods(IdName var) { return entries[0].getMods(); }
    public boolean mutable(IdName var) { return entries[0].isMutable(); }
}