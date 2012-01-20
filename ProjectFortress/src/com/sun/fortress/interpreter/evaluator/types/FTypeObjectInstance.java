/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;
import java.util.Set;


public class FTypeObjectInstance extends FTypeObject implements GenericTypeInstance {

    public FTypeObjectInstance(String name,
                               Environment interior,
                               FTypeGeneric generic,
                               List<FType> bind_args,
                               List<FType> name_args,
                               Option<List<Param>> params,
                               List<Decl> members) {
        super(name, interior, interior.getAt(), params, members, generic.getDecl());
        this.generic = generic;
        this.bind_args = bind_args;
        this.name_args = name_args;
    }

    final private FTypeGeneric generic;
    final private List<FType> bind_args;
    final private List<FType> name_args;

    @Override
    public FTypeGeneric getGeneric() {
        return generic;
    }

    @Override
    public List<FType> getTypeParams() {
        return bind_args;
    }

    @Override
    public List<FType> getTypeParamsForName() {
        return name_args;
    }

    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.Type)
     */
    @Override
    protected boolean unifyNonVar(Environment unify_env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        return unifyNonVarGeneric(unify_env, tp_set, abm, val);
    }


}
