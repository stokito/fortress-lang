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

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;

public class FOrdinaryObject extends FObject {

    final BetterEnv lexicalEnv;
    final BetterEnv selfEnv;
    final FTypeObject ftype;

    public FOrdinaryObject(FTypeObject selfType,
                           BetterEnv lexical_env, BetterEnv self_dot_env) {
        this.lexicalEnv = lexical_env;
        this.selfEnv = self_dot_env;
        this.ftype = selfType;
    }

    /**
     * The environment that you get from "self."
     */
    public BetterEnv getSelfEnv() {
        return selfEnv;
    }

    public BetterEnv getLexicalEnv() {
        return lexicalEnv;
    }

    public FType type() {
        return ftype;
    }

    public boolean seqv(FValue v) {
        if (!(ftype.isValueType())) return false;
        if (!(v instanceof FOrdinaryObject)) return false;
        FOrdinaryObject o = (FOrdinaryObject) v;
        if (ftype != o.type()) return false;
        if (getLexicalEnv() != o.getLexicalEnv()) return false;
        for (Id fld : ftype.getFieldNames()) {
            String name = fld.getText();
            if (!(select(name).seqv(o.select(name)))) return false;
        }
        return true;
    }

}
