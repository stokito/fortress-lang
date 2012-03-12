/*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

/**
 * Comprises {@link Function} and {@link Method}.
 */
public abstract class Functional extends InferredTypeIndex {

    // public abstract Node ast();

    public abstract List<StaticParam> staticParameters();

    public abstract List<Param> parameters();

    public abstract List<Type> thrownTypes();

    public abstract Modifiers mods();

    public abstract Option<Expr> body();

    // public abstract Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor);

    public abstract IdOrOp name();

    /* Added this to help with overloading; wish I knew what it meant,
     * beyond "overloading uses it".
     */
    public abstract IdOrOpOrAnonymousName toUndecoratedName();

    public abstract IdOrOp unambiguousName();

    public String toString() {
        StringBuilder sb = new StringBuilder();

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
    
    // Copy a static parameter but make it lifted.
    static final protected Lambda<StaticParam, StaticParam> liftStaticParam = new Lambda<StaticParam, StaticParam>() {
        public StaticParam value(StaticParam that) {
            return new StaticParam(that.getInfo(), that.getVariance(), that.getName(), that.getExtendsClause(),that.getDominatesClause(), that.getDimParam(), that.isAbsorbsParam(), that.getKind(), true);
        }
    };
}
