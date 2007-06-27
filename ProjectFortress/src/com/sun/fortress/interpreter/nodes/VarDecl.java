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

import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.Useful;


// / and var_def = var_def_rec node
// / and var_def_rec =
// / {
// / var_def_mods : modifier list;
// / var_def_name : id;
// / var_def_type : type_ref option;
// / var_def_init : expr;
// / }
// /
public class VarDecl extends VarDefOrDecl implements Decl {
    // Option<TypeRef> type;

    Expr init;

    public VarDecl(Span s, List<LValue> lhs, Expr init) {
        super(s);
        this.lhs = lhs;
        this.init = init;
    }

    public VarDecl(Span s, Id name, List<Modifier> mods, Option<TypeRef> type,
            Expr init) {
        super(s);
        super.init(name, type, mods, true);
        this.init = init;
    }

    public VarDecl(Span s, Id name, List<Modifier> mods, TypeRef type, Expr init) {
        this(s, name, mods, new Some<TypeRef>(type), init);
    }

    public VarDecl(Span s, Id name, TypeRef type, Expr init) {
        this(s, name, Collections.<Modifier> emptyList(), new Some<TypeRef>(
                type), init);
    }

    public VarDecl(Span s, Id name, List<Modifier> mods, Expr init) {
        this(s, name, mods, new None<TypeRef>(), init);
    }

    public VarDecl(Span s, Id name, Expr init) {
        this(s, name, Collections.<Modifier> emptyList(), new None<TypeRef>(),
                init);
    }

    @Override
    public String toString() {
        return Useful.listInParens(getLhs())+"="+init+getSpan();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VarDecl) {
            VarDecl v = (VarDecl) o;
            return init.equals(v.getInit()) && superEquals(v);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.D * init.hashCode() + superHashCode();
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forVarDecl(this);
    }

    VarDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the init.
     */
    public Expr getInit() {
        return init;
    }

    // /**
    // * @return Returns the type.
    // */
    // public Option<TypeRef> getType() {
    // return type;
    // }
}
