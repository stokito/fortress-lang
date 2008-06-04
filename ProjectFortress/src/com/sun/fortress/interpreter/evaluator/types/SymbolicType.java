/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.values.OverloadedFunction.exclDump;
import static com.sun.fortress.interpreter.evaluator.values.OverloadedFunction.exclDumpln;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;

abstract public class SymbolicType extends FTypeTrait {

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.types.FType#typeMatch(com.sun.fortress.interpreter.evaluator.values.FValue)
     */
    @Override
    public boolean typeMatch(FValue val) {
        bug(errorMsg("Symbolic type ",this," is being matched to value ",val));
        return false;
    }

    public SymbolicType(String name, Environment interior, List<? extends AbsDeclOrDecl> members, AbstractNode decl) {
        super(name, interior, interior.getAt(), members, decl);
        membersInitialized = true;
    }

    public String toString() {
        return getName()+"@"+getAt().at();
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

    protected boolean excludesOtherInner(FType other) {
        if (other instanceof SymbolicType) {
            for (FType t1 : getExtends()) {
                for (FType t2 : other.getExtends()) {
                    exclDump("Checking exclusion of upper bounds; ");
                    if (t1.excludesOther(t2)) {
                        exclDumpln("upper bounds ",t1," and ",t2," exclude.");
                        addExclude(other);
                        return true;
                    }
                }
            }
        } else {
            for (FType t1 : getExtends()) {
                exclDump("Checking exclusion of upper bound; ");
                if (t1.excludesOther(other)) {
                    exclDumpln("upper bound ",t1," excludes.");
                    addExclude(other);
                    return true;
                }
            }
        }
        exclDumpln("No upper bound exclusion.");
        return false;
    }
}
