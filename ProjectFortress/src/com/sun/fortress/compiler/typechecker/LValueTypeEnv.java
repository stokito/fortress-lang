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
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static edu.rice.cs.plt.tuple.Option.*;

class LValueTypeEnv extends TypeEnv {
    private LValueBind[] entries;
    private TypeEnv parent;
    
    LValueTypeEnv(LValueBind[] _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }
    
    public Option<LValueBind> binding(Id var) {
        for (LValueBind entry : entries) {
            if (var.equals(entry.getName())) { 
                return wrap(entry);
            }
        }
        return parent.binding(var);
    }
    
    public Option<Type> type(Id var) { 
        for (int i = 0; i < entries.length; i++) {
            LValueBind entry = entries[i];

            if (var.equals(entry.getName())) {
                Option<Type> type = entry.getType();
                if (type.isSome()) { 
                    return type; 
                } else { 
                    Type implicitType = new _RewriteImplicitType();
                    entries[i] = 
                        NodeFactory.makeLValue(entry, implicitType);
                    return wrap(implicitType);
                }
            }
        }
        return Option.none();
    }
        
    public Option<List<Modifier>> mods(Id var) { 
        Option<LValueBind> binding = binding(var);
        
        if (binding.isSome()) { return wrap(unwrap(binding).getMods()); }
        else { return Option.none(); }
    }

    public Option<Boolean> mutable(Id var) { 
        Option<LValueBind> binding = binding(var);
        
        if (binding.isSome()) { return wrap(unwrap(binding).isMutable()); }
        else { return Option.none(); }
    }        

}