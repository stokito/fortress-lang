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

public class BaseNatType extends NatRef {

    int value;

    public BaseNatType(Span span, IntLiteral value) {
        super(span);
        this.value = value.getVal().intValue();
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forBaseNatType(this);
    }

    BaseNatType(Span span) {
        super(span);
    }

    /**
     *
     *
     * @return Returns thegetVale.
     */
    public int getValue() {
        return value;
    }

    public static BaseNatType make(int i) {
        BaseNatType bnt = new BaseNatType(new Span());
        bnt.value = i;
        return bnt;
    }

    @Override
    public String toString() {
        return ("" + getValue());
    }
}
