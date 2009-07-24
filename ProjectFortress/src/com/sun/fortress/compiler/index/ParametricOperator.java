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
import com.sun.fortress.nodes_util.NodeUtil;
import edu.rice.cs.plt.lambda.SimpleBox;

import java.util.List;

/**
 * This is a operator declared in a trait or object whose name is an opr parameter.
 * It needs to be promoted to top-level so it can match OpExprs appropriately.
 */
public class ParametricOperator extends FunctionalMethod {
    Op _name;

    public ParametricOperator(FnDecl ast, Id declaringTrait, List<StaticParam> traitParams) {
        super(ast, declaringTrait, traitParams);
        _name = (Op) NodeUtil.getName(ast);
        putThunk(SimpleBox.make(NodeUtil.getReturnType(_ast)));
    }

    @Override
    public Op name() {
        return _name;
    }

    @Override
    public Functional acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
        return new ParametricOperator((FnDecl) this.ast().accept(visitor), this._declaringTrait, this._traitParams);
    }
}
