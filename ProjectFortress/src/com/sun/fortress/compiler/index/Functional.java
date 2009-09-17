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
import com.sun.fortress.nodes_util.Modifiers;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

/**
 * Comprises {@link Function} and {@link Method}.
 */
public abstract class Functional extends InferredTypeIndex {

    //public abstract Node ast();

    public abstract List<StaticParam> staticParameters();

    public abstract List<Param> parameters();

    public abstract List<BaseType> thrownTypes();

    public abstract Modifiers mods();

    public abstract Option<Expr> body();

    public abstract Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor);

    public abstract IdOrOp name();

    public String toString() {
        StringBuffer sb = new StringBuffer();

        // Append the name.
        sb.append(name().getText());

        // Append static params, if any.
        List<StaticParam> staticParams = staticParameters();
        if (!staticParams.isEmpty()) {
            sb.append(IterUtil.toString(staticParams, "[\\", ", ", "\\]"));
        }

        // Append parameters.
        List<Param> params = parameters();
        sb.append(IterUtil.toString(params, "(", ", ", ")"));

        // Append return type.
        if (hasDeclaredReturnType()) sb.append(":" + getReturnType().unwrap());

        return sb.toString();
    }

    public Option<Type> getReturnType() {
        return getInferredType();
    }

    /**
     * Override in indices for functions that may have a return type.
     */
    public boolean hasDeclaredReturnType() {
        return false;
    }
    
    public boolean hasExplicitType() {
        return hasDeclaredReturnType();
    }
}
