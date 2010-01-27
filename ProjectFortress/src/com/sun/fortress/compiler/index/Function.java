/*******************************************************************************
 Copyright 2010 Sun Microsystems, Inc.,
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

import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.StaticParam;
import edu.rice.cs.plt.lambda.Lambda;

/**
 * Comprises Constructor, DeclaredFunction, FunctionalMethod, and Coercion.
 */
public abstract class Function extends Functional {

    public abstract IdOrOpOrAnonymousName toUndecoratedName();

    // Copy a static parameter but make it lifted.
    static final protected Lambda<StaticParam, StaticParam> liftStaticParam = new Lambda<StaticParam, StaticParam>() {
        public StaticParam value(StaticParam that) {
            return new StaticParam(that.getInfo(), that.getName(), that.getExtendsClause(), that.getDimParam(), that.isAbsorbsParam(), that.getKind(), true);
        }
    };
}
