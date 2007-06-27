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

// / and object_def = object_def_rec node
// / and object_def_rec =
// / {
// / object_def_mods : modifier list;
// / object_def_name : id;
// / object_def_type_params : type_param list option;
// / object_def_params : param list option;
// / object_def_traits : type_ref list option;
// / object_def_throws : type_ref list;
// / object_def_where : where_clause list;
// / object_def_contract : contract;
// / object_def_defs : def list;
// / }
// /
public class ObjectDecl extends ObjectDefOrDecl implements GenericDefWithParams {
    List<? extends DefOrDecl> defs;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forObjectDecl(this);
    }

    ObjectDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the defs.
     */
    @Override
    public List<? extends DefOrDecl> getDefOrDecls() {
        return defs;
    }

    public static ObjectDecl make(List<Decl> defs2, List<Modifier> mods,
            Id name, Option<List<StaticParam>> staticParams,
            Option<List<Param>> params, Option<List<TypeRef>> traits,
            List<TypeRef> throws_, List<WhereClause> where, Contract contract) {
        return new ObjectDecl(defs2, mods, name, staticParams, params, traits,
                throws_, where, contract);
    }

    public ObjectDecl(Span span, List<Modifier> mods, Id name,
            Option<List<StaticParam>> staticParams, Option<List<Param>> params,
            Option<List<TypeRef>> traits, List<TypeRef> throws_,
            List<WhereClause> where, Contract contract,
            List<? extends DefOrDecl> defs) {
        super(span, mods, name, staticParams, params, traits, throws_, where,
                contract);
        this.defs = defs;
    }

    public ObjectDecl(List<Decl> defs2, List<Modifier> mods, Id name,
            Option<List<StaticParam>> staticParams, Option<List<Param>> params,
            Option<List<TypeRef>> traits, List<TypeRef> throws_,
            List<WhereClause> where, Contract contract) {
        super(new Span(), mods, name, staticParams, params, traits, throws_,
                where, contract);
        defs = defs2;
    }
}
