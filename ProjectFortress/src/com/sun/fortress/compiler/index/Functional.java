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

import java.util.List;

import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.lambda.LazyThunk;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

/** Comprises {@link Function} and {@link Method}. */
public abstract class Functional {

    //public abstract Node ast();
    /**
     * Returns a version of this Functional, with params replaced with args.
     * The contract of this method requires
     * that all implementing subtypes must return their own type, rather than a supertype.
     */
    public abstract Functional instantiate(List<StaticParam> params, List<StaticArg> args);

    public abstract Span getSpan();

    public abstract List<StaticParam> staticParameters();

    public abstract List<Param> parameters();

    public abstract List<BaseType> thrownTypes();

    public abstract Option<Expr> body();

    public abstract Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor);
    
    // Lazy return type inference -----

    /**
     * Thunks are used to lazily get the return type of Functionals, as return
     * types may not be available for functionals in the current program during
     * type checking.
     */
    protected Option<Thunk<Option<Type>>> _thunk = Option.none();
    
    public void putThunk(Thunk<Option<Type>> thunk) {
        _thunk = Option.<Thunk<Option<Type>>>some(LazyThunk.make(thunk));
    }
    
    /**
     * Evaluate the thunk to get a return type. Then replace the thunk with one
     * that simply gets back the previously evaluated return type. After type
     * checking, the result of this method will always be Some(type).
     */
    public Option<Type> getReturnType() {
        if (_thunk.isNone()) return Option.none();
        return _thunk.unwrap().value();
    }
}
