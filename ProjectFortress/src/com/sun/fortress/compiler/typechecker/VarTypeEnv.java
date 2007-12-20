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

import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/** 
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class VarTypeEnv extends TypeEnv {
    private Map<Id, Variable> entries;
    private TypeEnv parent;
    
    VarTypeEnv(Map<Id, Variable> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }
    
    public Option<LValueBind> binding(Id var) {
        if (entries.containsKey(var)) { 
            Variable result = entries.get(var); 
            if (result instanceof DeclaredVariable) {
                return some(((DeclaredVariable)result).ast()); 
            } else if (result instanceof SingletonVariable) {
                SingletonVariable _result = (SingletonVariable)result;
                Id declaringTrait = _result.declaringTrait();
                
                return some(makeLValue(var, declaringTrait));
            } else { // result instanceof ParamVariable 
                ParamVariable _result = (ParamVariable)result;
                Param param = _result.ast();
                Option<Type> type = typeFromParam(param);
                
                return some(makeLValue(makeLValue(param.getName(), 
                                                  type),
                                       param.getMods()));
            }
        } else { return parent.binding(var); }
    }
    
    public Option<List<Modifier>> mods(Id var) { return none(); }
    public Option<Boolean> mutable(Id var) { return none(); }
    
    public Option<Type> type(Id var) { 
        if (entries.containsKey(var)) { 
            Variable result = entries.get(var); 
            if (result instanceof DeclaredVariable) {
                return ((DeclaredVariable)result).ast().getType(); 
            } else if (result instanceof SingletonVariable) {
                SingletonVariable _result = (SingletonVariable)result;
                Id declaringTrait = _result.declaringTrait();
                
                return Option.<Type>some(makeInstantiatedType(_result.declaringTrait().
                                                                  getSpan(), 
                                                              false, 
                                                              makeQualifiedIdName(Arrays.<Id>asList(),
                                                                                  declaringTrait)));
            } else { // result instanceof ParamVariable
                ParamVariable _result = (ParamVariable)result;
                Param param = _result.ast();
                
                return typeFromParam(param);
            }
        } else { return parent.type(var); }
    }
}