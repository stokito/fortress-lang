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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.useful.NI;

public class SymbolicWhereType extends SymbolicType {

    /**
     * @param name
     * @param interior
     */
    public SymbolicWhereType(String name, BetterEnv interior, AbstractNode decl) {
        super(name, interior, Collections.<AbsDeclOrDecl>emptyList(), decl);
        NI.nyi("Where clauses cause a stack overflow error");
        // TODO Auto-generated constructor stub
    }

    public void addSubtype(FType t) {

    }

    List<FType> subtypes;


}
