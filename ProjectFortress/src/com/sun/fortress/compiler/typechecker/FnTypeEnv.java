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
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * A type environment whose outermost lexical scope consists of a map from IDs
 * to Variables.
 */
class FnTypeEnv extends TypeEnv {
    private Relation<SimpleName, ? extends Function> entries;
    private TypeEnv parent;

    FnTypeEnv(Relation<SimpleName, ? extends Function> _entries, TypeEnv _parent) {
        entries = _entries;
        parent = _parent;
    }

    /**
     * Return a BindingLookup that binds the given Id to a type
     * (if the given Id is in this type environment).
     */
    public Option<BindingLookup> binding(SimpleName var) {
        Set<? extends Function> fns = entries.getSeconds(var);
        if (fns.isEmpty()) { return parent.binding(var); }
        
        Type type = Types.ANY;
        for (Function fn: fns) {
            if (fn instanceof DeclaredFunction) {
                DeclaredFunction _fn = (DeclaredFunction)fn;
                FnAbsDeclOrDecl decl = _fn.ast();
                type = new AndType(type,
                                   makeGenericArrowType(decl.getSpan(),
                                                        decl.getStaticParams(),
                                                        typeFromParams(decl.getParams()),
                                                        unwrap(decl.getReturnType()), // all types have been filled in at this point
                                                        decl.getThrowsClause(),
                                                        decl.getWhere()));
            } else if (fn instanceof FunctionalMethod) {
                FunctionalMethod _fn = (FunctionalMethod)fn;
                FnAbsDeclOrDecl decl = _fn.ast();
                type = new AndType(type,
                                   makeGenericArrowType(decl.getSpan(),
                                                        decl.getStaticParams(),
                                                        typeFromParams(decl.getParams()),
                                                        unwrap(decl.getReturnType()), // all types have been filled in at this point
                                                        decl.getThrowsClause(),
                                                        decl.getWhere()));
            } else { // fn instanceof Constructor
                final Constructor _fn = (Constructor)fn;

                // Invariant: _fn.params().isSome()
                // Otherwise, _fn should not have been in entries.
                type = new AndType(type,
                                   makeGenericArrowType(_fn.declaringTrait().getSpan(),
                                                        _fn.staticParams(),
                                                        typeFromParams(unwrap(_fn.params())),
                                                        makeInstantiatedType(makeQualifiedIdName(_fn.declaringTrait()),
                                                                             staticParamsToArgs(_fn.staticParams())),
                                                        _fn.throwsClause(),
                                                        _fn.where()));
                
            }
        }
        return some(new BindingLookup(var, type));
    }
}
