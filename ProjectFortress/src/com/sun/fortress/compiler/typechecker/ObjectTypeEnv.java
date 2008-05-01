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
 * to ObjectTraitIndex.
 */
class ObjectTypeEnv extends TypeEnv {
    private Map<Id, TypeConsIndex> entries;
    private TypeEnv parent;

    ObjectTypeEnv(Map<Id, TypeConsIndex> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given Id to a type
     * (if the given Id is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        if (!(var instanceof Id)) { return parent.binding(var); }
        Id _var = (Id)var;

        if (!entries.containsKey(_var)) { return parent.binding(var); }
        TypeConsIndex typeCons = entries.get(_var);
        
        if (!(typeCons instanceof ObjectTraitIndex)) { return parent.binding(var); }
        ObjectTraitIndex objIndex = (ObjectTraitIndex)typeCons;
        
        Type type;
        ObjectAbsDeclOrDecl decl = (ObjectAbsDeclOrDecl)objIndex.ast();
        if (decl.getStaticParams().isEmpty()) {
            if (decl.getParams().isNone()) {
                // No static params, no normal params
                type = NodeFactory.makeInstantiatedType(_var);
            } else {
                // No static params, some normal params
                type = new ArrowType(var.getSpan(),
                        typeFromParams(unwrap(decl.getParams())),
                        NodeFactory.makeInstantiatedType(_var));
            }
        } else {
            if (decl.getParams().isNone()) {
                // Some static params, no normal params
                type = NodeFactory.makeGenericSingletonType(_var, decl.getStaticParams());
            } else {
                // Some static params, some normal params
                // TODO: handle type variables bound in where clause
                type = NodeFactory.makeGenericArrowType(decl.getSpan(),
                        decl.getStaticParams(),
                        typeFromParams(unwrap(decl.getParams())),
                        NodeFactory.makeInstantiatedType(_var));
            }
        }

        return some(new BindingLookup(var, type, decl.getMods()));   
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.keySet()) {
            Option<BindingLookup> element = binding(name);
            if (element.isSome()) {
                result.add(unwrap(element));
            }
        }
        result.addAll(parent.contents());
        return result;
    }
}
