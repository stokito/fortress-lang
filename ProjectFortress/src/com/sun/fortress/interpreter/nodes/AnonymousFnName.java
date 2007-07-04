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

import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.NI;

/**
 * Not part of the actual AST; constructed during evaluation of anonymous
 * functions to give them a name.
 */
public class AnonymousFnName extends FnName {

    private static int sequence;

    static synchronized int aname() {
        return ++sequence;
    }

    // Note: private fields are not serialized with AST.
    transient private int serial;

    transient private HasAt at;

    public AnonymousFnName(Span span) {
        super(span);
        serial = aname();
    }

    public AnonymousFnName(HasAt at) {
        super(new Span());
        serial = aname();
        this.at = at;
    }

    public int getSerial() { return serial; }
    public HasAt getAt() { return at; }

   @Override
   public int hashCode() {
        return serial * MagicNumbers.y + span.hashCode();
    }

   @Override
   public boolean equals(Object o) {
        if (o instanceof AnonymousFnName) {
            AnonymousFnName afn = (AnonymousFnName) o;
            return afn.serial == serial;
        }
        return false;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return NI.<T> na();
    }

}
