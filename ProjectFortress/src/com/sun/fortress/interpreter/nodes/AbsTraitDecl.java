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

// / and trait_decl = trait_decl_rec node
// / and trait_decl_rec =
// / {
// / trait_decl_mods : modifier list;
// / trait_decl_name : id;
// / trait_decl_type_params : type_param list option;
// / trait_decl_extends : type_ref list option;
// / trait_decl_excludes : type_ref list;
// / trait_decl_bounds : type_ref list option;
// / trait_decl_where : where_clause list;
// / trait_decl_fns : fn_decl list;
// / }
// /
public class AbsTraitDecl extends TraitDefOrDecl implements GenericDefOrDecl, AbsDecl {
    List<? extends DefOrDecl> fns;

    public AbsTraitDecl(Span span, List<Modifier> mods, Id name,
            Option<List<StaticParam>> staticParams,
            Option<List<TypeRef>> extends_, List<TypeRef> excludes,
            Option<List<TypeRef>> bounds, List<WhereClause> wheres,
            List<? extends DefOrDecl> fns) {
        super(span);
        this.span = span;
        this.mods = mods;
        this.name = name;
        this.staticParams = staticParams;
        this.extends_ = extends_;
        this.excludes = excludes;
        this.bounds = bounds;
        this.wheres = wheres;
        this.fns = fns;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAbsTraitDecl(this);
    }

    AbsTraitDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the fns.
     */
    @Override
    public List<? extends DefOrDecl> getFns() {
        return fns;
    }

}
