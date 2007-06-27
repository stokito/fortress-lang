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
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import java.util.Collections;
import java.util.List;

// / and trait_def = trait_def_rec node
// / and trait_def_rec =
// / {
// / trait_def_mods : modifier list;
// / trait_def_name : id;
// / trait_def_type_params : type_param list option;
// / trait_def_extends : type_ref list option;
// / trait_def_excludes : type_ref list;
// / trait_def_bounds : type_ref list option;
// / trait_def_where : where_clause list;
// / trait_def_fns : fn_def_or_decl list;
// / }
// /
public class TraitDecl extends TraitDefOrDecl implements GenericDef {

    List<? extends DefOrDecl> fns;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTraitDecl(this);
    }

    public TraitDecl(Span span, List<Modifier> mods, Id name,
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

    static public TraitDecl make(String name, List<StaticParam> typeParameters) {
        // List<Modifier> mods;
        // Id name;
        // Option<List<StaticParam>> staticParams;
        // Option<List<TypeRef>> extends_;
        // List<TypeRef> excludes;
        // Option<List<TypeRef>> bounds;
        // List<WhereClause> wheres;
        TraitDecl td = new TraitDecl(new Span());
        td.mods = Collections.emptyList();
        td.name = new Id(new Span(), name);
        td.staticParams = new Some<List<StaticParam>>(typeParameters);
        td.extends_ = new None<List<TypeRef>>();
        td.bounds = new None<List<TypeRef>>();
        td.wheres = Collections.emptyList();
        td.fns = Collections.emptyList();
        return td;
    }

    TraitDecl(Span span) {
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
