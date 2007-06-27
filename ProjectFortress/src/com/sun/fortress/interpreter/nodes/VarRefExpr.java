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
import com.sun.fortress.interpreter.useful.MagicNumbers;

public class VarRefExpr extends Expr implements LHS {
    Id var;

    public VarRefExpr(Span span, String s) {
        super(span);
        var = new Id(span, s);
    }

    public VarRefExpr(Span span, Id var) {
        super(span);
        this.var = var;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VarRefExpr) {
            VarRefExpr vre = (VarRefExpr) o;
            return var.equals(vre.getVar());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return var.hashCode() * MagicNumbers.V;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forVarRefExpr(this);
    }

    VarRefExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the var.
     */
    public Id getVar() {
        return var;
    }
}
