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
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.ABoundingMap;
import com.sun.fortress.interpreter.useful.NI;


/**
 * An FTypeTraitInstance is exactly like an FTypeTrait,
 * except that it was created by instantiating a generic
 * type, and it knows how it was instantiated.  This is
 * helpful in the implementation of one-sided unification
 * of generic operators/functions with their parameter
 * lists.
 */
public class FTypeTraitInstance extends FTypeTrait implements GenericTypeInstance {

    public FTypeTraitInstance(String name, BetterEnv interior, FTypeGeneric generic, List<FType> args) {
        super(name, interior, interior.getAt());
        this.generic = generic;
        this.args = args;
   }

    final private FTypeGeneric generic;
    final private List<FType> args;

    public FTypeGeneric getGeneric() { return generic; }
    public List<FType> getTypeParams() { return args; }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unify(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap, com.sun.fortress.interpreter.nodes.TypeRef)
     */
    @Override
    public void unify(BetterEnv e, Set<StaticParam> tp_set, ABoundingMap<String, FType, TypeLatticeOps> abm, TypeRef val) {
        // TODO Auto-generated method stub
        // System.out.println("Unify "+this+"\nwith "+val);
        NI.nyi();
        super.unify(e, tp_set, abm, val);
    }

}
