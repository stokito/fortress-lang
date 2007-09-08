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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;


/**
 * An FTypeTraitInstance is exactly like an FTypeTrait,
 * except that it was created by instantiating a generic
 * type, and it knows how it was instantiated.  This is
 * helpful in the implementation of one-sided unification
 * of generic operators/functions with their parameter
 * lists.
 */
public class FTypeTraitInstance extends FTypeTrait implements GenericTypeInstance {

    public FTypeTraitInstance(String name, BetterEnv interior, FTypeGeneric generic, List<FType> bind_args, List<FType> name_args, List<? extends AbsDeclOrDecl> members) {
        super(name, interior, interior.getAt(), members);
        this.generic = generic;
        this.bind_args = bind_args;
        this.name_args = name_args;
   }

    final private FTypeGeneric generic;
    final private List<FType> bind_args;
    final private List<FType> name_args;

    @Override
    public FTypeGeneric getGeneric() { return generic; }

    @Override
    public List<FType> getTypeParams() { return bind_args; }

    @Override
    public List<FType> getTypeParamsForName() { return name_args; }
/*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.Type)
     */
    @Override
    protected Boolean unifyNonVar(BetterEnv unify_env, Set<StaticParam> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
        return new Boolean(unifyNonVarGeneric(unify_env,tp_set,abm,val));
    }

}
