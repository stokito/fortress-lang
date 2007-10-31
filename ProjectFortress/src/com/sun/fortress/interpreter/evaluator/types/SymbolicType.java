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

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.useful.NI;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;

abstract public class SymbolicType extends FTypeTrait {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#typeMatch(com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    @Override
    public boolean typeMatch(FValue val) {
        bug(errorMsg("Symbolic type ",this," is being matched to value ",val));
        return false;
    }

    public SymbolicType(String name, BetterEnv interior, List<? extends AbsDeclOrDecl> members, AbstractNode decl) {
        super(name, interior, interior.getAt(), members, decl);
        membersInitialized = true;
    }

    public void addExtend(FType t) {
        if (transitiveExtends != null)
            bug("Extending type added after transitive extends probed.");

        if (extends_ == null)
            extends_ = new ArrayList<FType>();

        extends_.add(t);
    }

    public void addExtends(List<FType> t) {
        if (transitiveExtends != null)
            bug("Extending type added after transitive extends probed.");
        extends_.addAll(t);
    }

}
