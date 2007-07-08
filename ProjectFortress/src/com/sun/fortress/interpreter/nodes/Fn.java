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
import com.sun.fortress.interpreter.nodes_util.NodeUtil;
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Option;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.UnitIterable;
import com.sun.fortress.interpreter.useful.Useful;


// / and fn_expr = fn_expr_rec node
// / and fn_expr_rec =
// / {
// / fn_expr_params : param list;
// / fn_expr_return_type : type_ref option;
// / fn_expr_throws : type_ref list;
// / fn_expr_body : expr;
// / }
// /
public class Fn extends ValueExpr implements Decl, Applicable {

    public Fn(Span span, List<Param> params, Option<TypeRef> returnType,
            List<TypeRef> throws_, Expr body) {
        super(span);
        this.params = params;
        this.returnType = returnType;
        this.throws_ = throws_;
        this.body = body;
        // Note that each Fn is sui generis, unless cloned.
        afn = new AnonymousFnName(span);
    }

    public Fn(Span span, List<Param> params, Expr body) {
        this(span,params,new None<TypeRef>(),Collections.<TypeRef>emptyList(), body);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Fn) {
            Fn f = (Fn) o;
            return afn.equals(f.getFnName()) && params.equals(f.getParams())
                    && returnType.equals(f.getReturnType())
                    && throws_.equals(f.getThrows_())
                    && body.equals(f.getBody());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.hashList(params, MagicNumbers.p)
                + MagicNumbers.hashList(throws_, MagicNumbers.t)
                + returnType.hashCode() + body.hashCode() + afn.hashCode();
    }

    AnonymousFnName afn;

    List<Param> params;

    Option<TypeRef> returnType;

    List<TypeRef> throws_;

    Expr body;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFn(this);
    }

    Fn(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
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

    public FnName getFnName() {
        return afn;
    }

    // For the interface.
    public Option<List<StaticParam>> getStaticParams() {
        return new None<List<StaticParam>>();
    }

    public List<WhereClause> getWhere() {
        return Collections.<WhereClause> emptyList();
    }
}
