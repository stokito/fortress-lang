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
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;

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
                overloadedTypes.add(genericArrowFromDecl(_fn.ast()));
            } else if (fn instanceof FunctionalMethod) {
                FunctionalMethod _fn = (FunctionalMethod)fn;
                FnAbsDeclOrDecl decl = _fn.ast();
                overloadedTypes.add(genericArrowFromDecl(_fn.ast()));
            } else { // fn instanceof Constructor
                final Constructor _fn = (Constructor)fn;
                Span loc = _fn.declaringTrait().getSpan();
                Type selfType = makeTraitType(_fn.declaringTrait(),
                                              staticParamsToArgs(_fn.staticParams()));

                // Invariant: _fn.params().isSome()
                // Otherwise, _fn should not have been in entries.
                overloadedTypes.add(new _RewriteGenericArrowType(loc, _fn.staticParams(),
                                                                 domainFromParams(_fn.params().unwrap()),
                                                                 selfType,
                                                                 makeEffect(loc.getEnd(), _fn.throwsClause()),
                                                                 _fn.where()));
            }
        }
        return Option.some(new BindingLookup(var, makeAndType(overloadedTypes)));
    }

    @Override
    public List<BindingLookup> contents() {
        List<BindingLookup> result = new ArrayList<BindingLookup>();
        for (IdOrOpOrAnonymousName name : entries.firstSet()) {
            Option<BindingLookup> element = binding(name);
            if (element.isSome()) {
                result.add(element.unwrap());
            }
        }
        result.addAll(parent.contents());
        return result;
    }
}
