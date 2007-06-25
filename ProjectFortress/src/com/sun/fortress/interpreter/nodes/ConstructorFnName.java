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

import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.NI;

/**
 * Not part of the actual AST; constructed during evaluation of anonymous
 * functions to give them a name.
 */
public class ConstructorFnName extends FnName {

    private static int sequence;

    static synchronized int aname() {
        return ++sequence;
    }

    transient private int serial;

    DefOrDecl def;

    public ConstructorFnName(DefOrDecl def) {
        super(def.getSpan());
        serial = aname();
        this.def = def;
    }

    @Override
    protected int mandatoryHashCode() {
        return serial * MagicNumbers.y + span.hashCode();
    }

    @Override
    protected boolean mandatoryEquals(Object o) {
        if (o instanceof ConstructorFnName) {
            ConstructorFnName afn = (ConstructorFnName) o;
            return afn.serial == serial;
        }
        return false;
    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        return def.stringName() + "#" + serial;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return NI.<T> na();
    }

}
