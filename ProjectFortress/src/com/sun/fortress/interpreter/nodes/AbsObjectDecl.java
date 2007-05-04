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

// / and object_decl = object_decl_rec node
// / and object_decl_rec =
// / {
// / object_decl_mods : modifier list;
// / object_decl_name : id;
// / object_decl_type_params : type_param list option;
// / object_decl_params : param list option;
// / object_decl_traits : type_ref list option;
// / object_decl_throws : type_ref list;
// / object_decl_where : where_clause list;
// / object_decl_contract : contract;
// / object_decl_decls : decl list;
// / }
// /
public class AbsObjectDecl extends ObjectDefOrDecl implements GenericDefOrDeclWithParams, AbsDecl {
    List<? extends DefOrDecl> decls;

    public AbsObjectDecl(Span span, List<Modifier> mods, Id name,
            Option<List<StaticParam>> staticParams, Option<List<Param>> params,
            Option<List<TypeRef>> traits, List<TypeRef> throws_,
            List<WhereClause> where, Contract contract,
            List<? extends DefOrDecl> decls) {
        super(span, mods, name, staticParams, params, traits, throws_, where,
                contract);
        this.decls = decls;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAbsObjectDecl(this);
    }

    AbsObjectDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the decls.
     */
    @Override
    public List<? extends DefOrDecl> getDefOrDecls() {
        return decls;
    }
}
