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

// / and field_selection = field_selection_rec node
// / and field_selection_rec =
// / {
// / field_selection_obj : expr;
// / field_selection_name : id;
// / }
// /
public class FieldSelection extends Expr implements LHS {

    Expr obj;

    Id id;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFieldSelection(this);
    }

    FieldSelection(Span span) {
        super(span);
    }

    /**
     * @return Returns the id.
     */
    public Id getId() {
        return id;
    }

    /**
     * @return Returns the obj.
     */
    public Expr getObj() {
        return obj;
    }

    public FieldSelection(Span original, Expr o, Id i) {
        super(original);
        obj = o;
        id = i;
    }

    /**
     * Used when rewriting an existing node into a field selection.
     *
     * @param original
     * @param o
     * @param i
     */
    public FieldSelection(Expr original, Expr o, Id i) {
        super(original);
        obj = o;
        id = i;
    }
}
