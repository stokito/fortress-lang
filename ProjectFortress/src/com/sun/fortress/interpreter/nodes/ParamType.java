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

import com.sun.fortress.interpreter.useful.Useful;


// / and param_type = param_type_rec node
// / and param_type_rec =
// / {
// / param_type_generic : type_ref;
// / param_type_args : type_arg list;
// / }
// /
public class ParamType extends TypeRef {
    TypeRef generic;

    List<StaticArg> args;

    public ParamType(Span s, TypeRef generic, List<StaticArg> args) {
        super(s);
        this.generic = generic;
        this.args = args;
    }

    @Override
    public String toString() {
        return generic + Useful.listInOxfords(args);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forParamType(this);
    }

    ParamType(Span span) {
        super(span);
    }

    /**
     * @return Returns the args.
     */
    public List<StaticArg> getArgs() {
        return args;
    }

    /**
     * @return Returns the generic.
     */
    public TypeRef getGeneric() {
        return generic;
    }

    public static ParamType make(String string, List<StaticArg> args) {
        ParamType pt = new ParamType(new Span());
        pt.generic = new IdType(new Span(), new DottedId(new Span(), string));
        pt.args = args;

        return pt;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        ParamType x = (ParamType) o;
        int c = generic.compareTo(x.generic);
        if (c != 0) {
            return c;
        }
        return StaticArg.typeargListComparer.compare(args, x.args);
    }
}
