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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.IterableOnce;


// / and lvalue_bind = lvalue_bind_rec node
// / and lvalue_bind_rec =
// / {
// / lvalue_bind_name : id;
// / lvalue_bind_type : type_ref option;
// / }
// /
public class LValueBind extends LValue implements LHS {
    Id name;

    Option<TypeRef> type;

    List<Modifier> mods = Collections.emptyList();

    boolean mutable = false;

    public static final Fn<LValueBind, String> toStringFn = new Fn<LValueBind, String>() {

        @Override
        public String apply(LValueBind x) {
            return x.getName().getName();
        }

    };

    public LValueBind(Span span, Id name, Option<TypeRef> type) {
        super(span);
        this.name = name;
        this.type = type;
    }

    public LValueBind(Span span, Id name, Option<TypeRef> type,
            List<Modifier> mods, boolean mutable) {
        super(span);
        this.name = name;
        this.type = type;
        this.mods = mods;
        this.mutable = mutable;
    }

    public LValueBind(Span span, Id name, Option<TypeRef> type,
            List<Modifier> mods) {
        super(span);
        this.name = name;
        this.type = type;
        this.mods = mods;
        this.mutable = false;
        Modifier var = new Modifier.Var(new Span());
        Modifier settable = new Modifier.Settable(new Span());
        for (Iterator<Modifier> i = mods.iterator(); i.hasNext();) {
            if (i.next().getClass().equals(var.getClass())
                    || i.next().getClass().equals(settable.getClass())) {
                this.mutable = true;
            }
        }
    }

    LValueBind(Span span) {
        super(span);
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forLValueBind(this);
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

    public void setType(TypeRef ty) {
        type = new Some<TypeRef>(ty);
    }

    public List<Modifier> getMods() {
        return mods;
    }

    public boolean getMutable() {
        return mutable;
    }

    public void setMutable() {
        mutable = true;
    }

    @Override
    public IterableOnce<String> stringNames() {
        return new com.sun.fortress.interpreter.useful.UnitIterable<String>(getName().getName());
    }
}
