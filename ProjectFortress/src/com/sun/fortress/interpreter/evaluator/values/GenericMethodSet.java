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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.nodes.FnName;


/**
 * A generic method set can be either instantiated, or (incorrectly, but
 * this is required for now) directly applied.
 *
 * Instantiation creates a method closure or trait method closure.
 * Application performs an inference (!) on the types to attempt
 * to match when of the methods in the set.
 *
 * ACTUALLY, it is probably the case that these are never directly instantiated
 * in this form, because any object must necessarily sort out generic methods
 * from all the traits that it extends.
 */
public class GenericMethodSet extends GenericFunctionOrMethodSet<GenericMethod>
{
   
    public GenericMethodSet(FnName name, BetterEnv within) {
        super(name, within, GenericMethod.genFullComparer);
    }

    public boolean isMethod(){
        return true;
    }


}
