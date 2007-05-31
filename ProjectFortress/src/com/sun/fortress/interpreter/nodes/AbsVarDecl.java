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

import java.util.List;
import com.sun.fortress.interpreter.useful.Useful;

// / and var_decl = var_decl_rec node
// / and var_decl_rec =
// / {
// / var_decl_mods : modifier list;
// / var_decl_name : id;
// / var_decl_type : type_ref;
// / }
// /
public class AbsVarDecl extends VarDefOrDecl implements AbsDecl {

    public AbsVarDecl(Span s, List<LValue> lhs) {
        super(s);
        this.lhs = lhs;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAbsVarDecl(this);
    }

    AbsVarDecl(Span span) {
        super(span);
    }

    @Override
    public String toString() {
        return "abs "+Useful.listInParens(getLhs())+getSpan();
    }

    /**
     * @return Returns the type.
     */
    // public TypeRef getType() {
    // return type.getVal();
    // }
}
