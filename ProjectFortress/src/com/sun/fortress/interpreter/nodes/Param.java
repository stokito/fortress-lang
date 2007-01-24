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

import com.sun.fortress.interpreter.useful.MagicNumbers;


// / and param = param_rec node
// / and param_rec =
// / {
// / param_mods : modifier list;
// / param_name : id;
// / param_type : type_ref option;
// / param_default : expr option;
// / }
// /
public class Param extends Node {
    List<Modifier> mods;

    Id name;

    Option<TypeRef> type;

    Option<Expr> defaultExpr;

    public Param(Span s, List<Modifier> mods, Id name, Option<TypeRef> type,
            Option<Expr> defaultExpr) {
        super(s);
        this.mods = mods;
        this.name = name;
        this.type = type;
        this.defaultExpr = defaultExpr;
    }

    public Param(Span s, List<Modifier> mods, Id name, TypeRef type,
            Expr defaultExpr) {
        this(s, mods, name, new Some<TypeRef>(type),
                new Some<Expr>(defaultExpr));
    }

    public Param(Span s, List<Modifier> mods, Id name, Expr defaultExpr) {
        this(s, mods, name, new None<TypeRef>(), new Some<Expr>(defaultExpr));
    }

    public Param(Span s, List<Modifier> mods, Id name, TypeRef type) {
        this(s, mods, name, new Some<TypeRef>(type), new None<Expr>());
    }

    public Param(Span s, List<Modifier> mods, Id name) {
        this(s, mods, name, new None<TypeRef>(), new None<Expr>());
    }

    @Override
    public int hashCode() {
        return MagicNumbers.hashList(mods, MagicNumbers.m) + name.hashCode()
                * MagicNumbers.n + type.hashCode() * MagicNumbers.t
                * defaultExpr.hashCode() * MagicNumbers.d;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Param) {
            Param p = (Param) o;
            return mods.equals(p.getMods()) && name.equals(p.getName())
                    && type.equals(p.getType())
                    && defaultExpr.equals(p.getDefaultExpr());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(name));
        if (type.isPresent()) {
            sb.append(":");
            sb.append(type.getVal());
        }
        if (defaultExpr.isPresent()) {
            sb.append("=");
            sb.append(defaultExpr.getVal());
        }
        return sb.toString();
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forParam(this);
    }

    Param(Span span) {
        super(span);
    }

    /**
     * @return Returns the defaultExpr.
     */
    public Option<Expr> getDefaultExpr() {
        return defaultExpr;
    }

    /**
     * @return Returns the mods.
     */
    public List<Modifier> getMods() {
        return mods;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    /**
     * @return Returns the type.
     */
    public Option<TypeRef> getType() {
        return type;
    }

    public boolean isTransient() {
        for (Modifier m : mods) {
            if (m instanceof Modifier.Transient) {
                return true;
            }
        }
        return false;
    }

}
