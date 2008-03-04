/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
 * A type environment whose outermost lexical scope consists of a map from
 * Ids to Variables.
 */
class VarTypeEnv extends TypeEnv {
    private Map<Id, Variable> entries;
    private TypeEnv parent;

    VarTypeEnv(Map<Id, Variable> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given SimpleName to a type
     * (if the given SimpleName is in this type environment).
     */
    public Option<BindingLookup> binding(SimpleName var) {
    	if (!(var instanceof Id)) {	return parent.binding(var); }
    	Id _var = (Id)var;
        if (entries.containsKey(_var)) {
            Variable result = entries.get(_var);
            if (result instanceof DeclaredVariable) {
                return some(new BindingLookup(((DeclaredVariable)result).ast()));
            } else if (result instanceof SingletonVariable) {
                SingletonVariable _result = (SingletonVariable)result;
                Id declaringTrait = _result.declaringTrait();

                return some(new BindingLookup(makeLValue(_var, declaringTrait)));
            } else { // result instanceof ParamVariable
                ParamVariable _result = (ParamVariable)result;
                Param param = _result.ast();
                Option<Type> type = typeFromParam(param);

                return some(new BindingLookup(makeLValue(
                		makeLValue(param.getName(), type), param.getMods())));
            }
        } else { return parent.binding(var); }
    }
}
