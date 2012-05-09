/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.useful.NI;

import java.util.Collections;
import java.util.List;

public class SymbolicWhereType extends SymbolicType {

    /**
     * @param name
     * @param interior
     */
    public SymbolicWhereType(String name, Environment interior, AbstractNode decl) {
        super(name, interior, Collections.<Decl>emptyList(), decl);
        NI.nyi("Where clauses cause a stack overflow error");
        // TODO Auto-generated constructor stub
    }

    public void addSubtype(FType t) {

    }
}
