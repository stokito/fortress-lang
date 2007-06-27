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
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.IterableOnce;

public interface DefOrDecl extends HasAt, NodeVisitorHost {
    // public String stringName();
    public IterableOnce<String> stringNames();

    public String at(); // See Node.at()

    public Span getSpan();

    abstract public <T> T accept(NodeVisitor<T> v);

        /**
         * Returns the index of the 'self' parameter in the list,
         * or -1 if it does not appear.
         */
    public int selfParameterIndex();
}

// / and def_or_decl =
// / [
// / | `Dimension of id
// / | `UnitVar of unit_var
// / | `TypeAlias of type_alias
// / ] node
// /
