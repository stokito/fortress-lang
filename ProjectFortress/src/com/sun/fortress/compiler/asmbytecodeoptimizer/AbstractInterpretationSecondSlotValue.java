/*******************************************************************************
    Copyright 2011 Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

public class AbstractInterpretationSecondSlotValue extends AbstractInterpretationValue{

    AbstractInterpretationValue originalValue;

    AbstractInterpretationSecondSlotValue(Insn definition, String type, AbstractInterpretationValue v) {
        super(definition, type);
        this.originalValue = v;
    }

    AbstractInterpretationSecondSlotValue(AbstractInterpretationValue v) {
        super(v.getDefinition(), v.getType());
        this.originalValue = v;
    }


    public String idString() {
        if (originalValue != null)
            return "AISS[" + originalValue.getValueNumber() + "]";
        else
            throw new RuntimeException("Shouldn't happen");
    }
}