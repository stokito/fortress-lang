/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
