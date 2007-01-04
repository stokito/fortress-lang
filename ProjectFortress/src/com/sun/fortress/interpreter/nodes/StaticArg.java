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

import java.util.Comparator;

import com.sun.fortress.interpreter.useful.AnyListComparer;


abstract public class StaticArg extends TypeRef {
    // NOTE: This extends TypeRef only because the OCaml AST
    // treats products as typeargs, not natargs. There seems
    // to be an ambiguity here, since syntactically there is
    // not a good way to tell the two apart.

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forStaticArg(this);
    }

    StaticArg(Span span) {
        super(span);
    }

    // Got in a fight with Java generics -- and lost. This seems to get the job
    // done.
    private static final Comparator<StaticArg> typeargComparer = new Comparator<StaticArg>() {
        public int compare(StaticArg o1, StaticArg o2) {
            return ((TypeRef) o1).compareTo(o2);
        }
    };

    public static AnyListComparer<StaticArg> typeargListComparer = new AnyListComparer<StaticArg>(
            typeargComparer);

}

// / and type_arg =
// / [
// / | `StaticArg of type_ref
// / | `NatTypeArg of nat_type
// / | `OprTypeArg of fn_name
// / ] node
// /
