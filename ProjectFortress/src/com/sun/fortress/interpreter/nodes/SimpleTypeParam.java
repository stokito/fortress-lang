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
import java.util.List;

// / and simple_type_param = simple_type_param_rec node
// / and simple_type_param_rec =
// / {
// / simple_type_param_name : id;
// / simple_type_param_extends : type_ref list option;
// / }
// /
public class SimpleTypeParam extends StaticParam {
    Id name;

    Option<List<TypeRef>> extends_;

    boolean absorbs;

    public SimpleTypeParam(Span s, Id name, Option<List<TypeRef>> extends_,
            boolean absorbs) {
        super(s);
        this.name = name;
        this.extends_ = extends_;
        this.absorbs = absorbs;
    }

    static public SimpleTypeParam make(String s) {
        SimpleTypeParam stp = new SimpleTypeParam(new Span());
        stp.name = new Id(new Span(), s);
        stp.extends_ = new None<List<TypeRef>>();
        stp.absorbs = false;
        return stp;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forSimpleTypeParam(this);
    }

    SimpleTypeParam(Span span) {
        super(span);
    }

    /**
     * @return Returns the extends_.
     */
    public Option<List<TypeRef>> getExtends_() {
        return extends_;
    }

    /**
     * @return Returns the name.
     */
    public Id getId() {
        return name;
    }

    @Override
    public String getName() {
        return name.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.StaticParam#subtypeCompareTo(com.sun.fortress.interpreter.nodes.StaticParam)
     */
    @Override
    int subtypeCompareTo(StaticParam p) {
        SimpleTypeParam o = (SimpleTypeParam) p;
        int x = getName().compareTo(o.getName());
        if (x != 0) {
            return x;
        }
        if (absorbs != o.absorbs) {
            return absorbs ? 1 : -1;
        }
        if (extends_.isPresent() != o.extends_.isPresent()) {
            return extends_.isPresent() ? 1 : -1;
        }
        if (!extends_.isPresent()) {
            return 0;
        }
        List<TypeRef> l = extends_.getVal();
        List<TypeRef> ol = o.extends_.getVal();

        return TypeRef.listComparer.compare(l, ol);

    }

}
