/*******************************************************************************
 Copyright 2007 Sun Microsystems, Inc.,
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
        _thunk = Option.<Thunk<Option<Type>>>some(SimpleBox.make(_ast.getIdType()));
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
