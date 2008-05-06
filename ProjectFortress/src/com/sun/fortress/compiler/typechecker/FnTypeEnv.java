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
import com.sun.fortress.nodes_util.OprUtil;

import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class FnTypeEnv extends TypeEnv {
    private Relation<IdOrOpOrAnonymousName, ? extends Function> entries;
    private TypeEnv parent;

    FnTypeEnv(Relation<IdOrOpOrAnonymousName, ? extends Function> _entries, TypeEnv _parent) {
        parent = _parent;
        entries = _entries;
    }

    /**
     * Return a BindingLookup that binds the given Id to a type
     * (if the given Id is in this type environment).
     */
    public Option<BindingLookup> binding(IdOrOpOrAnonymousName var) {
        Set<? extends Function> fns = entries.getSeconds(var);
        if (fns.isEmpty()) {
            if (var instanceof Id) {
                Id _var = (Id)var;
                if (_var.getApi().isSome())
                    return binding(new Id(_var.getSpan(), _var.getText()));
            }
            return parent.binding(var);
        }

        LinkedList<Type> overloadedTypes = new LinkedList<Type>();
        for (Function fn: fns) {
            if (fn instanceof DeclaredFunction) {
                DeclaredFunction _fn = (DeclaredFunction)fn;
                FnAbsDeclOrDecl decl = _fn.ast();
                overloadedTypes.add(makeGenericArrowType(decl.getSpan(),
                                                        decl.getStaticParams(),
                                                        typeFromParams(decl.getParams()),
                                                        unwrap(decl.getReturnType()), // all types have been filled in at this point
                                                        decl.getThrowsClause(),
                                                        decl.getWhere()));
            } else if (fn instanceof FunctionalMethod) {
                FunctionalMethod _fn = (FunctionalMethod)fn;
                FnAbsDeclOrDecl decl = _fn.ast();
                overloadedTypes.add(makeGenericArrowType(decl.getSpan(),
                                                        decl.getStaticParams(),
                                                        typeFromParams(decl.getParams()),
                                                        unwrap(decl.getReturnType()), // all types have been filled in at this point
                                                        decl.getThrowsClause(),
                                                        decl.getWhere()));
            } else { // fn instanceof Constructor
                final Constructor _fn = (Constructor)fn;

                // Invariant: _fn.params().isSome()
                // Otherwise, _fn should not have been in entries.
                overloadedTypes.add(makeGenericArrowType(_fn.declaringTrait().getSpan(),
                                                        _fn.staticParams(),
                                                        typeFromParams(unwrap(_fn.params())),
                                                        makeInstantiatedType(_fn.declaringTrait(),
                                                                             staticParamsToArgs(_fn.staticParams())),
                                                        _fn.throwsClause(),
                                                        _fn.where()));

            }
        }
        return some(new BindingLookup(var, makeAndType(overloadedTypes)));
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.firstSet()) {
            Option<BindingLookup> element = binding(name);
            if (element.isSome()) {
                result.add(unwrap(element));
            }
        }
        result.addAll(parent.contents());
        return result;
    }
}
