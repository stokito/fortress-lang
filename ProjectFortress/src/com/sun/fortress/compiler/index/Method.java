/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;

import java.util.List;

import edu.rice.cs.plt.tuple.Option;

/** Comprises DeclaredMethod, FieldGetterMethod, and FieldSetterMethod. */
public abstract class Method extends Functional {
    public abstract Node ast();
    public abstract Id getDeclaringTrait();
    
    /**
     * Returns a version of this Functional, with params replaced with args.
     * The contract of this method requires
     * that all implementing subtypes must return their own type, rather than a supertype.
     */
    public abstract Method instantiate(List<StaticParam> params, List<StaticArg> args);

    @Override
    public String toString() {
        return String.format("%s.%s",
                             getDeclaringTrait().getText(),
                             super.toString());
    }
}
