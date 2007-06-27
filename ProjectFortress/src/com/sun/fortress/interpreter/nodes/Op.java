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

import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.parser.precedence.resolver.PrecedenceMap;

// / type op = string node
public class Op extends AbstractNode {

    String name;

    public Op(Span span, String name) {
        super(span);
        this.name = PrecedenceMap.ONLY.canon(name);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forOp(this);
    }

    Op(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
}
