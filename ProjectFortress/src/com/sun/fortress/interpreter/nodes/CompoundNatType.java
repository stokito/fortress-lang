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

public abstract class CompoundNatType extends NatRef {

    public CompoundNatType(Span s2) {
        super(s2);
    }

    List<StaticArg> value;

    public CompoundNatType(Span span, List<StaticArg> value) {
        super(span);
        this.value = value;
    }

    /**
     * @return Returns the value.
     */
    public List<StaticArg> getValue() {
        return value;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.TypeRef#subtypeCompareTo(com.sun.fortress.interpreter.nodes.TypeRef)
     */
    @Override
    int subtypeCompareTo(TypeRef o) {
        // TODO Auto-generated method stub
        return StaticArg.typeargListComparer.compare(value,
                ((CompoundNatType) o).value);
    }
}
