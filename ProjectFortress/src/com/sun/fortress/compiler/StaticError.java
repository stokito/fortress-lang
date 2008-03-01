/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;

public abstract class StaticError extends RuntimeException implements HasAt {
    public static String errorMsg(Object... messages) {
        return ErrorMsgMaker.errorMsg(messages);
    }

    public abstract String description();

    public abstract String at();

    public String stringName() { return toString(); }

    public String getMessage() { return toString(); }

    public String toString() {
        return at() + ": " + description();
    }

    /**
     * Make a simple static error with the given location.
     */
    public static StaticError make(String description, HasAt location) {
        return make(description, location.at());
    }

    /**
     * Make a simple static error with type description "Error" and the given
     * location.
     */
    public static StaticError make(final String description, final String location) {
        return new StaticError() {
            public String description() { return description; }
            public String at() { return location; }
        };
    }

}
