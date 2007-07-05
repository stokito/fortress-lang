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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.*;

// / type fn_name = fn_name_variant node
// /

// Note well; because this is a com.sun.fortress.interpreter.useful abstraction for the
// generalized names seen in Fortress, it will persist into
// more semantically aware parts of the system (i.e., into
// the interpreter, compiler, com.sun.fortress.interpreter.typechecker, etc).
public abstract class FnName extends AbstractNode {
    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.Node#accept(com.sun.fortress.interpreter.nodes.NodeVisitor)
     */
    @Override
    public <T> T accept(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return null;
    }

    FnName(Span span) {
        super(span);
    }

}
