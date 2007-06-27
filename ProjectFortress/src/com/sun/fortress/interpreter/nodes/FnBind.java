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

import com.sun.fortress.interpreter.useful.Option;
import java.util.List;

// / and fn_bind = fn_bind_rec node
// / and fn_bind_rec =
// / {
// / fn_bind_name : id;
// / fn_bind_params : param list;
// / fn_bind_return_type : type_ref option;
// / fn_bind_throws : type_ref list;
// / fn_bind_body : expr;
// / }
// /
public class FnBind extends Node {

    Id name;

    List<Param> params;

    Option<TypeRef> returnType;

    List<TypeRef> throws_;

    Expr body;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFnBind(this);
    }

    FnBind(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    /**
     * @return Returns the params.
     */
    public List<Param> getParams() {
        return params;
    }

    /**
     * @return Returns the returnType.
     */
    public Option<TypeRef> getReturnType() {
        return returnType;
    }

    /**
     * @return Returns the throws_.
     */
    public List<TypeRef> getThrows_() {
        return throws_;
    }
}
