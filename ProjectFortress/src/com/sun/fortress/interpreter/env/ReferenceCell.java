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

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;

import dstm2.atomic;
import dstm2.factory.Factory;


/**
 * What the interpreter stores mutable things (fields, variables)
 * in.  It will eventually acquire transactional semantics.
 */
public class ReferenceCell extends IndirectionCell {
    private FType theType;
    protected FNode node;
    static Factory<FNode> factory = FortressTaskRunner.makeFactory(FNode.class);


    ReferenceCell(FType t, FValue v) {
        super();
        theType = t;
	node = factory.create();
        node.setValue(v);
    }

    ReferenceCell(FType t) {
        super();
        theType = t;
        node = factory.create();
    }

    ReferenceCell() {
        super();
        node = factory.create();
    }

    public void assignValue(FValue f2) {
        theValue = f2;
        node.setValue(f2);
    }

    public FType getType() {
        return theType;
    }


    @atomic public interface FNode {
        FValue getValue();
        void setValue(FValue value);
    }

    public void storeValue(FValue f2) {
        if (node.getValue() != null)
            throw new InterpreterError("Internal error, second store of indirection cell");
        node.setValue(f2);
    }

    public void storeType(FType f2) {
        if (theType != null)
            throw new InterpreterError("Internal error, second store of type");
        theType = f2;
    }

    public boolean isInitialized() {
        return node.getValue() != null;
    }

    public FValue getValue() {
        FValue theValue = node.getValue();
        if (theValue == null) {
            throw new ProgramError("Attempt to read uninitialized variable");
        }
        return theValue;
    }

}
