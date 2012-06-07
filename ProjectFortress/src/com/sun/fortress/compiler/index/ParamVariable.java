/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.lambda.SimpleBox;
import edu.rice.cs.plt.lambda.Thunk;
import edu.rice.cs.plt.tuple.Option;

public class ParamVariable extends Variable {
    private Param _ast;

    public ParamVariable(Param ast) {
        _ast = ast;

        // ParamVariable should _always_ have a type.
        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(NodeUtil.optTypeOrPatternToType(_ast.getIdType())));
    }

    public Param ast() {
        return _ast;
    }

    public Modifiers modifiers() {
        return _ast.getMods();
    }

    public boolean mutable() {
        return false;
    }

    @Override
    public String toString() {
        return _ast.toString();
    }

    @Override
    public Span getSpan() {
        return NodeUtil.getSpan(_ast);
    }
}
