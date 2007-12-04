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

import static com.sun.fortress.nodes_util.NodeFactory.makeIdName;

/** 
 * This class is used by the type checker to represent static type environments,
 * mapping bound variables to their types. 
 */
public abstract class TypeEnv {
    public static TypeEnv make(LValueBind... entries) {
        return EmptyTypeEnv.ONLY.extend(entries);
    }
    
    public abstract Option<LValueBind> binding(IdName var);
    public abstract Option<Type> type(IdName var);
    public abstract Option<List<Modifier>> mods(IdName var);
    public abstract Option<Boolean> mutable(IdName var);
    
    public Option<Type> type(String var) { return type(makeIdName(var)); }
    public Option<List<Modifier>> mods(String var) { return mods(makeIdName(var)); }
    public Option<Boolean> mutable(String var) { return mutable(makeIdName(var)); }
        
    /**
     * Produce a new type environment extending this with the given variable bindings.
     */
    public TypeEnv extend(LValueBind... entries) {
        if (entries.length == 0) { return EmptyTypeEnv.ONLY; }
        else { return new NonEmptyTypeEnv(entries, this); }
    }
}
