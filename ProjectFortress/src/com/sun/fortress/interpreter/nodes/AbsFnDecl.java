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

import com.sun.fortress.interpreter.glue.WellKnownNames;

// /
// / and fn_decl = fn_decl_rec node
// / and fn_decl_rec =
// / {
// / fn_decl_mods : modifier list;
// / fn_decl_name : fn_name;
// / fn_decl_type_params : type_param list option;
// / fn_decl_params : param list;
// / fn_decl_return_type : type_ref option;
// / fn_decl_throws : type_ref list;
// / fn_decl_where : where_clause list;
// / fn_decl_contract : contract;
// / }
// /
public class AbsFnDecl extends FnDefOrDecl implements Decl, AbsDecl, Applicable {

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAbsFnDecl(this);
    }

    AbsFnDecl(Span span) {
        super(span);
    }

    Option<Id> optSelfName = new None<Id>();

    public AbsFnDecl(Span s, List<Modifier> mods, Option<Id> optSelfName,
            FnName name, Option<List<StaticParam>> staticParams,
            List<Param> params, Option<TypeRef> returnType,
            List<TypeRef> throwss, List<WhereClause> where, Contract contract) {
        super(s, mods, name, staticParams, params, returnType, throwss, where,
                contract);
        this.optSelfName = optSelfName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.Applicable#getBody()
     */
    public Expr getBody() {
        // Must return a value or NativeApp.checkAndLoadNative will fail.
        return null;
    }
    
   
}
