/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;

import java.util.Set;

/**
 * Various things that may appear as a "nat" parameter to a
 * generic -- integers, variables (other nat? general??),
 * sums, products.
 */
abstract public class FTypeNat extends FType {

    protected FTypeNat(String s) {
        super(s);
        // TODO Auto-generated constructor stub
    }

    public final boolean isValueType() {
        return true;
    }

    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.Type)
     */
    @Override
    protected boolean unifyNonVar(Environment env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        return FType.unifySymbolic(this, env, tp_set, abm, val);
    }

}
