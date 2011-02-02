/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Decl;

import java.util.Collections;

public class SymbolicOprType extends SymbolicType {

    public SymbolicOprType(String name, Environment interior, AbstractNode decl) {
        super(name, interior, Collections.<Decl>emptyList(), decl);
        isSymbolic = true;
    }

}
