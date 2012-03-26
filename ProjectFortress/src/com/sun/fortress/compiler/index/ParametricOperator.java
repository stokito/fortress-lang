/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.lambda.SimpleBox;

import java.util.List;

/**
 * This is a operator declared in a trait or object whose name is an opr parameter.
 * It needs to be promoted to top-level so it can match OpExprs appropriately.
 */
public class ParametricOperator extends FunctionalMethod {
    Op _name;

    public ParametricOperator(FnDecl ast, TraitObjectDecl traitDecl, List<StaticParam> traitParams) {
        super(ast, traitDecl, traitParams);
        _name = (Op) NodeUtil.getName(ast);
        putThunk(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    public ParametricOperator(ParametricOperator that, List<StaticParam> params, StaticTypeReplacer visitor) {
        super(that, params, visitor);
    }

    @Override
    public Op name() {
        return _name;
    }

}
