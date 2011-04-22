/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
