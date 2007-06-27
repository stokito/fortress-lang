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
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.UnitIterable;


// / and type_alias = type_alias_rec node
// / and type_alias_rec =
// / {
// / type_alias_name : id;
// / type_alias_type : type_ref;
// / }
// /
public class TypeAlias extends WhereClause implements Decl {
    Id name;

    // This field must go away after getting rid of the OCaml com.sun.fortress.interpreter.parser!!!
    List<Id> params;

    List<StaticParam> sparams;

    TypeRef type;

    public TypeAlias(Span span, Id name, List<StaticParam> sparams, TypeRef type) {
        super(span);
        this.name = name;
        this.sparams = sparams;
        this.type = type;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTypeAlias(this);
    }

    TypeAlias(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    @Override
    public String stringName() {
        return name.getName();
    }

    /**
     * @return Returns the type.
     */
    public TypeRef getType() {
        return type;
    }

    /**
     * @return Returns the static parameters.
     */
    public List<StaticParam> getStaticParams() {
        return sparams;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#stringNames()
     */
    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(stringName());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TypeAlias) {
            TypeAlias t = (TypeAlias) o;
            return name.equals(t.getName())
                    && sparams.equals(t.getStaticParams())
                    && type.equals(t.getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.e + name.hashCode() * MagicNumbers.a
                + MagicNumbers.hashList(sparams, MagicNumbers.w)
                * MagicNumbers.r + type.hashCode() * MagicNumbers.w;
    }
}
