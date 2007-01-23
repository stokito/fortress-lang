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

import com.sun.fortress.interpreter.useful.Fn;

// / type id = string node
public class Id extends Node implements Comparable<Id> {
    public static final Fn<Id, String> toStringFn = new Fn<Id, String>() {

        @Override
        public String apply(Id x) {
            return x.getName();
        }

    };

    String name;

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Id)) {
            return false;
        }
        Id i = (Id) o;
        return name.equals(i.name);
    }

    public Id(Span span, String s) {
        super(span);
        name = s;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forId(this);
    }

    Id(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static Id make(String string) {
        return make(new Span(), string);
    }

    public static Id make(Span s, String string) {
        return new Id(s, string);
    }

    public int compareTo(Id o) {
        return name.compareTo(o.name);
    }
}
