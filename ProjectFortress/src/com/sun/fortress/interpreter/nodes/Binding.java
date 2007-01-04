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

// / and binding = binding_rec node
// / and binding_rec =
// / {
// / binding_name : id;
// / binding_init : expr;
// / }
// /
public class Binding extends Tree {

    Id name;

    Expr init;

    public Binding(Span span, Id name, Expr init) {
        super(span);
        this.name = name;
        this.init = init;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forBinding(this);
    }

    Binding(Span span) {
        super(span);
    }

    /**
     * @return Returns the init.
     */
    public Expr getInit() {
        return init;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }
}
