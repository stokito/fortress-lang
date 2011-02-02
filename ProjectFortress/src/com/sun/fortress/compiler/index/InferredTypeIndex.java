/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

/**
 * Represents functional and variable indices because they may or may not have
 * types that need to be inferred during type checking.
 */
abstract public class InferredTypeIndex { /* comprises {Functional, Variable} */

    /**
     * A function to evaluate to get the type of this index.
     */
    protected Option<Thunk<Option<Type>>> _thunk = Option.none();

    public boolean hasThunk() {
        return _thunk.isSome();
    }

    public void putThunk(Thunk<Option<Type>> thunk) {
        if (_thunk.isSome()) return;
        _thunk = Option.some(thunk);
    }

    /**
     * This index might have built up some visitors to apply to the inferred
     * type. Apply all of these in order when accessing the inferred type.
     */
    protected Iterable<NodeUpdateVisitor> _thunkVisitors = CollectUtil.emptyList();

    protected void pushVisitor(NodeUpdateVisitor visitor) {
        _thunkVisitors = IterUtil.compose(_thunkVisitors, visitor);
    }

    /**
     * By default, indices have an explicit type. Override in indices that may
     * or may not have an explicit type.
     */
    public boolean hasExplicitType() {
        return true;
    }

    /**
     * Evaluate the thunk to get the inferred type. Then replace the thunk with
     * one that simply gets back the previously evaluated inferred type. After
     * type checking, the result of this method will always be Some(type).
     */
    public Option<Type> getInferredType() {
        if (_thunk.isNone()) return Option.none();
        Option<Type> result = _thunk.unwrap().value();

        // Use the visitors if there are any.
        if (result.isSome() && !IterUtil.isEmpty(_thunkVisitors)) {
            Type type = result.unwrap();
            for (NodeUpdateVisitor visitor : _thunkVisitors) {
                type = (Type) type.accept(visitor);
            }
            _thunkVisitors = CollectUtil.emptyList();
            result = Option.some(type);
        }

        // Store the computed return type.
        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(result));
        return result;
    }

    public abstract Span getSpan();
}
