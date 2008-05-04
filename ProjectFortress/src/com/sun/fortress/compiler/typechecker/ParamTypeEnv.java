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
import com.sun.fortress.compiler.typechecker.TypeEnv.BindingLookup;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class ParamTypeEnv extends TypeEnv {
    private List<Param> entries;
    private TypeEnv parent;

    ParamTypeEnv(List<Param> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given IdOrOpOrAnonymousName to a type
     * (if the given IdOrOpOrAnonymousName is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.binding(var); }
        Id _var = (Id)var;
        for (Param param : entries) {
            if (param.getName().getText().equals(_var.getText())) {
                return some(new BindingLookup(_var, typeFromParam(param)));
            }
        }
        if (_var.getApi().isSome())
            return binding(new Id(_var.getSpan(), _var.getText()));
        return parent.binding(_var);
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (Param param : entries) {
            result.add(new BindingLookup(param.getName(), typeFromParam(param)));
        }
        result.addAll(parent.contents());
        return result;
    }
}
