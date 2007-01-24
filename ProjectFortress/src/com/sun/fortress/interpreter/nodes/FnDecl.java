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

// / and fn_def = fn_def_rec node
// / and fn_def_rec =
// / {
// / fn_def_mods : modifier list;
// / fn_def_name : fn_name;
// / fn_def_type_params : type_param list option;
// / fn_def_params : param list;
// / fn_def_return_type : type_ref option;
// / fn_def_throws : type_ref list;
// / fn_def_where : where_clause list;
// / fn_def_contract : contract;
// / fn_def_body : expr;
// / }

public class FnDecl extends FnDefOrDecl implements Decl, Applicable {

    Expr body;

    Option<Id> optSelfName = new None<Id>();

    public FnDecl(Span s, List<Modifier> mods, FnName name,
            Option<List<StaticParam>> staticParams, List<Param> params,
            Option<TypeRef> returnType, List<TypeRef> throwss,
            List<WhereClause> where, Contract contract, Expr body,
            Option<Id> optSelfName) {
        super(s, mods, name, staticParams, params, returnType, throwss, where,
                contract);
        this.body = body;
        this.optSelfName = optSelfName;
    }

    public FnDecl(Span s, List<Modifier> mods, Option<Id> optSelfName,
            FnName name, Option<List<StaticParam>> staticParams,
            List<Param> params, Option<TypeRef> returnType,
            List<TypeRef> throwss, List<WhereClause> where, Contract contract,
            Expr body) {
        super(s, mods, name, staticParams, params, returnType, throwss, where,
                contract);
        this.body = body;
        this.optSelfName = optSelfName;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFnDecl(this);
    }

    FnDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    @Override
    public String getSelfName() {
        if (optSelfName != null && optSelfName.isPresent()) {
            return optSelfName.getVal().getName();
        } else {
            return super.getSelfName();
        }
    }

}
