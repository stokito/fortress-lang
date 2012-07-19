/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

/**
 * Represents the variable produced by a singleton object declaration.
 */
public class SingletonVariable extends Variable {
    private final Id _declaringTrait;

    public SingletonVariable(Id declaringTrait) {
        _declaringTrait = declaringTrait;

        Type singletonType = NodeFactory.makeVarType(NodeUtil.getSpan(_declaringTrait), _declaringTrait);
        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(Option.some(singletonType)));
    }

    public Id declaringTrait() {
        return _declaringTrait;
    }

    public Modifiers modifiers() {
        // TODO: verify!
        return Modifiers.None;
    }

    public boolean mutable() {
        // TODO: verify!
        return false;
    }

    @Override
    public String toString() {
        return _declaringTrait.getText();
    }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_declaringTrait);
    }
}
